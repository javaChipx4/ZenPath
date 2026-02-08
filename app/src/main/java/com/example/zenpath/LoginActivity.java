package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS = "zen_path_prefs";
    private static final String KEY_USERS_SET = "users_set";
    private static final int REQ_ADD_USER = 1001;

    private EditText etUsername;
    private Button btnLogin;
    private TextView tvQuote, tvAddUser;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        etUsername = findViewById(R.id.etUsername);
        btnLogin = findViewById(R.id.btnLogin);
        tvQuote = findViewById(R.id.tvQuote);
        tvAddUser = findViewById(R.id.tvAddUser);

        tvQuote.setText("“Small moments of calm still count.”");

        btnLogin.setOnClickListener(v -> handleLogin());

        // You can rename text in XML to "+ Create Account" if you want
        tvAddUser.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, AddUserActivity.class);
            startActivityForResult(i, REQ_ADD_USER);
        });
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Please enter a username");
            return;
        }

        ZenPathRepository repo = new ZenPathRepository(this);

        // ✅ Must exist in DB
        if (!repo.userExists(username)) {
            Toast.makeText(this,
                    "User not found. Please create an account first.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        long userId = repo.getUserId(username);
        if (userId == -1) {
            Toast.makeText(this, "Login error. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Store current user (better to store userId, not username)
        // For now: store userId as String so your SessionManager doesn't change yet
        SessionManager.setCurrentUser(this, String.valueOf(userId));

        Toast.makeText(this, "Welcome back, " + username + "!", Toast.LENGTH_SHORT).show();
        goHome();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_ADD_USER && resultCode == RESULT_OK && data != null) {
            String newUsername = data.getStringExtra("new_username");
            if (!TextUtils.isEmpty(newUsername)) {
                etUsername.setText(newUsername);
                etUsername.setSelection(newUsername.length());
                Toast.makeText(this, "Account created: " + newUsername, Toast.LENGTH_SHORT).show();

                // ✅ Optional: auto-login immediately after creating account
                // handleLogin();
            }
        }
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
