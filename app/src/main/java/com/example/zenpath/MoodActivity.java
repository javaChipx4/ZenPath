package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MoodActivity extends AppCompatActivity {

    private static final String PREFS = "zen_path_prefs";
    private SharedPreferences prefs;

    // UI Components
    private CalendarView calendarView;
    private View sheetScrim, bottomSheet;
    private BottomSheetBehavior<View> sheetBehavior;
    private TextView tvSelectedDate;
    private EditText etReflection;
    private SeekBar seekStress;
    private TextView[] moodViews;
    private TextView[] dayCircles;

    // State Variables
    private int selectedMoodIndex = -1;
    private int selectedCircleIndex = -1;
    private String selectedDateKey = ""; // Format: yyyyMMdd
    private boolean isLoading = false;

    // Keys for Prefs
    private static String moodKey(String dateKey)   { return "mood_" + dateKey; }
    private static String stressKey(String dateKey) { return "stress_" + dateKey; }
    private static String noteKey(String dateKey)   { return "note_" + dateKey; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_mt);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // ===== Initialize UI =====
        calendarView = findViewById(R.id.calendarView);
        sheetScrim = findViewById(R.id.sheetScrim);
        bottomSheet = findViewById(R.id.moodBottomSheet);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        etReflection = findViewById(R.id.etReflection);
        seekStress = findViewById(R.id.seekStress);

        moodViews = new TextView[]{
                findViewById(R.id.tvMood0), findViewById(R.id.tvMood1),
                findViewById(R.id.tvMood2), findViewById(R.id.tvMood3),
                findViewById(R.id.tvMood4)
        };

        dayCircles = new TextView[]{
                findViewById(R.id.circleSun), findViewById(R.id.circleMon),
                findViewById(R.id.circleTue), findViewById(R.id.circleWed),
                findViewById(R.id.circleThu), findViewById(R.id.circleFri),
                findViewById(R.id.circleSat)
        };

        setupSettingsPopup();
        setupBottomSheet();
        setupCalendar();
        setupMoodClicks();
        setupCircleClicks();
        setupStressSaver();
        setupReflectionSaver();

        hideSheet(false);
        selectTodayWithoutOpeningSheet();
    }

    // ===================== ROOM DATABASE LOGIC (NEW) =====================

    // This method converts your date key (20260203) to database format (2026-02-03)
    // and saves everything to the Room Database so Screen 2 can see it.
    private void syncToRoomDatabase() {
        if (selectedDateKey.isEmpty()) return;

        // Convert 20260203 -> 2026-02-03
        String dbDate = selectedDateKey.substring(0, 4) + "-" +
                selectedDateKey.substring(4, 6) + "-" +
                selectedDateKey.substring(6, 8);

        String note = etReflection.getText().toString();
        int stress = seekStress.getProgress();
        int mood = selectedMoodIndex + 1; // Room uses 1-5, your array uses 0-4

        MoodEntry entry = new MoodEntry(dbDate, mood, note, stress);

        new Thread(() -> {
            MoodDatabase.getInstance(this).moodDao().insert(entry);
        }).start();
    }

    // ===================== MODIFIED SAVE HELPERS =====================

    private void saveMoodForSelectedDate() {
        if (selectedDateKey.isEmpty()) return;
        prefs.edit().putInt(moodKey(selectedDateKey), selectedMoodIndex).apply();
        syncToRoomDatabase(); // Sync to History
    }

    private void saveStressForSelectedDate(int stress) {
        if (selectedDateKey.isEmpty()) return;
        prefs.edit().putInt(stressKey(selectedDateKey), stress).apply();
        syncToRoomDatabase(); // Sync to History
    }

    private void saveNoteForSelectedDate(String note) {
        if (selectedDateKey.isEmpty()) return;
        prefs.edit().putString(noteKey(selectedDateKey), note).apply();
        syncToRoomDatabase(); // Sync to History
    }

    // ===================== UI SETUP METHODS =====================

    private void setupMoodClicks() {
        for (int i = 0; i < moodViews.length; i++) {
            final int idx = i;
            if (moodViews[i] == null) continue;
            moodViews[i].setOnClickListener(v -> {
                selectedMoodIndex = idx;
                saveMoodForSelectedDate();
                updateMoodUI();
            });
        }
    }

    private void updateMoodUI() {
        for (int i = 0; i < moodViews.length; i++) {
            if (moodViews[i] == null) continue;
            boolean selected = (i == selectedMoodIndex);
            moodViews[i].setScaleX(selected ? 1.18f : 1.0f);
            moodViews[i].setScaleY(selected ? 1.18f : 1.0f);
            moodViews[i].setAlpha(selected ? 1.0f : 0.55f);
        }
    }

    private void setupCalendar() {
        if (calendarView == null) return;
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            onDateSelected(formatDateKey(cal));
            showSheetHalf();
        });
    }

    private void onDateSelected(String dateKey) {
        if (dateKey == null || dateKey.isEmpty()) return;
        selectedDateKey = dateKey;

        selectedMoodIndex = prefs.getInt(moodKey(dateKey), -1);
        int stress = prefs.getInt(stressKey(dateKey), 50);
        String note = prefs.getString(noteKey(dateKey), "");

        isLoading = true;
        if (seekStress != null) seekStress.setProgress(stress);
        if (etReflection != null) etReflection.setText(note);
        isLoading = false;

        updateMoodUI();
        Calendar selected = parseDateKey(dateKey);
        if (selected != null) updateWeekCircles(selected);
    }

    private void setupReflectionSaver() {
        etReflection.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isLoading) saveNoteForSelectedDate(s.toString());
            }
        });
    }

    private void setupStressSaver() {
        seekStress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isLoading) saveStressForSelectedDate(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    // ============ (Rest of your helper methods: setupBottomSheet, formatDateKey, etc.) ============

    private void setupBottomSheet() {
        if (bottomSheet == null) return;
        sheetBehavior = BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setHideable(true);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        if (sheetScrim != null) sheetScrim.setOnClickListener(v -> hideSheet(true));
    }

    private void showSheetHalf() {
        if (sheetBehavior == null) return;
        if (sheetScrim != null) { sheetScrim.setVisibility(View.VISIBLE); sheetScrim.setAlpha(1f); }
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void hideSheet(boolean animate) {
        if (sheetBehavior != null) sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        if (sheetScrim != null) sheetScrim.setVisibility(View.GONE);
    }

    private String formatDateKey(Calendar cal) {
        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.getTime());
    }

    private Calendar parseDateKey(String dateKey) {
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(dateKey));
            return c;
        } catch (Exception e) { return null; }
    }

    private void selectTodayWithoutOpeningSheet() {
        Calendar today = Calendar.getInstance();
        selectedDateKey = formatDateKey(today);
        if (calendarView != null) calendarView.setDate(today.getTimeInMillis(), false, true);
        onDateSelected(selectedDateKey);
    }

    private void setupCircleClicks() {
        for (int i = 0; i < dayCircles.length; i++) {
            final int idx = i;
            if (dayCircles[i] == null) continue;
            dayCircles[i].setOnClickListener(v -> { selectedCircleIndex = idx; updateCircleUI(); });
        }
    }

    private void updateWeekCircles(Calendar selectedDate) {
        Calendar start = (Calendar) selectedDate.clone();
        int dow = start.get(Calendar.DAY_OF_WEEK);
        start.add(Calendar.DAY_OF_MONTH, -(dow - Calendar.SUNDAY));
        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) start.clone();
            day.add(Calendar.DAY_OF_MONTH, i);
            if (dayCircles[i] != null) dayCircles[i].setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
        }
        selectedCircleIndex = dow - 1;
        updateCircleUI();
    }

    private void updateCircleUI() {
        for (int i = 0; i < dayCircles.length; i++) {
            if (dayCircles[i] == null) continue;
            boolean selected = (i == selectedCircleIndex);
            dayCircles[i].setScaleX(selected ? 1.2f : 1.0f);
            dayCircles[i].setScaleY(selected ? 1.2f : 1.0f);
            dayCircles[i].setAlpha(selected ? 1.0f : 0.55f);
        }
    }

    private void setupSettingsPopup() {
        ViewGroup rootView = findViewById(android.R.id.content);
        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);
        ImageView menuIcon = findViewById(R.id.menuIcon);
        if (menuIcon != null) menuIcon.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));
        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));
        View btnHome = settingsPopup.findViewById(R.id.btnHome);
        if (btnHome != null) btnHome.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        View btnBack = settingsPopup.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        View btnHistory = settingsPopup.findViewById(R.id.btnHistory);
        if (btnHistory != null) btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, MoodHistoryActivity.class));
        });
    }
}