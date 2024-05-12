package com.example.pictionary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * This class represents the statistics screen of the application.
 */
public class Statistics extends BaseMainActivity {
    /** constants */
    // the value to display when the user is not logged in
    public static final String DEFAULT_STATISTICS = "XXX";

    /** textViews */
    // the number of games played
    private TextView gamesPlayed;
    // the number of games won
    private TextView gamesWon;
    // the number of guesses
    private TextView guesses;
    // the number of correct guesses
    private TextView correctGuesses;
    // the accuracy of the user's guesses
    private TextView accuracy;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        getViewsById();
    }

    /**
     * Called when the user logs out.
     * It resets the displayed statistics to the default values.
     */
    @Override
    public void onUserLogout() {
        super.onUserLogout();
        gamesPlayed.setText(DEFAULT_STATISTICS);
        gamesWon.setText(DEFAULT_STATISTICS);
        guesses.setText(DEFAULT_STATISTICS);
        correctGuesses.setText(DEFAULT_STATISTICS);
        accuracy.setText(DEFAULT_STATISTICS);
    }

    /**
     * Called when the user logs in.
     * It updates the displayed statistics with the user's statistics from the database.
     * @param databaseUser The logged in user.
     */
    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onUserLoggedIn(DatabaseUser databaseUser) {
        super.onUserLoggedIn(databaseUser);
        DatabaseUser cachedUser = DatabaseController.getCachedUser();

        // update the statistics
        gamesPlayed.setText(String.valueOf(cachedUser.getGamesPlayed()));
        gamesWon.setText(String.valueOf(cachedUser.getGamesWon()));
        guesses.setText(String.valueOf(cachedUser.getGuesses()));
        correctGuesses.setText(String.valueOf(cachedUser.getCorrectGuesses()));

        // to avoid division by zero
        if (cachedUser.getGuesses() == 0) {
            accuracy.setText("0");
        }
        else {
            // calculate the accuracy
            double accuracyPercentage = ((double) cachedUser.getCorrectGuesses() / cachedUser.getGuesses()) * 100;
            String formattedAccuracy = String.format("%.2f%%", accuracyPercentage);
            accuracy.setText(formattedAccuracy);
        }
    }

    /**
     * Gets the views by their ID.
     */
    private void getViewsById() {
        gamesPlayed = findViewById(R.id.games_played);
        gamesWon = findViewById(R.id.games_won);
        guesses = findViewById(R.id.guesses);
        correctGuesses = findViewById(R.id.correct_guesses);
        accuracy = findViewById(R.id.accuracy);
    }

    /**
     * Navigates to the settings activity.
     * @param activity The current activity.
     */
    @Override
    public void goToSettingsActivity(Activity activity) {
        super.goToSettingsActivity(activity);
        finish();
    }

    /**
     * Navigates to the statistics activity.
     * @param activity The current activity.
     */
    @Override
    public void goToStatisticsActivity(Activity activity) {
        super.goToStatisticsActivity(activity);
        finish();
    }
}