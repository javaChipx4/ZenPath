package com.example.zenpath;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.text.InputType;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LanternReleaseView extends View {

    // =========================
    // UI listener (matches your Activity)
    // =========================
    public interface UiListener {
        void onUpdate(int total, String badge, String message);
    }

    private UiListener uiListener;

    public void setUiListener(UiListener l) {
        this.uiListener = l;
        pushUi();
    }

    // =========================
    // Lantern model
    // =========================
    private static class Lantern {
        float x, y;
        float w, h;
        float vy;
        boolean released;
        boolean glowing;
        boolean messageVisible;
        String message = "";

        long glowEndMs = 0;
        final RectF bounds = new RectF();
    }

    private final List<Lantern> lanterns = new ArrayList<>();
    private final Random rng = new Random();

    private boolean running = false;
    private boolean finished = false;

    private int totalScore = 0;
    private String badge = "—";
    private String hudMessage = "Tap to place a lantern ✨";

    // =========================
    // Background (night + stars)
    // =========================
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private LinearGradient bgGrad;

    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static class Star {
        float x, y, r;
        float twinklePhase;
    }
    private final ArrayList<Star> stars = new ArrayList<>();
    private int lastW = 0, lastH = 0;

    // =========================
    // Paints (lantern visuals)
    // =========================
    private final Paint lanternBodyFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lanternFrame = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lanternGlass = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lanternBars = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lanternGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lanternGlowStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final TextPaint msgPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    // timing
    private long lastMs = 0;

    public LanternReleaseView(Context c) { super(c); init(); }
    public LanternReleaseView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public LanternReleaseView(Context c, @Nullable AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        setClickable(true);

        // Lantern body
        lanternBodyFill.setStyle(Paint.Style.FILL);
        lanternBodyFill.setColor(Color.argb(255, 12, 12, 14));

        lanternFrame.setStyle(Paint.Style.STROKE);
        lanternFrame.setStrokeWidth(dp(2.2f));
        lanternFrame.setColor(Color.argb(220, 240, 240, 240));

        lanternGlass.setStyle(Paint.Style.FILL);
        lanternGlass.setColor(Color.argb(45, 255, 255, 255));

        lanternBars.setStyle(Paint.Style.STROKE);
        lanternBars.setStrokeWidth(dp(1.2f));
        lanternBars.setColor(Color.argb(170, 235, 235, 235));

        lanternGlow.setStyle(Paint.Style.FILL);

        lanternGlowStroke.setStyle(Paint.Style.STROKE);
        lanternGlowStroke.setStrokeWidth(dp(1.6f));
        lanternGlowStroke.setColor(Color.argb(170, 255, 220, 140));

        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(Color.argb(60, 0, 0, 0));

        msgPaint.setColor(Color.WHITE);
        msgPaint.setTextSize(sp(12));
        msgPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        // stars
        starPaint.setColor(Color.WHITE);
        starPaint.setStyle(Paint.Style.FILL);
        starPaint.setAlpha(180);

        starGlowPaint.setStyle(Paint.Style.FILL);
        starGlowPaint.setColor(Color.WHITE);
        starGlowPaint.setAlpha(60);
    }

    // =========================
    // Public API used by your Activity
    // =========================
    public boolean isRunning() { return running; }
    public boolean isFinished() { return finished; }

    public void play() {
        if (lanterns.isEmpty()) {
            hudMessage = "Place a lantern first ✨";
            pushUi();
            return;
        }

        running = true;
        finished = false;
        hudMessage = "Tap a flying lantern to glow ✨";
        pushUi();

        for (Lantern l : lanterns) {
            l.released = true;
            l.glowing = false;
            l.messageVisible = false;
            // slower up
            l.vy = -dp(55) - rng.nextFloat() * dp(18);
        }

        invalidate();
    }

    public void playAgain() {
        running = false;
        finished = false;
        hudMessage = "Tap to place a lantern ✨";
        pushUi();

        for (Lantern l : lanterns) {
            l.released = false;
            l.glowing = false;
            l.messageVisible = false;
            l.glowEndMs = 0;
        }

        invalidate();
    }

    public void resetToday() {
        lanterns.clear();
        totalScore = 0;
        badge = "—";
        running = false;
        finished = false;
        hudMessage = "Tap to place a lantern ✨";
        pushUi();
        invalidate();
    }

    // ✅ Compatibility: some versions of your Activity called this
    public void startRelease() {
        play();
    }

    // =========================
    // Touch logic (message + tap-to-glow)
    // =========================
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_DOWN) return true;

        float x = e.getX();
        float y = e.getY();

        Lantern hit = findLanternAt(x, y);

        if (!running) {
            // Before Play: tap lantern = edit message; tap empty = place
            if (hit != null) {
                openMessageDialog(hit);
            } else {
                placeLantern(x, y);
                hudMessage = "Tap a lantern to add a message ✨";
                pushUi();
                invalidate();
            }
            return true;
        }

        // During flight: tap lantern = glow + reveal message
        if (hit != null) {
            hit.glowing = true;
            hit.messageVisible = true;
            hit.glowEndMs = SystemClock.uptimeMillis() + 2500;
            totalScore += 10;
            updateBadge();
            hudMessage = "Glow ✨";
            pushUi();
            invalidate();
        }

        return true;
    }

    private void openMessageDialog(Lantern lantern) {
        EditText input = new EditText(getContext());
        input.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );
        input.setMinLines(2);
        input.setMaxLines(8);
        input.setText(lantern.message == null ? "" : lantern.message);
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(getContext())
                .setTitle("Write your message")
                .setMessage("This stays hidden until the lantern glows.")
                .setView(input)
                .setPositiveButton("Save", (d, which) -> {
                    lantern.message = input.getText().toString();
                    hudMessage = "Saved ✨ Press Play when ready";
                    pushUi();
                    invalidate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void placeLantern(float x, float y) {
        Lantern l = new Lantern();

        // Bigger so message fits (as you asked)
        l.w = dp(72);
        l.h = dp(120);

        l.x = clamp(x, l.w * 0.6f, getWidth() - l.w * 0.6f);
        l.y = clamp(y, l.h * 0.6f, getHeight() - l.h * 0.6f);

        l.released = false;
        l.glowing = false;
        l.messageVisible = false;

        lanterns.add(l);
    }

    private Lantern findLanternAt(float x, float y) {
        for (int i = lanterns.size() - 1; i >= 0; i--) {
            Lantern l = lanterns.get(i);
            l.bounds.set(l.x - l.w / 2f, l.y - l.h / 2f, l.x + l.w / 2f, l.y + l.h / 2f);
            if (l.bounds.contains(x, y)) return l;
        }
        return null;
    }

    // =========================
    // Draw + Update
    // =========================
    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);

        long now = SystemClock.uptimeMillis();
        if (lastMs == 0) lastMs = now;
        float dt = (now - lastMs) / 1000f;
        lastMs = now;
        if (dt > 0.05f) dt = 0.05f;

        // ✅ Background first
        drawNightBackground(c, now);

        // ✅ Update flight
        if (running) {
            boolean anyStillOnScreen = false;

            for (Lantern l : lanterns) {
                if (!l.released) continue;

                l.y += l.vy * dt;

                // stop glow after time
                if (l.glowing && now > l.glowEndMs) {
                    l.glowing = false;
                    l.messageVisible = false; // message hidden again
                }

                if (l.y + l.h > -dp(40)) anyStillOnScreen = true;
            }

            if (!anyStillOnScreen) {
                running = false;
                finished = true;
                hudMessage = "Done ✨ Press Play Again";
                pushUi();
            } else {
                postInvalidateOnAnimation();
            }
        }

        // ✅ Draw lanterns
        for (Lantern l : lanterns) {
            drawSkyLantern(c, l);
        }
    }

    private void drawNightBackground(Canvas c, long now) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // rebuild if size changed
        if (w != lastW || h != lastH || bgGrad == null || stars.isEmpty()) {
            lastW = w;
            lastH = h;

            bgGrad = new LinearGradient(
                    0, 0, 0, h,
                    new int[]{
                            Color.rgb(7, 8, 22),
                            Color.rgb(10, 14, 40),
                            Color.rgb(12, 18, 55)
                    },
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP
            );
            bgPaint.setShader(bgGrad);

            stars.clear();
            int count = 140;
            for (int i = 0; i < count; i++) {
                Star s = new Star();
                s.x = rng.nextFloat() * w;
                s.y = rng.nextFloat() * h;
                s.r = dp(0.8f) + rng.nextFloat() * dp(1.8f);
                s.twinklePhase = rng.nextFloat() * 6.28f;
                stars.add(s);
            }
        }

        // gradient sky
        c.drawRect(0, 0, w, h, bgPaint);

        // stars with tiny twinkle
        float t = (now % 5000L) / 5000f; // 0..1
        float tw = (float) Math.sin(t * 6.283f);

        for (Star s : stars) {
            float alpha = 120 + 60f * (float) Math.sin(s.twinklePhase + tw * 1.5f);
            alpha = clamp(alpha, 60, 200);

            starPaint.setAlpha((int) alpha);
            c.drawCircle(s.x, s.y, s.r, starPaint);

            // subtle glow for bigger stars
            if (s.r > dp(1.8f)) {
                starGlowPaint.setAlpha((int) (alpha * 0.35f));
                c.drawCircle(s.x, s.y, s.r * 2.2f, starGlowPaint);
            }
        }
    }

    /**
     * ✅ Square lantern like your reference (frame + roof + glass grid + warm light).
     * No gameplay logic changed.
     */
    // ✅ Real sky lantern (paper lantern + flame glow)
    private void drawSkyLantern(Canvas c, Lantern l) {
        float x = l.x;
        float y = l.y;
        float w = l.w;
        float h = l.h;

        // Main paper body (rounded top)
        RectF paper = new RectF(
                x - w * 0.40f,
                y - h * 0.48f,
                x + w * 0.40f,
                y + h * 0.35f
        );
        float rTop = w * 0.50f;

        // Bottom rim
        RectF rim = new RectF(
                x - w * 0.30f,
                y + h * 0.25f,
                x + w * 0.30f,
                y + h * 0.38f
        );
        float rimR = w * 0.18f;

        // Shadow
        c.drawRoundRect(
                new RectF(paper.left + dp(3), paper.top + dp(7), paper.right + dp(3), paper.bottom + dp(10)),
                rTop, rTop, shadowPaint
        );

        // Glow halo (only when glowing)
        if (l.glowing) {
            float glowR = Math.max(w, h) * 0.90f;
            lanternGlow.setShader(new RadialGradient(
                    x, y + h * 0.10f, glowR,
                    new int[]{
                            Color.argb(220, 255, 210, 130),
                            Color.argb(90, 255, 170, 90),
                            Color.argb(0, 255, 170, 90)
                    },
                    new float[]{0f, 0.6f, 1f},
                    Shader.TileMode.CLAMP
            ));
            c.drawCircle(x, y, glowR, lanternGlow);
            lanternGlow.setShader(null);
        }

        // Paper fill (warm paper)
        Paint paperFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        paperFill.setStyle(Paint.Style.FILL);
        paperFill.setColor(Color.argb(235, 246, 234, 205));

        // Paper gradient shading
        Paint paperShade = new Paint(Paint.ANTI_ALIAS_FLAG);
        paperShade.setShader(new LinearGradient(
                paper.left, paper.top, paper.right, paper.top,
                new int[]{
                        Color.argb(240, 252, 244, 220),
                        Color.argb(240, 235, 220, 190),
                        Color.argb(240, 252, 244, 220)
                },
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        ));

        c.drawRoundRect(paper, rTop, rTop, paperFill);
        c.drawRoundRect(paper, rTop, rTop, paperShade);
        paperShade.setShader(null);

        // Rim (a bit darker)
        Paint rimFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        rimFill.setColor(Color.argb(255, 210, 195, 165));
        c.drawRoundRect(rim, rimR, rimR, rimFill);

        // Seams (paper lines)
        Paint seam = new Paint(Paint.ANTI_ALIAS_FLAG);
        seam.setStyle(Paint.Style.STROKE);
        seam.setStrokeWidth(dp(1.4f));
        seam.setColor(Color.argb(110, 90, 70, 45));

        c.drawLine(x - w * 0.16f, paper.top + h * 0.10f, x - w * 0.11f, paper.bottom - h * 0.10f, seam);
        c.drawLine(x + w * 0.16f, paper.top + h * 0.10f, x + w * 0.11f, paper.bottom - h * 0.10f, seam);

        // Inner flame glow (only when glowing)
        if (l.glowing) {
            Paint flame = new Paint(Paint.ANTI_ALIAS_FLAG);
            flame.setShader(new RadialGradient(
                    x, y + h * 0.22f, w * 0.55f,
                    new int[]{
                            Color.argb(230, 255, 245, 190),
                            Color.argb(120, 255, 205, 120),
                            Color.argb(0, 255, 205, 120)
                    },
                    new float[]{0f, 0.65f, 1f},
                    Shader.TileMode.CLAMP
            ));
            c.drawCircle(x, y + h * 0.20f, w * 0.52f, flame);
            flame.setShader(null);

            Paint flameDot = new Paint(Paint.ANTI_ALIAS_FLAG);
            flameDot.setColor(Color.argb(230, 255, 175, 90));
            c.drawCircle(x, y + h * 0.29f, dp(4.5f), flameDot);
        }

        // Message area (hidden until glow)
        RectF msgArea = new RectF(
                paper.left + w * 0.12f,
                paper.top + h * 0.20f,
                paper.right - w * 0.12f,
                paper.bottom - h * 0.22f
        );

        if (l.messageVisible && l.message != null && !l.message.trim().isEmpty()) {
            drawMessageInsideGlass(c, l.message.trim(), msgArea);
        }
    }


    // Draw wrapped message inside lantern glass
    private void drawMessageInsideGlass(Canvas c, String msg, RectF glass) {
        Paint bubble = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubble.setColor(Color.argb(140, 0, 0, 0));
        c.drawRoundRect(new RectF(glass.left, glass.top, glass.right, glass.bottom), dp(10), dp(10), bubble);

        float maxW = glass.width() - dp(10);
        float x = glass.left + dp(5);
        float y = glass.top + dp(16);

        String[] lines = wrapText(msg, maxW, 5); // allow 5 lines
        for (String line : lines) {
            if (line == null) break;
            c.drawText(line, x + dp(1.2f), y + dp(1.2f), shadowText());
            c.drawText(line, x, y, msgPaint);
            y += dp(14);
        }
    }

    private TextPaint shadowText() {
        TextPaint tp = new TextPaint(msgPaint);
        tp.setColor(Color.argb(140, 0, 0, 0));
        return tp;
    }

    private String[] wrapText(String text, float maxWidth, int maxLines) {
        String[] out = new String[maxLines];
        String[] words = text.replace("\n", " ").split("\\s+");

        StringBuilder line = new StringBuilder();
        int lineIndex = 0;

        for (String w : words) {
            String test = (line.length() == 0) ? w : (line + " " + w);
            if (msgPaint.measureText(test) <= maxWidth) {
                line.setLength(0);
                line.append(test);
            } else {
                out[lineIndex++] = line.toString();
                if (lineIndex >= maxLines) break;
                line.setLength(0);
                line.append(w);
            }
        }

        if (lineIndex < maxLines && line.length() > 0) {
            out[lineIndex] = line.toString();
        }

        // ellipsis if too long
        if (lineIndex >= maxLines) {
            String last = out[maxLines - 1];
            if (last == null) last = "";
            while (last.length() > 0 && msgPaint.measureText(last + "…") > maxWidth) {
                last = last.substring(0, last.length() - 1);
            }
            out[maxLines - 1] = last + "…";
        }

        return out;
    }

    // =========================
    // UI helpers
    // =========================
    private void updateBadge() {
        if (totalScore >= 300) badge = "Sky Keeper";
        else if (totalScore >= 150) badge = "Soft Glow";
        else if (totalScore >= 60) badge = "First Glow";
        else badge = "—";
    }

    private void pushUi() {
        if (uiListener != null) uiListener.onUpdate(totalScore, badge, hudMessage);
    }

    // =========================
    // Utils
    // =========================
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }

    private float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }
}