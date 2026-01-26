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
    private static final String KEY_CURRENT_USER = "current_user";
    private static final String KEY_USERS_SET = "users_set";

    private EditText etUsername;
    private Button btnLogin;
    private TextView tvQuote;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // OPTIONAL: auto-skip login if user already selected
        String currentUser = prefs.getString(KEY_CURRENT_USER, null);
        if (!TextUtils.isEmpty(currentUser)) {
            goHome();
            return;
        }

        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        btnLogin = findViewById(R.id.btnLogin);
        tvQuote = findViewById(R.id.tvQuote);

        // You can later replace this with your "quote of the day" logic
        tvQuote.setText("“Small moments of calm still count.”");

        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Please enter a username");
            return;
        }

        // Save current user
        prefs.edit().putString(KEY_CURRENT_USER, username).apply();

        // Add to known users list (Option B-ready)
        Set<String> users = prefs.getStringSet(KEY_USERS_SET, new HashSet<>());
        Set<String> updated = new HashSet<>(users); // IMPORTANT: copy set before editing
        updated.add(username);
        prefs.edit().putStringSet(KEY_USERS_SET, updated).apply();

        Toast.makeText(this, "Welcome, " + username + "!", Toast.LENGTH_SHORT).show();
        goHome();
    }


    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
