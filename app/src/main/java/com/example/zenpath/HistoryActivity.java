package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class HistoryActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private Button btnDiary, btnMood, btnStress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        btnDiary = findViewById(R.id.btnDiaryTab);
        btnMood = findViewById(R.id.btnMoodTab);
        btnStress = findViewById(R.id.btnStressTab);

        pager = findViewById(R.id.historyPager);

        // If this crashes / null -> you're not using activity_history.xml
        pager.setAdapter(new HistoryPagerAdapter(this));
        pager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        // smoother startup (don’t preload all 3 heavy pages)
        pager.setOffscreenPageLimit(1);

        btnDiary.setOnClickListener(v -> pager.setCurrentItem(0, true));
        btnMood.setOnClickListener(v -> pager.setCurrentItem(1, true));
        btnStress.setOnClickListener(v -> pager.setCurrentItem(2, true));

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                updateTabUI(position);
            }
        });

        updateTabUI(0);
        setupPopup();
    }

    private void updateTabUI(int position) {
        btnDiary.setBackgroundResource(position == 0 ? R.drawable.bg_tab_pill_selected_mh : R.drawable.bg_tab_pill_unselected_mh);
        btnMood.setBackgroundResource(position == 1 ? R.drawable.bg_tab_pill_selected_mh : R.drawable.bg_tab_pill_unselected_mh);
        btnStress.setBackgroundResource(position == 2 ? R.drawable.bg_tab_pill_selected_mh : R.drawable.bg_tab_pill_unselected_mh);

        btnDiary.setEnabled(position != 0);
        btnMood.setEnabled(position != 1);
        btnStress.setEnabled(position != 2);
    }

    private void setupPopup() {
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
