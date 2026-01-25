package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private ImageButton btnMenu;
    private Button btnPlay;
    private CalendarView calendarView;

    private Calendar selectedCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        btnMenu = findViewById(R.id.btnMenu);
        btnPlay = findViewById(R.id.btnPlay);
        calendarView = findViewById(R.id.calendarView);

        selectedCalendar = Calendar.getInstance();

        // ===== Settings Popup Overlay (same as Journal) =====
        ViewGroup rootView = findViewById(android.R.id.content);
        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(android.view.View.GONE);

// Burger icon opens popup
        btnMenu.setOnClickListener(v -> settingsPopup.setVisibility(android.view.View.VISIBLE));

// Tap outside closes popup
        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(android.view.View.GONE));

// Prevent closing when tapping card itself (optional but recommended)
        View settingsCard = settingsPopup.findViewById(R.id.settingsCard);
        if (settingsCard != null) {
            settingsCard.setOnClickListener(v -> {});
        }

// Buttons inside popup
        ImageButton btnHome = settingsPopup.findViewById(R.id.btnHome);
        ImageButton btnBack = settingsPopup.findViewById(R.id.btnBack);
        ImageButton btnHistory = settingsPopup.findViewById(R.id.btnHistory);
        ImageButton btnMood = settingsPopup.findViewById(R.id.btnMood);

        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Home", Toast.LENGTH_SHORT).show();
                // you're already on Home, so just close
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));
        }

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(MainActivity.this, DiaryHistoryActivity.class));
            });
        }


        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(MainActivity.this, MoodActivity.class));
            });
        }


        Button tvJournal = findViewById(R.id.tvJournal);

        tvJournal.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ActivityDiary.class));
        });


        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedCalendar.set(year, month, dayOfMonth);

            // 🔥 Go to Mood Tracker
            Intent intent = new Intent(MainActivity.this, MoodActivity.class);
            startActivity(intent);
        });

        // Play button
        btnPlay.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SelectionGamesActivity.class));
        });
    }
}
