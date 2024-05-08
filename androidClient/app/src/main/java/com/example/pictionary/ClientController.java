package com.example.pictionary;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientController {
    public enum MessageType {
        START_GAME, MANAGER, USERS, EXIT_OK, TIME, STATISTICS, CORRECT_GUESS, DRAW, GUESS, CONTINUE,
        WINNER, WRONG_GUESS, ALONE, WORD_TO_GUESS, CLUE, DRAWING_BYTES
    }
    public final static int ID_DOES_NOT_EXIST = -1;
    public final static int PORT = 6969;
    public final static String SERVER_IP = "64.226.121.246";
    public final static int QUALITY = 40;
    private Socket socket;
    private BufferedReader input;
    private OutputStream output;
    private static ClientController instance = null;

    public ClientController() {
        instance = this;
    }

    public static ClientController getInstance() {
        if (instance == null) {
            instance = new ClientController();
        }
        return instance;
    }

    public void sendBitmap(Bitmap bitmap) {
        CompletableFuture.runAsync(() -> {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, stream);
                byte[] byteArray = stream.toByteArray();
                String encodedByteArray = Base64.getEncoder().encodeToString(byteArray);

                sendMessage(encodedByteArray.length() + "dataBytes:" + encodedByteArray);
                output.flush();
            } catch (Exception exception) {
                Log.e("SocketClient", "Error whilst sending message", exception);
            }
        });
    }

    public void sendMessage(String message) {
        CompletableFuture.runAsync(() -> {
            try {
                output.write(message.getBytes(StandardCharsets.UTF_8));
                output.flush();
            } catch (Exception exception) {
                Log.e("SocketClient", "Error whilst sending message", exception);
                throw new RuntimeException(exception);
            }
        });
    }

    public CompletableFuture<String> receiveMessage() {
        CompletableFuture<String> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                String receivedMessage = input.readLine();
                future.complete(receivedMessage);
            } catch (Exception exception) {
                Log.e("SocketClient", "Error whilst receiving message", exception);
                future.completeExceptionally(exception);
            }

        });
        return future;
    }

    public byte[] transformToBitmap(String encodedBitmap){
        try {
            return Base64.getDecoder().decode(encodedBitmap);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public String receiveAll(String firstReceive) throws ExecutionException, InterruptedException {
        String[] tokenizedMessage = firstReceive.split("dataBytes:");
        int length = Integer.parseInt(tokenizedMessage[0]);
        StringBuilder data = new StringBuilder(tokenizedMessage[1]);

        while (data.length() < length) {
            data.append(receiveMessage().get());
        }
        return data.toString();
    }

    public void connectSocket(String username) {
        if (socket != null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                socket = new Socket(SERVER_IP, PORT);
                output = socket.getOutputStream();
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (Exception exception) {
                Log.e("SocketClient", "Error whilst opening socket", exception);
            }
        }).thenAccept(unused -> sendMessage(username));
    }

    public void closeSocket() {
        CompletableFuture.runAsync(() -> {
            try {
                if (input != null) {
                    input.close();
                }
                if (output != null) {
                    output.close();
                }
                if (socket != null) {
                    socket.close();
                }
                instance = null;
            } catch (Exception exception) {
                Log.e("SocketClient", "Error whilst closing socket", exception);
            }
        });
    }

    public void startGame() {
        sendMessage("start");
    }

    public void exitRoom() {
        sendMessage("exit");
    }

    public void updateUserLoggedIn(String username) {
        connectSocket(username);
    }

    public void updateUserLoggedOut() {
        closeSocket();
    }

    public void createPrivateRoom(JoinRoomCallback callback) {
        sendMessage("new room");
        receiveMessage().thenAccept(response -> {
            int gameId = Integer.parseInt(response);
            callback.onRoomJoined(gameId);
        });
    }

    public void joinPrivateRoom(int gameId, JoinRoomCallback callback) {
        sendMessage("find room " + gameId);
        receiveMessage().thenAccept(response -> {
            if (response.equals("no")) {
                callback.onRoomJoined(ID_DOES_NOT_EXIST);
            } else if (response.equals("yes")) {
                callback.onRoomJoined(gameId);
            }
        });
    }

    public void ackStartGame() {
        sendMessage("start ok");
    }

    public void ackManager() {
        sendMessage("manager ok");
    }

    public void submitGuess(String guess) {
        sendMessage(guess);
    }

    public boolean processSingleResponse(Handler receiveMessageHandler) throws ExecutionException, InterruptedException {
        AtomicBoolean stopListening = new AtomicBoolean(false);
        receiveMessage().thenAccept(response -> {
            if (response.equals("start")) {
                ackStartGame();
                receiveMessageHandler.sendEmptyMessage(MessageType.START_GAME.ordinal());
                stopListening.set(true);
            }
            else if (response.equals("manager")) {
                ackManager();
                receiveMessageHandler.sendEmptyMessage(MessageType.MANAGER.ordinal());
            }
            else if (response.startsWith("users")) {
                Message message = new Message();
                message.what = MessageType.USERS.ordinal();
                message.obj = response.split("users: ")[1];
                receiveMessageHandler.sendMessage(message);
            }
            else if (response.equals("exit ok")) {
                receiveMessageHandler.sendEmptyMessage(MessageType.EXIT_OK.ordinal());
                stopListening.set(true);
            }
            else if (response.startsWith("time")) {
                Message message = new Message();
                message.what = MessageType.TIME.ordinal();
                message.obj = Long.parseLong(response.split("time: ")[1]);
                receiveMessageHandler.sendMessage(message);
            }
            else if(response.startsWith("statistics")) {
                Message message = new Message();
                message.what = MessageType.STATISTICS.ordinal();
                message.obj = response.split("statistics: ")[1];
                receiveMessageHandler.sendMessage(message);
            }
        }).get();
        return stopListening.get();
    }

    public interface JoinRoomCallback {
        void onRoomJoined(int gameId);
    }
}
