package com.example.pictionary;

import static com.example.pictionary.DrawingScreen.DOUBLE_BACK_PRESS_INTERVAL;

import androidx.activity.OnBackPressedCallback;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class WaitingRoom extends BaseGameActivity {
    private final int MIN_PLAYERS_IN_ROOM = 2;
    private final int MAX_PLAYERS_IN_ROOM = 6;
    private Client client;
    private int gameId;
    private Button startGameBtn;
    private ImageView startGameIcon;
    private ListView usersListView;
    private boolean isManager;
    private UserAdapter userAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private int backButtonCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);
        gameId = getIntent().getIntExtra("gameId", -1);
        client = Client.getInstance();
        startGameBtn = findViewById(R.id.start_game);
        startGameIcon = findViewById(R.id.start_game_icon);

        isManager = getIntent().getBooleanExtra("isManager", false);
        updateManagerScreen(isManager);

        usersListView = findViewById(R.id.side_users_list_view);
        userAdapter = new UserAdapter(this, 0, 0);
        usersListView.setAdapter(userAdapter);

        startGameBtn.setOnClickListener(v -> {
            if (userAdapter.getCount() < MIN_PLAYERS_IN_ROOM) {
                alert("Too few people in room, need at least two").show();
                return;
            }
            if (userAdapter.getCount() > MAX_PLAYERS_IN_ROOM) {
                alert("Too many people in room, can host at most six").show();
                return;
            }
            client.startGame();
        });
        findViewById(R.id.invite_friends).setOnClickListener(this::inflateInvitationDialog);
        findViewById(R.id.exit).setOnClickListener(v -> exit(this, client));
        findViewById(R.id.open_users_side_bar).setOnClickListener(v ->
                showHideUsersSideBar(findViewById(R.id.users_side_bar)));

        overrideBackButton();

        listenForServer();
        showWinnerScreenIfAvailable();
    }

    private void showWinnerScreenIfAvailable() {
        String[] winnerData = getIntent().getStringArrayExtra("winnerData");
        if (winnerData == null) {
            return;
        }
        inflateWinnerScreen(winnerData[0], winnerData[1], getIntent().getIntExtra("selfPoints", 0));
        SoundEffects.playSound(SoundEffects.winner);
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

    }

    private void overrideBackButton() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (backButtonCount >= 1) {
                    exit(WaitingRoom.this, client);
                }
                else {
                    Toast.makeText(WaitingRoom.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                    backButtonCount++;
                    new Handler().postDelayed(() -> backButtonCount = 0, DOUBLE_BACK_PRESS_INTERVAL);
                }
            }

        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void listenForServer() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        client.receiveMessage().thenAccept(response -> runOnUiThread(()->{
            if (response.equals("start")) {
                SoundEffects.playSound(SoundEffects.start);
                client.sendMessage("start ok");
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
                client.sendMessage("manager ok");
                future.complete(true);
            }
            else if (response.startsWith("users")) {
                updateUsersSideBar(response, userAdapter);
            }
            else if (response.equals("exit ok")) {
                return;
            }
            else if (response.startsWith("time")) {
                long epochTime = Long.parseLong(response.split("time: ")[1]);
                startTimer(epochTime);
            }
            else if(response.startsWith("statistics")) {
                addToUserStatistics(response.split("statistics: ")[1]);
            }
            listenForServer();
        }));
    }


    @SuppressLint("SetTextI18n")
    private void inflateInvitationDialog(View v) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.invite_friends, null);
        dialogBuilder.setView(dialogView);

        TextView gameIdTv = dialogView.findViewById(R.id.game_id);
        gameIdTv.setText(gameIdTv.getText().toString() + gameId);
        Button sendInviteBtn = dialogView.findViewById(R.id.invite_friends_button);
        sendInviteBtn.setOnClickListener(v1 -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Join my game on Pictionary at id: " + gameId);
            sendIntent.setType("text/plain");

            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(shareIntent);
        });

        Dialog dialog = dialogBuilder.create();
        Window window = dialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        dialog.show();
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
        runnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTime*1000;
                updateElapsedTime(elapsedTime);
                handler.postDelayed(this, 1000); // update every second
            }
        };
        handler.post(runnable);
    }


    @SuppressLint("SetTextI18n")
    private void updateElapsedTime(long elapsedTime) {
        if (elapsedTime <= 0) {
            return;
        }
        int minutes = (int) ((elapsedTime % 3600000) / 60000);
        int seconds = (int) ((elapsedTime % 60000) / 1000);
        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        TextView elapsedTimeTextView = findViewById(R.id.time_elapsed);
        elapsedTimeTextView.setText("Elapsed time: " + formattedTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }
}