package com.example.pictionary;

import androidx.activity.OnBackPressedCallback;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.graphics.BitmapFactory;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DrawingScreen extends BaseGameActivity {
    public final static int ROUND_TIME = 60;
    public final static int NUM_OF_ROUNDS = 3;
    private DrawOnView paintClass;
    private ImageView colorPalette;
    private Client client;
    private FrameLayout drawing_screen;
    private ImageView guesserImageView;
    private ImageView clearBtn;
    private ImageView undoBtn;
    private FrameLayout mainScreen;
    private ImageView submitGuess;
    private TextView wordWas;
    private EditText guesserToolbar;
    private TextView hint;
    private int backButtonCount = 0;
    private boolean isManager;
    private int gameId;
    private String lastUserSideBarFormat;
    private LinearLayout allToolbars;
    private TextView currentDrawing;
    private TextView currScore;
    private Intent musicIntent;
    private ListView usersListView;
    public static final int DOUBLE_BACK_PRESS_INTERVAL = 1000;
    private String correctGuess;
    private UserAdapter userAdapter;
    private int timeLeft = ROUND_TIME;
    private Runnable updateCountdown;
    private final Handler handler = new Handler();
    private TextView timeLeftTextView;
    private int numOfPeopleInRoom;
    private int numOfGamesUntilRound;
    private int currRoundNum = 1;

    private com.skydoves.colorpickerview.ColorPickerView colorPickerView;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing_screen);

        client = Client.getInstance();
        isManager = getIntent().getBooleanExtra("isManager", false);

        drawing_screen = findViewById(R.id.drawing_screen);
        paintClass = new DrawOnView(this);
        drawing_screen.addView(paintClass);

        submitGuess = findViewById(R.id.submit_guess);
        guesserImageView = findViewById(R.id.guesser_image_view);
        clearBtn = findViewById(R.id.clear);
        undoBtn = findViewById(R.id.undo);
        colorPalette = findViewById(R.id.color_palette);
        clearBtn.setOnClickListener(v -> paintClass.clear());
        undoBtn.setOnClickListener(v -> paintClass.undo());
        colorPalette.setOnClickListener(this::pickColor);
        guesserToolbar = findViewById(R.id.player_guess);
        colorPickerView = findViewById(R.id.colorPickerView);
        mainScreen = findViewById(R.id.main_drawing_screen);
        hint = findViewById(R.id.hint);
        wordWas = findViewById(R.id.word_was);
        allToolbars = findViewById(R.id.tool_bars);
        currentDrawing = findViewById(R.id.current_drawing);
        currScore = findViewById(R.id.current_score);
        timeLeftTextView = findViewById(R.id.time_left);
        timeLeftTextView.setText(ROUND_TIME + "seconds");

        usersListView = findViewById(R.id.side_users_list_view);
        userAdapter = new UserAdapter(this, 0, 0);
        usersListView.setAdapter(userAdapter);

        gameId = getIntent().getIntExtra("gameId", -1);

        findViewById(R.id.exit).setOnClickListener(v -> exit(this, client));
        findViewById(R.id.open_users_side_bar).setOnClickListener(v ->
                showHideUsersSideBar(findViewById(R.id.users_side_bar)));


        submitGuess.setOnClickListener(v -> {
            correctGuess = guesserToolbar.getText().toString();
            client.sendMessage(guesserToolbar.getText().toString());
            guesserToolbar.setText("");
        });

        overrideBackButton();
    }

    @Override
    protected void onStart() {
        super.onStart();

        musicIntent = new Intent(this, MusicService.class);
        musicIntent.addCategory("start");
        startService(musicIntent);

        getInitialPlayingMode();
    }

    private void overrideBackButton() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (backButtonCount >= 1) {
                    exit(DrawingScreen.this, client);
                }
                else {
                    Toast.makeText(DrawingScreen.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                    backButtonCount++;
                    new Handler().postDelayed(() -> backButtonCount = 0, DOUBLE_BACK_PRESS_INTERVAL);
                }
            }

        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void getInitialPlayingMode() {
        client.receiveMessage().thenAccept(message -> runOnUiThread(() -> {
           if (message.equals("draw")) {
               getAppropriateInterface(true, cachedUser.getUsername());
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
               numOfPeopleInRoom = updateUsersSideBar(message, userAdapter);
               getInitialPlayingMode();
           }
           else {
               getInitialPlayingMode();
           }
        }));
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void listenForServerDrawer() {
        client.receiveMessage().thenAccept(message -> runOnUiThread(() -> {
            if (message.equals("exit ok")) {
                return;
            }
            else if (message.equals("alone")) {
                client.sendMessage("alone ok");
            }
            else if (message.equals("manager")) {
                isManager = true;
            }
            else if (message.startsWith("users")) {
                lastUserSideBarFormat = message;
                numOfPeopleInRoom = updateUsersSideBar(message, userAdapter);
                currScore.setText("Your score: " + getPointsByUsername(message, cachedUser.getUsername()));
            }
            else if (message.startsWith("guess")) {
                String drawerName = message.split("guess ")[1];
                getAppropriateInterface(false, drawerName);
                listenForServerNoneDrawer();
                return;
            }
            else if (message.startsWith("continue")) {
                String roundNumber = "";
                numOfGamesUntilRound++;
                if (numOfGamesUntilRound >= numOfPeopleInRoom) {
                    numOfGamesUntilRound = 0;
                    currRoundNum++;
                    if (currRoundNum <= NUM_OF_ROUNDS) {
                        roundNumber = String.format("Round %d/%d\n", currRoundNum, NUM_OF_ROUNDS);
                    }
                }
                mainScreen.setVisibility(View.VISIBLE);
                SoundEffects.playSound(SoundEffects.next);
                disableAllViews();
                handler.removeCallbacks(updateCountdown);
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
                    mainScreen.setVisibility(View.GONE);
                    paintClass.clearBoard();
                    enableAllViews();
                    resetClock();
                    startCountdown();
                    client.sendMessage("continue ok");
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
        client.receiveMessage().thenAccept(message -> {
            if (message.equals("waiting")) {
                client.sendMessage("waiting ok");
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
        intent.putExtra("selfPoints", getPointsByUsername(lastUserSideBarFormat, cachedUser.getUsername()));
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
        client.receiveMessage().thenAccept(message -> runOnUiThread(() -> {
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
                updateUsersSideBar(message, userAdapter);
                currScore.setText("Your score: " + getPointsByUsername(message, cachedUser.getUsername()));
            }
            else if (message.startsWith("continue")) {
                String roundNumber = "";
                numOfGamesUntilRound++;
                if (numOfGamesUntilRound >= numOfPeopleInRoom) {
                    numOfGamesUntilRound = 0;
                    currRoundNum++;
                    if (currRoundNum <= NUM_OF_ROUNDS) {
                        roundNumber = String.format("Round %d/%d\n", currRoundNum, NUM_OF_ROUNDS);
                    }
                }
                mainScreen.setVisibility(View.VISIBLE);
                SoundEffects.playSound(SoundEffects.next);
                disableAllViews();
                handler.removeCallbacks(updateCountdown);
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
                    mainScreen.setVisibility(View.GONE);
                    paintClass.clearBoard();
                    enableAllViews();
                    resetClock();
                    startCountdown();
                    client.sendMessage("continue ok");
                }));
            }
            else if (message.startsWith("clue")) {
                hint.setText(message.split("clue: ")[1]);
            }
            else if (message.equals("draw")) {
                paintClass.clear();
                getAppropriateInterface(true, cachedUser.getUsername());
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
                client.sendMessage("alone ok");
            }
            else if (message.contains("dataBytes")){
                String encodedBitmap;
                try {
                    encodedBitmap = client.receiveAll(message);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                byte[] bitmapBytes = client.transformToBitmap(encodedBitmap);
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
        guesserToolbar.setEnabled(false);
        hint.setText(correctGuess);
    }

    private void putBitmapOnImage(byte[] bitmapBytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        guesserImageView.setImageBitmap(bitmap);
    }

    private void pickColor(final View view) {
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
            guesserImageView.setVisibility(View.GONE);
            drawerToolBar.setVisibility(View.VISIBLE);
            guesserToolbar.setVisibility(View.GONE);
            paintClass.setVisibility(View.VISIBLE);
            currentDrawing.setText("Currently drawing: " + drawerName);
        }
        else {
            submitGuess.setVisibility(View.VISIBLE);
            guesserImageView.setVisibility(View.VISIBLE);
            drawerToolBar.setVisibility(View.GONE);
            guesserToolbar.setVisibility(View.VISIBLE);
            paintClass.setVisibility(View.GONE);
            currentDrawing.setText("Currently drawing: " + drawerName);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        musicIntent = new Intent(this, MusicService.class);
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
        updateCountdown = new Runnable() {
            @Override
            public void run() {
                if (timeLeft > 0) {
                    timeLeftTextView.setText(timeLeft + " seconds");
                    timeLeft--;
                    handler.postDelayed(this, 1000);
                } else {
                    handler.removeCallbacks(this);
                    timeLeftTextView.setText("0 seconds");
                }
            }
        };
        handler.post(updateCountdown);
    }

    private void disableAllViews() {
        guesserToolbar.setEnabled(false);
        undoBtn.setEnabled(false);
        colorPalette.setEnabled(false);
        clearBtn.setEnabled(false);
        findViewById(R.id.open_users_side_bar).setEnabled(false);
        submitGuess.setEnabled(false);
        findViewById(R.id.exit).setEnabled(false);
    }

    private void enableAllViews() {
        guesserToolbar.setEnabled(true);
        submitGuess.setEnabled(true);
        undoBtn.setEnabled(true);
        colorPalette.setEnabled(true);
        clearBtn.setEnabled(true);
        findViewById(R.id.open_users_side_bar).setEnabled(true);
        findViewById(R.id.exit).setEnabled(true);
    }
}