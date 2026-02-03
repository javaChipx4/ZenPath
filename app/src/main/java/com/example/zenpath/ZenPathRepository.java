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

    // ===== JOURNAL =====
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

        while (c.moveToNext()) {
            String date = c.getString(0);
            String text = c.getString(1);
            list.add(date + " - " + text);
        }

        c.close();
        return list;
    }

    // ===== MOOD =====
    public long addMood(String date, int moodValue, String note) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.M_DATE, date);
        cv.put(ZenPathDbHelper.M_VALUE, moodValue);
        cv.put(ZenPathDbHelper.M_NOTE, note);

        return db.insert(ZenPathDbHelper.T_MOOD, null, cv);
    }

    // ===== STRESS =====
    public long addStress(String date, int level, String suggestion) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.S_DATE, date);
        cv.put(ZenPathDbHelper.S_LEVEL, level);
        cv.put(ZenPathDbHelper.S_SUGGESTION, suggestion);

        return db.insert(ZenPathDbHelper.T_STRESS, null, cv);
    }

    public ArrayList<String> getMoodEntries() {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_MOOD,
                new String[]{ZenPathDbHelper.M_DATE, ZenPathDbHelper.M_VALUE, ZenPathDbHelper.M_NOTE},
                null, null, null, null,
                ZenPathDbHelper.M_DATE + " DESC"
        );

        while (c.moveToNext()) {
            String date = c.getString(0);
            int value = c.getInt(1);
            String note = c.getString(2);
            String moodText = getMoodText(value);
            list.add(date + " - Mood: " + moodText + (note != null && !note.isEmpty() ? " (" + note + ")" : ""));
        }

        c.close();
        return list;
    }

    public ArrayList<String> getStressEntries() {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_STRESS,
                new String[]{ZenPathDbHelper.S_DATE, ZenPathDbHelper.S_LEVEL, ZenPathDbHelper.S_SUGGESTION},
                null, null, null, null,
                ZenPathDbHelper.S_DATE + " DESC"
        );

        while (c.moveToNext()) {
            String date = c.getString(0);
            int level = c.getInt(1);
            String suggestion = c.getString(2);
            list.add(date + " - Stress Level: " + level + "/10" + (suggestion != null && !suggestion.isEmpty() ? " (" + suggestion + ")" : ""));
        }

        c.close();
        return list;
    }

    private String getMoodText(int value) {
        switch (value) {
            case 1: return "Very Sad";
            case 2: return "Sad";
            case 3: return "Neutral";
            case 4: return "Happy";
            case 5: return "Very Happy";
            default: return "Unknown";
        }
    }
}
