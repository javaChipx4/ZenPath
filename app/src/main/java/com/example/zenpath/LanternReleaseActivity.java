package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LanternReleaseActivity extends AppCompatActivity {

    private LanternReleaseView lanternView;

    private TextView tvScore, tvBadge, tvHint;

    private Button btnPlay, btnReset;

    // pause overlay
    private View pauseOverlay;
    private TextView btnResume, btnBackToSelection;

    // instructions overlay
    private View instructionsOverlay;
    private Button btnStartPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lantern_release);

        lanternView = findViewById(R.id.lanternView);

        tvScore = findViewById(R.id.tvScore);
        tvBadge = findViewById(R.id.tvBadge);
        tvHint  = findViewById(R.id.tvHint);

        btnPlay  = findViewById(R.id.btnPlay);
        btnReset = findViewById(R.id.btnReset);

        // Cog (pause)
        ImageButton btnCog = findViewById(R.id.btnCog);

        // overlays
        pauseOverlay = findViewById(R.id.pauseOverlay);
        instructionsOverlay = findViewById(R.id.instructionsOverlay);

        // Pause overlay inner views
        if (pauseOverlay != null) {
            View pauseCard = pauseOverlay.findViewById(R.id.pauseCard);
            btnResume = pauseOverlay.findViewById(R.id.btnResume);
            btnBackToSelection = pauseOverlay.findViewById(R.id.btnBackToSelection);

            pauseOverlay.setOnClickListener(v -> hidePause());
            if (pauseCard != null) pauseCard.setOnClickListener(v -> {});
            if (btnResume != null) btnResume.setOnClickListener(v -> hidePause());
            if (btnBackToSelection != null) {
                btnBackToSelection.setOnClickListener(v -> {
                    hidePause();
                    startActivity(new Intent(this, SelectionGamesActivity.class));
                    finish();
                });
            }
        }

        // Instructions overlay inner views
        if (instructionsOverlay != null) {
            btnStartPlay = instructionsOverlay.findViewById(R.id.btnStartPlay);
            View instrCard = instructionsOverlay.findViewById(R.id.instructionCard);

            instructionsOverlay.setOnClickListener(v -> hideInstructions());
            if (instrCard != null) instrCard.setOnClickListener(v -> {});
            if (btnStartPlay != null) btnStartPlay.setOnClickListener(v -> hideInstructions());
        }

        // UI listener
        lanternView.setUiListener((total, badge, message) -> {
            tvScore.setText("Score: " + total);
            tvBadge.setText("Badge: " + badge);
            tvHint.setText(message);
        });

        // ✅ show instructions once when opening
        if (savedInstanceState == null && instructionsOverlay != null) {
            showInstructions();
        }

        // ✅ Smart Play: if finished -> auto playAgain -> start
        btnPlay.setOnClickListener(v -> {
            if (lanternView.isRunning()) return;

            if (lanternView.isFinished()) {
                lanternView.playAgain(); // reset to pre-play state
            }
            lanternView.startRelease(); // start flight
        });

        btnReset.setOnClickListener(v -> lanternView.resetToday());

        if (btnCog != null) btnCog.setOnClickListener(v -> showPause());
    }

    private void showPause() {
        if (pauseOverlay != null) pauseOverlay.setVisibility(View.VISIBLE);
    }

    private void hidePause() {
        if (pauseOverlay != null) pauseOverlay.setVisibility(View.GONE);
    }

    private void showInstructions() {
        if (instructionsOverlay != null) instructionsOverlay.setVisibility(View.VISIBLE);
    }

    private void hideInstructions() {
        if (instructionsOverlay != null) instructionsOverlay.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (pauseOverlay != null && pauseOverlay.getVisibility() == View.VISIBLE) {
            hidePause();
            return;
        }
        if (instructionsOverlay != null && instructionsOverlay.getVisibility() == View.VISIBLE) {
            hideInstructions();
            return;
        }
        super.onBackPressed();
    }
}