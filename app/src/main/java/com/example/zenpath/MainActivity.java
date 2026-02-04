package com.example.zenpath;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private ImageButton btnMenu;
    private Button btnPlay;
    private CalendarView calendarView;

    private Calendar selectedCalendar;

    // swipe vars
    private View viewport, container, panelA, panelB;
    private float downY;
    private float startTranslationY;
    private boolean isPanelBShown = false;
    private int pageHeight = 0;

    // book overlay
    private View openBookOverlay;
    private View bookContainer, pageLeft, pageRight, bookShadow;
    private boolean isBookAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ===== Book overlay views (must exist in activity_main via include) =====
        openBookOverlay = findViewById(R.id.openBookOverlay);
        bookContainer = findViewById(R.id.bookContainer);
        pageLeft = findViewById(R.id.pageLeft);
        pageRight = findViewById(R.id.pageRight);
        bookShadow = findViewById(R.id.bookShadow);

        // Overlay behavior
        if (openBookOverlay != null) {
            // Tap outside book = close
            openBookOverlay.setOnClickListener(v -> hideBookOverlay());
        }
        if (bookContainer != null) {
            // Tap book = open animation then go diary
            bookContainer.setOnClickListener(v -> playOpenBookThenGoDiary());
        }

        // ===== Bind views =====
        btnMenu = findViewById(R.id.btnMenu);
        btnPlay = findViewById(R.id.btnPlay);
        calendarView = findViewById(R.id.calendarView);
        selectedCalendar = Calendar.getInstance();

        // ===== Username =====
        SharedPreferences prefs = getSharedPreferences("zen_path_prefs", MODE_PRIVATE);
        String username = prefs.getString("current_user", null);
        if (TextUtils.isEmpty(username)) username = "User";

        TextView tvUsername = findViewById(R.id.tvUsername);
        TextView tvHelloUser = findViewById(R.id.tvHelloUser);

        if (tvUsername != null) tvUsername.setText(username.toUpperCase());
        if (tvHelloUser != null) tvHelloUser.setText("Hello, " + username + "!");

        // ===== Settings Popup Overlay =====
        ViewGroup rootView = findViewById(android.R.id.content);
        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);

        if (btnMenu != null) btnMenu.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));
        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

        View settingsCard = settingsPopup.findViewById(R.id.settingsCard);
        if (settingsCard != null) settingsCard.setOnClickListener(v -> {});

        ImageButton btnHome = settingsPopup.findViewById(R.id.btnHome);
        ImageButton btnBack = settingsPopup.findViewById(R.id.btnBack);
        ImageButton btnHistory = settingsPopup.findViewById(R.id.btnHistory);
        ImageButton btnMood = settingsPopup.findViewById(R.id.btnMood);

        if (btnHome != null) btnHome.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));
        if (btnBack != null) btnBack.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            });
        }

        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(MainActivity.this, MoodActivity.class));
            });
        }

        // ===== Journal click -> show book (NOT go diary immediately) =====
        Button tvJournal = findViewById(R.id.tvJournal);
        if (tvJournal != null) {
            tvJournal.setOnClickListener(v -> showBookOverlay());
        }

        // ===== Calendar -> Mood =====
        if (calendarView != null) {
            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                selectedCalendar.set(year, month, dayOfMonth);
                startActivity(new Intent(MainActivity.this, MoodActivity.class));
            });
        }

        // ===== Play =====
        if (btnPlay != null) {
            btnPlay.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, SelectionGamesActivity.class))
            );
        }

        // ===== Swipe setup =====
        viewport = findViewById(R.id.mainContainer);
        container = findViewById(R.id.homePagerContainer);
        panelA = findViewById(R.id.panelA);
        panelB = findViewById(R.id.panelB);

        if (container != null && panelA != null && panelB != null && viewport != null) {
            viewport.post(() -> {
                pageHeight = viewport.getHeight();

                setExactHeight(panelA, pageHeight);
                setExactHeight(panelB, pageHeight);

                ViewGroup.LayoutParams lpC = container.getLayoutParams();
                lpC.height = pageHeight * 2;
                container.setLayoutParams(lpC);

                container.setTranslationY(0f);
                installSwipe();
            });
        }
    }

    private void showBookOverlay() {
        if (openBookOverlay == null || bookContainer == null) {
            // fallback
            startActivity(new Intent(MainActivity.this, ActivityDiary.class));
            return;
        }

        // Reset visibility + starting state
        openBookOverlay.setVisibility(View.VISIBLE);
        openBookOverlay.setAlpha(0f);

        bookContainer.setScaleX(0.92f);
        bookContainer.setScaleY(0.92f);
        bookContainer.setAlpha(0f);

        // Make sure pages look "closed" initially
        if (pageLeft != null && pageRight != null) {
            float scale = getResources().getDisplayMetrics().density;
            float camera = 8000f * scale;
            pageLeft.setCameraDistance(camera);
            pageRight.setCameraDistance(camera);

            // We must wait for layout so widths are real
            bookContainer.post(() -> {
                pageLeft.setPivotX(pageLeft.getWidth());
                pageLeft.setPivotY(pageLeft.getHeight() / 2f);

                pageRight.setPivotX(0f);
                pageRight.setPivotY(pageRight.getHeight() / 2f);

                pageLeft.setRotationY(70f);
                pageRight.setRotationY(-70f);

                if (bookShadow != null) bookShadow.setAlpha(0f);

                // Pop-in animation
                ObjectAnimator overlayFade = ObjectAnimator.ofFloat(openBookOverlay, "alpha", 0f, 1f);
                ObjectAnimator cardAlpha = ObjectAnimator.ofFloat(bookContainer, "alpha", 0f, 1f);
                ObjectAnimator cardSX = ObjectAnimator.ofFloat(bookContainer, "scaleX", 0.92f, 1f);
                ObjectAnimator cardSY = ObjectAnimator.ofFloat(bookContainer, "scaleY", 0.92f, 1f);

                AnimatorSet set = new AnimatorSet();
                set.setDuration(180);
                set.setInterpolator(new DecelerateInterpolator());
                set.playTogether(overlayFade, cardAlpha, cardSX, cardSY);
                set.start();
            });
        } else {
            openBookOverlay.animate().alpha(1f).setDuration(160).start();
            bookContainer.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
        }
    }

    private void hideBookOverlay() {
        if (openBookOverlay == null || isBookAnimating) return;

        openBookOverlay.animate()
                .alpha(0f)
                .setDuration(140)
                .withEndAction(() -> {
                    openBookOverlay.setAlpha(1f);
                    openBookOverlay.setVisibility(View.GONE);
                })
                .start();
    }

    private void playOpenBookThenGoDiary() {
        if (isBookAnimating) return;

        if (openBookOverlay == null || pageLeft == null || pageRight == null) {
            startActivity(new Intent(MainActivity.this, ActivityDiary.class));
            return;
        }

        isBookAnimating = true;

        // Ensure pivots are correct (layout ready)
        bookContainer.post(() -> {
            float scale = getResources().getDisplayMetrics().density;
            float camera = 8000f * scale;
            pageLeft.setCameraDistance(camera);
            pageRight.setCameraDistance(camera);

            pageLeft.setPivotX(pageLeft.getWidth());
            pageLeft.setPivotY(pageLeft.getHeight() / 2f);

            pageRight.setPivotX(0f);
            pageRight.setPivotY(pageRight.getHeight() / 2f);

            // Animate to open
            ObjectAnimator leftOpen = ObjectAnimator.ofFloat(pageLeft, "rotationY", pageLeft.getRotationY(), 0f);
            ObjectAnimator rightOpen = ObjectAnimator.ofFloat(pageRight, "rotationY", pageRight.getRotationY(), 0f);

            leftOpen.setDuration(320);
            rightOpen.setDuration(320);
            leftOpen.setInterpolator(new DecelerateInterpolator());
            rightOpen.setInterpolator(new DecelerateInterpolator());

            ObjectAnimator shadowIn = null;
            if (bookShadow != null) {
                shadowIn = ObjectAnimator.ofFloat(bookShadow, "alpha", 0f, 0.35f);
                shadowIn.setDuration(220);
            }

            AnimatorSet set = new AnimatorSet();
            if (shadowIn != null) set.playTogether(leftOpen, rightOpen, shadowIn);
            else set.playTogether(leftOpen, rightOpen);
            set.start();

            openBookOverlay.postDelayed(() -> {
                // fade out and go
                openBookOverlay.animate()
                        .alpha(0f)
                        .setDuration(120)
                        .withEndAction(() -> {
                            openBookOverlay.setAlpha(1f);
                            openBookOverlay.setVisibility(View.GONE);
                            isBookAnimating = false;
                            startActivity(new Intent(MainActivity.this, ActivityDiary.class));
                        })
                        .start();
            }, 360);
        });
    }

    private void setExactHeight(View v, int h) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.height = h;
        v.setLayoutParams(lp);
    }

    private void installSwipe() {
        viewport.setOnTouchListener((v, event) -> {
            if (pageHeight <= 0) return false;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downY = event.getRawY();
                    startTranslationY = container.getTranslationY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dy = event.getRawY() - downY;

                    float newTy = startTranslationY + dy;
                    if (newTy > 0f) newTy = 0f;
                    if (newTy < -pageHeight) newTy = -pageHeight;

                    container.setTranslationY(newTy);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float endTy = container.getTranslationY();
                    float threshold = pageHeight * 0.25f;

                    if (!isPanelBShown) {
                        if (endTy < -threshold) showPanelB();
                        else showPanelA();
                    } else {
                        if (endTy > (-pageHeight + threshold)) showPanelA();
                        else showPanelB();
                    }
                    return true;
            }
            return false;
        });
    }

    private void showPanelA() {
        isPanelBShown = false;
        container.animate().translationY(0f).setDuration(220).start();
    }

    private void showPanelB() {
        isPanelBShown = true;
        container.animate().translationY(-pageHeight).setDuration(220).start();
    }
}
