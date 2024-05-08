package com.example.pictionary;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class WaitingRoom extends BaseGameActivity {
    // buttons
    private Button startGameBtn;
    private ImageView startGameIcon;
    private Button inviteFriendsBtn;

    // textViews
    private TextView elapsedTimeTextView;

    // time elapsed
    private final Handler timeElapsedHandler = new Handler(Looper.getMainLooper());
    private Runnable timeElapsedUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);
        setButtonListeners();
        updateManagerScreen(isManager);
        showWinnerScreenIfAvailable();
    }

    @Override
    public void listenForServer() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        clientController.receiveMessage().thenAccept(response -> runOnUiThread(()->{
            if (response.equals("start")) {
                SoundEffects.playSound(SoundEffects.start);
                clientController.sendMessage("start ok");
                Intent intent = new Intent(WaitingRoom.this, DrawingScreen.class);
                intent.putExtra("isManager", isManager);
                intent.putExtra("gameId", gameId);
                startActivity(intent);
                finish();
                future.complete(false);
                return;
            }
            else if (response.equals("manager")) {
                isManager = true;
                updateManagerScreen(true);
                clientController.sendMessage("manager ok");
                future.complete(true);
            }
            else if (response.startsWith("users")) {
                updateUsersSideBar(response);
            }
            else if (response.equals("exit ok")) {
                return;
            }
            else if (response.startsWith("time")) {
                long epochTime = Long.parseLong(response.split("time: ")[1]);
                startTimer(epochTime);
            }
            else if(response.startsWith("statistics")) {
                databaseController.addToUserStatistics(response.split("statistics: ")[1]);
            }
            listenForServer();
        }));
    }

    private void showWinnerScreenIfAvailable() {
        String[] winnerData = getIntent().getStringArrayExtra("winnerData");
        if (winnerData == null) {
            return;
        }

        String winnerName = winnerData[0];
        String winnerPoints = winnerData[1];
        int selfPoints = getIntent().getIntExtra("selfPoints", 0);

        inflateWinnerScreen(winnerName, winnerPoints, selfPoints);
    }

    @SuppressLint("SetTextI18n")
    private void inflateWinnerScreen(String winnerName, String winnerPoints, int selfPoints) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.winner_screen, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(true);

        ImageView exitWinnerScreen = dialogView.findViewById(R.id.back_to_waiting_room);
        TextView winnerNameTextView = dialogView.findViewById(R.id.winner_name);
        TextView winnerPointsTextView = dialogView.findViewById(R.id.winner_score);
        TextView selfPointsTextView = dialogView.findViewById(R.id.winner_screen_player_score);

        winnerNameTextView.setText("Winner:\n" + winnerName);
        winnerPointsTextView.setText("With a score of:\n" + winnerPoints + " points!");
        selfPointsTextView.setText("Your score: " + selfPoints);

        Dialog dialog = dialogBuilder.create();
        Window window = dialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        dialog.show();

        exitWinnerScreen.setOnClickListener(v -> dialog.dismiss());
        SoundEffects.playSound(SoundEffects.winner);
    }

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

    private void sendGameInvite() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Join my game on Pictionary at id: " + gameId);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    private void updateManagerScreen(boolean isManager) {
        if (isManager) {
            startGameBtn.setVisibility(View.VISIBLE);
            startGameIcon.setVisibility(View.VISIBLE);
        }
        else {
            startGameBtn.setVisibility(View.GONE);
            startGameIcon.setVisibility(View.GONE);
        }
    }


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
        timeElapsedHandler.removeCallbacks(timeElapsedUpdater);
    }

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

    private void setButtonListeners() {
        startGameBtn = findViewById(R.id.start_game);
        startGameIcon = findViewById(R.id.start_game_icon);
        inviteFriendsBtn = findViewById(R.id.invite_friends);
        elapsedTimeTextView = findViewById(R.id.time_elapsed);

        startGameBtn.setOnClickListener(v -> startGame());
        inviteFriendsBtn.setOnClickListener(v -> inflateInvitationDialog());
    }
}