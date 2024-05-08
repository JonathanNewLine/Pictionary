package com.example.pictionary;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;

public class Settings extends BaseMainActivity {
    // seekbars
    private SeekBar musicSeekBar;
    private SeekBar sfxSeekBar;

    // volumes
    private int musicVolume;
    private int soundEffectsVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getViewsById();

        loadVolumeSettings();

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

    private void loadVolumeSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        musicVolume = sharedPreferences.getInt("musicVolume", 50);
        soundEffectsVolume = sharedPreferences.getInt("soundEffectsVolume", 50);
        musicSeekBar.setProgress(musicVolume);
        sfxSeekBar.setProgress(soundEffectsVolume);
    }

    private void getViewsById() {
        musicSeekBar = findViewById(R.id.music_seekbar);
        sfxSeekBar = findViewById(R.id.sound_effects_seekbar);
    }

    private void updateMusicVolume(int progress) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("musicVolume", progress);
        editor.apply();
    }

    private void updateSoundEffectsVolume(int progress) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("soundEffectsVolume", progress);
        editor.apply();

        SoundEffects.updateVolume(progress);
    }

    @Override
    public void goToSettingsActivity(Activity activity) {
        super.goToSettingsActivity(activity);
        finish();
    }

    @Override
    public void goToStatisticsActivity(Activity activity) {
        super.goToStatisticsActivity(activity);
        finish();
    }
}
