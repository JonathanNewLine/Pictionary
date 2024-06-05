package com.example.pictionary;

import static androidx.constraintlayout.widget.ConstraintLayoutStates.TAG;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This class handles the interactions with the Firebase database.
 */
public class DatabaseController {
    // the Firebase authentication
    private final FirebaseAuth mAuth;
    // the users table in the Firestore database
    private final CollectionReference usersTable;
    // the cached user
    private static DatabaseUser cachedUser;
    // singleton instance of the DatabaseController
    private static DatabaseController instance;


    /**
     * Returns the instance of the DatabaseController.
     * If the instance is null, a new instance is created.
     * @return The instance of the DatabaseController.
     */
    public static DatabaseController getInstance() {
        if (instance == null) {
            instance = new DatabaseController();
        }
        return instance;
    }

    /**
     * Private constructor for the DatabaseController class.
     * Initializes the FirebaseAuth and Firestore instances.
     */
    private DatabaseController() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        usersTable = database.collection("users");
    }

    /**
     * Logs in with the cached user.
     * @param callback The callback to be executed after login.
     */
    public void logInWithCachedUser(LoginCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        usersTable.document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            // transfer the user data a DatabaseUser object
            DatabaseUser databaseUser = documentSnapshot.toObject(DatabaseUser.class);
            assert databaseUser != null;
            cachedUser = databaseUser;
            callback.onLoginSuccess(databaseUser);
        });
    }

    /**
     * Logs in with the provided email and password.
     * @param email The email of the user.
     * @param password The password of the user.
     * @param activity The current activity.
     * @param callback The callback to be executed after login.
     */
    public void logIn(String email, String password, Activity activity, LoginCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(activity, (OnSuccessListener<? super AuthResult>) task -> {
                    Log.d(TAG, "signInWithEmail:success");
                    logInWithCachedUser(callback);
                })
                .addOnFailureListener(e ->
                        callback.onLoginFailure("Login up failed: " + e.getMessage()));
    }

    /**
     * Registers a new user with the provided email, password, and username.
     * @param email The email of the user.
     * @param password The password of the user.
     * @param confirmPassword The confirmed password of the user.
     * @param username The username of the user.
     * @param activity The current activity.
     * @param callback The callback to be executed after registration.
     */
    public void register(String email, String password, String confirmPassword, String username, Activity activity, RegisterCallback callback) {
        // check if the passwords match
        if (!password.equals(confirmPassword)) {
            callback.onRegisterFailure("Passwords do not match");
            return;
        }

        usersTable.whereEqualTo("username", username).get()
            .addOnSuccessListener(documentSnapshots -> {
                // check for username uniqueness
                if (!documentSnapshots.isEmpty()) {
                    // username already exists
                    callback.onRegisterFailure("Username already exists");
                }
                else {
                    // username unique
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener(activity, (OnSuccessListener<? super AuthResult>) task -> {
                                Log.d(TAG, "createUserWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();

                                assert user != null;
                                usersTable.document(user.getUid()).set(new DatabaseUser(email, username));
                                callback.onRegisterSuccess(email, password);
                            })
                            .addOnFailureListener(e ->
                                    callback.onRegisterFailure("Sign up failed: " + e.getMessage()));
                }
            });
    }

    /**
     * Adds to the user's statistics in the database.
     * @param statsJson The JSON string of the user's statistics.
     */
    public void addToUserStatistics(String statsJson) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        Gson gson = new Gson();
        // convert the JSON string to a DatabaseUser object
        DatabaseUser stats = gson.fromJson(statsJson, DatabaseUser.class);

        DocumentReference userDocRef = usersTable.document(user.getUid());
        // update the user's statistics in the database
        userDocRef.update("guesses", cachedUser.getGuesses() + stats.getGuesses(),
                        "correctGuesses",cachedUser.getCorrectGuesses() + stats.getCorrectGuesses(),
                        "gamesPlayed", cachedUser.getGamesPlayed() + stats.getGamesPlayed(),
                        "gamesWon", cachedUser.getGamesWon() + stats.getGamesWon())
                .addOnSuccessListener(aVoid -> {
                    // update the cached user
                    cachedUser.setGuesses(cachedUser.getGuesses() + stats.getGuesses());
                    cachedUser.setCorrectGuesses(cachedUser.getCorrectGuesses() + stats.getCorrectGuesses());
                    cachedUser.setGamesPlayed(cachedUser.getGamesPlayed() + stats.getGamesPlayed());
                    cachedUser.setGamesWon(cachedUser.getGamesWon() + stats.getGamesWon());
                });
    }

    /**
     * Logs out the current user.
     * @param callback The callback to be executed after logout.
     */
    public void logoutUser(LogoutCallback callback) {
        cachedUser = null;
        mAuth.signOut();

        callback.onLogout();
    }

    /**
     * Gets the points of a user by their username.
     * @param jsonString The JSON string of the user's statistics.
     * @param username The username of the user.
     * @return The points of the user.
     */
    public static int getPointsByUsername(String jsonString, String username) {
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            // iterate through the JSON array to find the user's points
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                // return the points if the username matches
                if (jsonObject.getString("username").equals(username)) {
                    return jsonObject.getInt("points");
                }
            }
            return -1;

        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Gets the cached user.
     * @return The cached user.
     */
    public static DatabaseUser getCachedUser() {
        return cachedUser;
    }

    /**
     * Interface for the login callback.
     */
    public interface LoginCallback {
        void onLoginSuccess(DatabaseUser user);
        void onLoginFailure(String errorMessage);
    }

    /**
     * Interface for the register callback.
     */
    public interface RegisterCallback {
        void onRegisterSuccess(String email, String password);
        void onRegisterFailure(String errorMessage);
    }

    /**
     * Interface for the logout callback.
     */
    public interface LogoutCallback {
        void onLogout();
    }
}
