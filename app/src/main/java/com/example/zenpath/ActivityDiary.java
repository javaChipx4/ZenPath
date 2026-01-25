package com.example.zenpath;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ActivityDiary extends AppCompatActivity {

    private Button btnSave;
    private EditText etJournal;

    private ZenPathRepository repo; // SQLite

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_diary);

        // DB
        repo = new ZenPathRepository(this);

        // Journal input (MAKE SURE THIS ID EXISTS)
        etJournal = findViewById(R.id.etJournal);

        // ===== POPUP (UNCHANGED) =====
        ViewGroup rootView = findViewById(android.R.id.content);
        View settingsPopup = getLayoutInflater().inflate(R.layout.dialog_settings, rootView, false);
        rootView.addView(settingsPopup);
        settingsPopup.setVisibility(View.GONE);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> settingsPopup.setVisibility(View.VISIBLE));
        settingsPopup.setOnClickListener(v -> settingsPopup.setVisibility(View.GONE));

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
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(ActivityDiary.this, DiaryHistoryActivity.class));
            });
        }


        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                settingsPopup.setVisibility(View.GONE);
                startActivity(new Intent(ActivityDiary.this, MoodActivity.class));
            });
        }

        // SAVE button
        btnSave = findViewById(R.id.btn_save);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> showSaveDialog());
        }
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);
        View customView = getLayoutInflater().inflate(R.layout.dialog_save, null);
        builder.setView(customView);

        final AlertDialog dialog = builder.create();

        Button btnYes = customView.findViewById(R.id.btn_dialog_yes);
        Button btnNo = customView.findViewById(R.id.btn_dialog_no);

        btnYes.setOnClickListener(v -> {

            // 1) Get journal text
            String text = etJournal != null ? etJournal.getText().toString().trim() : "";

            if (text.isEmpty()) {
                Toast.makeText(ActivityDiary.this, "Write something first.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2) Get today's date
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            // 3) Save to SQLite
            long id = repo.addJournalEntry(today, text);

            if (id != -1) {
                Toast.makeText(ActivityDiary.this, "Saved to Journal!", Toast.LENGTH_SHORT).show();
                etJournal.setText(""); // clear after saving
            } else {
                Toast.makeText(ActivityDiary.this, "Save failed.", Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
