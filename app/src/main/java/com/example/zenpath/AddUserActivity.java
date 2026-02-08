package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddUserActivity extends AppCompatActivity {

    private EditText etNewUsername;
    private ZenPathRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        repo = new ZenPathRepository(this);

        etNewUsername = findViewById(R.id.etNewUsername);
        Button btnCreate = findViewById(R.id.btnCreateUser);
        TextView tvCancel = findViewById(R.id.tvCancel);

        btnCreate.setOnClickListener(v -> createUser());
        tvCancel.setOnClickListener(v -> finish());
    }

    private void createUser() {
        String username = etNewUsername.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etNewUsername.setError("Enter a username");
            return;
        }

        // ✅ Check DB
        if (repo.userExists(username)) {
            Toast.makeText(this, "That username already exists.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Insert into DB
        long id = repo.createUser(username);

        if (id == -1) {
            Toast.makeText(this, "Failed to create user.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Return result to Login
        Intent data = new Intent();
        data.putExtra("new_username", username);
        data.putExtra("user_id", id);
        setResult(RESULT_OK, data);
        finish();
    }
}
