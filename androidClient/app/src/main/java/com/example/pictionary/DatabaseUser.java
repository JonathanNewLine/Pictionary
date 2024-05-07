package com.example.pictionary;

public class DatabaseUser {
    private int correctGuesses;
    private String email;
    private int gamesPlayed;
    private int gamesWon;
    private int guesses;
    private String username;

    public int getCorrectGuesses() {
        return correctGuesses;
    }

    public void setCorrectGuesses(int correctGuesses) {
        this.correctGuesses = correctGuesses;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public int getGuesses() {
        return guesses;
    }

    public void setGuesses(int guesses) {
        this.guesses = guesses;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


    public DatabaseUser(int correctGuesses, String email, int gamesPlayed, int gamesWon, int guesses, String username) {
        this.correctGuesses = correctGuesses;
        this.email = email;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
        this.guesses = guesses;
        this.username = username;
    }

    public DatabaseUser(String email, String username) {
        this.email = email;
        this.username = username;
        this.correctGuesses = 0;
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.guesses = 0;
    }

    public DatabaseUser() {
    }
}
