package com.example.zenpath;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class RippleOverlayView extends View {

    private static class Ripple {
        float x, y;
        float radius;
        float maxRadius;
        float speed;
        int alpha;
        float sparklePhase;
    }

    private final ArrayList<Ripple> ripples = new ArrayList<>();
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rng = new Random();

    public RippleOverlayView(Context c) { super(c); init(); }
    public RippleOverlayView(Context c, AttributeSet a) { super(c, a); init(); }
    public RippleOverlayView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setMaskFilter(new BlurMaskFilter(dp(10), BlurMaskFilter.Blur.NORMAL));

        sparklePaint.setStyle(Paint.Style.FILL);

        // ensure blur shows on more devices
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public void spawnRipple(float rawX, float rawY) {
        // Convert raw screen coords to this view coords
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        float x = rawX - loc[0];
        float y = rawY - loc[1];

        Ripple r = new Ripple();
        r.x = x;
        r.y = y;
        r.radius = dp(6);
        r.maxRadius = Math.max(getWidth(), getHeight()) * 0.55f;
        r.speed = dp(7.0f);
        r.alpha = 240;
        r.sparklePhase = rng.nextFloat() * (float)Math.PI * 2f;

        ripples.add(r);
        if (ripples.size() > 10) ripples.remove(0);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float t = SystemClock.uptimeMillis() / 1000f;

        Iterator<Ripple> it = ripples.iterator();
        while (it.hasNext()) {
            Ripple r = it.next();

            r.radius += r.speed;
            r.alpha -= 5;

            if (r.alpha <= 0 || r.radius > r.maxRadius) {
                it.remove();
                continue;
            }

            int ringColor = Color.argb(Math.max(0, r.alpha), 120, 90, 220);     // purple
            int glowColor = Color.argb(Math.max(0, r.alpha / 2), 80, 140, 255); // blue glow

            // Outer glow
            glowPaint.setColor(glowColor);
            glowPaint.setStrokeWidth(dp(5));
            canvas.drawCircle(r.x, r.y, r.radius, glowPaint);

            // Main ring
            ringPaint.setColor(ringColor);
            ringPaint.setStrokeWidth(dp(2.2f));
            canvas.drawCircle(r.x, r.y, r.radius, ringPaint);

            // Inner ring (double-ring look)
            ringPaint.setAlpha(Math.max(0, r.alpha - 60));
            ringPaint.setStrokeWidth(dp(1.6f));
            canvas.drawCircle(r.x, r.y, r.radius * 0.72f, ringPaint);

            // Sparkles around ring
            float sparkleCount = 10;
            for (int i = 0; i < sparkleCount; i++) {
                float ang = (float)(i * (2 * Math.PI / sparkleCount)) + r.sparklePhase + t * 0.7f;
                float sx = (float) (r.x + Math.cos(ang) * r.radius);
                float sy = (float) (r.y + Math.sin(ang) * r.radius);

                int a = Math.max(0, r.alpha - 120);
                sparklePaint.setColor(Color.argb(a, 255, 255, 255));
                canvas.drawCircle(sx, sy, dp(1.2f), sparklePaint);
            }
        }

        if (!ripples.isEmpty()) {
            postInvalidateOnAnimation();
        }
    }
}