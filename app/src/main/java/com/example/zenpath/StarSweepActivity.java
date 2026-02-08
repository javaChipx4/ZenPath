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

    private TextView tvBreath, tvStarsLeft, tvStreakGame, tvFact;
    private ProgressBar progressStars;

    private ImageButton btnSettings;
    private Button btnPlayAgain;

    // ✅ Play time tracker
    private GameTimeTracker playTracker;

    // ✅ streak prefs
    private SharedPreferences prefs;
    private static final String PREFS = "zenpath_star_sweep";
    private static final String KEY_STREAK = "daily_streak";

    private int goal = 1;

    // ✅ Settings dialog ref
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
        tvFact = findViewById(R.id.tvFact); // ✅ IMPORTANT
        progressStars = findViewById(R.id.progressStars);

        btnSettings = findViewById(R.id.menuIcon);
        btnPlayAgain = findViewById(R.id.btnPlayAgain);

        // ✅ Fact should be hidden until finished
        tvFact.setVisibility(View.GONE);

        // ✅ Connect HUD
        starSweepView.setHudListener(this);

        // ✅ Cog pauses + opens settings
        btnSettings.setOnClickListener(v -> openSettingsPaused());

        // ✅ Play again
        btnPlayAgain.setOnClickListener(v -> {
            btnPlayAgain.setVisibility(View.GONE);
            tvFact.setVisibility(View.GONE); // ✅ hide fact again
            starSweepView.setPaused(false);
            starSweepView.resetGame();
        });

        updateStreakUI();

        // ✅ Start play time tracking
        playTracker = new GameTimeTracker("Star Sweep");
        playTracker.start();
    }

    // ================= HUD CALLBACKS =================

    @Override
    public void onBreathText(String text) {
        tvBreath.setText(text);
    }

    @Override
    public void onFactText(String fact) {
        if (fact == null || fact.trim().isEmpty()) {
            tvFact.setText("Fact: —");
        } else {
            tvFact.setText("Fact: " + fact);
        }
        // keep it hidden until finished (we show it in onFinishedReady)
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
        // ✅ show play again + fact
        btnPlayAgain.setVisibility(View.VISIBLE);
        tvFact.setVisibility(View.VISIBLE);

        // ✅ streak++
        int streak = prefs.getInt(KEY_STREAK, 0);
        streak++;
        prefs.edit().putInt(KEY_STREAK, streak).apply();
        updateStreakUI();
    }

    // ================= SETTINGS (PAUSE) =================

    private void openSettingsPaused() {
        starSweepView.setPaused(true);

        String[] options = {"Resume", "Restart Shape", "Back to Selection"};

        settingsDialog = new AlertDialog.Builder(this)
                .setTitle("Paused ⚙️")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        resumeGame();
                    } else if (which == 1) {
                        btnPlayAgain.setVisibility(View.GONE);
                        tvFact.setVisibility(View.GONE); // ✅ hide
                        starSweepView.resetGame();
                        starSweepView.setPaused(false);
                    } else {
                        goBackToSelection();
                    }
                })
                .setOnCancelListener(d -> resumeGame())
                .show();
    }

    private void resumeGame() {
        if (settingsDialog != null && settingsDialog.isShowing()) {
            settingsDialog.dismiss();
        }
        starSweepView.setPaused(false);
    }

    private void goBackToSelection() {
        if (playTracker != null) playTracker.stopAndSave(this);

        starSweepView.setPaused(false);

        Intent i = new Intent(StarSweepActivity.this, SelectionGamesActivity.class);
        startActivity(i);
        finish();
    }

    // ================= STREAK UI =================

    private void updateStreakUI() {
        int streak = prefs.getInt(KEY_STREAK, 0);
        tvStreakGame.setText("Daily streak: " + streak + " 🔥");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (playTracker != null) playTracker.stopAndSave(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playTracker == null) playTracker = new GameTimeTracker("Star Sweep");
        playTracker.start();
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
