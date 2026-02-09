package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddUserActivity extends AppCompatActivity {

    private EditText etNewUsername;
    private Button btnCreate;

    private LinearLayout optMale, optFemale;
    private ImageView imgPreview;
    private TextView tvCancel;

    // default selected
    private String selectedGender = "Male";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        etNewUsername = findViewById(R.id.etNewUsername);
        btnCreate = findViewById(R.id.btnCreateUser);

        optMale = findViewById(R.id.optMale);
        optFemale = findViewById(R.id.optFemale);
        imgPreview = findViewById(R.id.imgPreview);
        tvCancel = findViewById(R.id.tvCancel);

        // Default highlight
        setSelectedGender("Male");

        optMale.setOnClickListener(v -> setSelectedGender("Male"));
        optFemale.setOnClickListener(v -> setSelectedGender("Female"));

        tvCancel.setOnClickListener(v -> finish());

        btnCreate.setOnClickListener(v -> createUser());
    }

    private void setSelectedGender(String gender) {
        selectedGender = gender;

        // Update preview image
        if ("Male".equals(gender)) {
            imgPreview.setImageResource(R.drawable.boy);
            optMale.setAlpha(1f);
            optFemale.setAlpha(0.65f);
        } else {
            imgPreview.setImageResource(R.drawable.girl);
            optMale.setAlpha(0.65f);
            optFemale.setAlpha(1f);
        }
    }

    private void createUser() {

        String username = etNewUsername.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etNewUsername.setError("Enter a username");
            return;
        }

        ZenPathRepository repo = new ZenPathRepository(this);

        if (repo.userExists(username)) {
            Toast.makeText(this,
                    "Username already exists. Try another.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // ✅ Create user
        long newId = repo.createUser(username);

        if (newId == -1) {
            Toast.makeText(this,
                    "Failed to create user. Try again.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Return username + gender back to LoginActivity
        Intent data = new Intent();
        data.putExtra("new_username", username);
        data.putExtra("new_gender", selectedGender);
        setResult(RESULT_OK, data);
        finish();
    }
}