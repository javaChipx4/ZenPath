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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

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

    // ✅ NEW: quote views
    private TextView tvQuote, tvCenterQuote, tvSubQuote;

    // ✅ NEW: glow overlay view
    private View glowOverlay;

    // ✅ prefs keys
    private static final String PREFS = "zen_path_prefs";
    private static final String KEY_QUOTE_DATE = "daily_quote_date";
    private static final String KEY_QUOTE_TEXT = "daily_quote_text";
    private static final String KEY_QUOTE_SUB = "daily_quote_sub";

    private static String moodKey(String d) { return "mood_" + d; }

    // Mood index mapping from your app:
    // 0😁 1😌 2😐 3🥰 4😭

    private static final String[] QUOTES_HAPPY = new String[]{
            "You’re allowed to enjoy this moment. Let it count.",
            "Small wins still count. Keep going.",
            "Your energy is contagious — share it gently.",
            "Today feels lighter. Hold onto that."
    };

    private static final String[] QUOTES_CALM = new String[]{
            "Breathe in. Breathe out. You’re doing enough.",
            "Slow is still progress.",
            "Quiet moments are healing moments.",
            "You don’t need to rush to be okay."
    };

    private static final String[] QUOTES_NEUTRAL = new String[]{
            "You don’t have to feel “great” to move forward.",
            "Steady days build strong weeks.",
            "Just show up for yourself — even a little.",
            "Neutral is valid. Rest here."
    };

    private static final String[] QUOTES_LOVED = new String[]{
            "You deserve softness — especially from yourself.",
            "Be proud of how far you’ve come.",
            "Love the way you keep trying.",
            "You are worth taking care of."
    };

    private static final String[] QUOTES_SAD = new String[]{
            "It’s okay to not be okay. Stay gentle.",
            "You’re not behind. You’re healing.",
            "One breath at a time. One step at a time.",
            "You don’t have to carry everything alone."
    };

    private static final String[] QUOTES_GENERAL = new String[]{
            "Take today slowly. Calm is still progress.",
            "You’re doing better than you think.",
            "Even small moments of peace matter.",
            "Your pace is valid. Keep going."
    };

    private static final String[] SUB_HAPPY = new String[]{
            "Keep the good energy 🌟", "Celebrate small wins ✨", "Let yourself smile 🙂"
    };
    private static final String[] SUB_CALM = new String[]{
            "Stay soft 🌿", "Breathe easy 💛", "One step at a time 🫧"
    };
    private static final String[] SUB_NEUTRAL = new String[]{
            "Steady and okay 🤍", "No pressure today ☁️", "You’re allowed to be neutral 🫶"
    };
    private static final String[] SUB_LOVED = new String[]{
            "You matter 🫶", "Be kind to yourself 💗", "You’re worthy 🌸"
    };
    private static final String[] SUB_SAD = new String[]{
            "Gentle day 🫧", "You’re not alone 🤍", "It will pass 🌙"
    };
    private static final String[] SUB_GENERAL = new String[]{
            "You are valid.", "Be gentle with yourself.", "Just breathe."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ===== Book overlay views =====
        openBookOverlay = findViewById(R.id.openBookOverlay);
        bookContainer = findViewById(R.id.bookContainer);
        pageLeft = findViewById(R.id.pageLeft);
        pageRight = findViewById(R.id.pageRight);
        bookShadow = findViewById(R.id.bookShadow);

        if (openBookOverlay != null) {
            openBookOverlay.setOnClickListener(v -> hideBookOverlay());
        }
        if (bookContainer != null) {
            bookContainer.setOnClickListener(v -> playOpenBookThenGoDiary());
        }

        // ===== Bind views =====
        btnMenu = findViewById(R.id.btnMenu);
        btnPlay = findViewById(R.id.btnPlay);
        calendarView = findViewById(R.id.calendarView);
        selectedCalendar = Calendar.getInstance();

        // ✅ quotes in both panels
        tvQuote = findViewById(R.id.tvQuote);              // Panel A
        tvCenterQuote = findViewById(R.id.tvCenterQuote);  // Panel B
        tvSubQuote = findViewById(R.id.tvSubQuote);        // Panel B (optional)
        glowOverlay = findViewById(R.id.glowOverlay);      // background glow layer

        // ===== Username =====
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String username = prefs.getString("current_user", null);
        if (TextUtils.isEmpty(username)) username = "User";

        TextView tvUsername = findViewById(R.id.tvUsername);
        TextView tvHelloUser = findViewById(R.id.tvHelloUser);

        if (tvUsername != null) tvUsername.setText(username.toUpperCase());
        if (tvHelloUser != null) tvHelloUser.setText("Hello, " + username + "!");

        // ✅ set daily mood-based quotes (once per day)
        applyDailyQuotes();

        // ✅ animate background glow
        startGlowAnimation();

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

        // ===== Journal click -> show book =====
        Button tvJournal = findViewById(R.id.tvJournal);
        if (tvJournal != null) {
            tvJournal.setOnClickListener(v -> showBookOverlay());
            installPressAnim(tvJournal);
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
            installPressAnim(btnPlay);
        }

        // ✅ press anim for menu (nice touch)
        if (btnMenu != null) installPressAnim(btnMenu);

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

    // =========================
    // ✅ DAILY MOOD-BASED QUOTES
    // =========================

    private void applyDailyQuotes() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        String todayKey = todayDateKey();
        String savedDate = prefs.getString(KEY_QUOTE_DATE, null);

        // if already generated for today, reuse
        if (todayKey.equals(savedDate)) {
            String q = prefs.getString(KEY_QUOTE_TEXT, "");
            String sub = prefs.getString(KEY_QUOTE_SUB, "");
            setQuotesToUI(q, sub);
            return;
        }

        // mood-based (if today's mood exists), else general
        int mood = prefs.getInt(moodKey(todayKey), -1);

        String quote = pickQuoteForMood(mood);
        String sub = pickSubForMood(mood);

        prefs.edit()
                .putString(KEY_QUOTE_DATE, todayKey)
                .putString(KEY_QUOTE_TEXT, quote)
                .putString(KEY_QUOTE_SUB, sub)
                .apply();

        setQuotesToUI(quote, sub);
    }

    private void setQuotesToUI(String quote, String sub) {
        if (tvQuote != null && !TextUtils.isEmpty(quote)) {
            tvQuote.setText(quote);
        }
        if (tvCenterQuote != null && !TextUtils.isEmpty(quote)) {
            tvCenterQuote.setText(quote);
        }
        if (tvSubQuote != null && !TextUtils.isEmpty(sub)) {
            tvSubQuote.setText(sub);
        }
    }

    private String todayDateKey() {
        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Calendar.getInstance().getTime());
    }

    private String pickQuoteForMood(int mood) {
        Random r = new Random();
        switch (mood) {
            case 0: return QUOTES_HAPPY[r.nextInt(QUOTES_HAPPY.length)];
            case 1: return QUOTES_CALM[r.nextInt(QUOTES_CALM.length)];
            case 2: return QUOTES_NEUTRAL[r.nextInt(QUOTES_NEUTRAL.length)];
            case 3: return QUOTES_LOVED[r.nextInt(QUOTES_LOVED.length)];
            case 4: return QUOTES_SAD[r.nextInt(QUOTES_SAD.length)];
            default: return QUOTES_GENERAL[r.nextInt(QUOTES_GENERAL.length)];
        }
    }

    private String pickSubForMood(int mood) {
        Random r = new Random();
        switch (mood) {
            case 0: return SUB_HAPPY[r.nextInt(SUB_HAPPY.length)];
            case 1: return SUB_CALM[r.nextInt(SUB_CALM.length)];
            case 2: return SUB_NEUTRAL[r.nextInt(SUB_NEUTRAL.length)];
            case 3: return SUB_LOVED[r.nextInt(SUB_LOVED.length)];
            case 4: return SUB_SAD[r.nextInt(SUB_SAD.length)];
            default: return SUB_GENERAL[r.nextInt(SUB_GENERAL.length)];
        }
    }

    // =========================
    // ✅ BACKGROUND GLOW
    // =========================

    private void startGlowAnimation() {
        if (glowOverlay == null) return;

        glowOverlay.setAlpha(0.0f);

        ObjectAnimator a1 = ObjectAnimator.ofFloat(glowOverlay, "alpha", 0.0f, 0.35f);
        a1.setDuration(1800);
        a1.setRepeatCount(ObjectAnimator.INFINITE);
        a1.setRepeatMode(ObjectAnimator.REVERSE);
        a1.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator t1 = ObjectAnimator.ofFloat(glowOverlay, "translationY", 0f, -40f);
        t1.setDuration(2600);
        t1.setRepeatCount(ObjectAnimator.INFINITE);
        t1.setRepeatMode(ObjectAnimator.REVERSE);
        t1.setInterpolator(new DecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(a1, t1);
        set.start();
    }

    // =========================
    // ✅ PRESS ANIM (scale)
    // =========================

    private void installPressAnim(View v) {
        if (v == null) return;

        v.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.98f).scaleY(0.98f).setDuration(70).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                    break;
            }
            // return false so click still works
            return false;
        });
    }

    // =========================
    // Existing book + swipe code
    // =========================

    private void showBookOverlay() {
        if (openBookOverlay == null || bookContainer == null) {
            startActivity(new Intent(MainActivity.this, ActivityDiary.class));
            return;
        }

        openBookOverlay.setVisibility(View.VISIBLE);
        openBookOverlay.setAlpha(0f);

        bookContainer.setScaleX(0.92f);
        bookContainer.setScaleY(0.92f);
        bookContainer.setAlpha(0f);

        if (pageLeft != null && pageRight != null) {
            float scale = getResources().getDisplayMetrics().density;
            float camera = 8000f * scale;
            pageLeft.setCameraDistance(camera);
            pageRight.setCameraDistance(camera);

            bookContainer.post(() -> {
                pageLeft.setPivotX(pageLeft.getWidth());
                pageLeft.setPivotY(pageLeft.getHeight() / 2f);

                pageRight.setPivotX(0f);
                pageRight.setPivotY(pageRight.getHeight() / 2f);

                pageLeft.setRotationY(70f);
                pageRight.setRotationY(-70f);

                if (bookShadow != null) bookShadow.setAlpha(0f);

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

        bookContainer.post(() -> {
            float scale = getResources().getDisplayMetrics().density;
            float camera = 8000f * scale;
            pageLeft.setCameraDistance(camera);
            pageRight.setCameraDistance(camera);

            pageLeft.setPivotX(pageLeft.getWidth());
            pageLeft.setPivotY(pageLeft.getHeight() / 2f);

            pageRight.setPivotX(0f);
            pageRight.setPivotY(pageRight.getHeight() / 2f);

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
