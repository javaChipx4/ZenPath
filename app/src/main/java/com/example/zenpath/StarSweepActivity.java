package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class StarSweepActivity extends AppCompatActivity {

    private StarSweepView starSweepView;
    private View btnPlayAgain;
    private TextView tvBreath, tvStarsLeft, tvStreakGame;
    private ProgressBar progressStars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_star_sweep);

        starSweepView = findViewById(R.id.starSweepView);

        btnPlayAgain = findViewById(R.id.btnPlayAgain);

        tvBreath = findViewById(R.id.tvBreath);
        tvStarsLeft = findViewById(R.id.tvStarsLeft);
        tvStreakGame = findViewById(R.id.tvStreakGame);
        progressStars = findViewById(R.id.progressStars);

        // Show current streak
        tvStreakGame.setText("Daily streak: " + StreakManager.getStreak(this) + " 🔥");

        // Hide Play Again until flash animation finishes
        btnPlayAgain.setVisibility(View.GONE);

        // ===== Settings Popup Overlay (reuse dialog_settings) =====
        ViewGroup rootView = findViewById(android.R.id.content);
        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);

        // Open popup via burger icon (menuIcon in your HUD)
        ImageView menuIcon = findViewById(R.id.menuIcon);
        if (menuIcon != null) {
            menuIcon.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));
        }

        // Close popup when tapping outside
        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

        // Prevent closing when tapping inside the card
        View settingsCard = settingsPopup.findViewById(R.id.settingsCard);
        if (settingsCard != null) settingsCard.setOnClickListener(v -> {});

        // Popup buttons
        ImageButton btnHome = settingsPopup.findViewById(R.id.btnHome);
        ImageButton btnBack = settingsPopup.findViewById(R.id.btnBack);
        ImageButton btnHistory = settingsPopup.findViewById(R.id.btnHistory);
        ImageButton btnMood = settingsPopup.findViewById(R.id.btnMood);

        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(StarSweepActivity.this, MainActivity.class));
                finish();
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                finish();
            });
        }

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(StarSweepActivity.this, DiaryHistoryActivity.class));
            });
        }

        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(StarSweepActivity.this, MoodActivity.class));
            });
        }

        // (Switch Profile is GONE by default in dialog_settings, so no need to hide it here)

        // ===== Game HUD callbacks =====
        starSweepView.setHudListener(new StarSweepView.HudListener() {
            @Override
            public void onBreathText(String text) {
                tvBreath.setText(text);
            }

            @Override
            public void onStarsLeft(int left, int total) {
                tvStarsLeft.setText("Stars left: " + left);
                int pct = (total == 0) ? 0 : (int) (((total - left) * 100f) / total);
                progressStars.setProgress(pct);
            }

            @Override
            public void onFinishFlashStarted() {
                // optional
            }

            @Override
            public void onFinishedReady() {
                int newStreak = StreakManager.recordCompletion(StarSweepActivity.this);
                tvStreakGame.setText("Daily streak: " + newStreak + " 🔥");
                btnPlayAgain.setVisibility(View.VISIBLE);
            }
        });

        btnPlayAgain.setOnClickListener(v -> {
            btnPlayAgain.setVisibility(View.GONE);
            starSweepView.resetGame();
        });
    }
}
