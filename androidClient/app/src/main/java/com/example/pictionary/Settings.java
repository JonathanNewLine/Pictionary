package com.example.pictionary;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;

public class Settings extends BaseMainActivity {
    private int musicVolume;
    private int soundEffectsVolume;
    private SeekBar musicSeekBar;
    private SeekBar sfxSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.exit).setOnClickListener(v -> finish());
        findViewById(R.id.statistics).setOnClickListener(v -> {
            finish();
            goToStatisticsActivity(this);
        });
        findViewById(R.id.settings).setOnClickListener(v -> {
            finish();
            goToSettingsActivity(this);
        });
        musicSeekBar = findViewById(R.id.music_seekbar);
        sfxSeekBar = findViewById(R.id.sound_effects_seekbar);

        loadVolumeSettings();

        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("musicVolume", progress);
                    editor.apply();
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
                    SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("soundEffectsVolume", progress);
                    editor.apply();

                    SoundEffects.updateVolume(progress);
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
}
