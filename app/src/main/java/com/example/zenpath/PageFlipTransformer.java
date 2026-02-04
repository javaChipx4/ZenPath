package com.example.zenpath;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class PageFlipTransformer implements ViewPager2.PageTransformer {

    @Override
    public void transformPage(@NonNull View page, float position) {
        page.setCameraDistance(page.getWidth() * 20f);

        if (position < -1f || position > 1f) {
            page.setAlpha(0f);
            return;
        }

        page.setAlpha(1f);

        // Slight scale for depth
        float scale = 0.92f + (1f - Math.abs(position)) * 0.08f;
        page.setScaleX(scale);
        page.setScaleY(scale);

        // Pivot to simulate page turn
        if (position < 0f) {
            page.setPivotX(page.getWidth());
            page.setRotationY(35f * position);
        } else {
            page.setPivotX(0f);
            page.setRotationY(35f * position);
        }
    }
}
