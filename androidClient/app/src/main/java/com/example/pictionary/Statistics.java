package com.example.pictionary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class Statistics extends BaseMainActivity {
    // constants
    public static final String DEFAULT_STATISTICS = "XXX";
    
    // TextViews
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
        getViewsById();
    }

    @Override
    public void onUserLogout() {
        super.onUserLogout();
        gamesPlayed.setText(DEFAULT_STATISTICS);
        gamesWon.setText(DEFAULT_STATISTICS);
        guesses.setText(DEFAULT_STATISTICS);
        correctGuesses.setText(DEFAULT_STATISTICS);
        accuracy.setText(DEFAULT_STATISTICS);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onUserLoggedIn(DatabaseUser databaseUser) {
        super.onUserLoggedIn(databaseUser);
        DatabaseUser cachedUser = DatabaseController.getCachedUser();

        gamesPlayed.setText(String.valueOf(cachedUser.getGamesPlayed()));
        gamesWon.setText(String.valueOf(cachedUser.getGamesWon()));
        guesses.setText(String.valueOf(cachedUser.getGuesses()));
        correctGuesses.setText(String.valueOf(cachedUser.getCorrectGuesses()));

        if (cachedUser.getGuesses() == 0) {
            accuracy.setText("0");
        }
        else {
            double accuracyPercentage = ((double) cachedUser.getCorrectGuesses() / cachedUser.getGuesses()) * 100;
            String formattedAccuracy = String.format("%.2f%%", accuracyPercentage);
            accuracy.setText(formattedAccuracy);
        }
    }

    private void getViewsById() {
        gamesPlayed = findViewById(R.id.games_played);
        gamesWon = findViewById(R.id.games_won);
        guesses = findViewById(R.id.guesses);
        correctGuesses = findViewById(R.id.correct_guesses);
        accuracy = findViewById(R.id.accuracy);
    }

    @Override
    public void goToSettingsActivity(Activity activity) {
        super.goToSettingsActivity(activity);
        finish();
    }

    @Override
    public void goToStatisticsActivity(Activity activity) {
        super.goToStatisticsActivity(activity);
        finish();
    }
}