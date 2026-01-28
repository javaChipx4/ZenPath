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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MoodActivity extends AppCompatActivity {

    // SharedPreferences file
    private static final String PREFS = "zen_path_prefs";

    // Per-date keys
    private static String moodKey(String dateKey)   { return "mood_" + dateKey; }
    private static String stressKey(String dateKey) { return "stress_" + dateKey; }
    private static String noteKey(String dateKey)   { return "note_" + dateKey; }

    private SharedPreferences prefs;

    // ✅ CalendarView (MainActivity style)
    private CalendarView calendarView;

    // Mood emojis (TextViews)
    private TextView tvMood0, tvMood1, tvMood2, tvMood3, tvMood4;
    private TextView[] moodViews;

    // Reflection + Stress
    private EditText etReflection;
    private SeekBar seekStress;

    // Week circles
    private View[] dayCircles;
    private int selectedCircleIndex = -1;

    // Selected date state
    private String selectedDateKey = ""; // yyyyMMdd
    private int selectedMoodIndex = -1;

    // Prevent saving while we are loading UI from prefs
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // IMPORTANT: use the NEW XML that has CalendarView with id calendarView
        setContentView(R.layout.activity_mood_mt); // <-- change if your new file name is different

        // ===== Settings Popup Overlay (reuse) =====
        ViewGroup rootView = findViewById(android.R.id.content);

        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        if (menuIcon != null) menuIcon.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));

        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

        View settingsCard = settingsPopup.findViewById(R.id.settingsCard);
        if (settingsCard != null) settingsCard.setOnClickListener(v -> {});

        View btnHome = settingsPopup.findViewById(R.id.btnHome);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(MoodActivity.this, MainActivity.class));
            });
        }

        ImageButton btnHistory = settingsPopup.findViewById(R.id.btnHistory);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(MoodActivity.this, HistoryActivity.class)); // use your new history hub if you have it
            });
        }

        View btnBack = settingsPopup.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                finish();
            });
        }

        // ===== Prefs =====
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // ===== Bind views =====
        calendarView = findViewById(R.id.calendarView);

        tvMood0 = findViewById(R.id.tvMood0);
        tvMood1 = findViewById(R.id.tvMood1);
        tvMood2 = findViewById(R.id.tvMood2);
        tvMood3 = findViewById(R.id.tvMood3);
        tvMood4 = findViewById(R.id.tvMood4);
        moodViews = new TextView[]{ tvMood0, tvMood1, tvMood2, tvMood3, tvMood4 };

        etReflection = findViewById(R.id.etReflection);
        seekStress = findViewById(R.id.seekStress);

        // Setup logic
        setupCalendarView();
        setupMoodClicks();
        setupCircleClicks();
        setupStressSaver();
        setupReflectionSaver();

        // Auto select today
        selectToday();
    }

    // ✅ CalendarView behavior: click date -> load/saves per date
    private void setupCalendarView() {
        if (calendarView == null) return;

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month); // month is 0-based
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            String dateKey = formatDateKey(cal);
            onDateSelected(dateKey);
        });
    }

    private void selectToday() {
        Calendar today = Calendar.getInstance();
        selectedDateKey = formatDateKey(today);

        // Ensure CalendarView highlights today (it does by default, but safe)
        if (calendarView != null) {
            calendarView.setDate(today.getTimeInMillis(), false, true);
        }

        onDateSelected(selectedDateKey);
    }

    private void onDateSelected(String dateKey) {
        if (dateKey == null || dateKey.isEmpty()) return;

        selectedDateKey = dateKey;

        // Load saved values for this date
        selectedMoodIndex = prefs.getInt(moodKey(dateKey), -1);
        int stress = prefs.getInt(stressKey(dateKey), 50);
        String note = prefs.getString(noteKey(dateKey), "");

        // Apply to UI (guard to avoid re-saving while setting text)
        isLoading = true;
        seekStress.setProgress(stress);
        etReflection.setText(note);
        isLoading = false;

        updateMoodUI();
    }

    private void setupMoodClicks() {
        for (int i = 0; i < moodViews.length; i++) {
            final int idx = i;
            moodViews[i].setOnClickListener(v -> {
                selectedMoodIndex = idx;
                saveMoodForSelectedDate();
                updateMoodUI();
            });
        }
    }

    private void updateMoodUI() {
        for (int i = 0; i < moodViews.length; i++) {
            boolean selected = (i == selectedMoodIndex);
            moodViews[i].setScaleX(selected ? 1.18f : 1.0f);
            moodViews[i].setScaleY(selected ? 1.18f : 1.0f);
            moodViews[i].setAlpha(selected ? 1.0f : 0.55f);
        }
    }

    // Week circles (unchanged)
    private void setupCircleClicks() {
        dayCircles = new View[]{
                findViewById(R.id.circleMon),
                findViewById(R.id.circleTue),
                findViewById(R.id.circleWed),
                findViewById(R.id.circleThu),
                findViewById(R.id.circleFri),
                findViewById(R.id.circleSat),
                findViewById(R.id.circleSun)
        };

        for (int i = 0; i < dayCircles.length; i++) {
            final int idx = i;

            if (dayCircles[i] == null) continue;

            dayCircles[i].setOnClickListener(v -> {
                selectedCircleIndex = idx;
                updateCircleUI();

                // test message (remove later if you want)
                Toast.makeText(this, "Circle tapped: " + idx, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateCircleUI() {
        if (dayCircles == null) return;

        for (int i = 0; i < dayCircles.length; i++) {
            if (dayCircles[i] == null) continue;

            boolean selected = (i == selectedCircleIndex);
            dayCircles[i].setScaleX(selected ? 1.25f : 1.0f);
            dayCircles[i].setScaleY(selected ? 1.25f : 1.0f);
            dayCircles[i].setAlpha(selected ? 1.0f : 0.55f);
        }
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

    private void setupReflectionSaver() {
        etReflection.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isLoading) saveNoteForSelectedDate(s.toString());
            }
        });
    }

    private void saveMoodForSelectedDate() {
        if (selectedDateKey.isEmpty()) return;
        prefs.edit().putInt(moodKey(selectedDateKey), selectedMoodIndex).apply();
    }

    private void saveStressForSelectedDate(int stress) {
        if (selectedDateKey.isEmpty()) return;
        prefs.edit().putInt(stressKey(selectedDateKey), stress).apply();
    }

    private void saveNoteForSelectedDate(String note) {
        if (selectedDateKey.isEmpty()) return;
        prefs.edit().putString(noteKey(selectedDateKey), note).apply();
    }

    private String formatDateKey(Calendar cal) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return df.format(cal.getTime());
    }
}
