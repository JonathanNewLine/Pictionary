package com.example.pictionary;

public class User {
    private String username;
    private int points;

    public User(String username, int points) {
        this.username = username;
        this.points = points;
    }

    public int getPoints() {
        return points;
    }


    public String getUsername() {
        return username;
    }

}
