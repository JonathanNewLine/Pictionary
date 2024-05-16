package com.example.pictionary;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class MainMenu extends BaseMainActivity{
    /** buttons */

    // creates a new room
    private Button createPrivateRoom;
    // searches for a room
    private ImageView search;
    // searches for a room
    private Button search2;
    // goes to statistics
    private ImageView statistics;
    // goes to settings
    private ImageView settings;

    /** editTexts */
    // input the id for the room
    private EditText searchGameId;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        setButtonListeners();
    }

    /**
     * Creates a private room if the user is connected.
     * If the user is not connected, it shows an alert.
     */
    private void createPrivateRoom(){
        if (DatabaseController.getCachedUser() == null) {
            alert(BaseMainActivity.HAVE_TO_CONNECT_ALERT).show();
            return;
        }
        clientController.createPrivateRoom(this::goToGameActivity);
    }

    /**
     * Joins a private room with the inputted game ID.
     * If the user is not connected or the game ID is empty, it shows an alert.
     */
    private void joinPrivateRoom() {
        String searchedId = searchGameId.getText().toString();
        searchGameId.setText("");

        if (DatabaseController.getCachedUser() == null) {
            alert(BaseMainActivity.HAVE_TO_CONNECT_ALERT).show();
            return;
        }
        if (searchedId.isEmpty()){
            alert(BaseMainActivity.EMPTY_ID_INPUT).show();
            return;
        }

        int gameId = Integer.parseInt(searchedId);
        clientController.joinPrivateRoom(gameId, this::goToGameActivity);
    }

    /**
     * Navigates to the game activity with the given game ID and manager status.
     * If the game ID does not exist, it shows an alert.
     * @param gameId The ID of the game to join.
     * @param isManager Whether the user is the manager of the game.
     */
    private void goToGameActivity(int gameId, boolean isManager) {
        if (gameId == ClientController.ID_DOES_NOT_EXIST) {
            runOnUiThread(() -> alert(BaseMainActivity.ID_DOES_NOT_EXIST).show());
            return;
        }
        if (gameId == ClientController.ID_ALREADY_IN_GAME) {
            runOnUiThread(() -> alert(BaseMainActivity.ALREADY_IN_GAME).show());
            return;
        }
        SoundEffects.playSound(SoundEffects.join_room);
        Intent intent = new Intent(MainMenu.this, WaitingRoom.class);
        intent.putExtra("gameId", gameId);
        intent.putExtra("isManager", isManager);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clientController.updateUserLoggedOut();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DatabaseController.getCachedUser() == null) {
            updateToLogoutInterface();
        }
    }

    /**
     * Sets the listeners for the buttons in the activity.
     */
    private void setButtonListeners() {
        createPrivateRoom = findViewById(R.id.create_private_room);
        search = findViewById(R.id.search);
        statistics = findViewById(R.id.statistics);
        settings = findViewById(R.id.settings);
        search2 = findViewById(R.id.search2);
        searchGameId = findViewById(R.id.search_game_id);

        createPrivateRoom.setOnClickListener(v -> createPrivateRoom());
        search.setOnClickListener(v -> joinPrivateRoom());
        search2.setOnClickListener(v -> joinPrivateRoom());
        statistics.setOnClickListener(v -> goToStatisticsActivity(this));
        settings.setOnClickListener(v -> goToSettingsActivity(this));
    }
}
