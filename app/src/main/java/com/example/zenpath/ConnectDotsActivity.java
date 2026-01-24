package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ConnectDotsActivity extends AppCompatActivity {

    private GameBoardViewActivity gameBoard;
    private TextView levelLabel;
    private SharedPreferences preferences;
    private int currentLevel = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        loadGameData();
        setupClickListeners();
    }

    private void initViews() {
        gameBoard = findViewById(R.id.game_board);
        levelLabel = findViewById(R.id.level_label);
        ImageView settingsIcon = findViewById(R.id.settings_icon);

        preferences = getSharedPreferences("GamePreferences", MODE_PRIVATE);
    }

    private void loadGameData() {
        currentLevel = preferences.getInt("current_level", 1);
        updateLevelDisplay();
        gameBoard.setCurrentLevel(currentLevel);
    }

    private void setupClickListeners() {
        ImageView settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void updateLevelDisplay() {
        int gridSize = gameBoard.getCurrentGridSize();
        int dotCount = getCurrentLevel() == 1 ? 6 : 8;
        int pairCount = dotCount / 2;
        levelLabel.setText(String.format(getString(R.string.level_format), currentLevel) +
                " (" + gridSize + "x" + gridSize + ", " + dotCount + " dots, " + pairCount + " pairs)");
    }

    private int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int level) {
        currentLevel = level;
        updateLevelDisplay();
        gameBoard.setCurrentLevel(currentLevel);

        // Save level to preferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("current_level", currentLevel);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh game data when returning from settings
        loadGameData();

        // Check for level completion and auto-advance
        checkLevelCompletion();
    }

    private void checkLevelCompletion() {
        if (gameBoard.isLevelComplete()) {
            // Auto-advance to next level after a short delay
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    currentLevel++;
                    setCurrentLevel(currentLevel);

                    // Show level complete message
                    android.widget.Toast.makeText(ConnectDotsActivity.this,
                            "Level " + (currentLevel - 1) + " Complete! Starting Level " + currentLevel,
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            }, 1000); // 1 second delay
        }
    }
}
