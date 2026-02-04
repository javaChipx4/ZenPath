package com.example.zenpath;

public class MoodRecord {
    public final String date;
    public final int moodValue;
    public final String note;

    public MoodRecord(String date, int moodValue, String note) {
        this.date = date;
        this.moodValue = moodValue;
        this.note = note == null ? "" : note;
    }
}
