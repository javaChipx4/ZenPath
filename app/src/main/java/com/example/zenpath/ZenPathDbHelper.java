package com.example.zenpath;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ZenPathDbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "zenpath.db";
    public static final int DB_VERSION = 2; // 🔴 INCREASE VERSION

    // ===================== JOURNAL =====================
    public static final String T_JOURNAL = "journal";
    public static final String J_ID = "_id";
    public static final String J_DATE = "entry_date";
    public static final String J_TEXT = "entry_text";
    public static final String J_CREATED_AT = "created_at";

    // ===================== MOOD =====================
    public static final String T_MOOD = "mood";
    public static final String M_ID = "_id";
    public static final String M_DATE = "mood_date";
    public static final String M_VALUE = "mood_value";
    public static final String M_NOTE = "note";

    // ===================== STRESS =====================
    public static final String T_STRESS = "stress";
    public static final String S_ID = "_id";
    public static final String S_DATE = "stress_date";
    public static final String S_LEVEL = "stress_level";
    public static final String S_SUGGESTION = "suggestion";

    // ===================== GAMES PLAYED =====================
    public static final String T_GAMES = "games_played";
    public static final String G_ID = "_id";
    public static final String G_DATE = "play_date";       // yyyy-MM-dd
    public static final String G_NAME = "game_name";       // game title
    public static final String G_CREATED_AT = "created_at"; // millis

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

        String createGames =
                "CREATE TABLE " + T_GAMES + " (" +
                        G_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        G_DATE + " TEXT NOT NULL, " +
                        G_NAME + " TEXT NOT NULL, " +
                        G_CREATED_AT + " INTEGER NOT NULL" +
                        ");";

        db.execSQL(createJournal);
        db.execSQL(createMood);
        db.execSQL(createStress);
        db.execSQL(createGames);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_JOURNAL);
        db.execSQL("DROP TABLE IF EXISTS " + T_MOOD);
        db.execSQL("DROP TABLE IF EXISTS " + T_STRESS);
        db.execSQL("DROP TABLE IF EXISTS " + T_GAMES);
        onCreate(db);
    }
}
