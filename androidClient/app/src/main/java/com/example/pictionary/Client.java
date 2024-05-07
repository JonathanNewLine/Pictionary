package com.example.pictionary;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Client {
    public final static int PORT = 6969;
    public final static String SERVER_IP = "64.226.121.246";
    public final static int QUALITY = 40;
    private Socket socket;
    private BufferedReader input;
    private OutputStream output;
    private static Client instance = null;

    public Client() {
        instance = this;
    }

    public static Client getInstance() {
        if (instance == null) {
            instance = new Client();
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
}
