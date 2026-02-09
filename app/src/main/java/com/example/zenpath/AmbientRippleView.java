package com.example.zenpath;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class AmbientRippleView extends View {

    // ===== Background =====
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private LinearGradient bgGradient;
    private float w, h;

    private final Random rng = new Random();

    // ===== Ripples =====
    private static class Ripple {
        float x, y;
        float r;
        float speed;
        int alpha;
        float wobblePhase;
        float driftUp;
    }

    private final ArrayList<Ripple> ripples = new ArrayList<>();

    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rippleGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public AmbientRippleView(Context c) { super(c); init(); }
    public AmbientRippleView(Context c, AttributeSet a) { super(c, a); init(); }
    public AmbientRippleView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setWillNotDraw(false);

        // Main ring
        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStrokeCap(Paint.Cap.ROUND);
        ripplePaint.setStrokeWidth(dp(3.6f));
        ripplePaint.setColor(Color.argb(130, 196, 170, 255));
        ripplePaint.setMaskFilter(new BlurMaskFilter(dp(1.6f), BlurMaskFilter.Blur.NORMAL));

        // Glow
        rippleGlowPaint.setStyle(Paint.Style.STROKE);
        rippleGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        rippleGlowPaint.setStrokeWidth(dp(9.0f));
        rippleGlowPaint.setColor(Color.argb(30, 210, 190, 255));
        rippleGlowPaint.setMaskFilter(new BlurMaskFilter(dp(10f), BlurMaskFilter.Blur.NORMAL));

        // Needed for BlurMaskFilter
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        // Don’t block touches for buttons
        setClickable(false);
        setFocusable(false);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private void ensureGradient() {
        float hh = (h > 0) ? h : getHeight();
        if (hh <= 0) hh = 1;

        bgGradient = new LinearGradient(
                0, 0, 0, hh,
                new int[]{
                        Color.parseColor("#D9F7FF"),
                        Color.parseColor("#BEEFFF"),
                        Color.parseColor("#86E0EF"),
                        Color.parseColor("#EFD8B6")
                },
                new float[]{0.0f, 0.45f, 0.78f, 1.0f},
                Shader.TileMode.CLAMP
        );
        bgPaint.setShader(bgGradient);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);
        w = width;
        h = height;
        ensureGradient();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            addRipple(event.getX(), event.getY());
            return false; // don’t consume
        }
        return false;
    }

    private void addRipple(float x, float y) {
        for (int i = 0; i < 3; i++) {
            Ripple r = new Ripple();
            r.x = x;
            r.y = y;

            r.r = dp(18 + i * 10);
            r.speed = dp(0.70f + i * 0.12f);
            r.alpha = 165 - (i * 28);
            r.wobblePhase = rng.nextFloat() * 999f;
            r.driftUp = dp(0.06f + i * 0.02f);

            ripples.add(r);
        }

        while (ripples.size() > 20) ripples.remove(0);

        // animate only when needed
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bgGradient == null) ensureGradient();

        float ww = (w > 0) ? w : getWidth();
        float hh = (h > 0) ? h : getHeight();

        canvas.drawRect(0, 0, ww, hh, bgPaint);

        if (ripples.isEmpty()) return;

        float t = SystemClock.uptimeMillis() / 1000f;

        Iterator<Ripple> it = ripples.iterator();
        while (it.hasNext()) {
            Ripple r = it.next();

            r.r += r.speed;
            r.alpha -= 2;
            r.y -= r.driftUp;

            if (r.alpha <= 0 || r.r > dp(230)) {
                it.remove();
                continue;
            }

            float wob = (float) Math.sin(t * 1.0f + r.wobblePhase) * dp(0.9f);
            float rr = r.r + wob;

            rippleGlowPaint.setAlpha(Math.max(0, r.alpha / 2));
            ripplePaint.setAlpha(Math.max(0, r.alpha));

            canvas.drawCircle(r.x, r.y, rr, rippleGlowPaint);
            canvas.drawCircle(r.x, r.y, rr, ripplePaint);
        }

        if (!ripples.isEmpty()) postInvalidateOnAnimation();
    }
}