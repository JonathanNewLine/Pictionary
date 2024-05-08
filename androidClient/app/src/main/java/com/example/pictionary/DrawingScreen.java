package com.example.pictionary;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.graphics.BitmapFactory;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DrawingScreen extends BaseGameActivity {
    // constants
    public final static int ROUND_TIME = 60;
    public final static int NUM_OF_ROUNDS = 3;
    public static final int DOUBLE_BACK_PRESS_INTERVAL = 1000;

    // textViews
    private TextView hint;
    private TextView timeLeftTextView;
    private TextView currentDrawing;
    private TextView wordWas;
    private TextView currScore;

    // buttons
    private ImageView clearBtn;
    private ImageView undoBtn;
    private ImageView colorPalette;
    private ImageView submitGuess;

    // editTexts
    private EditText guesserInputBox;
    
    // drawing screen
    private DrawOnView paintClass;
    private FrameLayout drawing_screen;
    private ImageView displayedDrawingForGuesser;
    
    // other views
    private FrameLayout coolDownScreen;
    private com.skydoves.colorpickerview.ColorPickerView colorPickerView;
    private LinearLayout allToolbars;
    
    // countdown
    private Runnable countDownUpdater;
    private final Handler countDownHandler = new Handler();

    // other
    private int timeLeft = ROUND_TIME;
    private int currRoundNum = 1;
    private int numOfPeopleInRoom;
    private int numOfGames;
    private String correctGuess;
    private String lastUserSideBarFormat;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing_screen);
        setButtonListeners();
        setUpPaintClass();
        timeLeftTextView.setText(ROUND_TIME + "seconds");
    }

    @Override
    protected void onStart() {
        super.onStart();
        startMusic();
        getInitialPlayingMode();
    }

    @Override
    public void listenForServer() {
    }

    private void getInitialPlayingMode() {
        clientController.receiveMessage().thenAccept(message -> runOnUiThread(() -> {
           if (message.equals("draw")) {
               getAppropriateInterface(true, DatabaseController.getCachedUser().getUsername());
               startCountdown();
               listenForServerDrawer();
           }
           else if (message.startsWith("guess")) {
               String drawerName = message.split("guess ")[1];
               getAppropriateInterface(false, drawerName);
               startCountdown();
               listenForServerNoneDrawer();
           }
           else if (message.startsWith("users")) {
               lastUserSideBarFormat = message;
               numOfPeopleInRoom = updateUsersSideBar(message);
               getInitialPlayingMode();
           }
           else {
               getInitialPlayingMode();
           }
        }));
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void listenForServerDrawer() {
        clientController.receiveMessage().thenAccept(message -> runOnUiThread(() -> {
            if (message.equals("exit ok")) {
                return;
            }
            else if (message.equals("alone")) {
                clientController.sendMessage("alone ok");
            }
            else if (message.equals("manager")) {
                isManager = true;
            }
            else if (message.startsWith("users")) {
                lastUserSideBarFormat = message;
                numOfPeopleInRoom = updateUsersSideBar(message);
                currScore.setText("Your score: " + getPointsByUsername(message, DatabaseController.getCachedUser().getUsername()));
            }
            else if (message.startsWith("guess")) {
                String drawerName = message.split("guess ")[1];
                getAppropriateInterface(false, drawerName);
                listenForServerNoneDrawer();
                return;
            }
            else if (message.startsWith("continue")) {
                String roundNumber = "";
                numOfGames++;
                if (numOfGames >= numOfPeopleInRoom) {
                    numOfGames = 0;
                    currRoundNum++;
                    if (currRoundNum <= NUM_OF_ROUNDS) {
                        roundNumber = String.format("Round %d/%d\n", currRoundNum, NUM_OF_ROUNDS);
                    }
                }
                coolDownScreen.setVisibility(View.VISIBLE);
                SoundEffects.playSound(SoundEffects.next);
                disableAllViews();
                countDownHandler.removeCallbacks(countDownUpdater);
                correctGuess = message.split("continue ")[1];
                hint.setText(correctGuess);
                wordWas.setText(roundNumber + "The word was: " + correctGuess);
                CompletableFuture.runAsync(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                }).thenAccept(unused -> runOnUiThread(() -> {
                    coolDownScreen.setVisibility(View.GONE);
                    paintClass.clearBoard();
                    enableAllViews();
                    resetClock();
                    startCountdown();
                    clientController.sendMessage("continue ok");
                }));
            }
            else if (message.startsWith("winner")) {
                backToWaitingRoom(message);
                return;
            }
            else if (message.contains("word")){
                String word = message.split("word: ")[1];
                hint.setText(word);
                CompletableFuture.runAsync(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                        runOnUiThread(() -> wordWas.setText("The word was: " + word));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                });
            }
            listenForServerDrawer();
        }));
    }

    private void backToWaitingRoom(String data) {
        clientController.receiveMessage().thenAccept(message -> {
            if (message.equals("waiting")) {
                clientController.sendMessage("waiting ok");
                createWaitingRoomIntent(data);
                return;
            }
            backToWaitingRoom(data);
        });
    }

    private void createWaitingRoomIntent(String data) {
        String[] winnerData = data.split("winner: ")[1].split(",");
        Intent intent = new Intent(DrawingScreen.this, WaitingRoom.class);
        intent.putExtra("isManager", isManager);
        intent.putExtra("gameId", gameId);
        intent.putExtra("winnerData", winnerData);
        intent.putExtra("selfPoints", getPointsByUsername(lastUserSideBarFormat, DatabaseController.getCachedUser().getUsername()));
        startActivity(intent);
        finish();
    }

    private static int getPointsByUsername(String jsonString, String username) {
        try {
            JSONArray jsonArray = new JSONArray(jsonString.split("users: ")[1]);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("username").equals(username)) {
                    return jsonObject.getInt("points");
                }
            }
            return -1;

        } catch (Exception e) {
            return -1;
        }
    }


    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void listenForServerNoneDrawer() {
        clientController.receiveMessage().thenAccept(message -> runOnUiThread(() -> {
            if (message.equals("exit ok")) {
                return;
            }
            else if (message.equals("manager")) {
                isManager = true;
                Toast.makeText(DrawingScreen.this, "you're the manager", Toast.LENGTH_SHORT).show();
            }
            else if (message.equals("correct")) {
                SoundEffects.playSound(SoundEffects.correct);
                updateCorrectGuess();
            }
            else if (message.equals("wrong")) {
                SoundEffects.playSound(SoundEffects.wrong);
            }
            else if (message.startsWith("users")) {
                lastUserSideBarFormat = message;
                updateUsersSideBar(message);
                currScore.setText("Your score: " + getPointsByUsername(message, DatabaseController.getCachedUser().getUsername()));
            }
            else if (message.startsWith("continue")) {
                String roundNumber = "";
                numOfGames++;
                if (numOfGames >= numOfPeopleInRoom) {
                    numOfGames = 0;
                    currRoundNum++;
                    if (currRoundNum <= NUM_OF_ROUNDS) {
                        roundNumber = String.format("Round %d/%d\n", currRoundNum, NUM_OF_ROUNDS);
                    }
                }
                coolDownScreen.setVisibility(View.VISIBLE);
                SoundEffects.playSound(SoundEffects.next);
                disableAllViews();
                countDownHandler.removeCallbacks(countDownUpdater);
                correctGuess = message.split("continue ")[1];
                hint.setText(correctGuess);
                wordWas.setText(roundNumber + "The word was: " + correctGuess);
                CompletableFuture.runAsync(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                }).thenAccept(unused -> runOnUiThread(() -> {
                    coolDownScreen.setVisibility(View.GONE);
                    paintClass.clearBoard();
                    enableAllViews();
                    resetClock();
                    startCountdown();
                    clientController.sendMessage("continue ok");
                }));
            }
            else if (message.startsWith("clue")) {
                hint.setText(message.split("clue: ")[1]);
            }
            else if (message.equals("draw")) {
                paintClass.clear();
                getAppropriateInterface(true, DatabaseController.getCachedUser().getUsername());
                listenForServerDrawer();
                return;
            }
            else if (message.startsWith("guess")) {
                String drawerName = message.split("guess ")[1];
                getAppropriateInterface(false, drawerName);
            }
            else if (message.startsWith("winner")) {
                backToWaitingRoom(message);
                return;
            }
            else if (message.equals("alone")) {
                clientController.sendMessage("alone ok");
            }
            else if (message.contains("dataBytes")){
                String encodedBitmap;
                try {
                    encodedBitmap = clientController.receiveAll(message);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                byte[] bitmapBytes = clientController.transformToBitmap(encodedBitmap);
                runOnUiThread(() -> {
                    if (bitmapBytes != null) {
                        putBitmapOnImage(bitmapBytes);
                    }
                });
            }
            listenForServerNoneDrawer();
        }));
    }

    @SuppressLint("SetTextI18n")
    private void updateCorrectGuess() {
        allToolbars.setBackgroundColor(Color.parseColor("#B5189501"));
        guesserInputBox.setEnabled(false);
        hint.setText(correctGuess);
    }

    private void putBitmapOnImage(byte[] bitmapBytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        displayedDrawingForGuesser.setImageBitmap(bitmap);
    }

    private void pickColor() {
        colorPickerView.setColorListener((ColorEnvelopeListener) (envelope, fromUser) -> {
            colorPalette.setColorFilter(envelope.getColor());
            paintClass.setColor(envelope.getColor());
            colorPickerView.setVisibility(View.GONE);
        });
        if (colorPickerView.getVisibility() == View.GONE) {
            colorPickerView.setVisibility(View.VISIBLE);
        }
        else if (colorPickerView.getVisibility() == View.VISIBLE) {
            colorPickerView.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetTextI18n")
    private void getAppropriateInterface(boolean isDrawer, String drawerName) {
        View drawerToolBar = findViewById(R.id.drawer_tool_bar);

        allToolbars.setBackgroundColor(Color.parseColor("#83AABBCC"));

        if (isDrawer) {
            colorPalette.setColorFilter(Color.BLACK);
            paintClass.setColor(Color.BLACK);
            submitGuess.setVisibility(View.GONE);
            displayedDrawingForGuesser.setVisibility(View.GONE);
            drawerToolBar.setVisibility(View.VISIBLE);
            guesserInputBox.setVisibility(View.GONE);
            paintClass.setVisibility(View.VISIBLE);
            currentDrawing.setText("Currently drawing: " + drawerName);
        }
        else {
            submitGuess.setVisibility(View.VISIBLE);
            displayedDrawingForGuesser.setVisibility(View.VISIBLE);
            drawerToolBar.setVisibility(View.GONE);
            guesserInputBox.setVisibility(View.VISIBLE);
            paintClass.setVisibility(View.GONE);
            currentDrawing.setText("Currently drawing: " + drawerName);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent musicIntent = new Intent(this, MusicService.class);
        musicIntent.addCategory("pause");
        startService(musicIntent);
    }

    @SuppressLint("SetTextI18n")
    private void resetClock() {
        timeLeftTextView.setText(ROUND_TIME+ " seconds");
        timeLeft = ROUND_TIME;
    }

    @SuppressLint("SetTextI18n")
    private void startCountdown() {
        final TextView timeLeftTextView = findViewById(R.id.time_left);
        countDownUpdater = new Runnable() {
            @Override
            public void run() {
                if (timeLeft > 0) {
                    timeLeftTextView.setText(timeLeft + " seconds");
                    timeLeft--;
                    countDownHandler.postDelayed(this, 1000);
                } else {
                    countDownHandler.removeCallbacks(this);
                    timeLeftTextView.setText("0 seconds");
                }
            }
        };
        countDownHandler.post(countDownUpdater);
    }
    
    private void setButtonListeners() {
        drawing_screen = findViewById(R.id.drawing_screen);
        submitGuess = findViewById(R.id.submit_guess);
        displayedDrawingForGuesser = findViewById(R.id.displayed_drawing_for_guesser);
        clearBtn = findViewById(R.id.clear);
        undoBtn = findViewById(R.id.undo);
        colorPalette = findViewById(R.id.color_palette);
        guesserInputBox = findViewById(R.id.player_guess);
        colorPickerView = findViewById(R.id.colorPickerView);
        coolDownScreen = findViewById(R.id.cooldown_screen);
        hint = findViewById(R.id.hint);
        wordWas = findViewById(R.id.word_was);
        allToolbars = findViewById(R.id.global_toolbar);
        currentDrawing = findViewById(R.id.current_drawing);
        currScore = findViewById(R.id.current_score);
        timeLeftTextView = findViewById(R.id.time_left);

        clearBtn.setOnClickListener(v -> paintClass.clear());
        undoBtn.setOnClickListener(v -> paintClass.undo());
        colorPalette.setOnClickListener(v -> pickColor());
        submitGuess.setOnClickListener(v -> submitGuess());
    }
    
    private void submitGuess() {
        correctGuess = guesserInputBox.getText().toString();
        clientController.submitGuess(correctGuess);
        guesserInputBox.setText("");
    }

    private void disableAllViews() {
        guesserInputBox.setEnabled(false);
        undoBtn.setEnabled(false);
        colorPalette.setEnabled(false);
        clearBtn.setEnabled(false);
        findViewById(R.id.open_users_side_bar).setEnabled(false);
        submitGuess.setEnabled(false);
        findViewById(R.id.exit).setEnabled(false);
    }

    private void enableAllViews() {
        guesserInputBox.setEnabled(true);
        submitGuess.setEnabled(true);
        undoBtn.setEnabled(true);
        colorPalette.setEnabled(true);
        clearBtn.setEnabled(true);
        findViewById(R.id.open_users_side_bar).setEnabled(true);
        findViewById(R.id.exit).setEnabled(true);
    }
    
    private void setUpPaintClass() {
        paintClass = new DrawOnView(this);
        drawing_screen.addView(paintClass);
    }
    
    private void startMusic() {
        Intent musicIntent = new Intent(this, MusicService.class);
        musicIntent.addCategory("start");
        startService(musicIntent);
    }
}