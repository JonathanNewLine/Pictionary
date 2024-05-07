package com.example.pictionary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

public class BaseGameActivity extends AppCompatActivity {
    protected FirebaseAuth mAuth = FirebaseAuth.getInstance();
    protected static DatabaseUser cachedUser = null;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onStart() {
        super.onStart();

        SoundEffects.init(this);

        TextView loggedAs = findViewById(R.id.logged_as_game);
        user = mAuth.getCurrentUser();

        if (user == null) {

            loggedAs.setText("Logged in as:\nguest");
        }

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        CollectionReference usersTable = database.collection("users");
        assert user != null;
        usersTable.document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            DatabaseUser databaseUser = documentSnapshot.toObject(DatabaseUser.class);
            assert databaseUser != null;
            cachedUser = databaseUser;
            loggedAs.setText("Logged in as:\n" + databaseUser.getUsername());
        });
    }

    public void addToUserStatistics(String statsJson) {
        if (user != null) {
            Gson gson = new Gson();
            DatabaseUser stats = gson.fromJson(statsJson, DatabaseUser.class);

            DocumentReference userDocRef = FirebaseFirestore.getInstance().collection("users").document(user.getUid());
            userDocRef.update("guesses", cachedUser.getGuesses() + stats.getGuesses(),
                            "correctGuesses",cachedUser.getCorrectGuesses() + stats.getCorrectGuesses(),
                            "gamesPlayed", cachedUser.getGamesPlayed() + stats.getGamesPlayed(),
                            "gamesWon", cachedUser.getGamesWon() + stats.getGamesWon())
                    .addOnSuccessListener(aVoid -> Log.d("statistics", "DocumentSnapshot successfully updated!"))
                    .addOnFailureListener(e -> Log.w("statistics", "Error updating document", e));
        } else {
            Log.d("statistics", "No user is signed in.");
        }
    }

    public void exit(Activity activity, Client client) {
        client.exitRoom();
        activity.finish();
    }

    public void showHideUsersSideBar(View sideBar) {
        if(sideBar.getVisibility() == View.VISIBLE) {
            sideBar.setVisibility(View.GONE);
        } else {
            sideBar.setVisibility(View.VISIBLE);
        }
    }

    public int updateUsersSideBar(String format, UserAdapter userAdapter) {
        ArrayList<User> usersConnectedToRoom = getUsersList(format.split("users: ")[1]);
        userAdapter.clear();
        userAdapter.addAll(usersConnectedToRoom);
        return userAdapter.getCount();
    }

    private ArrayList<User> getUsersList(String message) {
        ArrayList<User> connectedUsers = new ArrayList<>();
        Gson gson = new Gson();
        ArrayList<User> userList = gson.fromJson(message, new TypeToken<ArrayList<User>>(){}.getType());
        if (userList != null) {
            connectedUsers.addAll(userList);
        }
        return connectedUsers;
    }

    public AlertDialog.Builder alert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", null);
        return builder;
    }
}