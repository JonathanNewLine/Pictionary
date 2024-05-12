package com.example.pictionary;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

/**
 * This abstract class represents the base game activity of the application.
 */
public abstract class BaseGameActivity extends AppCompatActivity {
    /** error messages */
    public static final String TOO_FEW_PLAYERS_IN_ROOM = "Too few people in room, need at least two";
    public static final String TOO_MANY_PLAYERS_IN_ROOM = "Too many people in room, can host at most six";

    /** constants */
    // min and max players in room
    public static final int MIN_PLAYERS_IN_ROOM = 2;
    public static final int MAX_PLAYERS_IN_ROOM = 6;
    // time constants
    public static final int ONE_SECOND_IN_MILLIS = 1000;
    public static final int MILLIS_IN_HOUR = 3600000;
    public static final int MILLIS_IN_MINUTE = 60000;
    public static final int MILLIS_IN_SECOND = 1000;
    // back button constants
    public static final int DOUBLE_BACK_PRESS_INTERVAL = 1000;
    public static final int COOL_DOWN_SECONDS = 3;

    /** side bar of users */
    private View usersSideBar;
    private ListView usersListView;

    /** textViews */
    // logged in as
    private TextView loggedAs;

    /** buttons */
    // exit button
    private ImageView exit;
    // open users side bar button
    private ImageView openUsersSideBar;

    /** controllers */
    // database controller
    protected DatabaseController databaseController;
    // client controller
    protected ClientController clientController;

    /** adapters */
    // adapter for users
    private UserAdapter userAdapter;

    /** game intent data */
    // game id
    protected int gameId;
    // is manager
    protected boolean isManager;

    /** handlers */
    // handler for receiving messages
    private Handler receiveMessageHandler;

    /** other */
    // back button count
    private int backButtonCount = 0;
    // flag to check if onStart is called already
    private boolean isOnStartCalled = false;
    // flag to continue listening
    private boolean continueListening = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clientController = ClientController.getInstance();
        databaseController = DatabaseController.getInstance();
    }


    @SuppressLint("SetTextI18n")
    @Override
    protected void onStart() {
        super.onStart();
        // check if onStart is called already
        if (isOnStartCalled) {
            return;
        }
        // initialize everything
        SoundEffects.init(this);
        setButtonListenersAndAdapters();
        getIntentData();
        updateLoggedAs();
        overrideBackButton();

        // get message handler
        receiveMessageHandler = getMessageHandler();
        listenForServer();

        isOnStartCalled = true;
    }

    /**
     * Exits the current room and finishes the activity.
     */
    public void exit() {
        clientController.exitRoom();
        finish();
    }

    /**
     * Shows or hides the users side bar.
     */
    public void showHideUsersSideBar() {
        if(usersSideBar.getVisibility() == View.VISIBLE) {
            usersSideBar.setVisibility(View.GONE);
        } else {
            usersSideBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Updates the users side bar with the given users JSON.
     * @param usersJson The JSON string of the users.
     * @return The count of the users.
     */
    public int updateUsersSideBar(String usersJson) {
        ArrayList<User> usersConnectedToRoom = getUsersList(usersJson);
        userAdapter.clear();
        userAdapter.addAll(usersConnectedToRoom);
        return userAdapter.getCount();
    }

    /**
     * Gets the list of users from the given message.
     * @param message The message containing the users.
     * @return The list of users.
     */
    private ArrayList<User> getUsersList(String message) {
        ArrayList<User> connectedUsers = new ArrayList<>();
        Gson gson = new Gson();
        ArrayList<User> userList = gson.fromJson(message, new TypeToken<ArrayList<User>>(){}.getType());
        if (userList != null) {
            connectedUsers.addAll(userList);
        }
        return connectedUsers;
    }

    /**
     * Creates an alert with the given message.
     * @param message The message of the alert.
     * @return The alert builder.
     */
    public AlertDialog.Builder alert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", null);
        return builder;
    }

    /**
     * Sets the button listeners and adapters.
     */
    private void setButtonListenersAndAdapters() {
        loggedAs = findViewById(R.id.logged_as_game);
        exit = findViewById(R.id.exit);
        openUsersSideBar = findViewById(R.id.open_users_side_bar);
        usersSideBar = findViewById(R.id.users_side_bar);
        usersListView = findViewById(R.id.side_users_list_view);

        userAdapter = new UserAdapter(this, 0, 0);
        usersListView.setAdapter(userAdapter);

        exit.setOnClickListener(v -> exit());
        openUsersSideBar.setOnClickListener(v -> showHideUsersSideBar());
    }

    /**
     * Updates the logged as text view.
     */
    @SuppressLint("SetTextI18n")
    private void updateLoggedAs() {
        String username = DatabaseController.getCachedUser().getUsername();
        loggedAs.setText("Logged in as:\n" + username);
    }

    /**
     * Gets the number of users in the room.
     * @return The number of users in the room.
     */
    protected int getNumUsersInRoom() {
        return userAdapter.getCount();
    }

    /**
     * Gets the intent data.
     */
    protected void getIntentData() {
        gameId = getIntent().getIntExtra("gameId", -1);
        isManager = getIntent().getBooleanExtra("isManager", false);
    }

    /**
     * Overrides the back button.
     */
    private void overrideBackButton() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            // if the back button is pressed twice within the DOUBLE_BACK_PRESS_INTERVAL time, exit the activity
            @Override
            public void handleOnBackPressed() {
                if (backButtonCount >= 1) {
                    exit();
                    return;
                }

                Toast.makeText(BaseGameActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                backButtonCount++;
                // reset the back button count after DOUBLE_BACK_PRESS_INTERVAL time
                new Handler().postDelayed(() -> backButtonCount = 0, DOUBLE_BACK_PRESS_INTERVAL);
            }

        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    /**
     * Listens for the server.
     */
    private void listenForServer() {
        Thread thread = new Thread(() -> {
            while (continueListening) {
                try {
                    continueListening = clientController.processSingleResponse(receiveMessageHandler);
                } catch (Exception ignored) {
                }
            }
        });
        thread.start();
    }

    /**
     * Updates the manager status.
     * @param isManager The manager status.
     */
    protected void updateIsManager(boolean isManager) {
        this.isManager = isManager;
    }

    /**
     * Gets the message handler.
     * @return The message handler.
     */
    public abstract Handler getMessageHandler();
}