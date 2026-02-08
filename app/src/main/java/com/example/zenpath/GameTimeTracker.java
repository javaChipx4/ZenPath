package com.example.zenpath;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GameTimeTracker {

    private static final String PREFS = "zen_path_prefs";
    private static final String KEY_CURRENT_USER = "current_user"; // userId

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
        long durMs = Math.max(0, now - startMs);
        startMs = 0;

        // Ignore tiny sessions
        if (durMs < 1500) return;

        int seconds = (int) (durMs / 1000);

        String dateKey = new SimpleDateFormat(
                "yyyyMMdd",
                Locale.getDefault()
        ).format(new Date());

        long userId = currentUserId(ctx);
        if (userId <= 0) return;

        ZenPathRepository repo = new ZenPathRepository(ctx);

        // Map game → DB column
        String gameKey;
        if ("Star Sweep".equalsIgnoreCase(gameName)) {
            gameKey = "STAR_SWEEP";
        } else if ("Lantern Release".equalsIgnoreCase(gameName)) {
            gameKey = "LANTERN_RELEASE";
        } else if ("Planet".equalsIgnoreCase(gameName)) {
            gameKey = "PLANET";
        } else {
            return;
        }

        // ✅ Increment playtime safely in DB
        repo.addGamePlaytime(userId, dateKey, gameKey, seconds);
    }

    private long currentUserId(Context ctx) {
        SharedPreferences prefs =
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String s = prefs.getString(KEY_CURRENT_USER, null);
        if (s == null) return -1;
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return -1;
        }
    }
}
