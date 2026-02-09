package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private LinearLayout btnLogin;
    private TextView tvAddUser;
    private ImageView imgAvatarLogin;

    private ZenPathRepository repo;

    private SharedPreferences prefs;
    private static final String PREF_NAME = "zenpath_user";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_GENDER = "gender";

    private static final int REQ_ADD_USER = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        repo = new ZenPathRepository(this);

        etUsername = findViewById(R.id.etUsername);
        btnLogin = findViewById(R.id.btnLogin);
        tvAddUser = findViewById(R.id.tvAddUser);
        imgAvatarLogin = findViewById(R.id.imgAvatarLogin);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        loadLastUser();
        updateAvatarFromPrefs();

        // ✅ Auto-login if session exists AND userId still exists in DB
        autoLoginIfRemembered();

        if (imgAvatarLogin != null) {
            imgAvatarLogin.setOnClickListener(v -> {
                toggleGender();
                updateAvatarFromPrefs();
            });
        }

        btnLogin.setOnClickListener(v -> loginUser());

        tvAddUser.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, AddUserActivity.class);
            startActivityForResult(i, REQ_ADD_USER);
        });
    }

    private void autoLoginIfRemembered() {
        SharedPreferences sessionPrefs = getSharedPreferences("zen_path_prefs", MODE_PRIVATE);
        String s = sessionPrefs.getString("current_user", null);
        if (TextUtils.isEmpty(s)) return;

        long userId = -1;
        try { userId = Long.parseLong(s); } catch (Exception ignored) {}

        if (userId > 0 && repo != null && repo.userIdExists(userId)) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        } else {
            // invalid session, clear it
            sessionPrefs.edit().remove("current_user").apply();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_ADD_USER && resultCode == RESULT_OK && data != null) {
            String newUser = data.getStringExtra("new_username");
            String newGender = data.getStringExtra("new_gender");

            if (!TextUtils.isEmpty(newUser)) {
                etUsername.setText(newUser);
                saveLastUsername(newUser);

                if (!TextUtils.isEmpty(newGender)) saveGender(newGender);

                updateAvatarFromPrefs();
                Toast.makeText(this, "Registered: " + newUser, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Please enter username", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ BLOCK if not registered
        if (repo == null || !repo.userExists(username)) {
            Toast.makeText(this,
                    "User not registered. Please tap +Add New User first.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        saveLastUsername(username);

        long userId = repo.getUserIdByUsername(username);

        // ✅ Save session so app remembers user
        getSharedPreferences("zen_path_prefs", MODE_PRIVATE)
                .edit()
                .putString("current_user", String.valueOf(userId))
                .apply();

        // ✅ OPTIONAL: try saving gender to DB (won’t crash if column missing)
        String gender = prefs.getString(KEY_GENDER, "Male");
        repo.updateUserGender(userId, gender);

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("gender", gender);
        startActivity(intent);
        finish();
    }

    private void saveLastUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    private void saveGender(String gender) {
        prefs.edit().putString(KEY_GENDER, gender).apply();
    }

    private void loadLastUser() {
        String lastUser = prefs.getString(KEY_USERNAME, "");
        if (!TextUtils.isEmpty(lastUser)) {
            etUsername.setText(lastUser);
        }
        if (TextUtils.isEmpty(prefs.getString(KEY_GENDER, ""))) {
            saveGender("Male");
        }
    }

    private void toggleGender() {
        String g = prefs.getString(KEY_GENDER, "Male");
        String next = "Male".equalsIgnoreCase(g) ? "Female" : "Male";
        saveGender(next);
    }

    private void updateAvatarFromPrefs() {
        if (imgAvatarLogin == null) return;

        String g = prefs.getString(KEY_GENDER, "Male");
        if ("Female".equalsIgnoreCase(g)) imgAvatarLogin.setImageResource(R.drawable.girl);
        else imgAvatarLogin.setImageResource(R.drawable.boy);
    }
}