package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class StarSweepActivity extends AppCompatActivity
        implements StarSweepView.HudListener {

    private StarSweepView starSweepView;

    private TextView tvBreath, tvStarsLeft, tvStreakGame;
    private ProgressBar progressStars;

    private ImageButton btnSettings;
    private Button btnPlayAgain;

    // ✅ streak prefs
    private SharedPreferences prefs;
    private static final String PREFS = "zenpath_star_sweep";
    private static final String KEY_STREAK = "daily_streak";

    private int goal = 1;

    // ✅ Settings dialog ref (so we can close it safely)
    private AlertDialog settingsDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_star_sweep);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // ✅ View references
        starSweepView = findViewById(R.id.starSweepView);

        tvBreath = findViewById(R.id.tvBreath);
        tvStarsLeft = findViewById(R.id.tvStarsLeft);
        tvStreakGame = findViewById(R.id.tvStreakGame);
        progressStars = findViewById(R.id.progressStars);

        btnSettings = findViewById(R.id.btnSettings);
        btnPlayAgain = findViewById(R.id.btnPlayAgain);

        // ✅ Connect HUD
        starSweepView.setHudListener(this);

        // ✅ Cog pauses + opens settings
        btnSettings.setOnClickListener(v -> openSettingsPaused());

        // ✅ Play again
        btnPlayAgain.setOnClickListener(v -> {
            btnPlayAgain.setVisibility(View.GONE);
            starSweepView.setPaused(false);
            starSweepView.resetGame();
        });

        updateStreakUI();
    }

    // ================= HUD CALLBACKS =================

    @Override
    public void onBreathText(String text) {
        tvBreath.setText(text);
    }

    @Override
    public void onProgress(int selected, int goal) {
        this.goal = Math.max(1, goal);

        tvStarsLeft.setText("Connect: " + selected + " / " + goal);

        int pct = (int) ((selected * 100f) / this.goal);
        progressStars.setProgress(pct);
    }

    @Override
    public void onFinishFlashStarted() {
        // optional
    }

    @Override
    public void onFinishedReady() {
        // ✅ show play again
        btnPlayAgain.setVisibility(View.VISIBLE);

        // ✅ streak++
        int streak = prefs.getInt(KEY_STREAK, 0);
        streak++;
        prefs.edit().putInt(KEY_STREAK, streak).apply();

        updateStreakUI();
    }

    // ================= SETTINGS (PAUSE) =================

    private void openSettingsPaused() {
        // ✅ Pause game when settings opens
        starSweepView.setPaused(true);

        String[] options = {
                "Resume",
                "Restart Shape",
                "Back to Selection"
        };

        settingsDialog = new AlertDialog.Builder(this)
                .setTitle("Paused ⚙️")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // ✅ Resume
                        resumeGame();
                    } else if (which == 1) {
                        // ✅ Restart
                        btnPlayAgain.setVisibility(View.GONE);
                        starSweepView.resetGame();
                        starSweepView.setPaused(false);
                    } else if (which == 2) {
                        // ✅ Back to selection screen
                        goBackToSelection();
                    }
                })
                .setOnCancelListener(d -> {
                    // ✅ If user taps outside/back button: resume
                    resumeGame();
                })
                .show();
    }

    private void resumeGame() {
        if (settingsDialog != null && settingsDialog.isShowing()) {
            settingsDialog.dismiss();
        }
        starSweepView.setPaused(false);
    }

    private void goBackToSelection() {
        // ✅ Unpause before leaving (clean)
        starSweepView.setPaused(false);

        // 🔥 CHANGE THIS to your real selection activity class name if different
        Intent i = new Intent(StarSweepActivity.this, SelectionGamesActivity.class);

        startActivity(i);
        finish(); // close this game screen
    }

    // ================= STREAK UI =================

    private void updateStreakUI() {
        int streak = prefs.getInt(KEY_STREAK, 0);
        tvStreakGame.setText("Daily streak: " + streak + " 🔥");
    }

    // ✅ Safety: if user leaves app while paused, don't keep stuck
    @Override
    protected void onPause() {
        super.onPause();
        // keep paused (ok) — or resume automatically if you want
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (settingsDialog != null) {
            settingsDialog.dismiss();
            settingsDialog = null;
        }
    }
}
