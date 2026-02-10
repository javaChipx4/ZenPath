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

    // ✅ NEW: only one date (today), multiple pages
    private String currentDate;
    private ArrayList<String> pages = new ArrayList<>();
    private DiaryPagerAdapter adapter;

    // intro overlay
    private View bookIntroOverlay;
    private View bookIntroCard;
    private Button btnOpenBook;

    // ✅ NEW: add page button (optional but recommended)
    private View btnAddPage;

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

        // ===== Volume control =====
        android.widget.SeekBar seekVolume = settingsPopup.findViewById(R.id.seekVolume);
        android.widget.ImageView imgVolume = settingsPopup.findViewById(R.id.imgVolume);

        if (seekVolume != null) {
            // set initial progress from saved volume
            int saved = MusicController.getVolumePercent(ActivityDiary.this);
            seekVolume.setProgress(saved);

            if (imgVolume != null) {
                imgVolume.setImageResource(saved == 0
                        ? android.R.drawable.ic_lock_silent_mode
                        : android.R.drawable.ic_lock_silent_mode_off);
            }

            seekVolume.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser) return;

                    MusicController.setVolumePercent(ActivityDiary.this, progress);

                    if (imgVolume != null) {
                        imgVolume.setImageResource(progress == 0
                                ? android.R.drawable.ic_lock_silent_mode
                                : android.R.drawable.ic_lock_silent_mode_off);
                    }
                }

                @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
            });
        }

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
        btnAddPage = findViewById(R.id.btnAddPage); // ✅ add in XML (below)

        // ✅ Today key
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        // ✅ Load pages for today (at least 1 page)
        pages = repo.getDiaryPagesByDate(currentDate);
        if (pages == null) pages = new ArrayList<>();
        if (pages.isEmpty()) pages.add("");

        adapter = new DiaryPagerAdapter(this, currentDate, pages, repo, this);
        diaryPager.setAdapter(adapter);
        diaryPager.setOffscreenPageLimit(1);

        // ✅ Keep page flip animation (still feels like book)
        diaryPager.setPageTransformer(new PageFlipTransformer());

        // Start at last page (most recent writing)
        diaryPager.setCurrentItem(pages.size() - 1, false);

        // ✅ Save all pages
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> showSaveDialog());
        }

        // ✅ Add page (manual, safe)
        if (btnAddPage != null) {
            btnAddPage.setOnClickListener(v -> {
                adapter.addNewPage();
                diaryPager.setCurrentItem(adapter.getItemCount() - 1, true);
            });
        }

        // ===== Open-book intro overlay (UNCHANGED) =====
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

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);
        View customView = getLayoutInflater().inflate(R.layout.dialog_save, null);
        builder.setView(customView);

        final AlertDialog dialog = builder.create();

        Button btnYes = customView.findViewById(R.id.btn_dialog_yes);
        Button btnNo = customView.findViewById(R.id.btn_dialog_no);

        btnYes.setOnClickListener(v -> {
            if (diaryPager != null && diaryPager.getAdapter() instanceof DiaryPagerAdapter) {
                DiaryPagerAdapter ad = (DiaryPagerAdapter) diaryPager.getAdapter();
                boolean ok = ad.saveAllPages();

                if (ok) Toast.makeText(ActivityDiary.this, "Saved!", Toast.LENGTH_SHORT).show();
                else Toast.makeText(ActivityDiary.this, "Write something first.", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override public void onSaved(String date) {
        // optional
    }
}
