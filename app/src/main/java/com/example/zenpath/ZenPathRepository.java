package com.example.zenpath;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class ZenPathRepository {

    private final ZenPathDbHelper helper;
    private final Context context;

    private static final String PREFS = "zen_path_prefs";
    private static final String KEY_CURRENT_USER = "current_user"; // stores userId as String

    public ZenPathRepository(Context context) {
        this.context = context.getApplicationContext();
        helper = new ZenPathDbHelper(this.context);
    }

    // =========================
    // ✅ Current userId from session
    // =========================
    private long currentUserId() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String s = prefs.getString(KEY_CURRENT_USER, null);
        if (s == null) return -1;
        try { return Long.parseLong(s); } catch (Exception e) { return -1; }
    }

    // =========================
    // ===== USERS (ACCOUNTS) ===
    // =========================

    public long createUser(String username) {
        if (username == null) return -1;
        username = username.trim();
        if (username.isEmpty()) return -1;

        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.U_USERNAME, username);
        cv.put(ZenPathDbHelper.U_CREATED_AT, System.currentTimeMillis());

        return db.insert(ZenPathDbHelper.T_USERS, null, cv);
    }

    public boolean userExists(String username) {
        if (username == null) return false;
        username = username.trim();
        if (username.isEmpty()) return false;

        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_USERS,
                new String[]{ZenPathDbHelper.U_ID},
                ZenPathDbHelper.U_USERNAME + "=?",
                new String[]{username},
                null, null, null,
                "1"
        );

        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    public long getUserId(String username) {
        if (username == null) return -1;
        username = username.trim();
        if (username.isEmpty()) return -1;

        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_USERS,
                new String[]{ZenPathDbHelper.U_ID},
                ZenPathDbHelper.U_USERNAME + "=?",
                new String[]{username},
                null, null, null,
                "1"
        );

        long id = -1;
        if (c.moveToFirst()) id = c.getLong(0);
        c.close();
        return id;
    }

    public String getUsernameById(long userId) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_USERS,
                new String[]{ ZenPathDbHelper.U_USERNAME },
                ZenPathDbHelper.U_ID + "=?",
                new String[]{ String.valueOf(userId) },
                null, null, null,
                "1"
        );

        String username = "";
        if (c.moveToFirst()) username = c.getString(0);
        c.close();
        return username;
    }

    // =========================
    // ===== DIARY PAGES =======
    // =========================

    public ArrayList<String> getDiaryPagesByDate(String date) {
        long userId = currentUserId();
        return getDiaryPagesByDate(userId, date);
    }

    public ArrayList<String> getDiaryPagesByDate(long userId, String date) {
        String raw = getJournalTextByDate(userId, date);
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
        long userId = currentUserId();
        upsertDiaryPages(userId, date, pages);
    }

    public void upsertDiaryPages(long userId, String date, ArrayList<String> pages) {
        if (pages == null || pages.isEmpty()) {
            upsertJournalEntry(userId, date, "");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            if (i > 0) sb.append("\n<<PAGE_BREAK>>\n");
            sb.append(pages.get(i) == null ? "" : pages.get(i));
        }

        upsertJournalEntry(userId, date, sb.toString());
    }

    // =========================
    // ===== JOURNAL (PER USER) =
    // =========================

    public long addJournalEntry(long userId, String date, String text) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.COL_USER_ID, userId);
        cv.put(ZenPathDbHelper.J_DATE, date);
        cv.put(ZenPathDbHelper.J_TEXT, text);
        cv.put(ZenPathDbHelper.J_CREATED_AT, System.currentTimeMillis());

        return db.insert(ZenPathDbHelper.T_JOURNAL, null, cv);
    }

    public boolean hasJournalEntry(long userId, String date) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_JOURNAL,
                new String[]{ ZenPathDbHelper.J_ID },
                ZenPathDbHelper.COL_USER_ID + "=? AND " + ZenPathDbHelper.J_DATE + "=?",
                new String[]{ String.valueOf(userId), date },
                null, null, null,
                "1"
        );

        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    public int updateJournalEntry(long userId, String date, String text) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.J_TEXT, text);
        cv.put(ZenPathDbHelper.J_CREATED_AT, System.currentTimeMillis());

        return db.update(
                ZenPathDbHelper.T_JOURNAL,
                cv,
                ZenPathDbHelper.COL_USER_ID + "=? AND " + ZenPathDbHelper.J_DATE + "=?",
                new String[]{ String.valueOf(userId), date }
        );
    }

    public long upsertJournalEntry(long userId, String date, String text) {
        if (hasJournalEntry(userId, date)) {
            updateJournalEntry(userId, date, text);
            return 1L;
        } else {
            return addJournalEntry(userId, date, text);
        }
    }

    public String getJournalTextByDate(long userId, String date) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_JOURNAL,
                new String[]{ ZenPathDbHelper.J_TEXT },
                ZenPathDbHelper.COL_USER_ID + "=? AND " + ZenPathDbHelper.J_DATE + "=?",
                new String[]{ String.valueOf(userId), date },
                null, null,
                ZenPathDbHelper.J_CREATED_AT + " DESC",
                "1"
        );

        String text = "";
        if (c.moveToFirst()) text = c.getString(0);
        c.close();
        return text;
    }

    // =========================
    // ===== DIARY HISTORY (PER USER)
    // =========================

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

    public ArrayList<DiaryEntryMeta> getSavedDiaryHistory() {
        long userId = currentUserId();
        return getSavedDiaryHistory(userId);
    }

    public ArrayList<DiaryEntryMeta> getSavedDiaryHistory(long userId) {
        ArrayList<DiaryEntryMeta> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        String where =
                ZenPathDbHelper.COL_USER_ID + "=? AND " +
                        ZenPathDbHelper.J_TEXT + " IS NOT NULL AND length(trim(" + ZenPathDbHelper.J_TEXT + ")) > 0";

        Cursor c = db.query(
                ZenPathDbHelper.T_JOURNAL,
                new String[]{ ZenPathDbHelper.J_DATE, ZenPathDbHelper.J_TEXT, ZenPathDbHelper.J_CREATED_AT },
                where,
                new String[]{ String.valueOf(userId) },
                null, null,
                ZenPathDbHelper.J_CREATED_AT + " DESC"
        );

        while (c.moveToNext()) {
            String date = c.getString(0);
            String text = c.getString(1);
            long createdAt = c.getLong(2);

            list.add(new DiaryEntryMeta(date, makePreview(text), createdAt));
        }

        c.close();
        return list;
    }

    private String makePreview(String raw) {
        if (raw == null) return "";
        String clean = raw.replace("\n<<PAGE_BREAK>>\n", "\n").trim();
        if (clean.length() > 120) clean = clean.substring(0, 120).trim() + "…";
        return clean;
    }

    // ✅ used for streak calculation (checks if a mood exists for that date)
    public boolean hasMoodOnDate(long userId, String dateKey) {
        if (userId <= 0) return false;

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(
                ZenPathDbHelper.T_MOOD,
                new String[]{ ZenPathDbHelper.M_ID },
                ZenPathDbHelper.COL_USER_ID + "=? AND " + ZenPathDbHelper.M_DATE + "=? AND " +
                        ZenPathDbHelper.M_TEXT + " IS NOT NULL AND length(trim(" + ZenPathDbHelper.M_TEXT + "))>0",
                new String[]{ String.valueOf(userId), dateKey },
                null, null,
                ZenPathDbHelper.M_CREATED_AT + " DESC",
                "1"
        );

        boolean ok = c.moveToFirst();
        c.close();
        return ok;
    }

    // ✅ weekly reflection count (last 7 days), based on created_at so date format won't break it
    public int getWeeklyReflectionCount(long userId) {
        if (userId <= 0) return 0;

        long since = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + ZenPathDbHelper.T_JOURNAL +
                        " WHERE " + ZenPathDbHelper.COL_USER_ID + "=? " +
                        " AND " + ZenPathDbHelper.J_CREATED_AT + ">=? " +
                        " AND " + ZenPathDbHelper.J_TEXT + " IS NOT NULL AND length(trim(" + ZenPathDbHelper.J_TEXT + "))>0",
                new String[]{ String.valueOf(userId), String.valueOf(since) }
        );

        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }


    // =========================
    // ===== MOOD (PER USER) ====
    // =========================

    public long upsertMood(String date, String moodText, String reflection) {
        long userId = currentUserId();
        return upsertMood(userId, date, moodText, reflection);
    }

    public long upsertMood(long userId, String date, String moodText, String reflection) {
        if (userId <= 0) return -1;

        SQLiteDatabase db = helper.getWritableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_MOOD,
                new String[]{ZenPathDbHelper.M_ID},
                ZenPathDbHelper.COL_USER_ID + "=? AND " + ZenPathDbHelper.M_DATE + "=?",
                new String[]{String.valueOf(userId), date},
                null, null, null,
                "1"
        );

        boolean exists = c.moveToFirst();
        long rowId = exists ? c.getLong(0) : -1;
        c.close();

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.COL_USER_ID, userId);
        cv.put(ZenPathDbHelper.M_DATE, date);
        cv.put(ZenPathDbHelper.M_TEXT, moodText == null ? "" : moodText);
        cv.put(ZenPathDbHelper.M_REFLECTION, reflection);
        cv.put(ZenPathDbHelper.M_CREATED_AT, System.currentTimeMillis());

        if (exists) {
            db.update(ZenPathDbHelper.T_MOOD, cv,
                    ZenPathDbHelper.M_ID + "=?",
                    new String[]{String.valueOf(rowId)});
            return rowId;
        } else {
            return db.insert(ZenPathDbHelper.T_MOOD, null, cv);
        }
    }

    public String[] getMoodByDate(String date) {
        long userId = currentUserId();
        return getMoodByDate(userId, date);
    }

    public String[] getMoodByDate(long userId, String date) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_MOOD,
                new String[]{ZenPathDbHelper.M_TEXT, ZenPathDbHelper.M_REFLECTION},
                ZenPathDbHelper.COL_USER_ID + "=? AND " + ZenPathDbHelper.M_DATE + "=?",
                new String[]{String.valueOf(userId), date},
                null, null,
                ZenPathDbHelper.M_CREATED_AT + " DESC",
                "1"
        );

        String mood = "";
        String reflection = "";
        if (c.moveToFirst()) {
            mood = c.getString(0);
            reflection = c.getString(1);
        }
        c.close();

        return new String[]{mood, reflection};
    }

    // =========================
    // ===== STRESS (PER USER) ==
    // =========================

    public long upsertStress(String date, int stressLevel, int starSweepSec, int lanternSec, int planetSec) {
        long userId = currentUserId();
        return upsertStress(userId, date, stressLevel, starSweepSec, lanternSec, planetSec);
    }

    public long upsertStress(long userId, String date, int stressLevel, int starSweepSec, int lanternSec, int planetSec) {
        if (userId <= 0) return -1;

        SQLiteDatabase db = helper.getWritableDatabase();

        Cursor c = db.query(
                ZenPathDbHelper.T_STRESS,
                new String[]{ZenPathDbHelper.S_ID},
                ZenPathDbHelper.COL_USER_ID + "=? AND " + ZenPathDbHelper.S_DATE + "=?",
                new String[]{String.valueOf(userId), date},
                null, null, null,
                "1"
        );

        boolean exists = c.moveToFirst();
        long rowId = exists ? c.getLong(0) : -1;
        c.close();

        ContentValues cv = new ContentValues();
        cv.put(ZenPathDbHelper.COL_USER_ID, userId);
        cv.put(ZenPathDbHelper.S_DATE, date);
        cv.put(ZenPathDbHelper.S_LEVEL, stressLevel);
        cv.put(ZenPathDbHelper.S_PLAY_STAR, starSweepSec);
        cv.put(ZenPathDbHelper.S_PLAY_LANTERN, lanternSec);
        cv.put(ZenPathDbHelper.S_PLAY_PLANET, planetSec);
        cv.put(ZenPathDbHelper.S_CREATED_AT, System.currentTimeMillis());

        if (exists) {
            db.update(ZenPathDbHelper.T_STRESS, cv,
                    ZenPathDbHelper.S_ID + "=?",
                    new String[]{String.valueOf(rowId)});
            return rowId;
        } else {
            return db.insert(ZenPathDbHelper.T_STRESS, null, cv);
        }
    }

    public void addGamePlaytime(String date, String gameKey, int secondsToAdd) {
        long userId = currentUserId();
        addGamePlaytime(userId, date, gameKey, secondsToAdd);
    }

    public void addGamePlaytime(long userId, String date, String gameKey, int secondsToAdd) {
        if (userId <= 0) return;

        SQLiteDatabase db = helper.getWritableDatabase();

        String col;
        if ("STAR_SWEEP".equals(gameKey)) col = ZenPathDbHelper.S_PLAY_STAR;
        else if ("LANTERN_RELEASE".equals(gameKey)) col = ZenPathDbHelper.S_PLAY_LANTERN;
        else if ("PLANET".equals(gameKey)) col = ZenPathDbHelper.S_PLAY_PLANET;
        else return;

        // Ensure row exists for the date
        Cursor c = db.query(
                ZenPathDbHelper.T_STRESS,
                new String[]{ZenPathDbHelper.S_ID},
                ZenPathDbHelper.COL_USER_ID + "=? AND " + ZenPathDbHelper.S_DATE + "=?",
                new String[]{String.valueOf(userId), date},
                null, null, null,
                "1"
        );

        boolean exists = c.moveToFirst();
        long rowId = exists ? c.getLong(0) : -1;
        c.close();

        if (!exists) {
            ContentValues cv = new ContentValues();
            cv.put(ZenPathDbHelper.COL_USER_ID, userId);
            cv.put(ZenPathDbHelper.S_DATE, date);
            cv.put(ZenPathDbHelper.S_LEVEL, 0);
            cv.put(ZenPathDbHelper.S_PLAY_STAR, 0);
            cv.put(ZenPathDbHelper.S_PLAY_LANTERN, 0);
            cv.put(ZenPathDbHelper.S_PLAY_PLANET, 0);
            cv.put(ZenPathDbHelper.S_CREATED_AT, System.currentTimeMillis());
            rowId = db.insert(ZenPathDbHelper.T_STRESS, null, cv);
        }

        db.execSQL(
                "UPDATE " + ZenPathDbHelper.T_STRESS +
                        " SET " + col + " = " + col + " + ?, " +
                        ZenPathDbHelper.S_CREATED_AT + " = ? " +
                        " WHERE " + ZenPathDbHelper.S_ID + " = ?",
                new Object[]{secondsToAdd, System.currentTimeMillis(), rowId}
        );
    }
}
