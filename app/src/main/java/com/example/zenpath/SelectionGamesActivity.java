package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SelectionGamesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection_games);

        // ✅ Popup Settings Menu (overlay)
        ViewGroup rootView = findViewById(android.R.id.content);
        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        if (menuIcon != null) {
            menuIcon.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));
        }

        // Tap outside to close
        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

        // Prevent closing when tapping the card itself
        View settingsCard = settingsPopup.findViewById(R.id.settingsCard);
        if (settingsCard != null) settingsCard.setOnClickListener(v -> {});

        // ✅ Buttons inside popup
        ImageButton btnHome = settingsPopup.findViewById(R.id.btnHome);
        ImageButton btnBack = settingsPopup.findViewById(R.id.btnBack);
        ImageButton btnHistory = settingsPopup.findViewById(R.id.btnHistory);
        ImageButton btnMood = settingsPopup.findViewById(R.id.btnMood);

        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(this, MainActivity.class));
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
                startActivity(new Intent(this, DiaryHistoryActivity.class));
            });
        }

        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(this, MoodActivity.class));
            });
        }

        // ✅ GAME 1 - Lantern Release (SHOW INSTRUCTIONS FIRST)
        View game1 = findViewById(R.id.btnGame1);
        if (game1 != null) game1.setOnClickListener(v -> showLanternInstructions());

        // ✅ GAME 2 - Star Sweep (OPEN INTRO FIRST)
        View game2 = findViewById(R.id.btnGame2);
        if (game2 != null) {
            game2.setOnClickListener(v ->
                    startActivity(new Intent(this, StarSweepIntroActivity.class)));
        }

        // ✅ GAME 3 - Breathing Bubble
        View game3 = findViewById(R.id.btnGame3);
        if (game3 != null) {
            game3.setOnClickListener(v ->
                    startActivity(new Intent(this, InhaleExhaleActivity.class)));
        }
    }

    private void showLanternInstructions() {
        ViewGroup rootView = findViewById(android.R.id.content);
        View overlay = getLayoutInflater().inflate(R.layout.dialog_lantern_instructions, rootView, false);
        rootView.addView(overlay);

        overlay.setOnClickListener(v -> rootView.removeView(overlay));

        View card = overlay.findViewById(R.id.card);
        if (card != null) card.setOnClickListener(v -> {});

        View btnClose = overlay.findViewById(R.id.btnClose);
        View btnStart = overlay.findViewById(R.id.btnStart);

        if (btnClose != null) btnClose.setOnClickListener(v -> rootView.removeView(overlay));

        if (btnStart != null) {
            btnStart.setOnClickListener(v -> {
                rootView.removeView(overlay);
                startActivity(new Intent(this, LanternReleaseActivity.class));
            });
        }
    }
}
