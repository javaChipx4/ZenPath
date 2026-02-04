package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class StarSweepIntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_star_sweep_intro);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnSkip = findViewById(R.id.btnSkip);

        btnStart.setOnClickListener(v -> openGame());
        btnSkip.setOnClickListener(v -> openGame());
    }

    private void openGame() {
        Intent intent = new Intent(StarSweepIntroActivity.this, StarSweepActivity.class);
        startActivity(intent);
        finish();
    }
}
