package com.example.zenpath;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

    // existing quote views
    private TextView tvQuote, tvCenterQuote, tvSubQuote;

    // top pill quote
    private TextView tvPillQuote;

    // fortune overlay
    private View fortuneOverlay;
    private View fortuneCard;

    // glow overlay
    private View glowOverlay;

    // ✅ Today Check-in card
    private TextView tvCheckInMood, tvCheckInStress;
    private Button btnCheckInNow;

    // ✅ Repo
    private ZenPathRepository repo;

    // prefs
    private static final String PREFS = "zen_path_prefs";
    private static final String KEY_QUOTE_DATE = "daily_quote_date";
    private static final String KEY_QUOTE_TEXT = "daily_quote_text";
    private static final String KEY_FORTUNE_REVEAL_DATE = "fortune_reveal_date";

    // ✅ Wind down panel B
    private TextView tvWindDownSuggestion;
    private Button btnBrowseActivities;

    // ✅ Latest recommendation cached (so we can pass to SelectionGames)
    private GameRecommender.Recommendation currentRec;

    // Quote pool
    private static final String[] QUOTES_DAILY = new String[]{
            "Take today slowly. Calm is still progress.",
            "You are doing better than you think.",
            "Small steps still move you forward.",
            "Breathe. You’re safe in this moment.",
            "You don’t need to rush to be okay.",
            "Be proud of how far you’ve come.",
            "You are allowed to rest without guilt.",
            "Your pace is valid. Keep going.",
            "Gentleness is strength too.",
            "Today, choose one kind thing for yourself."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Repo
        repo = new ZenPathRepository(this);

        // ===== Bind views =====
        btnMenu = findViewById(R.id.btnMenu);
        calendarView = findViewById(R.id.calendarView);
        selectedCalendar = Calendar.getInstance();

        tvQuote = findViewById(R.id.tvQuote);
        tvCenterQuote = findViewById(R.id.tvCenterQuote);
        tvSubQuote = findViewById(R.id.tvSubQuote);
        glowOverlay = findViewById(R.id.glowOverlay);

        // pill + fortune
        tvPillQuote = findViewById(R.id.tvPillQuote);
        fortuneOverlay = findViewById(R.id.fortuneOverlay);
        fortuneCard = findViewById(R.id.fortuneCard);

        // ✅ Today Check-in bindings (Panel A now)
        tvCheckInMood = findViewById(R.id.tvCheckInMood);
        tvCheckInStress = findViewById(R.id.tvCheckInStress);
        btnCheckInNow = findViewById(R.id.btnCheckInNow);

        if (btnCheckInNow != null) {
            btnCheckInNow.setOnClickListener(v -> openMoodForDate(todayDateKey()));
            installPressAnim(btnCheckInNow);
        }

        // ✅ Wind Down bindings (Panel B)
        tvWindDownSuggestion = findViewById(R.id.tvWindDownSuggestion);
        btnBrowseActivities = findViewById(R.id.btnBrowseActivities);

        if (btnBrowseActivities != null) {
            btnBrowseActivities.setOnClickListener(v -> {
                Intent i = new Intent(MainActivity.this, SelectionGamesActivity.class);

                // ✅ pass suggestion title if available
                if (currentRec != null && !TextUtils.isEmpty(currentRec.title)) {
                    i.putExtra(SelectionGamesActivity.EXTRA_SUGGESTION_TITLE, currentRec.title);
                }

                startActivity(i);
            });
            installPressAnim(btnBrowseActivities);
        }

        // date in pill
        TextView tvDate = findViewById(R.id.tvDate);
        if (tvDate != null) {
            String pretty = new SimpleDateFormat("MMMM dd", Locale.getDefault())
                    .format(Calendar.getInstance().getTime());
            tvDate.setText(pretty);
        }

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

        // ===== Username =====
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String current = prefs.getString("current_user", null);

        String displayName = "User";
        if (!TextUtils.isEmpty(current)) {
            try {
                long userId = Long.parseLong(current);
                String nameFromDb = repo.getUsernameById(userId);
                if (!TextUtils.isEmpty(nameFromDb)) displayName = nameFromDb;
            } catch (NumberFormatException ignored) {
                displayName = current;
            }
        }

        TextView tvUsername = findViewById(R.id.tvUsername);
        TextView tvHelloUser = findViewById(R.id.tvHelloUser);

        if (tvUsername != null) tvUsername.setText(displayName.toUpperCase());
        if (tvHelloUser != null) tvHelloUser.setText("Hello, " + displayName + "!");

        // ✅ Quote for the day
        String todayQuote = getOrCreateDailyQuote();

        if (isFortuneRevealedToday()) {
            setPillQuote(todayQuote);
        } else {
            setPillQuote("Tap to reveal");
            showFortuneOverlay();
        }

        // Panel B quote
        setQuotesToUI(todayQuote, "");

        // glow
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
        ImageButton btnSwitchProfile = settingsPopup.findViewById(R.id.btnSwitchProfile);
        TextView tvSwitchProfile = settingsPopup.findViewById(R.id.tvSwitchProfile);

        if (btnHome != null) btnHome.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));
        if (btnBack != null) btnBack.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));
        if (btnSwitchProfile != null) btnSwitchProfile.setVisibility(View.VISIBLE);
        if (tvSwitchProfile != null) tvSwitchProfile.setVisibility(View.VISIBLE);

        if (btnSwitchProfile != null) {
            btnSwitchProfile.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                prefs.edit().remove("current_user").apply();

                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            });
        }

        if (tvSwitchProfile != null) {
            tvSwitchProfile.setOnClickListener(v -> {
                if (btnSwitchProfile != null) btnSwitchProfile.performClick();
            });
        }

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            });
        }

        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                openMoodForDate(todayDateKey());
            });
        }

        // ===== Virtual Diary oval click -> show book =====
        View ovalCard = findViewById(R.id.ovalCard);
        if (ovalCard != null) {
            ovalCard.setOnClickListener(v -> showBookOverlay());
            installPressAnim(ovalCard);
        }

        // ✅ Calendar -> Mood (send dateKey)
        if (calendarView != null) {
            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                selectedCalendar.set(year, month, dayOfMonth);
                String dateKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                        .format(selectedCalendar.getTime());
                openMoodForDate(dateKey);
            });
        }

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

    @Override
    protected void onResume() {
        super.onResume();
        refreshTodayCheckInCard();
        refreshWindDownSuggestion(); // updates Panel B suggestion + currentRec
    }

    // =========================
    // ✅ Quote of the day
    // =========================

    private String getOrCreateDailyQuote() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        String todayKey = todayDateKey();
        String savedDate = prefs.getString(KEY_QUOTE_DATE, null);

        if (todayKey.equals(savedDate)) {
            return prefs.getString(KEY_QUOTE_TEXT, "");
        }

        String quote = QUOTES_DAILY[new Random().nextInt(QUOTES_DAILY.length)];
        prefs.edit()
                .putString(KEY_QUOTE_DATE, todayKey)
                .putString(KEY_QUOTE_TEXT, quote)
                .apply();

        return quote;
    }

    private void setPillQuote(String text) {
        if (tvPillQuote != null) tvPillQuote.setText(text);
    }

    private String todayDateKey() {
        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
    }

    // =========================
    // ✅ Fortune reveal logic
    // =========================

    private boolean isFortuneRevealedToday() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String today = todayDateKey();
        String revealedDate = prefs.getString(KEY_FORTUNE_REVEAL_DATE, null);
        return today.equals(revealedDate);
    }

    private void markFortuneRevealedToday() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putString(KEY_FORTUNE_REVEAL_DATE, todayDateKey()).apply();
    }

    private void showFortuneOverlay() {
        if (fortuneOverlay == null) return;

        fortuneOverlay.setVisibility(View.VISIBLE);
        fortuneOverlay.setAlpha(0f);
        fortuneOverlay.animate().alpha(1f).setDuration(180).start();

        if (fortuneCard != null) {
            fortuneCard.setOnClickListener(v -> revealFortune());
            installPressAnim(fortuneCard);
        }

        fortuneOverlay.setOnClickListener(v -> {
            // force reveal (no dismiss)
        });
    }

    private void hideFortuneOverlay() {
        if (fortuneOverlay == null) return;

        fortuneOverlay.animate()
                .alpha(0f)
                .setDuration(140)
                .withEndAction(() -> {
                    fortuneOverlay.setAlpha(1f);
                    fortuneOverlay.setVisibility(View.GONE);
                })
                .start();
    }

    private void revealFortune() {
        String quote = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_QUOTE_TEXT, "");
        if (TextUtils.isEmpty(quote)) quote = getOrCreateDailyQuote();

        if (fortuneCard != null) {
            fortuneCard.animate()
                    .scaleX(1.06f).scaleY(1.06f)
                    .setDuration(120)
                    .withEndAction(() -> fortuneCard.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                    .start();
        }

        markFortuneRevealedToday();
        setPillQuote(quote);
        setQuotesToUI(quote, "");

        if (fortuneOverlay != null) {
            fortuneOverlay.postDelayed(this::hideFortuneOverlay, 280);
        }
    }

    private void setQuotesToUI(String quote, String sub) {
        if (tvQuote != null && !TextUtils.isEmpty(quote)) tvQuote.setText(quote);
        if (tvCenterQuote != null && !TextUtils.isEmpty(quote)) tvCenterQuote.setText(quote);
        if (tvSubQuote != null) tvSubQuote.setText(sub);
    }

    // =========================
    // ✅ Today Check-in card logic
    // =========================

    private void openMoodForDate(String dateKey) {
        Intent i = new Intent(this, MoodActivity.class);
        i.putExtra(MoodActivity.EXTRA_DATE_KEY, dateKey);
        startActivity(i);
    }

    private long currentUserId() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String s = prefs.getString("current_user", null);
        if (TextUtils.isEmpty(s)) return -1;
        try { return Long.parseLong(s); } catch (Exception e) { return -1; }
    }

    private void refreshTodayCheckInCard() {
        String key = todayDateKey();
        long userId = currentUserId();

        if (userId <= 0) {
            if (tvCheckInMood != null) tvCheckInMood.setText("Mood: —");
            if (tvCheckInStress != null) tvCheckInStress.setText("Stress: —");
            if (btnCheckInNow != null) btnCheckInNow.setText("Check in");
            return;
        }

        String[] moodData = (repo != null) ? repo.getMoodByDate(userId, key) : null;
        String moodText = (moodData != null) ? moodData[0] : "";

        if (tvCheckInMood != null) {
            tvCheckInMood.setText(TextUtils.isEmpty(moodText)
                    ? "Mood: Not checked in yet"
                    : "Mood: " + moodText);
        }

        int stress = getStressLevel(userId, key);
        if (tvCheckInStress != null) tvCheckInStress.setText("Stress: " + stress + "%");

        if (btnCheckInNow != null) {
            btnCheckInNow.setText(TextUtils.isEmpty(moodText) ? "Check in" : "Edit check-in");
        }
    }

    private int getStressLevel(long userId, String dateKey) {
        int out = 50;

        ZenPathDbHelper helper = new ZenPathDbHelper(this);
        try (SQLiteDatabase db = helper.getReadableDatabase()) {

            String sql =
                    "SELECT " + ZenPathDbHelper.S_LEVEL +
                            " FROM " + ZenPathDbHelper.T_STRESS +
                            " WHERE " + ZenPathDbHelper.COL_USER_ID + "=? AND " + ZenPathDbHelper.S_DATE + "=? " +
                            " ORDER BY " + ZenPathDbHelper.S_CREATED_AT + " DESC LIMIT 1";

            try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(userId), dateKey})) {
                if (c.moveToFirst()) out = c.getInt(0);
            }

        } catch (Exception ignored) { }

        return out;
    }

    private int moodTextToLevel(String moodText) {
        if (TextUtils.isEmpty(moodText)) return 3;

        String t = moodText.trim().toLowerCase();

        try {
            int n = Integer.parseInt(t);
            if (n >= 1 && n <= 5) return n;
        } catch (Exception ignored) {}

        if (t.contains("sad") || t.contains("down") || t.contains("depress") || t.contains("low")) return 1;
        if (t.contains("anx") || t.contains("stressed") || t.contains("overwhelm") || t.contains("upset")) return 2;
        if (t.contains("okay") || t.contains("neutral") || t.contains("meh") || t.contains("fine")) return 3;
        if (t.contains("good") || t.contains("calm") || t.contains("better") || t.contains("content")) return 4;
        if (t.contains("happy") || t.contains("great") || t.contains("amazing") || t.contains("excited")) return 5;

        return 3;
    }

    // =========================
    // ✅ Wind down suggestion (Panel B)
    // =========================

    private void refreshWindDownSuggestion() {
        String key = todayDateKey();
        long userId = currentUserId();

        if (tvWindDownSuggestion == null) return;

        if (userId <= 0 || repo == null) {
            tvWindDownSuggestion.setText("Suggestion: Check in to get a personalized pick.");
            currentRec = null;
            return;
        }

        String[] moodData = repo.getMoodByDate(userId, key);
        String moodText = (moodData != null) ? moodData[0] : "";

        if (TextUtils.isEmpty(moodText)) {
            tvWindDownSuggestion.setText("Suggestion: Check in to get a personalized pick.");
            currentRec = null;
            return;
        }

        int moodLevel = moodTextToLevel(moodText);
        int stress = getStressLevel(userId, key);

        SharedPreferences sp = getSharedPreferences("zenpath", MODE_PRIVATE);
        String lastGame = sp.getString("last_game", null);

        GameRecommender.Recommendation rec = GameRecommender.recommend(moodLevel, stress, lastGame);

        // ✅ update UI + cache so Browse button can pass it
        tvWindDownSuggestion.setText("Suggestion: " + rec.title);
        currentRec = rec;
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
    // ✅ PRESS ANIM
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
            return false;
        });
    }

    // =========================
    // Book overlay
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

    // =========================
    // Swipe panels
    // =========================

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
