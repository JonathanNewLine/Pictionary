package com.example.pictionary;

import android.graphics.Bitmap;
import android.os.Bundle;
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
        WRONG_GUESS, CLUE, DRAWING_BYTES, GO_TO_WAITING_ROOM
    }
    public final static int ID_DOES_NOT_EXIST = -1;
    public final static int PORT = 6969;
    public final static String SERVER_IP = "64.226.121.246";
    public final static int QUALITY = 40;
    private Socket socket;
    private BufferedReader input;
    private OutputStream output;
    private static ClientController instance = null;

    private ClientController() {
    }

    public static ClientController getInstance() {
        if (instance == null) {
            instance = new ClientController();
        }
        return instance;
    }

    public void sendBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
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
        System.out.println("Sending message: " + message);
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

    public String receiveAll(String firstReceive){
        String[] tokenizedMessage = firstReceive.split("dataBytes:");
        int length = Integer.parseInt(tokenizedMessage[0]);
        StringBuilder data = new StringBuilder(tokenizedMessage[1]);

        try {
            while (data.length() < length) {
                data.append(receiveMessage().get());
            }
        } catch (Exception ignored) {
            return null;
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

    public CompletableFuture<Void> closeSocket() {
        return CompletableFuture.runAsync(() -> {
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
        try {
            closeSocket().get();
        } catch (ExecutionException|InterruptedException ignored) {
        }
    }

    public void createPrivateRoom(JoinRoomCallback callback) {
        sendMessage("new room");
        receiveMessage().thenAccept(response -> {
            int gameId = Integer.parseInt(response);
            callback.onRoomJoined(gameId, true);
        });
    }

    public void joinPrivateRoom(int gameId, JoinRoomCallback callback) {
        sendMessage("find room " + gameId);
        receiveMessage().thenAccept(response -> {
            if (response.equals("no")) {
                callback.onRoomJoined(ID_DOES_NOT_EXIST, false);
            } else if (response.equals("yes")) {
                callback.onRoomJoined(gameId, false);
            }
        });
    }

    public void ackStartGame() {
        sendMessage("start ok");
    }

    public void ackManager() {
        sendMessage("manager ok");
    }

    public void ackAlone() {
        sendMessage("alone ok");
    }

    public void ackWaiting() {
        sendMessage("waiting ok");
    }

    public void ackConfirm() {
        sendMessage("continue ok");
    }

    public void submitGuess(String guess) {
        sendMessage(guess);
    }

    public boolean processSingleResponse(Handler receiveMessageHandler) throws ExecutionException, InterruptedException {
        AtomicBoolean continueListening = new AtomicBoolean(true);
        receiveMessage().thenAccept(response -> {
            Message message = new Message();
            if (response.equals("start")) {
                ackStartGame();
                receiveMessageHandler.sendEmptyMessage(MessageType.START_GAME.ordinal());
                continueListening.set(false);
            }
            else if (response.equals("manager")) {
                ackManager();
                receiveMessageHandler.sendEmptyMessage(MessageType.MANAGER.ordinal());
            }
            else if (response.startsWith("users")) {
                message.what = MessageType.USERS.ordinal();
                message.obj = response.split("users: ")[1];
                receiveMessageHandler.sendMessage(message);
            }
            else if (response.equals("exit ok")) {
                receiveMessageHandler.sendEmptyMessage(MessageType.EXIT_OK.ordinal());
                continueListening.set(false);
            }
            else if (response.startsWith("time")) {
                message.what = MessageType.TIME.ordinal();
                message.obj = Long.parseLong(response.split("time: ")[1]);
                receiveMessageHandler.sendMessage(message);
            }
            else if(response.startsWith("statistics")) {
                message.what = MessageType.STATISTICS.ordinal();
                message.obj = response.split("statistics: ")[1];
                receiveMessageHandler.sendMessage(message);
            }
            else if(response.equals("correct")) {
                receiveMessageHandler.sendEmptyMessage(MessageType.CORRECT_GUESS.ordinal());
            }
            else if(response.startsWith("draw")) {
                message.what = MessageType.DRAW.ordinal();
                message.obj = response.split("draw: ")[1];
                receiveMessageHandler.sendMessage(message);
            }
            else if(response.startsWith("guess")) {
                String[] data = response.split("guess: ")[1].split(",");
                String drawerName = data[0];
                String clue = data[1];
                message.what = MessageType.GUESS.ordinal();

                Bundle bundle = new Bundle();
                bundle.putString("drawerName", drawerName);
                bundle.putString("clue", clue);
                message.setData(bundle);

                receiveMessageHandler.sendMessage(message);
            }
            else if(response.startsWith("continue")) {
                message.what = MessageType.CONTINUE.ordinal();
                message.obj = response.split("continue: ")[1];
                receiveMessageHandler.sendMessage(message);
            }
            else if(response.startsWith("wrong")) {
                receiveMessageHandler.sendEmptyMessage(MessageType.WRONG_GUESS.ordinal());
            }
            else if(response.equals("alone")) {
                ackAlone();
            }
            else if(response.contains("dataBytes")) {
                message.what = MessageType.DRAWING_BYTES.ordinal();
                message.obj = response;
                receiveMessageHandler.sendMessage(message);
            }
            else if(response.startsWith("waiting")) {
                ackWaiting();

                String[] data = response.split("waiting: ")[1].split(",");
                String winnerName = data[0];
                String winnerPoints = data[1];
                message.what = MessageType.GO_TO_WAITING_ROOM.ordinal();

                Bundle bundle = new Bundle();
                bundle.putString("winnerName", winnerName);
                bundle.putString("winnerPoints", winnerPoints);
                message.setData(bundle);

                receiveMessageHandler.sendMessage(message);
                continueListening.set(false);
            }
        }).get();
        return continueListening.get();
    }

    public interface JoinRoomCallback {
        void onRoomJoined(int gameId, boolean isManager);
    }
}
