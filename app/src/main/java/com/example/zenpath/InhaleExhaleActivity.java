package com.example.zenpath;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class InhaleExhaleActivity extends AppCompatActivity {

    private View breathingBubble;
    private View settingsOverlay;
    private View settingsPanel;
    private ImageView settingsButton;
    private TextView btnHome, btnInstructions, btnReset, btnMoodTracker, btnCloseSettings;
    private SeekBar volumeSeekBar;
    private ValueAnimator breathingAnimator;
    private boolean isAnimating = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inhaleexhale);

        initViews();
        setupBreathingAnimation();
        setupSettingsPanel();
        setupVolumeControl();
        setupIndicatorDots();
    }

    private void initViews() {
        breathingBubble = findViewById(R.id.breathing_bubble);
        settingsOverlay = findViewById(R.id.settings_overlay);
        settingsPanel = findViewById(R.id.settings_panel);
        settingsButton = findViewById(R.id.settings_button);
        btnHome = findViewById(R.id.btn_home);
        btnInstructions = findViewById(R.id.btn_instructions);
        btnReset = findViewById(R.id.btn_reset);
        btnMoodTracker = findViewById(R.id.btn_mood_tracker);
        btnCloseSettings = findViewById(R.id.btn_close_settings);
        volumeSeekBar = findViewById(R.id.volume_seekbar);
    }

    private void setupBreathingAnimation() {
        // Create continuous breathing animation
        breathingAnimator = ValueAnimator.ofFloat(1.0f, 1.8f, 1.0f, 0.6f, 1.0f);
        breathingAnimator.setDuration(8000); // 8 seconds for complete cycle
        breathingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathingAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        breathingAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            breathingBubble.setScaleX(scale);
            breathingBubble.setScaleY(scale);
        });

        breathingAnimator.start();
    }

    private void setupSettingsPanel() {

        // We will reuse the SAME popup layout used in other screens (dialog_settings)
        ViewGroup rootView = findViewById(android.R.id.content);

        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);

        // Tap outside closes popup
        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

        // Prevent close when touching card
        View settingsCard = settingsPopup.findViewById(R.id.settingsCard);
        if (settingsCard != null) settingsCard.setOnClickListener(v -> {});

        // Open popup when cog icon clicked (your existing icon)
        settingsButton.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));

        // Wire only the buttons you requested
        ImageButton btnHomePopup = settingsPopup.findViewById(R.id.btnHome);
        ImageButton btnBackPopup = settingsPopup.findViewById(R.id.btnBack);
        ImageButton btnMoodPopup = settingsPopup.findViewById(R.id.btnMood);

        if (btnHomePopup != null) {
            btnHomePopup.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                Intent i = new Intent(InhaleExhaleActivity.this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
            });
        }

        if (btnBackPopup != null) {
            btnBackPopup.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                finish();
            });
        }

        if (btnMoodPopup != null) {
            btnMoodPopup.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(InhaleExhaleActivity.this, MoodActivity.class));
            });
        }

        // OPTIONAL: If you want to hide your old panel completely so it never shows:
        if (settingsOverlay != null) settingsOverlay.setVisibility(View.GONE);
        if (settingsPanel != null) settingsPanel.setVisibility(View.GONE);
    }


    private void showInstructions() {
        Toast.makeText(this, "Follow the bubble: Inhale as it grows, Exhale as it shrinks", Toast.LENGTH_LONG).show();
    }

    private void resetBreathingAnimation() {
        if (breathingAnimator != null) {
            breathingAnimator.cancel();
            breathingAnimator.start();
        }
        Toast.makeText(this, "Breathing animation reset", Toast.LENGTH_SHORT).show();
    }

    private void setupVolumeControl() {
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Volume control implementation (placeholder)
                // In a real app, you would control actual sound volume here
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(InhaleExhaleActivity.this, "Volume: " + seekBar.getProgress() + "%", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupIndicatorDots() {
        View indicator1 = findViewById(R.id.indicator1);
        View indicator2 = findViewById(R.id.indicator2);
        View indicator3 = findViewById(R.id.indicator3);

        // Set different colors for the indicator dots
        indicator1.setBackgroundColor(getResources().getColor(R.color.indicator_pink, null));
        indicator2.setBackgroundColor(getResources().getColor(R.color.indicator_blue, null));
        indicator3.setBackgroundColor(getResources().getColor(R.color.indicator_green, null));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (breathingAnimator != null && isAnimating) {
            breathingAnimator.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (breathingAnimator != null && isAnimating) {
            breathingAnimator.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (breathingAnimator != null) {
            breathingAnimator.cancel();
        }
    }
}