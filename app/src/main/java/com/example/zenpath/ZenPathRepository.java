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

    public boolean hasJournalEntry(String date) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_JOURNAL,
                new String[]{ ZenPathDbHelper.J_DATE },
                ZenPathDbHelper.J_DATE + "=?",
                new String[]{ date },
                null, null, null,
                "1"
        );

        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    public int updateJournalEntry(String date, String text) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.J_TEXT, text);
        cv.put(ZenPathDbHelper.J_CREATED_AT, System.currentTimeMillis());

        return db.update(
                ZenPathDbHelper.T_JOURNAL,
                cv,
                ZenPathDbHelper.J_DATE + "=?",
                new String[]{ date }
        );
    }

    // ✅ FIXED UPSERT — no redline, no ambiguity
    public long upsertJournalEntry(String date, String text) {
        if (hasJournalEntry(date)) {
            updateJournalEntry(date, text);
            return 1L;
        } else {
            return addJournalEntry(date, text);
        }
    }

    // Get the journal text for a specific date (yyyy-MM-dd)
// Returns "" if none found.
    public String getJournalTextByDate(String date) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_JOURNAL,
                new String[]{ ZenPathDbHelper.J_TEXT },
                ZenPathDbHelper.J_DATE + "=?",
                new String[]{ date },
                null, null,
                ZenPathDbHelper.J_CREATED_AT + " DESC",
                "1"
        );

        String text = "";
        if (c.moveToFirst()) {
            text = c.getString(0);
        }
        c.close();
        return text;
    }

    // ===== READ ALL (optional) =====
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
            list.add(c.getString(0) + " - " + c.getString(1));
        }

        c.close();
        return list;
    }
}
