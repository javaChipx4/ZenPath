package com.example.zenpath;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ZenPathDbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "zenpath.db";
    public static final int DB_VERSION = 1;

    // Table: journal
    public static final String T_JOURNAL = "journal";
    public static final String J_ID = "_id";
    public static final String J_DATE = "entry_date";     // "2026-01-25"
    public static final String J_TEXT = "entry_text";
    public static final String J_CREATED_AT = "created_at"; // millis

    // Table: mood
    public static final String T_MOOD = "mood";
    public static final String M_ID = "_id";
    public static final String M_DATE = "mood_date";
    public static final String M_VALUE = "mood_value";   // 1..5 or emoji index
    public static final String M_NOTE = "note";

    // Table: stress
    public static final String T_STRESS = "stress";
    public static final String S_ID = "_id";
    public static final String S_DATE = "stress_date";
    public static final String S_LEVEL = "stress_level"; // 1..10
    public static final String S_SUGGESTION = "suggestion";

    public ZenPathDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createJournal =
                "CREATE TABLE " + T_JOURNAL + " (" +
                        J_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        J_DATE + " TEXT NOT NULL, " +
                        J_TEXT + " TEXT NOT NULL, " +
                        J_CREATED_AT + " INTEGER NOT NULL" +
                        ");";

        String createMood =
                "CREATE TABLE " + T_MOOD + " (" +
                        M_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        M_DATE + " TEXT NOT NULL, " +
                        M_VALUE + " INTEGER NOT NULL, " +
                        M_NOTE + " TEXT" +
                        ");";

        String createStress =
                "CREATE TABLE " + T_STRESS + " (" +
                        S_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        S_DATE + " TEXT NOT NULL, " +
                        S_LEVEL + " INTEGER NOT NULL, " +
                        S_SUGGESTION + " TEXT" +
                        ");";

        db.execSQL(createJournal);
        db.execSQL(createMood);
        db.execSQL(createStress);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For now just drop and recreate (fine for school projects)
        db.execSQL("DROP TABLE IF EXISTS " + T_JOURNAL);
        db.execSQL("DROP TABLE IF EXISTS " + T_MOOD);
        db.execSQL("DROP TABLE IF EXISTS " + T_STRESS);
        onCreate(db);
    }
}

