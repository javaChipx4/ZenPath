package com.example.zenpath;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class StarSweepActivity extends AppCompatActivity {

    private StarSweepView starSweepView;
    private Button btnPlayAgain, btnPause, btnResume, btnReset;
    private View pauseOverlay;
    private TextView tvBreath, tvStarsLeft, tvStreakGame;
    private ProgressBar progressStars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_star_sweep);

        starSweepView = findViewById(R.id.starSweepView);

        btnPlayAgain = findViewById(R.id.btnPlayAgain);
        btnPause = findViewById(R.id.btnPause);
        btnResume = findViewById(R.id.btnResume);
        btnReset = findViewById(R.id.btnReset);

        pauseOverlay = findViewById(R.id.pauseOverlay);

        tvBreath = findViewById(R.id.tvBreath);
        tvStarsLeft = findViewById(R.id.tvStarsLeft);
        tvStreakGame = findViewById(R.id.tvStreakGame);
        progressStars = findViewById(R.id.progressStars);

        // Show current streak
        tvStreakGame.setText("Daily streak: " + StreakManager.getStreak(this) + " 🔥");

        // IMPORTANT: Hide Play Again until flash animation finishes
        btnPlayAgain.setVisibility(View.GONE);

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
                // Optional: you can show a message here if you want
                // tvBreath.setText("Nice ✨");
            }

            @Override
            public void onFinishedReady() {
                // Streak counts ONLY when the 2s flash is done
                int newStreak = StreakManager.recordCompletion(StarSweepActivity.this);
                tvStreakGame.setText("Daily streak: " + newStreak + " 🔥");

                btnPlayAgain.setVisibility(View.VISIBLE);
            }
        });

        btnPlayAgain.setOnClickListener(v -> {
            btnPlayAgain.setVisibility(View.GONE);
            starSweepView.resetGame();
        });

        btnPause.setOnClickListener(v -> {
            starSweepView.setPaused(true);
            pauseOverlay.setVisibility(View.VISIBLE);
        });

        btnResume.setOnClickListener(v -> {
            pauseOverlay.setVisibility(View.GONE);
            starSweepView.setPaused(false);
        });

        btnReset.setOnClickListener(v -> {
            pauseOverlay.setVisibility(View.GONE);
            btnPlayAgain.setVisibility(View.GONE);
            starSweepView.resetGame();
            starSweepView.setPaused(false);
        });
    }
}
