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

public class DatabaseController {
    private final FirebaseAuth mAuth;
    private final CollectionReference usersTable;
    private static DatabaseUser cachedUser;

    public DatabaseController() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        usersTable = database.collection("users");
    }

    public void logInWithCachedUser(LoginCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        usersTable.document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            DatabaseUser databaseUser = documentSnapshot.toObject(DatabaseUser.class);
            assert databaseUser != null;
            cachedUser = databaseUser;
            callback.onLoginSuccess(databaseUser);
        });
    }

    public void logIn(String email, String password, Activity activity, LoginCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(activity, (OnSuccessListener<? super AuthResult>) task -> {
                    Log.d(TAG, "signInWithEmail:success");
                    logInWithCachedUser(callback);
                })
                .addOnFailureListener(e ->
                        callback.onLoginFailure("Login up failed: " + e.getMessage()));
    }

    public void register(String email, String password, String username, Activity activity, RegisterCallback callback) {
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

    public void addToUserStatistics(String statsJson) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        Gson gson = new Gson();
        DatabaseUser stats = gson.fromJson(statsJson, DatabaseUser.class);
        DocumentReference userDocRef = usersTable.document(user.getUid());
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

    public void logoutUser(LogoutCallback callback) {
        cachedUser = null;
        mAuth.signOut();

        callback.onLogout();
    }

    public static DatabaseUser getCachedUser() {
        return cachedUser;
    }

    public interface LoginCallback {
        void onLoginSuccess(DatabaseUser user);
        void onLoginFailure(String errorMessage);
    }

    public interface RegisterCallback {
        void onRegisterSuccess(String email, String password);
        void onRegisterFailure(String errorMessage);
    }

    public interface LogoutCallback {
        void onLogout();
    }
}
