package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

public class HistoryActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private Button btnDiary, btnMood, btnStress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        btnDiary = findViewById(R.id.btnDiaryTab);
        btnMood  = findViewById(R.id.btnMoodTab);
        btnStress = findViewById(R.id.btnStressTab);

        pager = findViewById(R.id.historyPager);
        pager.setAdapter(new HistoryPagerAdapter(this));
        pager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        // ✅ preload neighbors (smooth)
        pager.setOffscreenPageLimit(2);

        // ✅ smoother feel + less “jank”
        tunePagerRecyclerView(pager);

        // ✅ nicer transition feel (subtle)
        applySmoothTransformer(pager);

        // Tabs click -> pager
        btnDiary.setOnClickListener(v -> pager.setCurrentItem(0, true));
        btnMood.setOnClickListener(v -> pager.setCurrentItem(1, true));
        btnStress.setOnClickListener(v -> pager.setCurrentItem(2, true));

        // Swipe -> update tab highlight
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                updateTabUI(position);
            }
        });

        // Start on Diary
        pager.post(() -> {
            pager.setCurrentItem(0, false);
            updateTabUI(0);
        });

        setupPopup();
    }

    private void updateTabUI(int position) {
        btnDiary.setBackgroundResource(position == 0 ? R.drawable.bg_tab_pill_selected_mh : R.drawable.bg_tab_pill_unselected_mh);
        btnMood.setBackgroundResource(position == 1 ? R.drawable.bg_tab_pill_selected_mh : R.drawable.bg_tab_pill_unselected_mh);
        btnStress.setBackgroundResource(position == 2 ? R.drawable.bg_tab_pill_selected_mh : R.drawable.bg_tab_pill_unselected_mh);

        // optional: prevents double tap on current tab
        btnDiary.setEnabled(position != 0);
        btnMood.setEnabled(position != 1);
        btnStress.setEnabled(position != 2);
    }

    private void tunePagerRecyclerView(ViewPager2 pager) {
        View child = pager.getChildAt(0);
        if (child instanceof RecyclerView) {
            RecyclerView rv = (RecyclerView) child;

            // smoother + no glow
            rv.setOverScrollMode(View.OVER_SCROLL_NEVER);

            // caching improves smoothness
            rv.setItemViewCacheSize(3);
            rv.setHasFixedSize(true);

            // prefetch next page
            RecyclerView.LayoutManager lm = rv.getLayoutManager();
            if (lm != null) {
                lm.setItemPrefetchEnabled(true);
            }
        }
    }

    private void applySmoothTransformer(ViewPager2 pager) {
        CompositePageTransformer cpt = new CompositePageTransformer();

        // tiny spacing (not required, but improves "page" feel)
        cpt.addTransformer(new MarginPageTransformer(dp(6)));

        // subtle scale so it feels fluid (not dramatic)
        cpt.addTransformer((page, position) -> {
            float abs = Math.abs(position);
            float scale = 0.98f + (1f - abs) * 0.02f; // 0.98 -> 1.00
            page.setScaleY(scale);
            page.setAlpha(0.92f + (1f - abs) * 0.08f); // 0.92 -> 1.00
        });

        pager.setPageTransformer(cpt);
    }

    private int dp(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (dp * d);
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
