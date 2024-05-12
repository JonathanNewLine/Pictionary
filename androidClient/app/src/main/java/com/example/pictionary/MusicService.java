package com.example.pictionary;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.IBinder;

/**
 * This class represents a service that plays music in the application.
 */
public class MusicService extends Service {
    // MediaPlayer object that plays the music.
    MediaPlayer player;

    /**
     * Constructor for the MusicService class.
     */
    public MusicService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Initializes the MediaPlayer and sets the volume.
     */
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
            // if command is to start
            if (intent.hasCategory("start")) {
                updateVolume(getVolumeFromSharedPreferences());
                player.seekTo(0);
                player.start();
            }
            // if command is to pause
            else if (intent.hasCategory("pause")) {
                player.pause();
            }
        }

        return START_STICKY;
    }

    /**
     * Updates the volume of the music.
     * @param volume The new volume, as a percentage.
     */
    public void updateVolume(int volume) {
        player.setVolume(volume, volume);
    }

    /**
     * Gets the volume from the shared preferences.
     * @return The volume, as a percentage.
     */
    private int getVolumeFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        return sharedPreferences.getInt("musicVolume", 50);
    }
}