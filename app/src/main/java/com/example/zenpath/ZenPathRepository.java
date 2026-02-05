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

    public ArrayList<String> getDiaryPagesByDate(String date) {
        String raw = getJournalTextByDate(date); // reuse your existing method
        ArrayList<String> pages = new ArrayList<>();

        if (raw == null || raw.trim().isEmpty()) {
            pages.add("");
            return pages;
        }

        String[] parts = raw.split("\n<<PAGE_BREAK>>\n", -1);
        for (String p : parts) pages.add(p);

        if (pages.isEmpty()) pages.add("");
        return pages;
    }

    public void upsertDiaryPages(String date, ArrayList<String> pages) {
        if (pages == null || pages.isEmpty()) {
            upsertJournalEntry(date, "");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            if (i > 0) sb.append("\n<<PAGE_BREAK>>\n");
            sb.append(pages.get(i) == null ? "" : pages.get(i));
        }

        upsertJournalEntry(date, sb.toString()); // reuse existing DB save
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

    // ===== DIARY HISTORY META =====

    public static class DiaryEntryMeta {
        public final String date;
        public final String preview;
        public final long createdAt;

        public DiaryEntryMeta(String date, String preview, long createdAt) {
            this.date = date;
            this.preview = preview;
            this.createdAt = createdAt;
        }
    }

    // Latest first, only entries that are saved (non-empty text)
    public ArrayList<DiaryEntryMeta> getSavedDiaryHistory() {
        ArrayList<DiaryEntryMeta> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        // Only non-empty text
        String where = ZenPathDbHelper.J_TEXT + " IS NOT NULL AND length(trim(" + ZenPathDbHelper.J_TEXT + ")) > 0";

        Cursor c = db.query(
                ZenPathDbHelper.T_JOURNAL,
                new String[]{ ZenPathDbHelper.J_DATE, ZenPathDbHelper.J_TEXT, ZenPathDbHelper.J_CREATED_AT },
                where,
                null,
                null, null,
                ZenPathDbHelper.J_CREATED_AT + " DESC"
        );

        while (c.moveToNext()) {
            String date = c.getString(0);
            String text = c.getString(1);
            long createdAt = c.getLong(2);

            String preview = makePreview(text);
            list.add(new DiaryEntryMeta(date, preview, createdAt));
        }

        c.close();
        return list;
    }

    private String makePreview(String raw) {
        if (raw == null) return "";
        // Remove page breaks so preview is clean
        String clean = raw.replace("\n<<PAGE_BREAK>>\n", "\n").trim();
        // Take first ~120 chars
        if (clean.length() > 120) clean = clean.substring(0, 120).trim() + "…";
        return clean;
    }

}
