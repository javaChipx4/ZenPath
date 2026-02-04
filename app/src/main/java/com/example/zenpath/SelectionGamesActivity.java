package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SelectionGamesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection_games);

        // OPTIONAL: reuse your popup settings menu (inflate overlay method)
        ViewGroup rootView = findViewById(android.R.id.content);
        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        if (menuIcon != null) {
            menuIcon.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));
        }
        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

        // Prevent closing when tapping card itself (optional but recommended)
        View settingsCard = settingsPopup.findViewById(R.id.settingsCard);
        if (settingsCard != null) {
            settingsCard.setOnClickListener(v -> {});
        }

// Buttons inside popup
        ImageButton btnHome = settingsPopup.findViewById(R.id.btnHome);
        ImageButton btnBack = settingsPopup.findViewById(R.id.btnBack);
        ImageButton btnHistory = settingsPopup.findViewById(R.id.btnHistory);
        ImageButton btnMood = settingsPopup.findViewById(R.id.btnMood);

        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(SelectionGamesActivity.this, MainActivity.class));
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
                startActivity(new Intent(SelectionGamesActivity.this, DiaryHistoryActivity.class));
            });
        }


        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(SelectionGamesActivity.this, MoodActivity.class));
            });
        }

        // Game clicks
        findViewById(R.id.cardConstella).setOnClickListener(v ->
                        Toast.makeText(this, "Open Tap the Calm Game", Toast.LENGTH_SHORT).show()
                // startActivity(new Intent(this, TapCalmActivity.class));
        );

        findViewById(R.id.cardLantelle).setOnClickListener(v ->
                        Toast.makeText(this, "Open Color Match Relax", Toast.LENGTH_SHORT).show()
                // startActivity(new Intent(this, ColorMatchActivity.class));
        );

        findViewById(R.id.cardAsthera).setOnClickListener(v ->
                        Toast.makeText(this, "Open Breathing Bubble", Toast.LENGTH_SHORT).show()
                // startActivity(new Intent(this, InhaleExhaleActivity.class));
        );

        findViewById(R.id.cardConstella).setOnClickListener(v -> {
            startActivity(new Intent(SelectionGamesActivity.this, StarSweepIntroActivity.class));
        });

        findViewById(R.id.cardLantelle).setOnClickListener(v -> {
                    startActivity(new Intent(SelectionGamesActivity.this, LanternReleaseActivity.class));
                });

        findViewById(R.id.cardAsthera).setOnClickListener(v -> {
            startActivity(new Intent(SelectionGamesActivity.this, PlanetActivity.class));
        });
    }
}
