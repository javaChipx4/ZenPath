package com.example.zenpath;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ZenPathDbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "zenpath.db";
    public static final int DB_VERSION = 5;

    // ===== USERS TABLE =====
    public static final String T_USERS = "users";
    public static final String U_ID = "_id";
    public static final String U_USERNAME = "username";
    public static final String U_CREATED_AT = "created_at";

    // ✅ NEW PROFILE COLUMNS
    public static final String U_GENDER = "gender";          // "Male"/"Female"
    public static final String U_AVATAR_RES = "avatar_res";  // drawable resId (int)

    // ✅ Shared column for per-user data
    public static final String COL_USER_ID = "user_id";

    // ===== JOURNAL =====
    public static final String T_JOURNAL = "journal";
    public static final String J_ID = "_id";
    public static final String J_DATE = "entry_date";
    public static final String J_TEXT = "entry_text";
    public static final String J_CREATED_AT = "created_at";

    // ===== MOOD =====
    public static final String T_MOOD = "mood";
    public static final String M_ID = "_id";
    public static final String M_DATE = "mood_date";          // yyyyMMdd
    public static final String M_TEXT = "mood_text";          // "Angry", "Calm", etc.
    public static final String M_REFLECTION = "reflection";   // user reflection
    public static final String M_CREATED_AT = "created_at";

    // ===== STRESS =====
    public static final String T_STRESS = "stress";
    public static final String S_ID = "_id";
    public static final String S_DATE = "stress_date";        // yyyyMMdd
    public static final String S_LEVEL = "stress_level";      // 0..100
    public static final String S_PLAY_STAR = "play_star_sweep";          // seconds
    public static final String S_PLAY_LANTERN = "play_lantern_release";  // seconds
    public static final String S_PLAY_PLANET = "play_planet";            // seconds
    public static final String S_CREATED_AT = "created_at";

    public ZenPathDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createUsers =
                "CREATE TABLE " + T_USERS + " (" +
                        U_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        U_USERNAME + " TEXT UNIQUE NOT NULL, " +
                        U_CREATED_AT + " INTEGER NOT NULL, " +
                        U_GENDER + " TEXT DEFAULT 'Male', " +
                        U_AVATAR_RES + " INTEGER DEFAULT 0" +
                        ");";

        String createJournal =
                "CREATE TABLE " + T_JOURNAL + " (" +
                        J_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_USER_ID + " INTEGER NOT NULL, " +
                        J_DATE + " TEXT NOT NULL, " +
                        J_TEXT + " TEXT NOT NULL, " +
                        J_CREATED_AT + " INTEGER NOT NULL" +
                        ");";

        String createMood =
                "CREATE TABLE " + T_MOOD + " (" +
                        M_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_USER_ID + " INTEGER NOT NULL, " +
                        M_DATE + " TEXT NOT NULL, " +
                        M_TEXT + " TEXT NOT NULL, " +
                        M_REFLECTION + " TEXT, " +
                        M_CREATED_AT + " INTEGER NOT NULL" +
                        ");";

        String createStress =
                "CREATE TABLE " + T_STRESS + " (" +
                        S_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_USER_ID + " INTEGER NOT NULL, " +
                        S_DATE + " TEXT NOT NULL, " +
                        S_LEVEL + " INTEGER NOT NULL, " +
                        S_PLAY_STAR + " INTEGER NOT NULL DEFAULT 0, " +
                        S_PLAY_LANTERN + " INTEGER NOT NULL DEFAULT 0, " +
                        S_PLAY_PLANET + " INTEGER NOT NULL DEFAULT 0, " +
                        S_CREATED_AT + " INTEGER NOT NULL" +
                        ");";

        db.execSQL(createUsers);
        db.execSQL(createJournal);
        db.execSQL(createMood);
        db.execSQL(createStress);

        // indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_users_username ON " + T_USERS + "(" + U_USERNAME + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_journal_user_date ON " + T_JOURNAL + "(" + COL_USER_ID + "," + J_DATE + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_mood_user_date ON " + T_MOOD + "(" + COL_USER_ID + "," + M_DATE + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_stress_user_date ON " + T_STRESS + "(" + COL_USER_ID + "," + S_DATE + ")");

        createGuestIfNeeded(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // ✅ 1) Ensure users table exists (very old db)
        if (oldVersion < 2) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS " + T_USERS + " (" +
                            U_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            U_USERNAME + " TEXT UNIQUE NOT NULL, " +
                            U_CREATED_AT + " INTEGER NOT NULL" +
                            ");"
            );
        }

        // ✅ 2) VERY IMPORTANT: ensure profile columns exist EARLY
        // so createGuestIfNeeded() will never crash during later migrations
        safeAddColumn(db, T_USERS, U_GENDER, "TEXT DEFAULT 'Male'");
        safeAddColumn(db, T_USERS, U_AVATAR_RES, "INTEGER DEFAULT 0");

        // ✅ 3) If upgrading to v3+ and tables may need user_id defaults
        if (oldVersion < 3) {
            createGuestIfNeeded(db);
            long guestId = getGuestId(db);
            if (guestId <= 0) guestId = 1;

            safeAddColumn(db, T_JOURNAL, COL_USER_ID, "INTEGER DEFAULT " + guestId);
            safeAddColumn(db, T_MOOD, COL_USER_ID, "INTEGER DEFAULT " + guestId);
            safeAddColumn(db, T_STRESS, COL_USER_ID, "INTEGER DEFAULT " + guestId);

            db.execSQL("UPDATE " + T_JOURNAL + " SET " + COL_USER_ID + "=" + guestId + " WHERE " + COL_USER_ID + " IS NULL");
            db.execSQL("UPDATE " + T_MOOD + " SET " + COL_USER_ID + "=" + guestId + " WHERE " + COL_USER_ID + " IS NULL");
            db.execSQL("UPDATE " + T_STRESS + " SET " + COL_USER_ID + "=" + guestId + " WHERE " + COL_USER_ID + " IS NULL");
        }

        // ✅ 4) v3 -> v4 replace mood + stress schemas (your existing logic)
        if (oldVersion < 4) {
            createGuestIfNeeded(db);
            long guestId = getGuestId(db);
            if (guestId <= 0) guestId = 1;

            // ---- MOOD ----
            db.execSQL("ALTER TABLE " + T_MOOD + " RENAME TO mood_old");

            db.execSQL(
                    "CREATE TABLE " + T_MOOD + " (" +
                            M_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            COL_USER_ID + " INTEGER NOT NULL, " +
                            M_DATE + " TEXT NOT NULL, " +
                            M_TEXT + " TEXT NOT NULL, " +
                            M_REFLECTION + " TEXT, " +
                            M_CREATED_AT + " INTEGER NOT NULL" +
                            ");"
            );

            db.execSQL(
                    "INSERT INTO " + T_MOOD + " (" +
                            COL_USER_ID + "," + M_DATE + "," + M_TEXT + "," + M_REFLECTION + "," + M_CREATED_AT +
                            ") " +
                            "SELECT " +
                            "COALESCE(" + COL_USER_ID + ", " + guestId + "), " +
                            "COALESCE(mood_date, ''), " +
                            "CAST(COALESCE(mood_value, '') AS TEXT), " +
                            "note, " +
                            "strftime('%s','now')*1000 " +
                            "FROM mood_old"
            );

            db.execSQL("DROP TABLE mood_old");

            // ---- STRESS ----
            db.execSQL("ALTER TABLE " + T_STRESS + " RENAME TO stress_old");

            db.execSQL(
                    "CREATE TABLE " + T_STRESS + " (" +
                            S_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            COL_USER_ID + " INTEGER NOT NULL, " +
                            S_DATE + " TEXT NOT NULL, " +
                            S_LEVEL + " INTEGER NOT NULL, " +
                            S_PLAY_STAR + " INTEGER NOT NULL DEFAULT 0, " +
                            S_PLAY_LANTERN + " INTEGER NOT NULL DEFAULT 0, " +
                            S_PLAY_PLANET + " INTEGER NOT NULL DEFAULT 0, " +
                            S_CREATED_AT + " INTEGER NOT NULL" +
                            ");"
            );

            db.execSQL(
                    "INSERT INTO " + T_STRESS + " (" +
                            COL_USER_ID + "," + S_DATE + "," + S_LEVEL + "," +
                            S_PLAY_STAR + "," + S_PLAY_LANTERN + "," + S_PLAY_PLANET + "," + S_CREATED_AT +
                            ") " +
                            "SELECT " +
                            "COALESCE(" + COL_USER_ID + ", " + guestId + "), " +
                            "COALESCE(stress_date, ''), " +
                            "COALESCE(stress_level, 0), " +
                            "0, 0, 0, " +
                            "strftime('%s','now')*1000 " +
                            "FROM stress_old"
            );

            db.execSQL("DROP TABLE stress_old");
        }

        // ✅ Always ensure guest exists at end
        createGuestIfNeeded(db);
    }

    private void safeAddColumn(SQLiteDatabase db, String table, String col, String typeSql) {
        try {
            if (!hasColumn(db, table, col)) {
                db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + col + " " + typeSql);
            }
        } catch (Exception ignored) {}
    }

    private boolean hasColumn(SQLiteDatabase db, String table, String col) {
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            if (c == null) return false;
            int nameIdx = c.getColumnIndex("name");
            while (c.moveToNext()) {
                String name = c.getString(nameIdx);
                if (col.equalsIgnoreCase(name)) return true;
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return false;
    }

    private void createGuestIfNeeded(SQLiteDatabase db) {
        ContentValues cv = new ContentValues();

        // ✅ Always safe columns
        cv.put(U_USERNAME, "Guest");
        cv.put(U_CREATED_AT, System.currentTimeMillis());

        // ✅ Only add these if they exist (prevents crash on older DBs)
        if (hasColumn(db, T_USERS, U_GENDER)) cv.put(U_GENDER, "Male");
        if (hasColumn(db, T_USERS, U_AVATAR_RES)) cv.put(U_AVATAR_RES, 0);

        db.insertWithOnConflict(T_USERS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private long getGuestId(SQLiteDatabase db) {
        Cursor c = db.rawQuery(
                "SELECT " + U_ID + " FROM " + T_USERS + " WHERE " + U_USERNAME + "=?",
                new String[]{"Guest"}
        );
        long id = -1;
        if (c.moveToFirst()) id = c.getLong(0);
        c.close();
        return id;
    }
}
