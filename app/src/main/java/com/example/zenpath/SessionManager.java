package com.example.zenpath;

import android.content.Context;

public class SessionManager {
    private static final String PREFS = "zen_path_prefs";
    private static final String KEY_CURRENT_USER = "current_user";

    public static void setCurrentUser(Context ctx, String username) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENT_USER, username)
                .apply();
    }

    public static String getCurrentUser(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_CURRENT_USER, null);
    }

    public static void clearCurrentUser(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_CURRENT_USER)
                .apply();
    }
}
