package com.example.pictionary;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.IBinder;

public class MusicService extends Service {
    MediaPlayer player;

    public MusicService() {

    }
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = MediaPlayer.create(this, R.raw.song);
        player.setLooping(true);

        int volume = getVolumeFromSharedPreferences();
        updateVolume(volume);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasCategory("start")) {
                updateVolume(getVolumeFromSharedPreferences());
                player.seekTo(0);
                player.start();
            }
            else if (intent.hasCategory("pause")) {
                player.pause();
            }
        }

        return START_STICKY;
    }

    public void updateVolume(int volume) {
        player.setVolume(volume, volume);
    }

    private int getVolumeFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        return sharedPreferences.getInt("musicVolume", 50);
    }
}