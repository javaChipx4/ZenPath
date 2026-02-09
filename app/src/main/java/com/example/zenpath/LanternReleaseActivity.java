package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LanternReleaseActivity extends AppCompatActivity {

    private LanternReleaseView lanternView;
    private TextView tvTotal, tvBadge, tvMessage;

    // ✅ Play time tracker
    private GameTimeTracker playTracker;

    // ✅ shared pause overlay helper
    private final GamePauseOverlay pause = new GamePauseOverlay();

    // ✅ How-to overlay views
    private View howToOverlay;
    private View howToCard;
    private View btnStart;
    private View btnClose;

    // prefs (show once)
    private static final String PREFS = "zen_path_prefs";
    private static final String KEY_LANTERN_HOWTO_SEEN = "lantern_howto_seen";

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
        if (lanternView != null) {
            lanternView.setUiListener((total, badge, message) -> {
                if (tvTotal != null) tvTotal.setText("Total: " + total);
                if (tvBadge != null) tvBadge.setText("Badge: " + badge);
                if (tvMessage != null) tvMessage.setText(message);
            });
        }

        // ✅ PLAY button logic
        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> {
                if (lanternView == null) return;

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

        // ✅ Cog -> open overlay menu
        if (btnCog != null) {
            btnCog.setOnClickListener(v -> openLanternSettings());
        }

        // ✅ Hook How-to overlay
        bindHowToOverlay();

        // ✅ Show once (first time only)
        if (!hasSeenHowTo()) {
            showHowToOverlay();
            markSeenHowTo();
        }
    }

    private void bindHowToOverlay() {
        // This is the include root
        howToOverlay = findViewById(R.id.includeLanternHowTo);

        if (howToOverlay == null) return;

        // Inside overlay_lantern_howto.xml
        howToCard = howToOverlay.findViewById(R.id.card);
        btnStart = howToOverlay.findViewById(R.id.btnStart);
        btnClose = howToOverlay.findViewById(R.id.btnClose);

        // tap outside card = close
        howToOverlay.setOnClickListener(v -> hideHowToOverlay());
        if (howToCard != null) howToCard.setOnClickListener(v -> { /* block closing */ });

        if (btnStart != null) {
            btnStart.setOnClickListener(v -> hideHowToOverlay());
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                hideHowToOverlay();
                // go back to selection
                startActivity(new Intent(LanternReleaseActivity.this, SelectionGamesActivity.class));
                finish();
            });
        }
    }

    private void showHowToOverlay() {
        if (howToOverlay == null) return;
        howToOverlay.setVisibility(View.VISIBLE);
        howToOverlay.setAlpha(0f);
        howToOverlay.animate().alpha(1f).setDuration(160).start();
    }

    private void hideHowToOverlay() {
        if (howToOverlay == null) return;
        howToOverlay.animate()
                .alpha(0f)
                .setDuration(140)
                .withEndAction(() -> {
                    howToOverlay.setAlpha(1f);
                    howToOverlay.setVisibility(View.GONE);
                })
                .start();
    }

    private boolean hasSeenHowTo() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        return sp.getBoolean(KEY_LANTERN_HOWTO_SEEN, false);
    }

    private void markSeenHowTo() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit().putBoolean(KEY_LANTERN_HOWTO_SEEN, true).apply();
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
                        // just close
                    }

                    @Override
                    public void onRestart() {
                        if (lanternView != null) lanternView.resetToday();
                    }

                    @Override
                    public void onBack() {
                        if (playTracker != null) playTracker.stopAndSave(LanternReleaseActivity.this);
                        startActivity(new Intent(LanternReleaseActivity.this, SelectionGamesActivity.class));
                        finish();
                    }

                    @Override
                    public void onExtra() {
                        showHowToOverlay(); // ✅ use the instruction card instead of AlertDialog
                    }
                }
        );
    }
}
