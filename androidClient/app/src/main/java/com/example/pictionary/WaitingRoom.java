package com.example.pictionary;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * This class represents the waiting room screen of the application.
 */
public class WaitingRoom extends BaseGameActivity {
    /** buttons */
    // start game button
    private Button startGameBtn;
    // start game button icon
    private ImageView startGameIcon;
    // invite friends button
    private Button inviteFriendsBtn;

    /** textViews */
    // elapsed time text
    private TextView elapsedTimeTextView;

    /** handler */
    // handler for updating the elapsed time
    private final Handler timeElapsedHandler = new Handler(Looper.getMainLooper());
    // updater for the elapsed time
    private Runnable timeElapsedUpdater;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);
        setButtonListeners();
    }


    @Override
    protected void onStart() {
        super.onStart();
        // update if the user is the manager
        updateIsManager(isManager);
        // if available, show the winner screen
        showWinnerScreenIfAvailable();
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
                ClientController.MessageType type = ClientController.MessageType.values()[msg.what];
                switch (type) {
                    case START_GAME:
                        goToGameScreen();
                        break;
                    case MANAGER:
                        updateIsManager(true);
                        break;
                    case USERS:
                        updateUsersSideBar(msg.obj.toString());
                        break;
                    case EXIT_OK:
                        break;
                    case TIME:
                        startTimer((Long) msg.obj);
                        break;
                    case STATISTICS:
                        databaseController.addToUserStatistics(msg.obj.toString());
                        break;
                }
            }
        };
    }

    /**
     * If available, shows the winner screen.
     */
    private void showWinnerScreenIfAvailable() {
        String winnerName = getIntent().getStringExtra("winnerName");
        String winnerPoints = getIntent().getStringExtra("winnerPoints");
        if (winnerName == null || winnerPoints == null) {
            return;
        }
        int selfPoints = getIntent().getIntExtra("selfPoints", 0);

        inflateWinnerScreen(winnerName, winnerPoints, selfPoints);
    }

    /**
     * Inflates the winner screen.
     * @param winnerName The name of the winner.
     * @param winnerPoints The points of the winner.
     * @param selfPoints The points of the self.
     */
    @SuppressLint("SetTextI18n")
    private void inflateWinnerScreen(String winnerName, String winnerPoints, int selfPoints) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.winner_screen, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(true);

        ImageView exitWinnerScreen = dialogView.findViewById(R.id.back_to_waiting_room);
        ImageView winnerScreenIcon = dialogView.findViewById(R.id.winner_loser_image);
        TextView winnerNameTextView = dialogView.findViewById(R.id.winner_name);
        TextView winnerPointsTextView = dialogView.findViewById(R.id.winner_score);
        TextView loserTextView = dialogView.findViewById(R.id.loser_text);
        TextView selfPointsTextView = dialogView.findViewById(R.id.winner_screen_player_score);

        winnerPointsTextView.setText("With a score of:\n" + winnerPoints + " points!");
        selfPointsTextView.setText("Your score: " + selfPoints);

        // if self is winner
        if (winnerName.equals(DatabaseController.getCachedUser().getUsername())) {
            SoundEffects.playSound(SoundEffects.winner);
            winnerScreenIcon.setImageResource(R.drawable.trophy);
            winnerNameTextView.setText("Winner:\n YOU!!!");
            loserTextView.setVisibility(View.INVISIBLE);
        }
        else {
            winnerNameTextView.setText("Winner:\n" + winnerName);
            winnerScreenIcon.setImageResource(R.drawable.loser);
            SoundEffects.playSound(SoundEffects.loser);
            loserTextView.setVisibility(View.VISIBLE);
        }

        Dialog dialog = dialogBuilder.create();
        Window window = dialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        dialog.show();

        exitWinnerScreen.setOnClickListener(v -> dialog.dismiss());
    }

    /**
     * Inflates the invitation dialog.
     */
    @SuppressLint("SetTextI18n")
    private void inflateInvitationDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.invite_friends, null);
        dialogBuilder.setView(dialogView);

        TextView gameIdTv = dialogView.findViewById(R.id.game_id);
        Button sendInviteBtn = dialogView.findViewById(R.id.invite_friends_button);

        gameIdTv.setText("Game ID: " + gameId);

        Dialog dialog = dialogBuilder.create();
        Window window = dialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        dialog.show();

        sendInviteBtn.setOnClickListener(v -> sendGameInvite());
    }

    /**
     * Sends a game invite.
     */
    private void sendGameInvite() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Join my game on Pictionary at id: " + gameId);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    /**
     * Goes to the game screen.
     */
    private void goToGameScreen() {
        SoundEffects.playSound(SoundEffects.start);
        Intent intent = new Intent(WaitingRoom.this, DrawingScreen.class);
        intent.putExtra("isManager", isManager);
        intent.putExtra("gameId", gameId);
        startActivity(intent);
        finish();
    }

    /**
     * Updates if the user is the manager.
     * @param isManager If the user is the manager.
     */
    @Override
    protected void updateIsManager(boolean isManager) {
        super.updateIsManager(isManager);
        if (isManager) {
            startGameBtn.setVisibility(View.VISIBLE);
            startGameIcon.setVisibility(View.VISIBLE);
        }
        else {
            startGameBtn.setVisibility(View.GONE);
            startGameIcon.setVisibility(View.GONE);
        }
    }

    /**
     * Starts the timer.
     * @param startTime The start time.
     */
    private void startTimer(long startTime) {
        // setup the timer
        timeElapsedUpdater = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTime*ONE_SECOND_IN_MILLIS;
                updateElapsedTime(elapsedTime);
                timeElapsedHandler.postDelayed(this, ONE_SECOND_IN_MILLIS); // update every second
            }
        };
        // start the timer
        timeElapsedHandler.post(timeElapsedUpdater);
    }

    /**
     * Updates the elapsed time.
     * @param elapsedTime The elapsed time.
     */
    @SuppressLint("SetTextI18n")
    private void updateElapsedTime(long elapsedTime) {
        if (elapsedTime <= 0) {
            return;
        }
        int minutes = (int) ((elapsedTime % MILLIS_IN_HOUR) / MILLIS_IN_MINUTE);
        int seconds = (int) ((elapsedTime % MILLIS_IN_MINUTE) / MILLIS_IN_SECOND);
        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        elapsedTimeTextView.setText("Elapsed time: " + formattedTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // stop the timer handler
        timeElapsedHandler.removeCallbacks(timeElapsedUpdater);
    }

    /**
     * Starts the game.
     */
    private void startGame() {
        if (getNumUsersInRoom() < MIN_PLAYERS_IN_ROOM) {
            alert(TOO_FEW_PLAYERS_IN_ROOM).show();
            return;
        }
        if (getNumUsersInRoom() > MAX_PLAYERS_IN_ROOM) {
            alert(TOO_MANY_PLAYERS_IN_ROOM).show();
            return;
        }
        clientController.startGame();
    }

    /**
     * Sets the button listeners.
     */
    private void setButtonListeners() {
        startGameBtn = findViewById(R.id.start_game);
        startGameIcon = findViewById(R.id.start_game_icon);
        inviteFriendsBtn = findViewById(R.id.invite_friends);
        elapsedTimeTextView = findViewById(R.id.time_elapsed);

        startGameBtn.setOnClickListener(v -> startGame());
        inviteFriendsBtn.setOnClickListener(v -> inflateInvitationDialog());
    }
}