package com.example.zenpath;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SelectionGamesActivity extends AppCompatActivity {

    public static final String EXTRA_SUGGESTION_TITLE = "extra_suggestion_title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection_games);

        // ✅ Suggestion pill text (soft suggestion, not forcing)

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

        // Prevent closing when tapping card itself
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
                startActivity(new Intent(SelectionGamesActivity.this, HistoryActivity.class));
            });
        }

        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(SelectionGamesActivity.this, MoodActivity.class));
            });
        }

        View cardConstella = findViewById(R.id.cardConstella);
        View cardLantelle = findViewById(R.id.cardLantelle);
        View cardAsthera = findViewById(R.id.cardAsthera);

        installPressAnim(cardConstella);
        installPressAnim(cardLantelle);
        installPressAnim(cardAsthera);

        if (cardConstella != null) {
            cardConstella.setOnClickListener(v ->
                    startActivity(new Intent(this, StarSweepIntroActivity.class)));
        }

        if (cardLantelle != null) {
            cardLantelle.setOnClickListener(v ->
                    startActivity(new Intent(this, LanternReleaseActivity.class)));
        }

        if (cardAsthera != null) {
            cardAsthera.setOnClickListener(v ->
                    startActivity(new Intent(this, PlanetActivity.class)));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void installPressAnim(View v) {
        if (v == null) return;

        v.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.985f).scaleY(0.985f).setDuration(70).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                    break;
            }
            return false;
        });
    }
}
