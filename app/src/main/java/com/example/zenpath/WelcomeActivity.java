package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private static final String PREFS = "zen_path_prefs";
    private static final String KEY_FIRST_TIME_USER = "first_time_user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button btn = findViewById(R.id.btnStartZenPath);

        btn.setOnClickListener(v -> {
            SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
            p.edit().putBoolean(KEY_FIRST_TIME_USER, false).apply();

            Intent i = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(i);
            finish(); // prevents going back to welcome screen
        });
    }
}
