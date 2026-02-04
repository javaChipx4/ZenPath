package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.zenpath.R;

public class MainActLR extends AppCompatActivity{

    // ✅ Button for Lantern Game
    private Button btnLanternGame = findViewById(R.id.btnLanternGame);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Setup popup menu (must be called here, not declared here)
        setupPopupMenu();

        if (btnLanternGame != null) {
            btnLanternGame.setOnClickListener(v -> {
                Intent i = new Intent( MainActLR.this, LanternReleaseActivity.class);
                startActivity(i);
            });
        }
    }

    // ✅ Method must be OUTSIDE onCreate()
    private void setupPopupMenu() {
        // ===== SAME POPUP LOGIC AS OTHER SCREENS =====
        ViewGroup rootView = findViewById(android.R.id.content);

        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        if (menuIcon != null) {
            menuIcon.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));
        }

        // Tap outside to close
        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

        // Prevent closing when tapping inside the card
        View settingsCard = settingsPopup.findViewById(R.id.settingsCard);
        if (settingsCard != null) settingsCard.setOnClickListener(v -> {});

        ImageButton btnHome = settingsPopup.findViewById(R.id.btnHome);
        ImageButton btnBack = settingsPopup.findViewById(R.id.btnBack);
        ImageButton btnMood = settingsPopup.findViewById(R.id.btnMood);
        ImageButton btnHistory = settingsPopup.findViewById(R.id.btnHistory);

        // Optional: add your navigation actions here if you want
        // if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }
}