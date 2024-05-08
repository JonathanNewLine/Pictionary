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

public abstract class BaseGameActivity extends AppCompatActivity {
    // alert constants
    public static final String TOO_FEW_PLAYERS_IN_ROOM = "Too few people in room, need at least two";
    public static final String TOO_MANY_PLAYERS_IN_ROOM = "Too many people in room, can host at most six";

    // constants
    public static final int MIN_PLAYERS_IN_ROOM = 2;
    public static final int MAX_PLAYERS_IN_ROOM = 6;
    public static final int ONE_SECOND_IN_MILLIS = 1000;
    public static final int MILLIS_IN_HOUR = 3600000;
    public static final int MILLIS_IN_MINUTE = 60000;
    public static final int MILLIS_IN_SECOND = 1000;
    public static final int DOUBLE_BACK_PRESS_INTERVAL = 1000;
    public static final int COOL_DOWN_SECONDS = 3;

    // views
    private View usersSideBar;
    private ListView usersListView;

    // textViews
    private TextView loggedAs;

    // buttons
    private ImageView exit;
    private ImageView openUsersSideBar;

    // controllers
    protected DatabaseController databaseController;
    protected ClientController clientController;

    // adapters
    private UserAdapter userAdapter;

    // game intent data
    protected int gameId;
    protected boolean isManager;

    // other
    private int backButtonCount = 0;

    // handler for receiving messages
    private boolean continueListening = true;
    private Handler receiveMessageHandler;


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
        SoundEffects.init(this);
        setButtonListenersAndAdapters();
        getIntentData();
        updateLoggedAs();
        overrideBackButton();

        receiveMessageHandler = getMessageHandler();
        listenForServer();
    }

    public void exit() {
        clientController.exitRoom();
        finish();
    }

    public void showHideUsersSideBar() {
        if(usersSideBar.getVisibility() == View.VISIBLE) {
            usersSideBar.setVisibility(View.GONE);
        } else {
            usersSideBar.setVisibility(View.VISIBLE);
        }
    }

    public int updateUsersSideBar(String usersJson) {
        ArrayList<User> usersConnectedToRoom = getUsersList(usersJson);
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

    @SuppressLint("SetTextI18n")
    private void updateLoggedAs() {
        String username = DatabaseController.getCachedUser().getUsername();
        loggedAs.setText("Logged in as:\n" + username);
    }

    protected int getNumUsersInRoom() {
        return userAdapter.getCount();
    }

    protected void getIntentData() {
        gameId = getIntent().getIntExtra("gameId", -1);
        isManager = getIntent().getBooleanExtra("isManager", false);
    }

    private void overrideBackButton() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (backButtonCount >= 1) {
                    exit();
                    return;
                }

                Toast.makeText(BaseGameActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                backButtonCount++;
                new Handler().postDelayed(() -> backButtonCount = 0, DOUBLE_BACK_PRESS_INTERVAL);
            }

        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

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

    protected void updateIsManager(boolean isManager) {
        this.isManager = isManager;
    }

    public abstract Handler getMessageHandler();
}