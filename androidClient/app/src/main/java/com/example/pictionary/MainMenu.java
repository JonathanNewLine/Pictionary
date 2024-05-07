package com.example.pictionary;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainMenu extends BaseMainActivity{
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        client = Client.getInstance();

        findViewById(R.id.create_private_room).setOnClickListener(this::createPrivateRoom);
        findViewById(R.id.search).setOnClickListener(this::joinPrivateRoom);
        findViewById(R.id.statistics).setOnClickListener(v -> goToStatisticsActivity(this));
        findViewById(R.id.settings).setOnClickListener(v -> goToSettingsActivity(this));
    }

    private void createPrivateRoom(View v){
        client.sendMessage("new room");
        if (cachedUser == null) {
            alert("Mannnnnnn You just have to connect as a user").show();
            return;
        }

        SoundEffects.playSound(SoundEffects.join_room);
        client.receiveMessage().thenAccept(response -> {
            int gameId = Integer.parseInt(response);
            intent = new Intent(MainMenu.this, WaitingRoom.class);
            intent.putExtra("gameId", gameId);
            intent.putExtra("isManager", true);
            startActivity(intent);
        });
    }

    private void joinPrivateRoom(View v) {
        EditText idInput = findViewById(R.id.search_game_id);
        if (cachedUser == null) {
            alert("Mannnnnnn You just have to connect as a user").show();
            return;
        }
        if (idInput.getText().toString().isEmpty()){
            alert("Boy you can't just expect me to guess this shit").show();
            return;
        }

        int gameId = Integer.parseInt(idInput.getText().toString());
        client.sendMessage("find room " + gameId);
        client.receiveMessage().thenAccept(response -> runOnUiThread(() -> {
            if (response.equals("no")) {
                alert("Damn bro, real good id, shame it doesn't fucking exist").show();
            } else if (response.equals("yes")) {
                SoundEffects.playSound(SoundEffects.join_room);
                Intent intent = new Intent(MainMenu.this, WaitingRoom.class);
                intent.putExtra("gameId", gameId);
                intent.putExtra("isManager", false);
                startActivity(intent);
            }
        }));
        idInput.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        client.closeSocket();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cachedUser == null) {
            updateToLogoutInterface();
        }
    }
}
