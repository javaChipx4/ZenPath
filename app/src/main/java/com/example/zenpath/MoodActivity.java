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

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MoodActivity extends AppCompatActivity {

    private static final String PREFS = "zen_path_prefs";
    private static String moodKey(String dateKey)   { return "mood_" + dateKey; }
    private static String stressKey(String dateKey) { return "stress_" + dateKey; }
    private static String noteKey(String dateKey)   { return "note_" + dateKey; }

    private SharedPreferences prefs;
    private ZenPathRepository repository;

    private CalendarView calendarView;

    private View sheetScrim;
    private View bottomSheet;
    private BottomSheetBehavior<View> sheetBehavior;

    private TextView tvSelectedDate;

    private TextView[] moodViews;
    private int selectedMoodIndex = -1;

    private EditText etReflection;
    private SeekBar seekStress;

    // Sun..Sat circles (TextViews now so we can show dates)
    private TextView[] dayCircles;
    private int selectedCircleIndex = -1;

    private String selectedDateKey = ""; // yyyyMMdd
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_mt);

        // ===== Settings Popup Overlay =====
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
        if (btnHome != null) btnHome.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
            startActivity(new Intent(MoodActivity.this, MainActivity.class));
        });

        ImageButton btnHistory = settingsPopup.findViewById(R.id.btnHistory);
        if (btnHistory != null) btnHistory.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
            startActivity(new Intent(MoodActivity.this, HistoryActivity.class));
        });

        View btnBack = settingsPopup.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
            finish();
        });

        // ===== Prefs =====
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        
        // ===== Repository =====
        repository = new ZenPathRepository(this);

        // ===== Bind =====
        calendarView = findViewById(R.id.calendarView);

        sheetScrim = findViewById(R.id.sheetScrim);
        bottomSheet = findViewById(R.id.moodBottomSheet);
        tvSelectedDate = findViewById(R.id.tvSelectedDate); // currently hidden in XML

        etReflection = findViewById(R.id.etReflection);
        seekStress = findViewById(R.id.seekStress);

        TextView tvMood0 = findViewById(R.id.tvMood0);
        TextView tvMood1 = findViewById(R.id.tvMood1);
        TextView tvMood2 = findViewById(R.id.tvMood2);
        TextView tvMood3 = findViewById(R.id.tvMood3);
        TextView tvMood4 = findViewById(R.id.tvMood4);
        moodViews = new TextView[]{ tvMood0, tvMood1, tvMood2, tvMood3, tvMood4 };

        // Sun..Sat order
        dayCircles = new TextView[]{
                findViewById(R.id.circleSun),
                findViewById(R.id.circleMon),
                findViewById(R.id.circleTue),
                findViewById(R.id.circleWed),
                findViewById(R.id.circleThu),
                findViewById(R.id.circleFri),
                findViewById(R.id.circleSat)
        };

        setupBottomSheet();
        setupCalendar();
        setupMoodClicks();
        setupCircleClicks();
        setupStressSaver();
        setupReflectionSaver();

        hideSheet(false);

        selectTodayWithoutOpeningSheet();
    }

    // ===================== BOTTOM SHEET =====================
    private void setupBottomSheet() {
        if (bottomSheet == null) return;

        sheetBehavior = BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setHideable(true);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        if (sheetScrim != null) {
            sheetScrim.setOnClickListener(v -> hideSheet(true));
        }

        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (sheetScrim != null) sheetScrim.setVisibility(View.GONE);
                } else {
                    if (sheetScrim != null) sheetScrim.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
                if (sheetScrim != null) {
                    if (slideOffset <= 0f) sheetScrim.setAlpha(0f);
                    else sheetScrim.setAlpha(Math.min(1f, slideOffset));
                }
            }
        });
    }

    private void showSheetHalf() {
        if (sheetBehavior == null) return;
        if (sheetScrim != null) {
            sheetScrim.setVisibility(View.VISIBLE);
            sheetScrim.setAlpha(1f);
        }
        try {
            sheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        } catch (Exception e) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private void hideSheet(boolean animate) {
        if (sheetBehavior == null) return;
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        if (sheetScrim != null) sheetScrim.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (sheetBehavior != null && sheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            hideSheet(true);
            return;
        }
        super.onBackPressed();
    }

    // ===================== CALENDAR =====================
    private void setupCalendar() {
        if (calendarView == null) return;

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            String dateKey = formatDateKey(cal);
            onDateSelected(dateKey);

            showSheetHalf();
        });
    }

    private void selectTodayWithoutOpeningSheet() {
        Calendar today = Calendar.getInstance();
        selectedDateKey = formatDateKey(today);

        if (calendarView != null) {
            calendarView.setDate(today.getTimeInMillis(), false, true);
        }

        onDateSelected(selectedDateKey);
    }

    private void onDateSelected(String dateKey) {
        if (dateKey == null || dateKey.isEmpty()) return;

        selectedDateKey = dateKey;

        // optional (hidden) label
        if (tvSelectedDate != null) {
            tvSelectedDate.setText(prettyDate(dateKey));
        }

        selectedMoodIndex = prefs.getInt(moodKey(dateKey), -1);
        int stress = prefs.getInt(stressKey(dateKey), 50);
        String note = prefs.getString(noteKey(dateKey), "");

        isLoading = true;
        if (seekStress != null) seekStress.setProgress(stress);
        if (etReflection != null) etReflection.setText(note);
        isLoading = false;

        updateMoodUI();

        // ✅ NEW: update week circles (numbers + auto-highlight weekday)
        Calendar selected = parseDateKey(dateKey);
        if (selected != null) {
            updateWeekCircles(selected);
        }
    }

    // ===================== MOOD =====================
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

    // ===================== WEEK CIRCLES =====================
    private void setupCircleClicks() {
        // Optional: tapping a circle just "highlights" it (does NOT change selected day)
        // If you want circle tap to change selected date later, tell me.
        for (int i = 0; i < dayCircles.length; i++) {
            final int idx = i;
            if (dayCircles[i] == null) continue;
            dayCircles[i].setOnClickListener(v -> {
                selectedCircleIndex = idx;
                updateCircleUI();
            });
        }
    }

    private void updateWeekCircles(Calendar selectedDate) {
        // Build Sunday of that week
        Calendar start = (Calendar) selectedDate.clone();
        int dow = start.get(Calendar.DAY_OF_WEEK); // 1=Sun ... 7=Sat
        int diffToSun = dow - Calendar.SUNDAY;     // 0..6
        start.add(Calendar.DAY_OF_MONTH, -diffToSun);

        // Fill Sun..Sat with day numbers
        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) start.clone();
            day.add(Calendar.DAY_OF_MONTH, i);

            if (dayCircles[i] != null) {
                dayCircles[i].setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
            }
        }

        // Auto-select weekday index (Sun=0 .. Sat=6)
        selectedCircleIndex = diffToSun;
        updateCircleUI();
    }

    private void updateCircleUI() {
        for (int i = 0; i < dayCircles.length; i++) {
            if (dayCircles[i] == null) continue;

            boolean selected = (i == selectedCircleIndex);

            dayCircles[i].setScaleX(selected ? 1.20f : 1.0f);
            dayCircles[i].setScaleY(selected ? 1.20f : 1.0f);
            dayCircles[i].setAlpha(selected ? 1.0f : 0.55f);

            // Optional: make selected number text a bit stronger
            dayCircles[i].setTextColor(selected ? 0xFF1E1E1E : 0xFF3A3A3A);
        }
    }

    // ===================== STRESS + REFLECTION =====================
    private void setupStressSaver() {
        if (seekStress == null) return;

        seekStress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isLoading) saveStressForSelectedDate(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupReflectionSaver() {
        if (etReflection == null) return;

        etReflection.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isLoading) saveNoteForSelectedDate(s.toString());
            }
        });
    }

    // ===================== SAVE HELPERS =====================
    private void saveMoodForSelectedDate() {
        if (selectedDateKey.isEmpty()) return;
        String date = prettyDate(selectedDateKey);
        repository.addMood(date, selectedMoodIndex, "");
    }

    private void saveStressForSelectedDate(int stress) {
        if (selectedDateKey.isEmpty()) return;
        String date = prettyDate(selectedDateKey);
        repository.addStress(date, stress, "");
    }

    private void saveNoteForSelectedDate(String note) {
        if (selectedDateKey.isEmpty()) return;
        String date = prettyDate(selectedDateKey);
        // For notes, we need to update existing mood entry or create new one
        // For simplicity, we'll add a new mood entry with the note
        repository.addMood(date, selectedMoodIndex, note);
    }

    // ===================== DATE HELPERS =====================
    private String formatDateKey(Calendar cal) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return df.format(cal.getTime());
    }

    private Calendar parseDateKey(String dateKey) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            Calendar c = Calendar.getInstance();
            c.setTime(in.parse(dateKey));
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private String prettyDate(String dateKey) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return out.format(in.parse(dateKey));
        } catch (Exception e) {
            return dateKey;
        }
    }
}
