package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LanternReleaseActivity extends AppCompatActivity {

    private LanternReleaseView lanternView;
    private TextView tvTotal, tvBadge, tvMessage;

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
        View btnReset = findViewById(R.id.btnResetToday);
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

                // If finished, allow replay
                else if (lanternView.isFinished()) {
                    lanternView.playAgain();
                }
            });
        }

        // ✅ Reset button
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> lanternView.resetToday());
        }

        // ✅ Cog popup menu
        if (btnCog != null) {
            btnCog.setOnClickListener(v -> showPausePopup());
        }
    }

    // ✅ Pause Popup Overlay
    private void showPausePopup() {

        ViewGroup rootView = findViewById(android.R.id.content);

        View overlay = getLayoutInflater()
                .inflate(R.layout.dialog_lantern_pause, rootView, false);

        rootView.addView(overlay);

        // Tap outside closes
        overlay.setOnClickListener(v -> rootView.removeView(overlay));

        // Prevent closing when tapping the card
        View card = overlay.findViewById(R.id.pauseCard);
        if (card != null) card.setOnClickListener(v -> {});

        // Buttons
        View btnResume = overlay.findViewById(R.id.btnResume);
        View btnRestart = overlay.findViewById(R.id.btnRestart);
        View btnBack = overlay.findViewById(R.id.btnBackToSelection);

        // ✅ Resume
        if (btnResume != null) {
            btnResume.setOnClickListener(v ->
                    rootView.removeView(overlay)
            );
        }

        // ✅ Restart
        if (btnRestart != null) {
            btnRestart.setOnClickListener(v -> {
                lanternView.playAgain();
                rootView.removeView(overlay);
            });
        }

        // ✅ Back to Selection
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                rootView.removeView(overlay);
                startActivity(new Intent(
                        LanternReleaseActivity.this,
                        SelectionGamesActivity.class
                ));
                finish();
            });
        }
    }
}