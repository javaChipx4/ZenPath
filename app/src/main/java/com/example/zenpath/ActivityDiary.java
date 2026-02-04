package com.example.zenpath;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class ActivityDiary extends AppCompatActivity implements DiaryPagerAdapter.Listener {

    private ViewPager2 diaryPager;
    private Button btnSave;

    private ZenPathRepository repo;

    private ArrayList<String> dates = new ArrayList<>();
    private String currentDate;

    private View bookIntroOverlay;
    private View bookIntroCard;
    private Button btnOpenBook;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_diary);

        repo = new ZenPathRepository(this);

        // ===== POPUP (UNCHANGED) =====
        ViewGroup rootView = findViewById(android.R.id.content);
        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        if (menuIcon != null) menuIcon.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));
        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

        View settingsCard = settingsPopup.findViewById(R.id.settingsCard);
        if (settingsCard != null) settingsCard.setOnClickListener(v -> {});

        ImageButton btnHome = settingsPopup.findViewById(R.id.btnHome);
        ImageButton btnBack = settingsPopup.findViewById(R.id.btnBack);
        ImageButton btnHistory = settingsPopup.findViewById(R.id.btnHistory);
        ImageButton btnMood = settingsPopup.findViewById(R.id.btnMood);

        if (btnHome != null) btnHome.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
            startActivity(new Intent(ActivityDiary.this, MainActivity.class));
            finish();
        });

        if (btnBack != null) btnBack.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
            finish();
        });

        if (btnHistory != null) btnHistory.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
            startActivity(new Intent(ActivityDiary.this, HistoryActivity.class));
        });

        if (btnMood != null) btnMood.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
            startActivity(new Intent(ActivityDiary.this, MoodActivity.class));
        });

        // ===== Bind =====
        diaryPager = findViewById(R.id.diaryPager);
        btnSave = findViewById(R.id.btn_save);

        // ===== Build dates (like a diary book: last 60 days -> today) =====
        buildDates(60);

        DiaryPagerAdapter adapter = new DiaryPagerAdapter(this, dates, repo, this);
        diaryPager.setAdapter(adapter);
        diaryPager.setOffscreenPageLimit(1);

        // ===== Page flip animation =====
        diaryPager.setPageTransformer(new PageFlipTransformer());

        // ===== Keep currentDate updated =====
        diaryPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                currentDate = dates.get(position);
            }
        });

        // Start on "today" page (last page)
        int startPos = dates.size() - 1;
        diaryPager.setCurrentItem(startPos, false);
        currentDate = dates.get(startPos);

        // ===== Save button =====
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> showSaveDialog());
        }

        // ===== Open-book intro overlay =====
        bookIntroOverlay = findViewById(R.id.bookIntroOverlay);
        bookIntroCard = findViewById(R.id.bookIntroCard);
        btnOpenBook = findViewById(R.id.btnOpenBook);

        if (bookIntroOverlay != null) {
            bookIntroOverlay.setVisibility(View.VISIBLE);
            bookIntroOverlay.setAlpha(0f);
            bookIntroOverlay.animate().alpha(1f).setDuration(220).start();

            if (bookIntroCard != null) {
                bookIntroCard.setScaleX(0.92f);
                bookIntroCard.setScaleY(0.92f);
                bookIntroCard.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(260)
                        .setInterpolator(new OvershootInterpolator())
                        .start();
            }

            if (btnOpenBook != null) {
                btnOpenBook.setOnClickListener(v -> hideIntro());
            }

            // Tap outside closes too
            bookIntroOverlay.setOnClickListener(v -> hideIntro());
            if (bookIntroCard != null) bookIntroCard.setOnClickListener(v -> {});
        }
    }

    private void hideIntro() {
        if (bookIntroOverlay == null) return;
        bookIntroOverlay.animate().alpha(0f).setDuration(180).withEndAction(() -> {
            bookIntroOverlay.setVisibility(View.GONE);
        }).start();
    }

    private void buildDates(int daysBackIncludingToday) {
        dates.clear();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // go back (daysBackIncludingToday-1) then add forward to today
        cal.add(Calendar.DAY_OF_YEAR, -(daysBackIncludingToday - 1));
        for (int i = 0; i < daysBackIncludingToday; i++) {
            dates.add(df.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);
        View customView = getLayoutInflater().inflate(R.layout.dialog_save, null);
        builder.setView(customView);

        final AlertDialog dialog = builder.create();

        Button btnYes = customView.findViewById(R.id.btn_dialog_yes);
        Button btnNo = customView.findViewById(R.id.btn_dialog_no);

        btnYes.setOnClickListener(v -> {
            // Ask adapter to save current page content
            if (diaryPager != null && diaryPager.getAdapter() instanceof DiaryPagerAdapter) {
                DiaryPagerAdapter ad = (DiaryPagerAdapter) diaryPager.getAdapter();
                boolean ok = ad.saveCurrentPage(diaryPager.getCurrentItem());
                if (ok) Toast.makeText(ActivityDiary.this, "Saved!", Toast.LENGTH_SHORT).show();
                else Toast.makeText(ActivityDiary.this, "Write something first.", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Listener from adapter: used if you want extra hooks later
    @Override public void onSaved(String date) {
        // optional
    }
}
