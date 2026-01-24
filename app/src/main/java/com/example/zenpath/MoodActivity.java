package com.example.zenpath;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    // Views from your XML
    private TextView tvMonthYearLeft, tvMonthYearRight;
    private RecyclerView rvCalendar;

    private TextView tvMood0, tvMood1, tvMood2, tvMood3, tvMood4;
    private TextView[] moodViews;

    private EditText etReflection;
    private SeekBar seekStress;

    // Week circles
    private View[] dayCircles;
    private int selectedCircleIndex = -1;

    // Calendar state
    private Calendar currentMonth;
    private CalendarDayAdapter adapter;

    private String selectedDateKey = ""; // yyyyMMdd
    private int selectedMoodIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_mt);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // Header
        tvMonthYearLeft = findViewById(R.id.tvMonthYearLeft);
        tvMonthYearRight = findViewById(R.id.tvMonthYearRight);

        // Calendar
        rvCalendar = findViewById(R.id.rvCalendar);
        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));

        // Mood emojis (TextViews)
        tvMood0 = findViewById(R.id.tvMood0);
        tvMood1 = findViewById(R.id.tvMood1);
        tvMood2 = findViewById(R.id.tvMood2);
        tvMood3 = findViewById(R.id.tvMood3);
        tvMood4 = findViewById(R.id.tvMood4);

        moodViews = new TextView[]{ tvMood0, tvMood1, tvMood2, tvMood3, tvMood4 };

        // Reflection + Stress
        etReflection = findViewById(R.id.etReflection);
        seekStress = findViewById(R.id.seekStress);

        // Set month = today
        currentMonth = Calendar.getInstance();

        setHeaderMonthYear();
        setupCalendarGrid();
        setupMoodClicks();
        setupCircleClicks();     // ✅ circles clickable
        setupStressSaver();
        setupReflectionSaver();

        // Auto select today
        selectToday();
    }

    private void setHeaderMonthYear() {
        SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM", Locale.getDefault());
        SimpleDateFormat yearFmt  = new SimpleDateFormat("yyyy", Locale.getDefault());

        tvMonthYearLeft.setText(monthFmt.format(currentMonth.getTime()).toUpperCase(Locale.getDefault()));
        tvMonthYearRight.setText(yearFmt.format(currentMonth.getTime()));
    }

    private void setupCalendarGrid() {
        ArrayList<String> dateKeys = new ArrayList<>();
        ArrayList<Integer> dayNums = new ArrayList<>();

        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        // Sunday-based calendar
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1=Sunday
        int blanks = firstDayOfWeek - Calendar.SUNDAY;      // 0..6

        for (int i = 0; i < blanks; i++) {
            dateKeys.add("");
            dayNums.add(0);
        }

        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= maxDay; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            dateKeys.add(formatDateKey(cal)); // yyyyMMdd
            dayNums.add(day);
        }

        // Fill remaining cells to make a full grid (42)
        while (dayNums.size() % 7 != 0) {
            dateKeys.add("");
            dayNums.add(0);
        }
        while (dayNums.size() < 42) {
            dateKeys.add("");
            dayNums.add(0);
        }

        adapter = new CalendarDayAdapter(dateKeys, dayNums, (position, dateKey, dayNumber) -> {
            adapter.setSelectedPos(position);
            onDateSelected(dateKey);
        });

        rvCalendar.setAdapter(adapter);
    }

    private void selectToday() {
        Calendar today = Calendar.getInstance();

        // If month changed, update month grid
        if (today.get(Calendar.MONTH) != currentMonth.get(Calendar.MONTH) ||
                today.get(Calendar.YEAR) != currentMonth.get(Calendar.YEAR)) {
            currentMonth = (Calendar) today.clone();
            setHeaderMonthYear();
            setupCalendarGrid();
        }

        selectedDateKey = formatDateKey(today);

        // Load data for today
        onDateSelected(selectedDateKey);
    }

    private void onDateSelected(String dateKey) {
        if (dateKey == null || dateKey.isEmpty()) return;

        selectedDateKey = dateKey;

        // Load saved values for this date
        selectedMoodIndex = prefs.getInt(moodKey(dateKey), -1);
        int stress = prefs.getInt(stressKey(dateKey), 50);
        String note = prefs.getString(noteKey(dateKey), "");

        // Apply to UI
        seekStress.setProgress(stress);
        etReflection.setText(note);

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

    // ✅ Make the top circles clickable (Mon-Sun)
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
            dayCircles[i].setOnClickListener(v -> {
                selectedCircleIndex = idx;
                updateCircleUI();

                // ✅ test message so you know it works (remove later if you want)
                Toast.makeText(this, "Circle tapped: " + idx, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateCircleUI() {
        for (int i = 0; i < dayCircles.length; i++) {
            boolean selected = (i == selectedCircleIndex);

            dayCircles[i].setScaleX(selected ? 1.25f : 1.0f);
            dayCircles[i].setScaleY(selected ? 1.25f : 1.0f);
            dayCircles[i].setAlpha(selected ? 1.0f : 0.55f);
        }
    }

    private void setupStressSaver() {
        seekStress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) saveStressForSelectedDate(progress);
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
                saveNoteForSelectedDate(s.toString());
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
