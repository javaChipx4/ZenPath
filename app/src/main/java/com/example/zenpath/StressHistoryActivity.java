package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class StressHistoryActivity extends AppCompatActivity {

    private ZenPathRepository repo;

    private ViewPager2 weekPager;
    private WeekDatesPagerAdapter weekAdapter;

    private ProgressBar progressStress;
    private TextView tvStressValue;

    // Recently played (3 games)
    private TextView tvGame1, tvGame2, tvGame3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stress_history);

        repo = new ZenPathRepository(this);

        // UI
        progressStress = findViewById(R.id.progressStress);
        tvStressValue = findViewById(R.id.tvStressValue);

        tvGame1 = findViewById(R.id.tvGame1);
        tvGame2 = findViewById(R.id.tvGame2);
        tvGame3 = findViewById(R.id.tvGame3);

        setupTabs();
        setupPopup();

        // Week pager
        weekPager = findViewById(R.id.weekPager);

        weekAdapter = new WeekDatesPagerAdapter(date -> {
            // click a date -> load stress from sqlite
            loadStressFromSql(date);
        });

        weekPager.setAdapter(weekAdapter);
        weekPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        weekPager.setOffscreenPageLimit(1);

        // ✅ When user swipes week, automatically load selected day in that week
        weekPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                weekAdapter.onPageChanged(position);
            }
        });

        // start center (current week) + trigger today load
        weekPager.post(() -> {
            weekPager.setCurrentItem(weekAdapter.getCenter(), false);
            weekAdapter.triggerTodaySelection(); // loads today automatically
        });

        // Swipe navigation between tabs (optional)
        View tabRow = findViewById(R.id.tabRow);
        if (tabRow != null) {
            HistorySwipeHelper.attach(this, tabRow, MoodHistoryActivity.class, null);
        }
        View root = findViewById(R.id.main);
        if (root != null) {
            HistorySwipeHelper.attach(this, root, MoodHistoryActivity.class, null);
        }
    }

    private void loadStressFromSql(String date) {
        new Thread(() -> {
            ZenPathRepository.StressRecord record = repo.getStressByDate(date);

            runOnUiThread(() -> {
                int percent = 0;

                if (record != null) {
                    percent = record.level; // 0..100
                }

                if (progressStress != null) progressStress.setProgress(percent);
                if (tvStressValue != null) tvStressValue.setText(percent + "%");

                // ✅ For now: static games list (next step: save/load from DB)
                showRecentGames();
            });
        }).start();
    }

    private void showRecentGames() {
        if (tvGame1 != null) tvGame1.setText("Tap the Calm Game");
        if (tvGame2 != null) tvGame2.setText("Star Sweep Game");
        if (tvGame3 != null) tvGame3.setText("Breathing Bubble");
    }

    private void setupTabs() {
        Button btnDiary = findViewById(R.id.btnDiaryTab);
        Button btnMood  = findViewById(R.id.btnMoodTab);
        Button btnStress = findViewById(R.id.btnStressTab);

        if (btnStress != null) btnStress.setEnabled(false);

        if (btnDiary != null) btnDiary.setOnClickListener(v -> {
            startActivity(new Intent(this, DiaryHistoryActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });

        if (btnMood != null) btnMood.setOnClickListener(v -> {
            startActivity(new Intent(this, MoodHistoryActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });
    }

    private void setupPopup() {
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

        if (btnHome != null) btnHome.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        if (btnBack != null) btnBack.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
            finish();
        });

        if (btnMood != null) btnMood.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
            startActivity(new Intent(this, MoodActivity.class));
        });
    }
}
