package com.example.zenpath;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private ImageButton btnMenu;
    private Button btnPlay;
    private CalendarView calendarView;

    private Calendar selectedCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
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



        // Calendar listener
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedCalendar.set(year, month, dayOfMonth);
            Toast.makeText(
                    this,
                    "Selected: " + dayOfMonth + "/" + (month + 1) + "/" + year,
                    Toast.LENGTH_SHORT
            ).show();
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
