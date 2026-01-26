package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ActivitySSMain extends AppCompatActivity {

    private Button btnOpenGame;
    private TextView tvStreakHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_star);

        tvStreakHome = findViewById(R.id.tvStreakHome);
        btnOpenGame = findViewById(R.id.btnOpenGame);

        btnOpenGame.setOnClickListener(v ->
                startActivity(new Intent(ActivitySSMain.this, StarSweepActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        int streak = StreakManager.getStreak(this);
        tvStreakHome.setText("Daily streak: " + streak + " 🔥");
    }
}
