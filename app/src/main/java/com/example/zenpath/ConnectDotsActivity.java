package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ConnectDotsActivity extends AppCompatActivity {

    private GameBoardViewActivity gameBoard;
    private TextView levelLabel;
    private SharedPreferences preferences;
    private int currentLevel = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectdots);

        initViews();
        loadGameData();
        setupPopupMenu(); // ✅ add popup
        checkLevelCompletion();
    }

    private void initViews() {
        gameBoard = findViewById(R.id.game_board);
        levelLabel = findViewById(R.id.level_label);
        preferences = getSharedPreferences("GamePreferences", MODE_PRIVATE);
    }

    private void loadGameData() {
        currentLevel = preferences.getInt("current_level", 1);
        updateLevelDisplay();
        gameBoard.setCurrentLevel(currentLevel);
    }

    private void updateLevelDisplay() {
        int gridSize = gameBoard.getCurrentGridSize();
        int dotCount = (currentLevel == 1) ? 6 : 8;
        int pairCount = dotCount / 2;

        levelLabel.setText("Level " + currentLevel +
                " (" + gridSize + "x" + gridSize + ", " +
                dotCount + " dots, " + pairCount + " pairs)");
    }

    private void setupPopupMenu() {
        // ===== SAME POPUP LOGIC AS OTHER SCREENS =====
        ViewGroup rootView = findViewById(android.R.id.content);

        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        if (menuIcon != null) {
            menuIcon.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));
        }

        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

        View settingsCard = settingsPopup.findViewById(R.id.settingsCard);
        if (settingsCard != null) settingsCard.setOnClickListener(v -> {});

        ImageButton btnHome = settingsPopup.findViewById(R.id.btnHome);
        ImageButton btnBack = settingsPopup.findViewById(R.id.btnBack);
        ImageButton btnMood = settingsPopup.findViewById(R.id.btnMood);
        ImageButton btnHistory = settingsPopup.findViewById(R.id.btnHistory);


        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                Intent i = new Intent(ConnectDotsActivity.this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                finish();
            });
        }

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(ConnectDotsActivity.this, DiaryHistoryActivity.class));
            });
        }


        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(ConnectDotsActivity.this, MoodActivity.class));
            });
        }
    }

    private void checkLevelCompletion() {
        if (gameBoard.isLevelComplete()) {
            new android.os.Handler().postDelayed(() -> {
                currentLevel++;
                setCurrentLevel(currentLevel);
                Toast.makeText(ConnectDotsActivity.this,
                        "Level " + (currentLevel - 1) + " Complete! Starting Level " + currentLevel,
                        Toast.LENGTH_SHORT).show();
            }, 1000);
        }
    }

    public void setCurrentLevel(int level) {
        currentLevel = level;
        updateLevelDisplay();
        gameBoard.setCurrentLevel(currentLevel);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("current_level", currentLevel);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGameData();
    }
}
