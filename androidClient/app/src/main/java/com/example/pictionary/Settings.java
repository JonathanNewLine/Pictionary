package com.example.pictionary;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;

/**
 * This class represents the settings screen of the application.
 */
public class Settings extends BaseMainActivity {
    /** seekbars */
    private SeekBar musicSeekBar;
    private SeekBar sfxSeekBar;

    /** volumes */
    // volume for music
    private int musicVolume;
    // volume for sound effects
    private int soundEffectsVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getViewsById();

        // load the volume settings
        loadVolumeSettings();

        // set the seekbar listeners
        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateMusicVolume(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // set the seekbar listeners
        sfxSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateSoundEffectsVolume(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    /**
     * Loads the volume settings from the shared preferences.
     */
    private void loadVolumeSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        musicVolume = sharedPreferences.getInt("musicVolume", 50);
        soundEffectsVolume = sharedPreferences.getInt("soundEffectsVolume", 50);
        musicSeekBar.setProgress(musicVolume);
        sfxSeekBar.setProgress(soundEffectsVolume);
    }

    /**
     * Gets the views by their ID.
     */
    private void getViewsById() {
        musicSeekBar = findViewById(R.id.music_seekbar);
        sfxSeekBar = findViewById(R.id.sound_effects_seekbar);
    }

    /**
     * Updates the music volume in the shared preferences.
     * @param progress The new volume, as a percentage.
     */
    private void updateMusicVolume(int progress) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("musicVolume", progress);
        editor.apply();
    }

    /**
     * Updates the sound effects volume in the shared preferences and in the SoundEffects class.
     * @param progress The new volume, as a percentage.
     */
    private void updateSoundEffectsVolume(int progress) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("soundEffectsVolume", progress);
        editor.apply();

        SoundEffects.updateVolume(progress);
    }

    /**
     * Navigates to the settings activity.
     * @param activity The current activity.
     */
    @Override
    public void goToSettingsActivity(Activity activity) {
        super.goToSettingsActivity(activity);
        finish();
    }

    /**
     * Navigates to the statistics activity.
     * @param activity The current activity.
     */
    @Override
    public void goToStatisticsActivity(Activity activity) {
        super.goToStatisticsActivity(activity);
        finish();
    }
}