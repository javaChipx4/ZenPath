package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LanternReleaseActivity extends AppCompatActivity {

    private LanternReleaseView lanternView;
    private TextView tvTotal, tvBadge, tvMessage;

    // ✅ Play time tracker
    private GameTimeTracker playTracker;

    // ✅ shared pause overlay helper
    private final GamePauseOverlay pause = new GamePauseOverlay();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lantern_release);

        // ✅ View + HUD
        lanternView = findViewById(R.id.lanternView);
        tvTotal = findViewById(R.id.tvTotal);
        tvBadge = findViewById(R.id.tvBadge);
        tvMessage = findViewById(R.id.tvMessage);

        // ✅ Buttons
        View btnPlay = findViewById(R.id.btnLanternGame);
        View btnCog = findViewById(R.id.menuIcon);

        // ✅ UI Listener updates
        lanternView.setUiListener((total, badge, message) -> {
            tvTotal.setText("Total: " + total);
            tvBadge.setText("Badge: " + badge);
            tvMessage.setText(message);
        });

        // ✅ PLAY button logic
        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> {
                // Start releasing lanterns
                if (!lanternView.isRunning()) {
                    lanternView.startRelease();
                }
                // If finished, allow replay (replay keeps lanterns, just resets flight)
                else if (lanternView.isFinished()) {
                    lanternView.playAgain();
                }
            });
        }

        // ✅ Cog -> open overlay menu (Lantern-specific)
        if (btnCog != null) {
            btnCog.setOnClickListener(v -> openLanternSettings());
        }
    }

    // ✅ Start tracking when activity becomes visible/active
    @Override
    protected void onResume() {
        super.onResume();
        if (playTracker == null) playTracker = new GameTimeTracker("Lantern Release");
        playTracker.start();
    }

    // ✅ Stop tracking when leaving / app goes background
    @Override
    protected void onPause() {
        super.onPause();
        if (playTracker != null) playTracker.stopAndSave(this);
    }

    // =========================
    // Lantern Settings Overlay
    // =========================
    private void openLanternSettings() {
        pause.show(
                this,
                R.layout.dialog_game_pause,
                "Lantern Release ⚙️",
                "Write it. Release it. Let it float ✨",
                "Restart lanterns",
                "How to play",
                new GamePauseOverlay.Actions() {

                    @Override
                    public void onResume() {
                        // no pause state here, just close
                    }

                    @Override
                    public void onRestart() {
                        // ✅ This replaces the old Reset button:
                        // Clears lanterns + resets score/badge/message
                        lanternView.resetToday();
                    }

                    @Override
                    public void onBack() {
                        if (playTracker != null) playTracker.stopAndSave(LanternReleaseActivity.this);
                        startActivity(new Intent(LanternReleaseActivity.this, SelectionGamesActivity.class));
                        finish();
                    }

                    @Override
                    public void onExtra() {
                        showLanternHowToPlay();
                    }
                }
        );
    }

    private void showLanternHowToPlay() {
        new AlertDialog.Builder(this)
                .setTitle("How to play")
                .setMessage(
                        "• Tap anywhere to place a lantern\n" +
                                "• Tap a lantern to write a message\n" +
                                "• Press Play\n" +
                                "• Tap a flying lantern to glow + reveal your message"
                )
                .setPositiveButton("Got it", null)
                .show();
    }
}
