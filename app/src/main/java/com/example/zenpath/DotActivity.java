package com.example.zenpath;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public class Dot {
    private float x, y;
    private int radius;
    private int color;
    private int colorType;
    private boolean isConnected;
    private Paint paint;

    public Dot(float x, float y, int radius, int color, int colorType) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.colorType = colorType;
        this.isConnected = false;

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
    }

    public void draw(Canvas canvas) {
        canvas.drawCircle(x, y, radius, paint);

        // Add subtle border
        Paint borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(0x40000000); // Semi-transparent black
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2);
        canvas.drawCircle(x, y, radius, borderPaint);
    }

    public boolean containsPoint(float x, float y) {
        float dx = this.x - x;
        float dy = this.y - y;
        return dx * dx + dy * dy <= radius * radius;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public int getRadius() { return radius; }
    public int getColor() { return color; }
    public int getColorType() { return colorType; }
    public boolean isConnected() { return isConnected; }

    public void setConnected(boolean connected) { isConnected = connected; }
}

