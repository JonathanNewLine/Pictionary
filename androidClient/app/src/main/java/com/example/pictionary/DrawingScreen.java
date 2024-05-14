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

/**
 * This class represents the drawing screen of the application.
 */
public class DrawingScreen extends BaseGameActivity {
    /** constants */
    // time for one game
    public final static int ROUND_TIME = 60;
    // number of round before game ends
    public final static int NUM_OF_ROUNDS = 3;

    /** textViews */
    // hint for word to draw or guess
    private TextView hint;
    // time left in the round
    private TextView timeLeftTextView;
    // current drawing player
    private TextView currentDrawing;
    // what the round word was
    private TextView wordWas;
    // current score of the player
    private TextView currScore;

    /** buttons */
    // clear drawing button
    private ImageView clearBtn;
    // undo last drawing action button
    private ImageView undoBtn;
    // open color palette button
    private ImageView colorPalette;
    // submit guess button
    private ImageView submitGuess;
    // exit game button
    private ImageView exit;
    // open users side bar button
    private ImageView openUsersSideBar;

    /** editTexts */
    // guesser input box
    private EditText guesserInputBox;
    
    /** drawing screen components */
    // paint to draw with
    private DrawOnView paintClass;
    // layout to draw on
    private FrameLayout drawing_screen;
    // image view to display drawing for guesser
    private ImageView displayedDrawingForGuesser;
    
    /** other views */
    // cool down screen
    private FrameLayout coolDownScreen;
    // color picker view
    private com.skydoves.colorpickerview.ColorPickerView colorPickerView;
    // all toolbars - guesser and drawer
    private LinearLayout allToolbars;
    // drawer toolbar
    private View drawerToolBar;
    
    /** handler */
    // handler for countdown
    private final Handler countDownHandler = new Handler(Looper.getMainLooper());
    // updater for countdown
    private Runnable countDownUpdater;

