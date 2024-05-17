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

/**
 * This class handles the client-side communication with the server.
 */
public class ClientController {
    // Enum for the types of messages that can be received
    public enum MessageType {
        START_GAME, MANAGER, USERS, EXIT_OK, TIME, STATISTICS, CORRECT_GUESS, DRAW, GUESS, CONTINUE,
        WRONG_GUESS, CLUE, DRAWING_BYTES, GO_TO_WAITING_ROOM
    }

    /** constants */
    public final static int ID_DOES_NOT_EXIST = -1;
    public final static int ID_ALREADY_IN_GAME = -2;
    public final static int PORT = 6969;
    public final static String SERVER_IP = "64.226.121.246";
    // quality of the image sent to the server
    public final static int QUALITY = 40;

    /** socket and IO variables */
    // socket to connect to the server
    private Socket socket;
    // input stream
    private BufferedReader input;
    // output stream
    private OutputStream output;

    // singleton instance of the ClientController
    private static ClientController instance = null;

    /**
     * Private constructor for the ClientController class.
     */
    private ClientController() {
    }

    /**
     * Returns the instance of the ClientController.
     * If the instance is null, a new instance is created.
     * @return The instance of the ClientController.
     */
    public static ClientController getInstance() {
        if (instance == null) {
            instance = new ClientController();
        }
        return instance;
    }

    /**
     * Sends a bitmap image to the server.
     * The bitmap is compressed and encoded as a Base64 string before being sent.
     * @param bitmap The bitmap to be sent.
     */
    public void sendBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // compress the bitmap and encode it as a Base64 string
        bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, stream);
        byte[] byteArray = stream.toByteArray();
        String encodedByteArray = Base64.getEncoder().encodeToString(byteArray);

        // send the length of the data and the data itself
        sendMessage(encodedByteArray.length() + "dataBytes:" + encodedByteArray);
    }

    /**
     * Sends a message to the server.
     * @param message The message to be sent.
     */
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

    /**
     * Receives a message from the server async.
     * @return A CompletableFuture that will be completed with the received message.
     */
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


    /**
     * Transforms a Base64 encoded string into a byte array.
     * @param encodedBitmap The Base64 encoded string.
     * @return The byte array.
     */
    private byte[] transformToBitmap(String encodedBitmap){
        try {
            return Base64.getDecoder().decode(encodedBitmap);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Gets the byte array of a bitmap image from the server.
     * @param initialData The initial data received from the server.
     * @return The byte array of the bitmap image.
     */
    public byte[] getBitmapBytes(String initialData){
        String encodedBitmap = receiveAll(initialData);
        return transformToBitmap(encodedBitmap);
    }

    /**
     * Receives all data from the server until the specified length is reached.
     * @param firstReceive The first part of the data received.
     * @return The complete data as a string.
     */
    private String receiveAll(String firstReceive){
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

    /**
     * Connects the socket to the server.
     * If the socket is already connected, this method does nothing.
     * @param username The username to be sent to the server after connecting.
     */
    private void connectSocket(String username) {
        if (socket != null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // open the socket and IO streams
                socket = new Socket(SERVER_IP, PORT);
                output = socket.getOutputStream();
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (Exception exception) {
                Log.e("SocketClient", "Error whilst opening socket", exception);
            }
            // send the client's username to the server
        }).thenAccept(unused -> sendMessage(username));
    }

    /**
     * Closes the socket connection to the server.
     */
    private void closeSocket() {
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
    }

    /**
     * Sends a message to the server to start the game.
     */
    public void startGame() {
        sendMessage("start");
    }

    /**
     * Sends a message to the server to exit the room.
     */
    public void exitRoom() {
        sendMessage("exit");
    }

    /**
     * Updates the server with the logged in user's username.
     * @param username The username of the logged in user.
     */
    public void updateUserLoggedIn(String username) {
        connectSocket(username);
    }

    /**
     * Updates the server when a user logs out.
     */
    public void updateUserLoggedOut() {
        closeSocket();
    }

    /**
     * Creates a new private room and notifies the server.
     * @param callback The callback to be executed after the room is created.
     */
    public void createPrivateRoom(JoinRoomCallback callback) {
        sendMessage("new room");
        receiveMessage().thenAccept(response -> {
            int gameId = Integer.parseInt(response);
            callback.onRoomJoined(gameId, true);
        });
    }

    /**
     * Joins a private room and notifies the server.
     * @param gameId The ID of the game to join.
     * @param callback The callback to be executed after joining the room.
     */
    public void joinPrivateRoom(int gameId, JoinRoomCallback callback) {
        sendMessage("find room " + gameId);
        receiveMessage().thenAccept(response -> {
            switch (response) {
                case "no":
                    callback.onRoomJoined(ID_DOES_NOT_EXIST, false);
                    break;
                case "yes":
                    callback.onRoomJoined(gameId, false);
                    break;
                case "ingame":
                    callback.onRoomJoined(ID_ALREADY_IN_GAME, false);
                    break;
            }
        });
    }

    /**
     * Acknowledges the start game message from the server.
     */
    public void ackStartGame() {
        sendMessage("start ok");
    }

    /**
     * Acknowledges the manager message from the server.
     */
    public void ackManager() {
        sendMessage("manager ok");
    }

    /**
     * Acknowledges the correct guess message from the server.
     */
    public void ackAlone() {
        sendMessage("alone ok");
    }

    /**
     * Acknowledges the waiting message from the server.
     */
    public void ackWaiting() {
        sendMessage("waiting ok");
    }

    /**
     * Acknowledges the correct guess message from the server.
     */
    public void ackContinue() {
        sendMessage("continue ok");
    }

    /**
     * Submits a guess to the server.
     * @param guess The guess to be submitted.
     */
    public void submitGuess(String guess) {
        sendMessage(guess);
    }

    /**
     * Processes a single response from the server.
     * @param receiveMessageHandler The handler for the received message.
     * @return A boolean indicating whether to continue listening for messages.
     * @throws ExecutionException If an error occurs while waiting for the CompletableFuture to complete.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
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

    /**
     * Interface for the join room callback.
     */
    public interface JoinRoomCallback {
        void onRoomJoined(int gameId, boolean isManager);
    }
}
