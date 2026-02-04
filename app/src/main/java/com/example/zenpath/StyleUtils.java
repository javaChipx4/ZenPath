package com.example.zenpath;

import android.graphics.drawable.GradientDrawable;

public class StyleUtils {

    public static GradientDrawable makeSwatchDrawable(int fillColor, int strokeColor, int strokeWidthPx, int radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fillColor);
        d.setCornerRadius(radiusPx);
        d.setStroke(strokeWidthPx, strokeColor);
        return d;
    }
}