package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ===== Bind views that already exist in your XML =====
        btnMenu = findViewById(R.id.btnMenu);
        btnPlay = findViewById(R.id.btnPlay);
        calendarView = findViewById(R.id.calendarView);

        selectedCalendar = Calendar.getInstance();

        // ===== Read + show username on Home (FIX) =====
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

        btnMenu.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));
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

        // ===== Existing navigation =====
        Button tvJournal = findViewById(R.id.tvJournal);
        if (tvJournal != null) {
            tvJournal.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, ActivityDiary.class))
            );
        }

        if (calendarView != null) {
            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                selectedCalendar.set(year, month, dayOfMonth);
                startActivity(new Intent(MainActivity.this, MoodActivity.class));
            });
        }

        if (btnPlay != null) {
            btnPlay.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, SelectionGamesActivity.class))
            );
        }

        // ===== Switch Profile ONLY on Home (FIX KEY) =====
        View btnSwitch = settingsPopup.findViewById(R.id.btnSwitchProfile);
        View tvSwitch = settingsPopup.findViewById(R.id.tvSwitchProfile);

        if (btnSwitch != null) btnSwitch.setVisibility(View.VISIBLE);
        if (tvSwitch != null) tvSwitch.setVisibility(View.VISIBLE);

        if (btnSwitch != null) {
            btnSwitch.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);

                // Clear current user (THIS is what LoginActivity uses)
                getSharedPreferences("zen_path_prefs", MODE_PRIVATE)
                        .edit()
                        .remove("current_user")
                        .apply();

                // Go back to Login
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            });
        }

        // ===== Swipe setup (NO XML CHANGES REQUIRED) =====
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
