package com.example.zenpath;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
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
        settingsButton.setOnClickListener(v -> toggleSettingsPanel());

        btnCloseSettings.setOnClickListener(v -> hideSettingsPanel());

        btnHome.setOnClickListener(v -> {
            hideSettingsPanel();
            Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
        });

        btnInstructions.setOnClickListener(v -> {
            hideSettingsPanel();
            showInstructions();
        });

        btnReset.setOnClickListener(v -> {
            hideSettingsPanel();
            resetBreathingAnimation();
        });

        btnMoodTracker.setOnClickListener(v -> {
            hideSettingsPanel();
            Toast.makeText(this, "Mood Tracker - Coming Soon!", Toast.LENGTH_SHORT).show();
        });

        settingsOverlay.setOnClickListener(v -> hideSettingsPanel());
    }

    private void toggleSettingsPanel() {
        if (settingsPanel.getVisibility() == View.VISIBLE) {
            hideSettingsPanel();
        } else {
            showSettingsPanel();
        }
    }

    private void showSettingsPanel() {
        settingsOverlay.setVisibility(View.VISIBLE);
        settingsPanel.setVisibility(View.VISIBLE);
        settingsPanel.setScaleX(0.8f);
        settingsPanel.setScaleY(0.8f);
        settingsPanel.setAlpha(0f);

        settingsPanel.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void hideSettingsPanel() {
        settingsPanel.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    settingsOverlay.setVisibility(View.GONE);
                    settingsPanel.setVisibility(View.GONE);
                })
                .start();
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