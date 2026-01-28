package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class StressHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stress_history);

        View tabRow = findViewById(R.id.tabRow);
        HistorySwipeHelper.attach(
                this,
                tabRow,
                MoodHistoryActivity.class,
                null                 // no next (Stress is last)
        );


        setupTabs();
        setupPopup("Stress History");

        // ✅ Swipe: Stress (RIGHT -> Mood)
        View root = findViewById(R.id.main);
        HistorySwipeHelper.attach(this, root, MoodHistoryActivity.class, null);
    }

    private void setupTabs() {
        Button btnDiary = findViewById(R.id.btnDiaryTab);
        Button btnMood  = findViewById(R.id.btnMoodTab);
        Button btnStress = findViewById(R.id.btnStressTab);

        btnStress.setEnabled(false);

        btnDiary.setOnClickListener(v -> {
            startActivity(new Intent(this, DiaryHistoryActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });

        btnMood.setOnClickListener(v -> {
            startActivity(new Intent(this, MoodHistoryActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });
    }

    private void setupPopup(String historyTitle) {
        ViewGroup rootView = findViewById(android.R.id.content);
        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));

        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

        View card = settingsPopup.findViewById(R.id.settingsCard);
        if (card != null) card.setOnClickListener(v -> {});

        ImageButton btnHome = settingsPopup.findViewById(R.id.btnHome);
        ImageButton btnBack = settingsPopup.findViewById(R.id.btnBack);
        ImageButton btnHistory = settingsPopup.findViewById(R.id.btnHistory);
        ImageButton btnMood = settingsPopup.findViewById(R.id.btnMood);

        if (btnHome != null) btnHome.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        if (btnBack != null) btnBack.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
            finish();
        });

        if (btnHistory != null) btnHistory.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

        if (btnMood != null) btnMood.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
            startActivity(new Intent(this, MoodActivity.class));
        });
    }
}
