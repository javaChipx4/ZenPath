package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

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

    // ✅ overlay helper
    private final GamePauseOverlay pause = new GamePauseOverlay();

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
        tvFact = findViewById(R.id.tvFact);
        progressStars = findViewById(R.id.progressStars);

        btnSettings = findViewById(R.id.menuIcon);
        btnPlayAgain = findViewById(R.id.btnPlayAgain);

        // ✅ Fact hidden until finished
        tvFact.setVisibility(View.GONE);

        // ✅ Connect HUD
        starSweepView.setHudListener(this);

        // ✅ Cog opens overlay pause/settings (Star-specific)
        btnSettings.setOnClickListener(v -> openStarSettings());

        // ✅ Play again
        btnPlayAgain.setOnClickListener(v -> {
            btnPlayAgain.setVisibility(View.GONE);
            tvFact.setVisibility(View.GONE);
            starSweepView.setPaused(false);
            starSweepView.resetGame();
        });

        updateStreakUI();

        // ✅ Start play time tracking
        playTracker = new GameTimeTracker("Star Sweep");
        playTracker.start();

        // ✅ Start game music immediately (so no silent moment)
        playMusic(MusicService.TRACK_CONSTELLA);
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
    }

    @Override
    public void onProgress(int selected, int goal) {
        this.goal = Math.max(1, goal);
        tvStarsLeft.setText("Connect: " + selected + " / " + this.goal);

        int pct = (int) ((selected * 100f) / this.goal);
        progressStars.setProgress(pct);
    }

    @Override
    public void onFinishFlashStarted() {
        // optional
    }

    @Override
    public void onFinishedReady() {
        btnPlayAgain.setVisibility(View.VISIBLE);
        tvFact.setVisibility(View.VISIBLE);

        int streak = prefs.getInt(KEY_STREAK, 0);
        streak++;
        prefs.edit().putInt(KEY_STREAK, streak).apply();
        updateStreakUI();
    }

    // ================= SETTINGS (OVERLAY) =================

    private void openStarSettings() {
        // pause game
        starSweepView.setPaused(true);

        pause.show(
                this,
                R.layout.dialog_game_pause,
                "Star Sweep ⚙️",
                "Slow breaths. Connect the stars 🌙",
                "Restart shape",
                null, // ✅ REMOVE extra button (Breathing tip)
                new GamePauseOverlay.Actions() {
                    @Override
                    public void onResume() {
                        starSweepView.setPaused(false);
                    }

                    @Override
                    public void onRestart() {
                        btnPlayAgain.setVisibility(View.GONE);
                        tvFact.setVisibility(View.GONE);
                        starSweepView.resetGame();
                        starSweepView.setPaused(false);
                    }

                    @Override
                    public void onBack() {
                        goBackToSelection();
                    }

                    @Override
                    public void onExtra() {
                        // hidden anyway
                    }
                }
        );
    }

    private void goBackToSelection() {
        if (playTracker != null) playTracker.stopAndSave(this);

        starSweepView.setPaused(false);

        // ✅ switch back to main background music
        playMusic(MusicService.TRACK_MAIN);

        Intent i = new Intent(StarSweepActivity.this, SelectionGamesActivity.class);
        startActivity(i);
        finish();
    }

    // ================= MUSIC HELPERS =================

    private void playMusic(int track) {
        Intent i = new Intent(this, MusicService.class);
        i.setAction(MusicService.ACTION_PLAY);
        i.putExtra(MusicService.EXTRA_TRACK, track);
        startService(i);
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

        // ⚠️ Don't force TRACK_MAIN here.
        // If user just opened the pause overlay or went multitask, you'll accidentally switch music.
        // We'll switch music only when leaving to Selection (goBackToSelection).
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playTracker == null) playTracker = new GameTimeTracker("Star Sweep");
        playTracker.start();

        // ✅ ensure Constella music is playing when returning
        playMusic(MusicService.TRACK_CONSTELLA);
    }
}
