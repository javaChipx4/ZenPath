package com.example.zenpath;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GameTimeTracker {

    private static final String PREFS = "zen_path_prefs";

    private long startMs = 0L;
    private final String gameName;

    public GameTimeTracker(String gameName) {
        this.gameName = gameName;
    }

    public void start() {
        startMs = System.currentTimeMillis();
    }

    public void stopAndSave(Context ctx) {
        if (startMs <= 0) return;

        long now = System.currentTimeMillis();
        long dur = Math.max(0, now - startMs);
        startMs = 0;

        // Ignore ultra-tiny sessions (prevents 0-1 second noise)
        if (dur < 1500) return;

        String dateKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(new Date());

        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // ✅ total today
        String totalKey = "play_ms_" + dateKey;
        long total = p.getLong(totalKey, 0L) + dur;

        // ✅ per game today
        String perGameKey = "play_ms_" + sanitize(gameName) + "_" + dateKey;
        long gameTotal = p.getLong(perGameKey, 0L) + dur;

        p.edit()
                .putLong(totalKey, total)
                .putLong(perGameKey, gameTotal)
                // fallback “last played”
                .putString("play_topapp_" + dateKey, gameName)
                .apply();
    }

    // Makes key safe even if game name has spaces
    private String sanitize(String s) {
        if (s == null) return "game";
        return s.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
