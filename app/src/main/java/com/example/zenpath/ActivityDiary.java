package com.example.zenpath;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ActivityDiary extends AppCompatActivity {

    // Declare the button from your layout
    private Button btnSave;

    @SuppressLint("MissingInflatedId")

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_diary);

// Get root view of the activity
        ViewGroup rootView = findViewById(android.R.id.content);

// Inflate the popup card
        View settingsPopup = getLayoutInflater()
                .inflate(R.layout.dialog_settings, rootView, false);

// Add it to the screen (but invisible)
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);

// Burger icon opens popup
        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.VISIBLE);
        });

// Tap outside to close popup
        settingsPopup.setOnClickListener(v -> {
            settingsPopup.setVisibility(View.GONE);
        });

        // ===== POPUP BUTTONS =====
        ImageButton btnHome = settingsPopup.findViewById(R.id.btnHome);
        ImageButton btnBack = settingsPopup.findViewById(R.id.btnBack);
        ImageButton btnHistory = settingsPopup.findViewById(R.id.btnHistory);
        ImageButton btnMood = settingsPopup.findViewById(R.id.btnMood);


        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(ActivityDiary.this, MainActivity.class));
                finish();
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                finish();
            });
        }

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                Toast.makeText(this, "History clicked", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                Toast.makeText(this, "Mood Tracker clicked", Toast.LENGTH_SHORT).show();
            });
        }


        // FIX: Match the ID exactly to the XML (btn_save)
        btnSave = findViewById(R.id.btn_save);

        // Added a null check to prevent future crashes
        if (btnSave != null) {
            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSaveDialog();
                }
            });
        }
    }

    private void showSaveDialog() {
        // 1. Create the builder using our custom theme
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);

        // 2. Inflate (load) the custom XML layout
        View customView = getLayoutInflater().inflate(R.layout.dialog_save, null);
        builder.setView(customView);

        // 3. Create the dialog
        final AlertDialog dialog = builder.create();

        // 4. Link the buttons inside the CUSTOM layout
        Button btnYes = customView.findViewById(R.id.btn_dialog_yes);
        Button btnNo = customView.findViewById(R.id.btn_dialog_no);

        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ActivityDiary.this, "Progress Saved!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // 5. Show it!
        dialog.show();
    }
}