    /** other */
    // time left in the round
    private int timeLeft = ROUND_TIME;
    // number of rounds played
    private int currRoundNum = 1;
    // number of people in the room
    private int numOfPeopleInRoom;
    // number of games played until the round
    private int numOfGames = 0;
    // the correct guess
    private String correctGuess;
    // the last user side bar message from server to pass on to the next activity
    private String lastUserSideBarFormat;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing_screen);
        // set up the views
        setButtonListeners();
        // set up the paint class to draw with
        setUpPaintClass();
        // set up the countdown
        timeLeftTextView.setText(ROUND_TIME + "seconds");
    }

    @Override
    protected void onStart() {
        super.onStart();
        startMusic();
    }

    /**
     * Returns a Handler that will be used to process messages sent from the client controller.
     * @return The Handler that will be used to process messages sent from the client controller.
     */
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

    /**
     * Navigates to the waiting room screen.
     * @param winnerName The name of the winner.
     * @param winnerPoints The points of the winner.
     */
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

    /**
     * Updates the interface to indicate a correct guess.
     */
    @SuppressLint("SetTextI18n")
    private void updateCorrectGuess() {
        SoundEffects.playSound(SoundEffects.correct);
        allToolbars.setBackgroundColor(Color.parseColor("#B5189501"));
        guesserInputBox.setEnabled(false);
        hint.setText(correctGuess);
    }

    /**
     * Displays a bitmap image on the screen.
     * @param bitmapBytes The byte array representing the bitmap.
     */
    private void putBitmapOnImage(byte[] bitmapBytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        displayedDrawingForGuesser.setImageBitmap(bitmap);
        displayedDrawingForGuesser.setVisibility(View.VISIBLE);
    }

    /**
     * Opens or closes the color picker.
     */
    private void pickColor() {
        colorPickerView.setColorListener((ColorEnvelopeListener) (envelope, fromUser) -> {
            int color = envelope.getColor();
            if (isShadeOfGray(color)) {
                color = Color.BLACK;
            }

            colorPalette.setColorFilter(color);
            paintClass.setColor(color);
            colorPickerView.setVisibility(View.GONE);
        });

        // toggle colorPickerView visibility
        if (colorPickerView.getVisibility() == View.GONE) {
            colorPickerView.setVisibility(View.VISIBLE);
        } else if (colorPickerView.getVisibility() == View.VISIBLE) {
            colorPickerView.setVisibility(View.GONE);
        }
    }

    /** Check if is shade of gray
     * @return if is gray */
    private boolean isShadeOfGray(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        // check if shade of gray
        return Math.abs(red - green) < 5 && Math.abs(red - blue) < 5 && Math.abs(green - blue) < 5 &&
                color != Color.WHITE;
    }

    /**
     * Updates the interface based on whether the user is the drawer or a guesser.
     * @param isDrawer Whether the user is the drawer.
     * @param drawerName The name of the drawer.
     */
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
        // pause the music
        Intent musicIntent = new Intent(this, MusicService.class);
        musicIntent.addCategory("pause");
        startService(musicIntent);
    }

    /**
     * Resets the countdown clock.
     */
    @SuppressLint("SetTextI18n")
    private void resetClock() {
        timeLeftTextView.setText(ROUND_TIME + " seconds");
        timeLeft = ROUND_TIME;
    }

    /**
     * Starts the countdown.
     */
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

    /**
     * Sets the listeners for the buttons on the screen.
     */
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

    /**
     * Submits a guess to the server.
     */
    private void submitGuess() {
        correctGuess = guesserInputBox.getText().toString();
        clientController.submitGuess(correctGuess);
        guesserInputBox.setText("");
    }

    /**
     * Disables all views on the screen.
     */
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

    /**
     * Enables all views on the screen.
     */
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

    /**
     * Updates the user sidebar when a user update is received from the server.
     * @param usersJson The JSON string representing the users.
     */
    @SuppressLint("SetTextI18n")
    private void onReceivedUsersUpdate(String usersJson) {
        int selfScore = DatabaseController.getPointsByUsername(usersJson, DatabaseController.getCachedUser().getUsername());
        updateUsersSideBar(usersJson);
        lastUserSideBarFormat = usersJson;
        numOfPeopleInRoom = updateUsersSideBar(usersJson);
        currScore.setText("Your score: " + selfScore);
    }

    /**
     * Sets up the paint class for the drawing screen.
     */
    private void setUpPaintClass() {
        paintClass = new DrawOnView(this);
        drawing_screen.addView(paintClass);
    }

    /**
     * Starts the music service.
     */
    private void startMusic() {
        Intent musicIntent = new Intent(this, MusicService.class);
        musicIntent.addCategory("start");
        startService(musicIntent);
    }

    /**
     * Continues to the next round.
     * @param roundWord The word for the round.
     */
    private void continueNextRound(String roundWord) {
        displayCoolDownScreen(roundWord);
        SoundEffects.playSound(SoundEffects.next);
    }

    /**
     * Displays the cool down screen.
     * @param roundWord The word for the round.
     */
    private void displayCoolDownScreen(String roundWord) {
        String roundNumber = getRoundNumber();
        // stop the countdown
        countDownHandler.removeCallbacks(countDownUpdater);
        addCoolDownScreen(roundWord, roundNumber);
        coolDownScreen.setVisibility(View.VISIBLE);
        waitForCoolDownScreen().thenAccept(unused ->
                runOnUiThread(this::removeCoolDownScreen));
    }

    /**
     * Gets the "round number: X" text and updates it.
     * @return The round number.
     */
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

    /**
     * Waits for the cool down screen to finish.
     * @return A CompletableFuture that will be completed when the cool down is finished.
     */
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

    /**
     * Adds the cool down screen to the view.
     * @param roundWord The word for the round.
     * @param roundNumber The "round number: X" text.
     */
    @SuppressLint("SetTextI18n")
    private void addCoolDownScreen(String roundWord, String roundNumber) {
        hint.setText(roundWord);
        wordWas.setText(roundNumber + "The word was: " + roundWord);
        disableAllViews();
    }

    /**
     * Removes the cool down screen from the screen.
     */
    private void removeCoolDownScreen() {
        coolDownScreen.setVisibility(View.GONE);
        displayedDrawingForGuesser.setBackgroundColor(Color.WHITE);
        enableAllViews();
        resetClock();
        startCountdown();
        clientController.ackConfirm();
    }

    /**
     * Updates the word to draw.
     * @param word The word to draw.
     */
    @SuppressLint("SetTextI18n")
    private void updateWordToDraw(String word) {
        wordWas.setText("The word was: " + word);
        hint.setText(word);
    }

    /**
     * Updates the interface to indicate a wrong guess.
     */
    private void updateWrongGuess() {
        SoundEffects.playSound(SoundEffects.wrong);
    }

    /**
     * Processes a bitmap from the server and displays it on the screen.
     * @param initialData The initial data received from the server.
     */
    private void processAndDisplayBitmapFromServer(String initialData) {
        byte[] bitmapBytes = clientController.getBitmapBytes(initialData);
        if (bitmapBytes != null) {
            putBitmapOnImage(bitmapBytes);
        }
    }

    /**
     * Updates the clue on the screen.
     * @param clue The clue.
     */
    private void updateClue(String clue) {
        hint.setText(clue);
    }
}