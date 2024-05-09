package com.example.pictionary;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.graphics.BitmapFactory;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DrawingScreen extends BaseGameActivity {
    // constants
    public final static int ROUND_TIME = 60;
    public final static int NUM_OF_ROUNDS = 3;

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
    private ImageView exit;
    private ImageView openUsersSideBar;

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
    private View drawerToolBar;
    
    // countdown
    private Runnable countDownUpdater;
    private final Handler countDownHandler = new Handler(Looper.getMainLooper());

    // other
    private int timeLeft = ROUND_TIME;
    private int currRoundNum = 1;
    private int numOfPeopleInRoom;
    private int numOfGames = 0;
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
    }

    @Override
    public Handler getMessageHandler() {
        return new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                Bundle bundle;
                ClientController.MessageType type = ClientController.MessageType.values()[msg.what];
                switch (type) {
                    case MANAGER:
                        updateIsManager(true);
                        break;
                    case USERS:
                        onReceivedUsersUpdate(msg.obj.toString());
                        break;
                    case EXIT_OK:
                        break;
                    case DRAW:
                        getAppropriateInterface(true, DatabaseController.getCachedUser().getUsername());
                        updateWordToDraw(msg.obj.toString());
                        startCountdown();
                        break;
                    case GUESS:
                        bundle = msg.getData();
                        String drawerName = bundle.getString("drawerName");
                        String clue = bundle.getString("clue");

                        getAppropriateInterface(false, drawerName);
                        updateClue(clue);
                        startCountdown();
                        break;
                    case CONTINUE:
                        continueNextRound(msg.obj.toString());
                        break;
                    case GO_TO_WAITING_ROOM:
                        bundle = msg.getData();
                        String winnerName = bundle.getString("winnerName");
                        String winnerPoints = bundle.getString("winnerPoints");

                        goToWaitingRoom(winnerName, winnerPoints);
                        break;
                    case CORRECT_GUESS:
                        updateCorrectGuess();
                        break;
                    case WRONG_GUESS:
                        updateWrongGuess();
                        break;
                    case DRAWING_BYTES:
                        processAndDisplayBitmapFromServer(msg.obj.toString());
                        break;
                }
            }
        };
    }
    private void goToWaitingRoom(String winnerName, String winnerPoints) {
        Intent intent = new Intent(DrawingScreen.this, WaitingRoom.class);
        intent.putExtra("isManager", isManager);
        intent.putExtra("gameId", gameId);
        intent.putExtra("winnerName", winnerName);
        intent.putExtra("winnerPoints", winnerPoints);
        intent.putExtra("selfPoints", DatabaseController.getPointsByUsername(lastUserSideBarFormat, DatabaseController.getCachedUser().getUsername()));
        startActivity(intent);
        finish();
    }

    @SuppressLint("SetTextI18n")
    private void updateCorrectGuess() {
        SoundEffects.playSound(SoundEffects.correct);
        allToolbars.setBackgroundColor(Color.parseColor("#B5189501"));
        guesserInputBox.setEnabled(false);
        hint.setText(correctGuess);
    }

    private void putBitmapOnImage(byte[] bitmapBytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        displayedDrawingForGuesser.setImageBitmap(bitmap);
        displayedDrawingForGuesser.setVisibility(View.VISIBLE);
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
        allToolbars.setBackgroundColor(Color.parseColor("#83AABBCC"));

        if (isDrawer) {
            paintClass.clear();
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
        timeLeftTextView.setText(ROUND_TIME + " seconds");
        timeLeft = ROUND_TIME;
    }

    @SuppressLint("SetTextI18n")
    private void startCountdown() {
        countDownHandler.removeCallbacks(countDownUpdater);
        countDownUpdater = new Runnable() {
            @Override
            public void run() {
                if (timeLeft > 0) {
                    timeLeftTextView.setText(timeLeft + " seconds");
                    timeLeft--;
                    countDownHandler.postDelayed(this, MILLIS_IN_SECOND);
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
        drawerToolBar = findViewById(R.id.drawer_tool_bar);
        exit = findViewById(R.id.exit);
        openUsersSideBar = findViewById(R.id.open_users_side_bar);

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
        openUsersSideBar.setEnabled(false);
        submitGuess.setEnabled(false);
        exit.setEnabled(false);
        paintClass.setDrawingEnabled(false);
    }

    private void enableAllViews() {
        guesserInputBox.setEnabled(true);
        submitGuess.setEnabled(true);
        undoBtn.setEnabled(true);
        colorPalette.setEnabled(true);
        clearBtn.setEnabled(true);
        openUsersSideBar.setEnabled(true);
        exit.setEnabled(true);
        paintClass.setDrawingEnabled(true);
    }

    @SuppressLint("SetTextI18n")
    private void onReceivedUsersUpdate(String usersJson) {
        int selfScore = DatabaseController.getPointsByUsername(usersJson, DatabaseController.getCachedUser().getUsername());
        updateUsersSideBar(usersJson);
        lastUserSideBarFormat = usersJson;
        numOfPeopleInRoom = updateUsersSideBar(usersJson);
        currScore.setText("Your score: " + selfScore);
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

    private void continueNextRound(String roundWord) {
        displayCoolDownScreen(roundWord);
        SoundEffects.playSound(SoundEffects.next);
    }

    private void displayCoolDownScreen(String roundWord) {
        String roundNumber = getRoundNumber();
        countDownHandler.removeCallbacks(countDownUpdater); // stop the countdown
        addCoolDownScreen(roundWord, roundNumber);
        coolDownScreen.setVisibility(View.VISIBLE);
        waitForCoolDownScreen().thenAccept(unused ->
                runOnUiThread(this::removeCoolDownScreen));
    }

    @SuppressLint("DefaultLocale")
    private String getRoundNumber() {
        String roundNumber = "";

        numOfGames++;
        if (numOfGames >= numOfPeopleInRoom) {
            numOfGames = 0;
            currRoundNum++;
            if (currRoundNum <= NUM_OF_ROUNDS) {
                roundNumber = String.format("Round %d/%d\n", currRoundNum, NUM_OF_ROUNDS);
            }
        }
        return roundNumber;
    }

    private CompletableFuture<Void> waitForCoolDownScreen() {
        return CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(COOL_DOWN_SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void addCoolDownScreen(String roundWord, String roundNumber) {
        hint.setText(roundWord);
        wordWas.setText(roundNumber + "The word was: " + roundWord);
        disableAllViews();
    }

    private void removeCoolDownScreen() {
        coolDownScreen.setVisibility(View.GONE);
        displayedDrawingForGuesser.setBackgroundColor(Color.WHITE);
        enableAllViews();
        resetClock();
        startCountdown();
        clientController.ackConfirm();
    }

    @SuppressLint("SetTextI18n")
    private void updateWordToDraw(String word) {
        wordWas.setText("The word was: " + word);
        hint.setText(word);
    }

    private void updateWrongGuess() {
        SoundEffects.playSound(SoundEffects.wrong);
    }

    private void processAndDisplayBitmapFromServer(String initialData) {
        String encodedBitmap = clientController.receiveAll(initialData);
        byte[] bitmapBytes = clientController.transformToBitmap(encodedBitmap);
        if (bitmapBytes != null) {
            putBitmapOnImage(bitmapBytes);
        }
    }

    private void updateClue(String clue) {
        hint.setText(clue);
    }
}