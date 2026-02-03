package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MoodHistoryActivity extends AppCompatActivity {

    private TextView tvMoodNote;
    private LinearLayout entriesContainer;
    private TextView tvEmptyMessage;
    private ZenPathRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_history);

        View tabRow = findViewById(R.id.tabRow);
        HistorySwipeHelper.attach(
                this,
                tabRow,
                DiaryHistoryActivity.class,
                StressHistoryActivity.class
        );

        tvMoodNote = findViewById(R.id.tvMoodNote);
        entriesContainer = findViewById(R.id.entriesContainer);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        
        repository = new ZenPathRepository(this);

        setupTabs();
        setupPopupWithRename();
        
        loadMoodEntries();

        // ✅ Swipe: Mood (RIGHT -> Diary) (LEFT -> Stress)
        View root = findViewById(R.id.main);
        HistorySwipeHelper.attach(this, root,
                DiaryHistoryActivity.class,
                StressHistoryActivity.class
        );
    }

    private void loadMoodEntries() {
        ArrayList<String> entries = repository.getMoodEntries();
        
        if (entries.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            entriesContainer.setVisibility(View.GONE);
            tvEmptyMessage.setText("No mood entries yet. Track your mood to see your history here!");
        } else {
            tvEmptyMessage.setVisibility(View.GONE);
            entriesContainer.setVisibility(View.VISIBLE);
            
            entriesContainer.removeAllViews();
            for (String entry : entries) {
                TextView entryView = new TextView(this);
                entryView.setText(entry);
                entryView.setPadding(16, 16, 16, 16);
                entryView.setTextSize(14);
                entryView.setBackgroundResource(R.drawable.bg_entry_card);
                entryView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                
                LinearLayout.MarginLayoutParams params = (LinearLayout.MarginLayoutParams) entryView.getLayoutParams();
                params.setMargins(0, 0, 0, 8);
                entryView.setLayoutParams(params);
                
                entriesContainer.addView(entryView);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMoodEntries();
    }

    private void setupTabs() {
        Button btnDiary = findViewById(R.id.btnDiaryTab);
        Button btnMood  = findViewById(R.id.btnMoodTab);
        Button btnStress = findViewById(R.id.btnStressTab);

        btnDiary.setOnClickListener(v -> {
            startActivity(new Intent(this, DiaryHistoryActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });

        btnMood.setEnabled(false);

        btnStress.setOnClickListener(v -> {
            startActivity(new Intent(this, StressHistoryActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
    }

    private void setupPopupWithRename() {
        ViewGroup rootView = findViewById(android.R.id.content);

        View settingsPopup = getLayoutInflater()
                .inflate(R.layout.dialog_settings, rootView, false);

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
            btnHistory.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));
        }

        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(this, MoodActivity.class));
            });
        }
    }
}
