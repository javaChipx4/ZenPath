package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MoodHistoryActivity extends AppCompatActivity {

    private ZenPathRepository repo;

    private TextView tvMoodNote;
    private TextView emoji1, emoji2, emoji3, emoji4, emoji5;

    // ✅ Swipeable dots pager
    private ViewPager2 weekPager;
    private WeekDatesPagerAdapter weekAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_history);

        repo = new ZenPathRepository(this);

        tvMoodNote = findViewById(R.id.tvMoodNote);
        emoji1 = findViewById(R.id.tvEmoji1);
        emoji2 = findViewById(R.id.tvEmoji2);
        emoji3 = findViewById(R.id.tvEmoji3);
        emoji4 = findViewById(R.id.tvEmoji4);
        emoji5 = findViewById(R.id.tvEmoji5);

        setupTabs();
        setupPopupWithRename();

        setupWeekPager(); // ✅ replaces setupWeekDots()

        View tabRow = findViewById(R.id.tabRow);
        if (tabRow != null) {
            HistorySwipeHelper.attach(this, tabRow, DiaryHistoryActivity.class, StressHistoryActivity.class);
        }

        View root = findViewById(R.id.main);
        if (root != null) {
            HistorySwipeHelper.attach(this, root, DiaryHistoryActivity.class, StressHistoryActivity.class);
        }
    }

    // ===================== WEEK PAGER (SWIPE LEFT/RIGHT) =====================

    private void setupWeekPager() {
        weekPager = findViewById(R.id.weekPager);
        if (weekPager == null) return;

        weekAdapter = new WeekDatesPagerAdapter(date -> {
            loadMoodFromSql(date);
        });


        weekPager.setAdapter(weekAdapter);
        weekPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        weekPager.setOffscreenPageLimit(1);

        int center = weekAdapter.getCenter();
        weekPager.setCurrentItem(center, false);

        // Default load: today (this week + today's weekday)
        int todayDotIndex = getTodayIndexMonFirst();
        weekAdapter.select(center, todayDotIndex);
        loadMoodFromSql(getDateForWeek(center, todayDotIndex, center));

        // When user swipes to another week, keep the same weekday selected (today's weekday)
        weekPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int dotIndex = getTodayIndexMonFirst();
                weekAdapter.select(position, dotIndex);

                String date = getDateForWeek(position, dotIndex, center);
                loadMoodFromSql(date);
            }
        });
    }

    // Monday-first index: Mon=0 ... Sun=6
    private int getTodayIndexMonFirst() {
        Calendar c = Calendar.getInstance();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SUNDAY) return 6;
        return dow - Calendar.MONDAY;
    }

    // Convert pager week + dot index into yyyy-MM-dd
    private String getDateForWeek(int pagePos, int dotIndex, int center) {
        int weekOffset = pagePos - center;

        Calendar monday = Calendar.getInstance();
        int dow = monday.get(Calendar.DAY_OF_WEEK);
        int diff = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        monday.add(Calendar.DAY_OF_MONTH, -diff);          // go to Monday
        monday.add(Calendar.DAY_OF_MONTH, weekOffset * 7); // shift weeks
        monday.add(Calendar.DAY_OF_MONTH, dotIndex);       // shift day inside week

        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(monday.getTime());
    }

    // ===================== LOAD FROM SQLITE =====================

    private void loadMoodFromSql(String date) {
        new Thread(() -> {
            MoodRecord record = repo.getMoodByDate(date);

            runOnUiThread(() -> {
                resetEmojiOpacity();
                if (record != null) {
                    tvMoodNote.setText(record.note);
                    highlightMoodEmoji(record.moodValue);
                } else {
                    tvMoodNote.setText("No mood saved for this day yet.");
                }
            });
        }).start();
    }

    private void highlightMoodEmoji(int moodValue) {
        if (moodValue == 1 && emoji1 != null) emoji1.setAlpha(1.0f);
        else if (moodValue == 2 && emoji2 != null) emoji2.setAlpha(1.0f);
        else if (moodValue == 3 && emoji3 != null) emoji3.setAlpha(1.0f);
        else if (moodValue == 4 && emoji4 != null) emoji4.setAlpha(1.0f);
        else if (moodValue == 5 && emoji5 != null) emoji5.setAlpha(1.0f);
    }

    private void resetEmojiOpacity() {
        float dim = 0.3f;
        if (emoji1 != null) emoji1.setAlpha(dim);
        if (emoji2 != null) emoji2.setAlpha(dim);
        if (emoji3 != null) emoji3.setAlpha(dim);
        if (emoji4 != null) emoji4.setAlpha(dim);
        if (emoji5 != null) emoji5.setAlpha(dim);
    }

    // ===================== TABS =====================

    private void setupTabs() {
        Button btnDiary = findViewById(R.id.btnDiaryTab);
        Button btnMood = findViewById(R.id.btnMoodTab);
        Button btnStress = findViewById(R.id.btnStressTab);

        if (btnDiary != null) {
            btnDiary.setOnClickListener(v -> {
                startActivity(new Intent(this, DiaryHistoryActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            });
        }

        if (btnMood != null) btnMood.setEnabled(false);

        if (btnStress != null) {
            btnStress.setOnClickListener(v -> {
                startActivity(new Intent(this, StressHistoryActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            });
        }
    }

    // ===================== POPUP =====================

    private void setupPopupWithRename() {
        ViewGroup rootView = findViewById(android.R.id.content);
        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        if (menuIcon != null) menuIcon.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));

        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

        View card = settingsPopup.findViewById(R.id.settingsCard);
        if (card != null) card.setOnClickListener(v -> {});

        ImageButton btnHome = settingsPopup.findViewById(R.id.btnHome);
        ImageButton btnBack = settingsPopup.findViewById(R.id.btnBack);
        ImageButton btnMood = settingsPopup.findViewById(R.id.btnMood);

        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        }

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(this, MoodActivity.class));
            });
        }
    }
}
