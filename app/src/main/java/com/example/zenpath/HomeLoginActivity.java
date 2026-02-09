package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

/**
 * HomeLoginActivity
 *
 * Purpose:
 * - Acts as launcher screen (as defined in Manifest)
 * - Shows ripple background
 * - Forwards user to real LoginActivity
 * - Allows ripple interaction while screen is visible
 *
 * You can treat this like a splash / ambient entry screen.
 */
public class HomeLoginActivity extends AppCompatActivity {

    private View ambientBg;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Uses the SAME layout with ripple
        setContentView(R.layout.activity_login);

        // ✅ IMPORTANT: this id must exist in activity_login.xml
        ambientBg = findViewById(R.id.ambientBg);

        // OPTIONAL: Delay before going to Login screen (visual effect)
        // Set to 0 if you want instant navigation
        handler.postDelayed(() -> {
            startActivity(new Intent(HomeLoginActivity.this, LoginActivity.class));
            finish();
        }, 800); // 0.8s ambient preview
    }

    /**
     * Allows tapping anywhere on this launcher screen
     * to generate ripples before navigating away.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        // ✅ IMPORTANT: null safety so it never crashes
        if (ambientBg != null) {
            MotionEvent copy = MotionEvent.obtain(ev);
            ambientBg.dispatchTouchEvent(copy);
            copy.recycle();
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ IMPORTANT: prevent delayed runnable running after activity is gone
        handler.removeCallbacksAndMessages(null);
    }
}