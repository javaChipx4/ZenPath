package com.example.zenpath;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
        setContentView(R.layout.activity_main);

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