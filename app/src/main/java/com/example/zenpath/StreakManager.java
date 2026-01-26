package com.example.zenpath;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class StreakManager {

    private static final String PREFS = "streak_prefs";
    private static final String KEY_STREAK = "streak_count";
    private static final String KEY_LAST_DONE = "last_done_yyyymmdd";

    private StreakManager() {}

    public static int getStreak(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getInt(KEY_STREAK, 0);
    }

    // Call when a session is completed (cleared all stars)
    public static int recordCompletion(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String today = yyyymmdd(0);
        String yesterday = yyyymmdd(-1);

        String last = sp.getString(KEY_LAST_DONE, "");
        int streak = sp.getInt(KEY_STREAK, 0);

        if (today.equals(last)) {
            // already counted today
            return streak;
        }

        if (yesterday.equals(last)) {
            streak = streak + 1; // continue streak
        } else {
            streak = 1; // reset streak
        }

        sp.edit()
                .putString(KEY_LAST_DONE, today)
                .putInt(KEY_STREAK, streak)
                .apply();

        return streak;
    }

    private static String yyyymmdd(int dayOffset) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, dayOffset);
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(cal.getTime());
    }
}
