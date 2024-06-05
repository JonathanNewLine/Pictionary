package com.example.pictionary;

/**
 * This class represents a user in the database.
 */
public class DatabaseUser {
    // number of correct guesses the user has made
    private int correctGuesses;
    // user's email
    private String email;
    // number of games the user has played
    private int gamesPlayed;
    // number of games the user has won
    private int gamesWon;
    // number of guesses the user has made
    private int guesses;
    // user's username
    private String username;

    /**
     * Gets the number of correct guesses the user has made.
     * @return The number of correct guesses.
     */
    public int getCorrectGuesses() {
        return correctGuesses;
    }

    /**
     * Sets the number of correct guesses the user has made.
     * @param correctGuesses The number of correct guesses.
     */
    public void setCorrectGuesses(int correctGuesses) {
        this.correctGuesses = correctGuesses;
    }

    /**
     * Gets the user's email.
     * @return The user's email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email.
     * @param email The user's email.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the number of games the user has played.
     * @return The number of games played.
     */
    public int getGamesPlayed() {
        return gamesPlayed;
    }

    /**
     * Sets the number of games the user has played.
     * @param gamesPlayed The number of games played.
     */
    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    /**
     * Gets the number of games the user has won.
     * @return The number of games won.
     */
    public int getGamesWon() {
        return gamesWon;
    }

    /**
     * Sets the number of games the user has won.
     * @param gamesWon The number of games won.
     */
    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    /**
     * Gets the number of guesses the user has made.
     * @return The number of guesses.
     */
    public int getGuesses() {
        return guesses;
    }

    /**
     * Sets the number of guesses the user has made.
     * @param guesses The number of guesses.
     */
    public void setGuesses(int guesses) {
        this.guesses = guesses;
    }

    /**
     * Gets the user's username.
     * @return The user's username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the user's username.
     * @param username The user's username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Constructor for the DatabaseUser class.
     * @param correctGuesses The number of correct guesses the user has made.
     * @param email The user's email.
     * @param gamesPlayed The number of games the user has played.
     * @param gamesWon The number of games the user has won.
     * @param guesses The number of guesses the user has made.
     * @param username The user's username.
     */
    public DatabaseUser(int correctGuesses, String email, int gamesPlayed, int gamesWon, int guesses, String username) {
        this.correctGuesses = correctGuesses;
        this.email = email;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
        this.guesses = guesses;
        this.username = username;
    }

    /**
     * Constructor for the DatabaseUser class.
     * @param email The user's email.
     * @param username The user's username.
     */
    public DatabaseUser(String email, String username) {
        this.email = email;
        this.username = username;
        this.correctGuesses = 0;
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.guesses = 0;
    }

    /**
     * Default constructor for the DatabaseUser class.
     */
    public DatabaseUser() {
    }
}