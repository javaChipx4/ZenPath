package com.example.zenpath;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class MusicController {

    private static final String PREFS = "zenpath_music";
    private static final String KEY_VOL = "music_volume"; // float 0..1

    // ===================== PLAY TRACK =====================
    public static void play(Context ctx, int track) {
        if (ctx == null) return;
        Intent i = new Intent(ctx, MusicService.class);
        i.setAction(MusicService.ACTION_PLAY);
        i.putExtra(MusicService.EXTRA_TRACK, track);
        ctx.startService(i);
    }

    // ===================== STOP MUSIC (optional) =====================
    public static void stop(Context ctx) {
        if (ctx == null) return;
        Intent i = new Intent(ctx, MusicService.class);
        i.setAction(MusicService.ACTION_STOP);
        ctx.startService(i);
    }

    // ===================== VOLUME API USED BY YOUR SEEKBARS =====================
    public static int getVolumePercent(Context ctx) {
        float v = loadVolume(ctx);
        return Math.round(v * 100f);
    }

    public static void setVolumePercent(Context ctx, int percent) {
        if (ctx == null) return;

        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;

        float v = percent / 100f;

        // 1) Save
        saveVolume(ctx, v);

        // 2) Apply immediately to currently playing track
        Intent i = new Intent(ctx, MusicService.class);
        i.setAction(MusicService.ACTION_SET_VOLUME);
        i.putExtra(MusicService.EXTRA_VOLUME, v);
        ctx.startService(i);
    }

    // ===================== INTERNAL SAVE/LOAD =====================
    public static void saveVolume(Context ctx, float v) {
        if (ctx == null) return;
        if (v < 0f) v = 0f;
        if (v > 1f) v = 1f;

        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putFloat(KEY_VOL, v).apply();
    }

    public static float loadVolume(Context ctx) {
        if (ctx == null) return 0.6f;
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getFloat(KEY_VOL, 0.6f);
    }
}
