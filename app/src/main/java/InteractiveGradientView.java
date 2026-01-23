package com.example.zenpath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

public class InteractiveGradientView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float x = -1f;
    private float y = -1f;

    private long startTime = 0L;
    private boolean animating = false;

    // #88BFE9FF
    private static final int AURA_COLOR = 0x88BFE9FF;

    public InteractiveGradientView(Context context) {
        super(context);
        init();
    }

    public InteractiveGradientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public InteractiveGradientView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
        setClickable(false); // important: don't block buttons
    }

    // ✅ Call this from MainActivity when user taps anywhere
    public void trigger(float touchX, float touchY) {
        x = touchX;
        y = touchY;
        startTime = SystemClock.uptimeMillis();
        animating = true;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!animating || x < 0 || y < 0) return;

        long elapsed = SystemClock.uptimeMillis() - startTime;
        float t = Math.min(1f, elapsed / 600f);

        float maxRadius = Math.max(getWidth(), getHeight()) * 0.9f;
        float radius = (0.10f + 0.90f * t) * maxRadius;

        int alpha = (int) (180 * (1f - t));
        alpha = Math.max(0, Math.min(255, alpha));

        int colorWithAlpha = (AURA_COLOR & 0x00FFFFFF) | (alpha << 24);

        RadialGradient gradient = new RadialGradient(
                x, y, radius,
                new int[]{colorWithAlpha, 0x00000000},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        );

        paint.setShader(gradient);
        canvas.drawCircle(x, y, radius, paint);
        paint.setShader(null);

        if (t < 1f) {
            postInvalidateOnAnimation();
        } else {
            animating = false;
        }
    }
}
