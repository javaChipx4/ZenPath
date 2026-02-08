package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActLR extends AppCompatActivity {

    private Button btnLanternGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_lr);

        // 🔥 Button connection
        btnLanternGame = findViewById(R.id.btnLanternGame);

        // Open Lantern Game
        btnLanternGame.setOnClickListener(v -> {
            Intent i = new Intent(MainActLR.this, LanternReleaseActivity.class);
            startActivity(i);
        });
    }
}