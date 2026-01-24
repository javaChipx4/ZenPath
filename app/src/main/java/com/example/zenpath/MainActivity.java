package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
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

        btnMenu.setOnClickListener(v -> {
            SettingsDialogFragment dialog = new SettingsDialogFragment(new SettingsDialogFragment.Listener() {
                @Override public void onHome() {
                    Toast.makeText(MainActivity.this, "Home", Toast.LENGTH_SHORT).show();
                }

                @Override public void onHistory() {
                    Toast.makeText(MainActivity.this, "History", Toast.LENGTH_SHORT).show();
                }

                @Override public void onBack() {
                    // example: just close popup, or go back
                    Toast.makeText(MainActivity.this, "Back", Toast.LENGTH_SHORT).show();
                }

                @Override public void onMood() {
                    Toast.makeText(MainActivity.this, "Mood Tracker", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.show(getSupportFragmentManager(), "settings_popup");
        });

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
            int y = selectedCalendar.get(Calendar.YEAR);
            int m = selectedCalendar.get(Calendar.MONTH) + 1;
            int d = selectedCalendar.get(Calendar.DAY_OF_MONTH);

            Toast.makeText(
                    this,
                    "Play clicked • " + d + "/" + m + "/" + y,
                    Toast.LENGTH_SHORT
            ).show();
        });
    }
}
