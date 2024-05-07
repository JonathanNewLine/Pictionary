package com.example.pictionary;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

public class Statistics extends BaseMainActivity {
    private TextView gamesPlayed;
    private TextView gamesWon;
    private TextView guesses;
    private TextView correctGuesses;
    private TextView accuracy;
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        gamesPlayed = findViewById(R.id.games_played);
        gamesWon = findViewById(R.id.games_won);
        guesses = findViewById(R.id.guesses);
        correctGuesses = findViewById(R.id.correct_guesses);
        accuracy = findViewById(R.id.accuracy);


        findViewById(R.id.exit).setOnClickListener(v -> finish());

        findViewById(R.id.statistics).setOnClickListener(v -> {
            finish();
            goToStatisticsActivity(this);
        });
        findViewById(R.id.settings).setOnClickListener(v -> {
            finish();
            goToSettingsActivity(this);
        });
    }

    @Override
    public void onUserLogout() {
        super.onUserLogout();

        gamesPlayed.setText("XXX");
        gamesWon.setText("XXX");
        guesses.setText("XXX");
        correctGuesses.setText("XXX");
        accuracy.setText("XXX");
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onUserLoggedIn(DatabaseUser databaseUser) {
        super.onUserLoggedIn(databaseUser);

        gamesPlayed.setText(String.valueOf(cachedUser.getGamesPlayed()));
        gamesWon.setText(String.valueOf(cachedUser.getGamesWon()));
        guesses.setText(String.valueOf(cachedUser.getGuesses()));
        correctGuesses.setText(String.valueOf(cachedUser.getCorrectGuesses()));

        if (cachedUser.getGuesses() == 0) {
            accuracy.setText("0");
        }
        else {
            double accuracyPercentage = ((double) cachedUser.getCorrectGuesses() / cachedUser.getGuesses()) * 100;
            @SuppressLint("DefaultLocale") String formattedAccuracy = String.format("%.2f%%", accuracyPercentage);
            accuracy.setText(formattedAccuracy);
        }
    }
}