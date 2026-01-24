package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private SeekBar volumeSeekBar;
    private TextView volumeLabel;
    private int currentVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_cd);

        preferences = getSharedPreferences("GamePreferences", MODE_PRIVATE);
        initViews();
        loadSettings();
        setupClickListeners();
    }

    private void initViews() {
        volumeSeekBar = findViewById(R.id.volume_seekbar);
        volumeLabel = findViewById(R.id.volume_label);

        Button homeButton = findViewById(R.id.home_button);
        Button privacyButton = findViewById(R.id.privacy_button);
        Button backButton = findViewById(R.id.back_button);
        Button resetButton = findViewById(R.id.reset_button);
    }

    private void loadSettings() {
        currentVolume = preferences.getInt("volume", 50);
        volumeSeekBar.setProgress(currentVolume);
        updateVolumeLabel(currentVolume);
    }

    private void setupClickListeners() {
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentVolume = progress;
                updateVolumeLabel(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveVolume();
            }
        });

        findViewById(R.id.home_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToHome();
            }
        });

        findViewById(R.id.privacy_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrivacyPolicy();
            }
        });

        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.reset_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetGame();
            }
        });
    }

    private void updateVolumeLabel(int volume) {
        volumeLabel.setText(getString(R.string.volume) + ": " + volume + "%");
    }

    private void saveVolume() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("volume", currentVolume);
        editor.apply();
    }

    private void goToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showPrivacyPolicy() {
        Toast.makeText(this, "Privacy Policy: This game stores local game data only.", Toast.LENGTH_LONG).show();
    }

    private void resetGame() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("current_level", 1);
        editor.apply();

        Toast.makeText(this, "Game reset to Level 1", Toast.LENGTH_SHORT).show();
        finish();
    }
}
