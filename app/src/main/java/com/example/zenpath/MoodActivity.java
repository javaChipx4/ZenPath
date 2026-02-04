package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MoodActivity extends AppCompatActivity {

    private static final String PREFS = "zen_path_prefs";
    private SharedPreferences prefs;

    private ZenPathRepository repo;

    // UI
    private CalendarView calendarView;
    private View sheetScrim, bottomSheet;
    private BottomSheetBehavior<View> sheetBehavior;
    private EditText etReflection;
    private SeekBar seekStress;
    private TextView[] moodViews;
    private TextView[] dayCircles;

    // State
    private int selectedMoodIndex = -1;
    private int selectedCircleIndex = -1;
    private String selectedDateKey = ""; // yyyyMMdd
    private boolean isLoading = false;

    // Pref keys (optional caching)
    private static String moodKey(String dateKey)   { return "mood_" + dateKey; }
    private static String stressKey(String dateKey) { return "stress_" + dateKey; }
    private static String noteKey(String dateKey)   { return "note_" + dateKey; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_mt);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        repo = new ZenPathRepository(this);

        calendarView = findViewById(R.id.calendarView);
        sheetScrim = findViewById(R.id.sheetScrim);
        bottomSheet = findViewById(R.id.moodBottomSheet);
        etReflection = findViewById(R.id.etReflection);
        seekStress = findViewById(R.id.seekStress);

        moodViews = new TextView[]{
                findViewById(R.id.tvMood0),
                findViewById(R.id.tvMood1),
                findViewById(R.id.tvMood2),
                findViewById(R.id.tvMood3),
                findViewById(R.id.tvMood4)
        };

        dayCircles = new TextView[]{
                findViewById(R.id.circleSun),
                findViewById(R.id.circleMon),
                findViewById(R.id.circleTue),
                findViewById(R.id.circleWed),
                findViewById(R.id.circleThu),
                findViewById(R.id.circleFri),
                findViewById(R.id.circleSat)
        };

        setupSettingsPopup();
        setupBottomSheet();
        setupCalendar();
        setupMoodClicks();
        setupCircleClicks();
        setupStressSaver();
        setupReflectionSaver();

        hideSheet();
        selectTodayWithoutOpeningSheet();
    }

    // ================= DATE HELPERS =================

    private String formatDateKey(Calendar cal) {
        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.getTime());
    }

    private String toDbDate(String dateKey) {
        return dateKey.substring(0, 4) + "-" +
                dateKey.substring(4, 6) + "-" +
                dateKey.substring(6, 8);
    }

    // ================= SAVE TO SQLITE =================

    private void syncToSqlite() {
        if (selectedDateKey.isEmpty()) return;
        if (selectedMoodIndex < 0) return; // mood is required

        String dbDate = toDbDate(selectedDateKey);
        String note = etReflection.getText().toString();
        int moodValue = selectedMoodIndex + 1;

        new Thread(() ->
                repo.saveMood(dbDate, moodValue, note)
        ).start();
    }

    private void saveMoodForSelectedDate() {
        prefs.edit().putInt(moodKey(selectedDateKey), selectedMoodIndex).apply();
        syncToSqlite();
    }

    private void saveNoteForSelectedDate(String note) {
        prefs.edit().putString(noteKey(selectedDateKey), note).apply();
        syncToSqlite();
    }

    // ================= UI SETUP =================

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
            moodViews[i].setScaleX(selected ? 1.2f : 1.0f);
            moodViews[i].setScaleY(selected ? 1.2f : 1.0f);
            moodViews[i].setAlpha(selected ? 1.0f : 0.55f);
        }
    }

    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view, year, month, day) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day);
            onDateSelected(formatDateKey(cal));
            showSheet();
        });
    }

    private void onDateSelected(String dateKey) {
        selectedDateKey = dateKey;

        selectedMoodIndex = prefs.getInt(moodKey(dateKey), -1);
        String note = prefs.getString(noteKey(dateKey), "");

        isLoading = true;
        etReflection.setText(note);
        isLoading = false;

        updateMoodUI();
        updateWeekCircles(Calendar.getInstance());
    }

    private void setupReflectionSaver() {
        etReflection.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isLoading) saveNoteForSelectedDate(s.toString());
            }
        });
    }

    private void setupStressSaver() {
        seekStress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    // ================= WEEK UI =================

    private void updateWeekCircles(Calendar selectedDate) {
        Calendar start = (Calendar) selectedDate.clone();
        int dow = start.get(Calendar.DAY_OF_WEEK);
        start.add(Calendar.DAY_OF_MONTH, -(dow - Calendar.SUNDAY));

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) start.clone();
            day.add(Calendar.DAY_OF_MONTH, i);
            dayCircles[i].setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
        }
    }

    private void setupCircleClicks() {
        for (int i = 0; i < dayCircles.length; i++) {
            final int idx = i;
            dayCircles[i].setOnClickListener(v -> {
                selectedCircleIndex = idx;
                updateCircleUI();
            });
        }
    }

    private void updateCircleUI() {
        for (int i = 0; i < dayCircles.length; i++) {
            boolean selected = i == selectedCircleIndex;
            dayCircles[i].setAlpha(selected ? 1.0f : 0.55f);
        }
    }

    // ================= BOTTOM SHEET =================

    private void setupBottomSheet() {
        sheetBehavior = BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setHideable(true);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        sheetScrim.setOnClickListener(v -> hideSheet());
    }

    private void showSheet() {
        sheetScrim.setVisibility(View.VISIBLE);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void hideSheet() {
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        sheetScrim.setVisibility(View.GONE);
    }

    private void selectTodayWithoutOpeningSheet() {
        Calendar today = Calendar.getInstance();
        selectedDateKey = formatDateKey(today);
        calendarView.setDate(today.getTimeInMillis(), false, true);
        onDateSelected(selectedDateKey);
    }

    // ================= MENU =================

    private void setupSettingsPopup() {
        ViewGroup rootView = findViewById(android.R.id.content);
        View popup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(popup);
        popup.setVisibility(View.GONE);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> popup.setVisibility(View.VISIBLE));

        popup.setOnClickListener(v -> popup.setVisibility(View.GONE));

        popup.findViewById(R.id.btnHome)
                .setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        popup.findViewById(R.id.btnBack)
                .setOnClickListener(v -> finish());

        popup.findViewById(R.id.btnHistory)
                .setOnClickListener(v ->
                        startActivity(new Intent(this, MoodHistoryActivity.class)));
    }
}
