package com.example.pictionary;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.SoundPool;

/**
 * This class manages the sound effects in the application.
 */
public class SoundEffects {
    // SoundPool object to play sound effects
    private static SoundPool soundPool;
    // Volume of the sound effects
    private static float volume;
    // IDs of the sound effects
    public static int correct;
    public static int wrong;
    public static int next;
    public static int winner;
    public static int join_room;
    public static int start;
    public static int loser;

    /**
     * Initializes the sound effects.
     * @param context The current context.
     */
    public static void init(Context context) {
        soundPool = new SoundPool.Builder().setMaxStreams(20).build();
        updateVolume(getVolumeFromSharedPreferences(context));
        loadSounds(context);
    }

    /**
     * Loads the sound effects into the SoundPool.
     * @param context The current context.
     */
    private static void loadSounds(Context context) {
        correct = soundPool.load(context, R.raw.correct, 1);
        wrong = soundPool.load(context, R.raw.wrong, 1);
        next = soundPool.load(context, R.raw.next, 1);
        winner = soundPool.load(context, R.raw.winner, 1);
        join_room = soundPool.load(context, R.raw.join_room, 1);
        start = soundPool.load(context, R.raw.start, 1);
        loser = soundPool.load(context, R.raw.loser, 1);
    }

    /**
     * Updates the volume of the sound effects.
     * @param volumePercent The new volume, as a percentage.
     */
    public static void updateVolume(int volumePercent) {
        volume = ((float)volumePercent)/100;
    }

    /**
     * Plays a specific sound effect.
     * @param id The ID of the sound effect to play.
     */
    public static void playSound(int id)
    {
        soundPool.play(id,volume,volume,1,0,1);
    }

    /**
     * Gets the volume from the shared preferences.
     * @param context The current context.
     * @return The volume, as a percentage.
     */
    private static int getVolumeFromSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppSettings", MODE_PRIVATE);
        return sharedPreferences.getInt("soundEffectsVolume", 50);
    }

}