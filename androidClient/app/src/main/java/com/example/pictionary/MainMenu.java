package com.example.pictionary;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class MainMenu extends BaseMainActivity{
    // buttons
    private Button createPrivateRoom;
    private ImageView search;
    private ImageView statistics;
    private ImageView settings;

    // editTexts
    private EditText searchGameId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        setButtonListeners();
    }

    private void createPrivateRoom(){
        if (DatabaseController.getCachedUser() == null) {
            alert(BaseMainActivity.HAVE_TO_CONNECT_ALERT).show();
            return;
        }
        clientController.createPrivateRoom(this::goToGameActivity);
    }

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

    private void goToGameActivity(int gameId, boolean isManager) {
        if (gameId == ClientController.ID_DOES_NOT_EXIST) {
            runOnUiThread(() -> alert(BaseMainActivity.ID_DOES_NOT_EXIST).show());
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

    private void setButtonListeners() {
        createPrivateRoom = findViewById(R.id.create_private_room);
        search = findViewById(R.id.search);
        statistics = findViewById(R.id.statistics);
        settings = findViewById(R.id.settings);
        searchGameId = findViewById(R.id.search_game_id);

        createPrivateRoom.setOnClickListener(v -> createPrivateRoom());
        search.setOnClickListener(v -> joinPrivateRoom());
        statistics.setOnClickListener(v -> goToStatisticsActivity(this));
        settings.setOnClickListener(v -> goToSettingsActivity(this));
    }
}
