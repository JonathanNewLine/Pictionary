package com.example.pictionary;


import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.SoundPool;

public class SoundEffects {

    private static SoundPool soundPool;
    private static float volume;
    public static int correct;
    public static int wrong;
    public static int next;
    public static int winner;
    public static int join_room;
    public static int start;

    public static void init(Context context) {
        soundPool = new SoundPool.Builder().setMaxStreams(20).build();
        updateVolume(getVolumeFromSharedPreferences(context));
        loadSounds(context);
    }

    private static void loadSounds(Context context) {
        correct = soundPool.load(context, R.raw.correct, 1);
        wrong = soundPool.load(context, R.raw.wrong, 1);
        next = soundPool.load(context, R.raw.next, 1);
        winner = soundPool.load(context, R.raw.winner, 1);
        join_room = soundPool.load(context, R.raw.join_room, 1);
        start = soundPool.load(context, R.raw.start, 1);
    }

    public static void updateVolume(int volumePercent) {
        volume = ((float)volumePercent)/100;
    }

    public static void playSound(int id)
    {
        soundPool.play(id,volume,volume,1,0,1);
    }

    private static int getVolumeFromSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppSettings", MODE_PRIVATE);
        return sharedPreferences.getInt("soundEffectsVolume", 50);
    }

}
