package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class LanternRelease extends AppCompatActivity {

    private Button btnLanternGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lantern_release);

        btnLanternGame = findViewById(R.id.btnLanternGame);

        btnLanternGame.setOnClickListener(v -> {
            Intent i = new Intent(LanternRelease.this, LanternReleaseActivity.class);
            startActivity(i);
        });
    }
}