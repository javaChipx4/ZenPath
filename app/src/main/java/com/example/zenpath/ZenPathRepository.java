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

    // ✅ UPSERT: One mood per day (update if exists, insert if not)
    public void saveMood(String date, int moodValue, String note) {
        SQLiteDatabase db = helper.getWritableDatabase();

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

    // ===== STRESS =====
    public long addStress(String date, int level, String suggestion) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.S_DATE, date);
        cv.put(ZenPathDbHelper.S_LEVEL, level);
        cv.put(ZenPathDbHelper.S_SUGGESTION, suggestion);

        return db.insert(ZenPathDbHelper.T_STRESS, null, cv);
    }
}
