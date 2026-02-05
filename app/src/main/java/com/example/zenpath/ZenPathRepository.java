package com.example.zenpath;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class ZenPathRepository {

    private final ZenPathDbHelper helper;

    public ZenPathRepository(Context context) {
        helper = new ZenPathDbHelper(context);
    }

    // ===================== JOURNAL =====================

    public long addJournalEntry(String date, String text) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.J_DATE, date);
        cv.put(ZenPathDbHelper.J_TEXT, text);
        cv.put(ZenPathDbHelper.J_CREATED_AT, System.currentTimeMillis());

        return db.insert(ZenPathDbHelper.T_JOURNAL, null, cv);
    }

    public ArrayList<String> getJournalEntries() {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_JOURNAL,
                new String[]{ZenPathDbHelper.J_DATE, ZenPathDbHelper.J_TEXT},
                null, null, null, null,
                ZenPathDbHelper.J_CREATED_AT + " DESC"
        );

        try {
            while (c.moveToNext()) {
                String date = c.getString(0);
                String text = c.getString(1);
                list.add(date + " - " + text);
            }
        } finally {
            c.close();
        }

        return list;
    }

    // ===================== MOOD =====================

    // ✅ UPSERT: One mood per day (update if exists, insert if not)
    public void saveMood(String date, int moodValue, String note) {
        SQLiteDatabase db = helper.getWritableDatabase();

        if (note == null) note = "";

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.M_DATE, date);
        cv.put(ZenPathDbHelper.M_VALUE, moodValue);
        cv.put(ZenPathDbHelper.M_NOTE, note);

        int rows = db.update(
                ZenPathDbHelper.T_MOOD,
                cv,
                ZenPathDbHelper.M_DATE + "=?",
                new String[]{date}
        );

        if (rows == 0) {
            db.insert(ZenPathDbHelper.T_MOOD, null, cv);
        }
    }

    // ✅ READ: Get mood + note for a specific date (yyyy-MM-dd)
    public MoodRecord getMoodByDate(String date) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_MOOD,
                new String[]{ZenPathDbHelper.M_VALUE, ZenPathDbHelper.M_NOTE},
                ZenPathDbHelper.M_DATE + "=?",
                new String[]{date},
                null, null,
                ZenPathDbHelper.M_ID + " DESC",
                "1"
        );

        MoodRecord record = null;

        try {
            if (c.moveToFirst()) {
                int moodValue = c.getInt(0);
                String note = c.getString(1);
                if (note == null) note = "";
                record = new MoodRecord(date, moodValue, note);
            }
        } finally {
            c.close();
        }

        return record;
    }

    // ===================== STRESS =====================

    /**
     * ✅ IMPORTANT:
     * Your UI shows 0..100% (progress bar max=100).
     * So store stress_level as 0..100 for consistency.
     */

    // ✅ UPSERT: One stress per day (update if exists, insert if not)
    public void saveStress(String date, int level, String suggestion) {
        SQLiteDatabase db = helper.getWritableDatabase();

        if (suggestion == null) suggestion = "";

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.S_DATE, date);
        cv.put(ZenPathDbHelper.S_LEVEL, level);
        cv.put(ZenPathDbHelper.S_SUGGESTION, suggestion);

        int rows = db.update(
                ZenPathDbHelper.T_STRESS,
                cv,
                ZenPathDbHelper.S_DATE + "=?",
                new String[]{date}
        );

        if (rows == 0) {
            db.insert(ZenPathDbHelper.T_STRESS, null, cv);
        }
    }

    // Optional: keep your old insert method (but avoid using it in the app)
    public long addStress(String date, int level, String suggestion) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.S_DATE, date);
        cv.put(ZenPathDbHelper.S_LEVEL, level);
        cv.put(ZenPathDbHelper.S_SUGGESTION, suggestion);

        return db.insert(ZenPathDbHelper.T_STRESS, null, cv);
    }

    // ✅ Container class for reading stress row
    public static class StressRecord {
        public final String date;
        public final int level; // 0..100
        public final String suggestion;

        public StressRecord(String date, int level, String suggestion) {
            this.date = date;
            this.level = level;
            this.suggestion = suggestion;
        }
    }

    // ✅ READ: Get stress for a specific date (yyyy-MM-dd)
    public StressRecord getStressByDate(String date) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_STRESS,
                new String[]{ZenPathDbHelper.S_LEVEL, ZenPathDbHelper.S_SUGGESTION},
                ZenPathDbHelper.S_DATE + "=?",
                new String[]{date},
                null, null,
                ZenPathDbHelper.S_ID + " DESC",
                "1"
        );

        StressRecord record = null;

        try {
            if (c.moveToFirst()) {
                int level = c.getInt(0);
                String suggestion = c.getString(1);
                if (suggestion == null) suggestion = "";
                record = new StressRecord(date, level, suggestion);
            }
        } finally {
            c.close();
        }

        return record;
    }

    // ===================== GAMES PLAYED =====================

    public static class GamePlayRecord {
        public final String date;
        public final String gameName;

        public GamePlayRecord(String date, String gameName) {
            this.date = date;
            this.gameName = gameName;
        }
    }

    // ✅ Save when a game is played/opened
    public void addGamePlayed(String date, String gameName) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.G_DATE, date);
        cv.put(ZenPathDbHelper.G_NAME, gameName);
        cv.put(ZenPathDbHelper.G_CREATED_AT, System.currentTimeMillis());

        db.insert(ZenPathDbHelper.T_GAMES, null, cv);
    }

    // ✅ Read last 3 games for a specific date (yyyy-MM-dd)
    public ArrayList<GamePlayRecord> getLast3GamesByDate(String date) {
        ArrayList<GamePlayRecord> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_GAMES,
                new String[]{ZenPathDbHelper.G_DATE, ZenPathDbHelper.G_NAME},
                ZenPathDbHelper.G_DATE + "=?",
                new String[]{date},
                null, null,
                ZenPathDbHelper.G_CREATED_AT + " DESC",
                "3"
        );

        try {
            while (c.moveToNext()) {
                String d = c.getString(0);
                String g = c.getString(1);
                list.add(new GamePlayRecord(d, g));
            }
        } finally {
            c.close();
        }

        return list;
    }

}
