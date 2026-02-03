package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MoodHistoryActivity extends AppCompatActivity {

    private TextView tvMoodNote;
    // Changed to TextView because your XML uses TextViews for emojis
    private TextView emoji1, emoji2, emoji3, emoji4, emoji5;
    private String currentSelectedDate = "2026-02-03"; // Default date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_history);

        // 1. Initialize UI Views (Matching your XML IDs)
        tvMoodNote = findViewById(R.id.tvMoodNote);
        emoji1 = findViewById(R.id.tvEmoji1);
        emoji2 = findViewById(R.id.tvEmoji2);
        emoji3 = findViewById(R.id.tvEmoji3);
        emoji4 = findViewById(R.id.tvEmoji4);
        emoji5 = findViewById(R.id.tvEmoji5);

        // 2. Setup Weekday Click Listeners (Using the dot IDs from your XML)
        if (findViewById(R.id.dotTue) != null) {
            findViewById(R.id.dotTue).setOnClickListener(v -> loadMoodFromDb("2026-02-03"));
        }
        if (findViewById(R.id.dotWed) != null) {
            findViewById(R.id.dotWed).setOnClickListener(v -> loadMoodFromDb("2026-02-04"));
        }
        // Add dotMon, dotThu, etc., here following the same pattern

        // 3. Load initial data
        loadMoodFromDb(currentSelectedDate);

        // 4. Setup existing UI helpers
        setupTabs();
        setupPopupWithRename();

        View tabRow = findViewById(R.id.tabRow);
        if (tabRow != null) {
            HistorySwipeHelper.attach(this, tabRow, DiaryHistoryActivity.class, StressHistoryActivity.class);
        }

        View root = findViewById(R.id.main);
        if (root != null) {
            HistorySwipeHelper.attach(this, root, DiaryHistoryActivity.class, StressHistoryActivity.class);
        }
    }

    // ✅ FIXED: Method to fetch data from Room Database
    private void loadMoodFromDb(String date) {
        new Thread(() -> {
            // Get data from database using the singleton instance
            MoodEntry entry = MoodDatabase.getInstance(this).moodDao().getMoodByDate(date);

            // Update UI on the main thread
            runOnUiThread(() -> {
                resetEmojiOpacity(); // Dim all emojis first
                if (entry != null) {
                    tvMoodNote.setText(entry.getReflection());
                    highlightMoodEmoji(entry.getMoodType());
                } else {
                    tvMoodNote.setText("Select a date to view mood note...");
                }
            });
        }).start();
    }

    // ✅ Helper to highlight the correct emoji
    private void highlightMoodEmoji(int moodType) {
        // moodType comes from Screen 1 (1 to 5)
        if (moodType == 1 && emoji1 != null) emoji1.setAlpha(1.0f);
        else if (moodType == 2 && emoji2 != null) emoji2.setAlpha(1.0f);
        else if (moodType == 3 && emoji3 != null) emoji3.setAlpha(1.0f);
        else if (moodType == 4 && emoji4 != null) emoji4.setAlpha(1.0f);
        else if (moodType == 5 && emoji5 != null) emoji5.setAlpha(1.0f);
    }

    // ✅ Helper to reset emoji appearance
    private void resetEmojiOpacity() {
        float dimAlpha = 0.3f; // Make them look unselected
        if (emoji1 != null) emoji1.setAlpha(dimAlpha);
        if (emoji2 != null) emoji2.setAlpha(dimAlpha);
        if (emoji3 != null) emoji3.setAlpha(dimAlpha);
        if (emoji4 != null) emoji4.setAlpha(dimAlpha);
        if (emoji5 != null) emoji5.setAlpha(dimAlpha);
    }

    private void setupTabs() {
        Button btnDiary = findViewById(R.id.btnDiaryTab);
        Button btnMood  = findViewById(R.id.btnMoodTab);
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
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(this, MoodActivity.class));
            });
        }
    }
}