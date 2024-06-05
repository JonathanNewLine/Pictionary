package com.example.pictionary;

/**
 * This class represents a User for the listView.
 */
public class User {
    private String username;
    private int points;

    /**
     * Constructor for the User class.
     * @param username The username of the user.
     * @param points The points of the user.
     */
    public User(String username, int points) {
        this.username = username;
        this.points = points;
    }

    /**
     * Gets the points of the user.
     * @return The points of the user.
     */
    public int getPoints() {
        return points;
    }

    /**
     * Gets the username of the user.
     * @return The username of the user.
     */
    public String getUsername() {
        return username;
    }
}