package com.example.zenpath;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class BookFlipPageTransformer implements ViewPager2.PageTransformer {

    private final float minScale = 0.92f;
    private final float maxRotate = 18f; // degrees

    @Override
    public void transformPage(@NonNull View page, float position) {
        // position: -1 (left off) ... 0 (center) ... 1 (right off)
        page.setCameraDistance(20000f);

        if (position < -1f || position > 1f) {
            page.setAlpha(0f);
            return;
        }

        page.setAlpha(1f);

        // Scale slightly
        float scale = Math.max(minScale, 1f - Math.abs(position) * 0.08f);
        page.setScaleX(scale);
        page.setScaleY(scale);

        // Rotate like flipping a page
        float rotation = -maxRotate * position;
        page.setRotationY(rotation);

        // Pivot left/right edge (book spine feel)
        if (position < 0) {
            page.setPivotX(page.getWidth()); // pivot on right edge for left page
        } else {
            page.setPivotX(0f);              // pivot on left edge for right page
        }
        page.setPivotY(page.getHeight() * 0.5f);

        // Slight translation to reduce “gap”
        page.setTranslationX(-position * page.getWidth() * 0.06f);
    }
}
