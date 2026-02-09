// java/com/example/zenpath/MoodActivity.java
package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MoodActivity extends AppCompatActivity {

    public static final String EXTRA_DATE_KEY = "extra_date_key"; // yyyyMMdd

    private CalendarView calendarView;

    private View sheetScrim;
    private View bottomSheet;
    private BottomSheetBehavior<View> sheetBehavior;

    private TextView tvSelectedDate;

    private TextView[] moodViews;
    private int selectedMoodIndex = -1;

    private EditText etReflection;

    private TextView tvStressValue;
    private SeekBar seekStress;

    // Sun..Sat circles
    private TextView[] dayCircles;
    private int selectedCircleIndex = -1;

    private String selectedDateKey = ""; // yyyyMMdd
    private boolean isLoading = false;

    // swipe-week vars
    private float weekDownX = 0f;
    private boolean weekDragging = false;

    // ✅ reduce visual noise: only highlight circles after user taps
    private boolean hasCircleInteracted = false;

    // ✅ DB repo
    private ZenPathRepository repo;

    private static final String[] MOOD_LABELS = new String[]{
            "Sad", "Angry", "Okay", "Good", "Happy"
    };

    private static final String PREFS = "zen_path_prefs";
    private static final String KEY_CURRENT_USER = "current_user";

    // ✅ soft saved feedback
    private TextView tvSaved;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private Runnable hideSavedRunnable;

    // ✅ reflection debounce
    private Runnable reflectionSaveRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_mt);

        repo = new ZenPathRepository(this);

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

        // ===== Bind =====
        calendarView = findViewById(R.id.calendarView);

        sheetScrim = findViewById(R.id.sheetScrim);
        bottomSheet = findViewById(R.id.moodBottomSheet);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);

        etReflection = findViewById(R.id.etReflection);

        seekStress = findViewById(R.id.seekStress);
        tvStressValue = findViewById(R.id.tvStressValue);

        tvSaved = findViewById(R.id.tvSaved);

        TextView tvMood0 = findViewById(R.id.tvMood0);
        TextView tvMood1 = findViewById(R.id.tvMood1);
        TextView tvMood2 = findViewById(R.id.tvMood2);
        TextView tvMood3 = findViewById(R.id.tvMood3);
        TextView tvMood4 = findViewById(R.id.tvMood4);
        moodViews = new TextView[]{tvMood0, tvMood1, tvMood2, tvMood3, tvMood4};

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
        installWeekSwipe();
        setupStressSaver();
        setupReflectionSaver();

        hideSheet(false);

        String fromHistory = getIntent().getStringExtra(EXTRA_DATE_KEY);
        if (!TextUtils.isEmpty(fromHistory)) {
            Calendar cal = parseDateKey(fromHistory);
            if (cal != null && calendarView != null) {
                calendarView.setDate(cal.getTimeInMillis(), false, true);
            }
            onDateSelected(fromHistory);
            showSheetHalf();
            return;
        }

        selectTodayWithoutOpeningSheet();
    }

    // ===================== USER ID =====================
    private long currentUserId() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String s = prefs.getString(KEY_CURRENT_USER, null);
        if (s == null) return -1;
        try { return Long.parseLong(s); } catch (Exception e) { return -1; }
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
        if (TextUtils.isEmpty(dateKey)) return;

        selectedDateKey = dateKey;

        if (tvSelectedDate != null) tvSelectedDate.setText(prettyDate(dateKey));

        long userId = currentUserId();
        if (userId <= 0) return;

        // ✅ LOAD MOOD
        String[] moodData = repo.getMoodByDate(userId, dateKey);
        String moodText = moodData != null ? moodData[0] : "";
        String reflection = moodData != null ? moodData[1] : "";
        selectedMoodIndex = moodTextToIndex(moodText);

        // ✅ LOAD STRESS
        StressRow sr = getStressRow(userId, dateKey);

        isLoading = true;

        if (seekStress != null) seekStress.setProgress(sr.level);
        updateStressUI(sr.level);

        if (etReflection != null) etReflection.setText(reflection == null ? "" : reflection);

        isLoading = false;

        updateMoodUI();

        // ✅ update week circles but don't highlight until user taps
        Calendar selected = parseDateKey(dateKey);
        if (selected != null) updateWeekCircles(selected);

        // hide saved label when switching dates
        hideSavedInstant();
    }

    // ===================== MOOD =====================
    private void setupMoodClicks() {
        for (int i = 0; i < moodViews.length; i++) {
            final int idx = i;
            if (moodViews[i] == null) continue;

            moodViews[i].setOnClickListener(v -> {
                selectedMoodIndex = idx;
                updateMoodUI();
                saveMoodAndReflection();
                showSavedSoft();
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

    private int moodTextToIndex(String moodText) {
        if (moodText == null) return -1;
        for (int i = 0; i < MOOD_LABELS.length; i++) {
            if (moodText.equalsIgnoreCase(MOOD_LABELS[i])) return i;
        }
        return -1;
    }

    private String indexToMoodText(int idx) {
        if (idx < 0 || idx >= MOOD_LABELS.length) return "";
        return MOOD_LABELS[idx];
    }

    private void saveMoodAndReflection() {
        if (TextUtils.isEmpty(selectedDateKey)) return;

        long userId = currentUserId();
        if (userId <= 0) return;

        String moodText = indexToMoodText(selectedMoodIndex);
        String reflection = etReflection != null ? etReflection.getText().toString() : "";

        repo.upsertMood(userId, selectedDateKey, moodText, reflection);
    }

    // ===================== WEEK CIRCLES =====================
    private void setupCircleClicks() {
        for (int i = 0; i < dayCircles.length; i++) {
            final int idx = i;
            if (dayCircles[i] == null) continue;
            dayCircles[i].setOnClickListener(v -> {
                hasCircleInteracted = true;
                selectedCircleIndex = idx;
                updateCircleUI();
            });
        }
    }

    private void updateWeekCircles(Calendar selectedDate) {
        Calendar start = (Calendar) selectedDate.clone();
        int dow = start.get(Calendar.DAY_OF_WEEK);
        int diffToSun = dow - Calendar.SUNDAY;
        start.add(Calendar.DAY_OF_MONTH, -diffToSun);

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) start.clone();
            day.add(Calendar.DAY_OF_MONTH, i);
            if (dayCircles[i] != null) {
                dayCircles[i].setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
            }
        }

        // ✅ don't highlight automatically (less noise)
        if (!hasCircleInteracted) selectedCircleIndex = -1;

        updateCircleUI();
    }

    private void updateCircleUI() {
        for (int i = 0; i < dayCircles.length; i++) {
            if (dayCircles[i] == null) continue;

            boolean selected = (i == selectedCircleIndex);

            dayCircles[i].setScaleX(selected ? 1.20f : 1.0f);
            dayCircles[i].setScaleY(selected ? 1.20f : 1.0f);
            dayCircles[i].setAlpha(selected ? 1.0f : 0.70f);

            dayCircles[i].setTextColor(selected ? 0xFF1E1E1E : 0xFF3A3A3A);
        }
    }

    // ===================== SWIPE WEEK =====================
    private void installWeekSwipe() {
        if (dayCircles == null || dayCircles.length == 0 || dayCircles[0] == null) return;

        View parent1 = (View) dayCircles[0].getParent();
        if (parent1 == null) return;
        View weekRow = (View) parent1.getParent();
        if (weekRow == null) return;

        weekRow.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    weekDownX = event.getRawX();
                    weekDragging = true;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!weekDragging) return false;
                    weekDragging = false;

                    float dx = event.getRawX() - weekDownX;
                    float threshold = dp(60);
                    if (Math.abs(dx) < threshold) return true;

                    // swiping week is a user interaction -> allow highlight after this
                    hasCircleInteracted = true;

                    if (dx < 0) shiftSelectedDateByDays(+7);
                    else shiftSelectedDateByDays(-7);
                    return true;
            }
            return false;
        });
    }

    private void shiftSelectedDateByDays(int days) {
        if (TextUtils.isEmpty(selectedDateKey)) {
            selectTodayWithoutOpeningSheet();
            return;
        }

        Calendar current = parseDateKey(selectedDateKey);
        if (current == null) return;

        current.add(Calendar.DAY_OF_MONTH, days);
        String newKey = formatDateKey(current);

        if (calendarView != null) {
            calendarView.setDate(current.getTimeInMillis(), false, true);
        }

        onDateSelected(newKey);
    }

    private int dp(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (dp * d);
    }

    // ===================== STRESS =====================
    private void setupStressSaver() {
        if (seekStress == null) return;

        seekStress.setMax(100);

        seekStress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateStressUI(progress);
                if (fromUser && !isLoading) {
                    saveStressForSelectedDate(progress);
                    showSavedSoft();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateStressUI(int stress) {
        if (tvStressValue != null) tvStressValue.setText(stress + "%");

        if (seekStress != null) {
            int resId;
            if (stress <= 33) resId = R.drawable.progress_stress_low;
            else if (stress <= 66) resId = R.drawable.progress_stress_med;
            else resId = R.drawable.progress_stress_high;

            seekStress.setProgressDrawable(ContextCompat.getDrawable(this, resId));
        }
    }

    // ===================== REFLECTION =====================
    private void setupReflectionSaver() {
        if (etReflection == null) return;

        etReflection.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override public void afterTextChanged(Editable s) {
                if (isLoading) return;

                // ✅ debounce: save after user pauses typing
                if (reflectionSaveRunnable != null) ui.removeCallbacks(reflectionSaveRunnable);

                reflectionSaveRunnable = () -> {
                    saveMoodAndReflection();
                    showSavedSoft();
                };

                ui.postDelayed(reflectionSaveRunnable, 450);
            }
        });
    }

    private void saveStressForSelectedDate(int stress) {
        if (TextUtils.isEmpty(selectedDateKey)) return;

        long userId = currentUserId();
        if (userId <= 0) return;

        StressRow sr = getStressRow(userId, selectedDateKey);
        repo.upsertStress(userId, selectedDateKey, stress, sr.starSec, sr.lanternSec, sr.planetSec);
    }

    // ===================== SAVED FEEDBACK =====================
    private void showSavedSoft() {
        if (tvSaved == null) return;

        // cancel hide if already scheduled
        if (hideSavedRunnable != null) ui.removeCallbacks(hideSavedRunnable);

        tvSaved.setVisibility(View.VISIBLE);
        tvSaved.animate().cancel();
        tvSaved.setAlpha(0f);
        tvSaved.animate().alpha(1f).setDuration(130).start();

        hideSavedRunnable = () -> {
            if (tvSaved == null) return;
            tvSaved.animate()
                    .alpha(0f)
                    .setDuration(220)
                    .withEndAction(() -> {
                        if (tvSaved != null) tvSaved.setVisibility(View.GONE);
                    })
                    .start();
        };

        ui.postDelayed(hideSavedRunnable, 1200);
    }

    private void hideSavedInstant() {
        if (tvSaved == null) return;
        if (hideSavedRunnable != null) ui.removeCallbacks(hideSavedRunnable);
        tvSaved.animate().cancel();
        tvSaved.setAlpha(0f);
        tvSaved.setVisibility(View.GONE);
    }

    // ===================== STRESS ROW LOADER =====================
    private StressRow getStressRow(long userId, String dateKey) {
        StressRow out = new StressRow();

        ZenPathDbHelper helper = new ZenPathDbHelper(this);
        try (android.database.sqlite.SQLiteDatabase db = helper.getReadableDatabase()) {

            String sql =
                    "SELECT " +
                            ZenPathDbHelper.S_LEVEL + ", " +
                            ZenPathDbHelper.S_PLAY_STAR + ", " +
                            ZenPathDbHelper.S_PLAY_LANTERN + ", " +
                            ZenPathDbHelper.S_PLAY_PLANET +
                            " FROM " + ZenPathDbHelper.T_STRESS +
                            " WHERE " + ZenPathDbHelper.COL_USER_ID + "=? AND " + ZenPathDbHelper.S_DATE + "=? " +
                            " ORDER BY " + ZenPathDbHelper.S_CREATED_AT + " DESC LIMIT 1";

            try (android.database.Cursor c = db.rawQuery(sql,
                    new String[]{String.valueOf(userId), dateKey})) {

                if (c.moveToFirst()) {
                    out.level = c.getInt(0);
                    out.starSec = c.getInt(1);
                    out.lanternSec = c.getInt(2);
                    out.planetSec = c.getInt(3);
                } else {
                    out.level = 50;
                }
            }
        } catch (Exception e) {
            out.level = 50;
        }

        return out;
    }

    private static class StressRow {
        int level = 50;
        int starSec = 0;
        int lanternSec = 0;
        int planetSec = 0;
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
