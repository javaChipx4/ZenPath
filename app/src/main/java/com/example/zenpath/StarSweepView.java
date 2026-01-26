package com.example.zenpath;

import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class StarSweepView extends View {

    // ---- HUD CALLBACKS ----
    public interface HudListener {
        void onBreathText(String text);
        void onStarsLeft(int left, int total);

        // NEW: Flash started (right when you finish)
        void onFinishFlashStarted();

        // NEW: Called AFTER 2 seconds flash (now show Play Again)
        void onFinishedReady();
    }

    private HudListener hudListener;

    public void setHudListener(HudListener l) {
        this.hudListener = l;
        pushHud();
    }

    // ---- PAUSE ----
    private boolean paused = false;

    public void setPaused(boolean paused) {
        this.paused = paused;
        if (!paused) {
            lastFrameMs = SystemClock.uptimeMillis();
            postInvalidateOnAnimation();
        }
    }

    // ---- PAINTS ----
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cometPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Random random = new Random();
    private final ArrayList<Star> stars = new ArrayList<>();
    private final ArrayList<Particle> particles = new ArrayList<>();

    // Constellation nodes
    private final ArrayList<ConstPoint> constPoints = new ArrayList<>();

    // ---- TOUCH ----
    private float fingerX = -9999, fingerY = -9999;
    private long lastFrameMs = 0;

    // ---- GAME STATE ----
    private int totalStars = 18;

    // NEW: finishing flash state
    private boolean flashing = false;
    private boolean finished = false;
    private long flashStartMs = 0;
    private static final long FLASH_DURATION_MS = 2000;

    // ---- BREATHING GUIDE ----
    private long startMs;

    // ---- COMET BONUS ----
    private Comet comet = null;
    private long nextCometMs = 0;

    // ---- VIBRATE ----
    private Vibrator vibrator;

    public StarSweepView(Context context) {
        super(context);
        init();
    }

    public StarSweepView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StarSweepView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        startMs = SystemClock.uptimeMillis();
        lastFrameMs = startMs;

        vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        starPaint.setStyle(Paint.Style.FILL);
        starPaint.setColor(Color.WHITE);

        glowPaint.setStyle(Paint.Style.FILL);

        cometPaint.setStyle(Paint.Style.FILL);
        cometPaint.setColor(Color.parseColor("#EDE7FF"));

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(Color.parseColor("#C6B7E2"));

        createStars();
        scheduleNextComet();
        pushHud();
    }

    public void resetGame() {
        finished = false;
        flashing = false;
        flashStartMs = 0;

        fingerX = fingerY = -9999;
        particles.clear();
        constPoints.clear();
        comet = null;

        createStars();
        scheduleNextComet();

        startMs = SystemClock.uptimeMillis();
        lastFrameMs = startMs;

        pushHud();
        invalidate();
    }

    private void createStars() {
        stars.clear();

        totalStars = 18;
        for (int i = 0; i < totalStars; i++) {
            float x = random.nextFloat() * 0.9f + 0.05f;
            float y = random.nextFloat() * 0.75f + 0.18f;
            float size = 16 + random.nextInt(16);
            float twinkleSpeed = 0.8f + random.nextFloat() * 1.8f;
            float phase = random.nextFloat() * 6.28f;
            stars.add(new Star(x, y, size, twinkleSpeed, phase));
        }
    }

    private void pushHud() {
        if (hudListener != null) {
            hudListener.onStarsLeft(stars.size(), totalStars);
            hudListener.onBreathText(getBreathText(SystemClock.uptimeMillis()));
        }
    }

    private void scheduleNextComet() {
        long now = SystemClock.uptimeMillis();
        nextCometMs = now + 7000 + random.nextInt(7000);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        Shader sky = new LinearGradient(
                0, 0, 0, h,
                Color.parseColor("#0E0B2B"),
                Color.parseColor("#2A1E5A"),
                Shader.TileMode.CLAMP
        );
        bgPaint.setShader(sky);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);

        long now = SystemClock.uptimeMillis();
        if (paused) {
            drawScene(canvas, now, 0);
            return;
        }

        long dt = Math.max(0, now - lastFrameMs);
        lastFrameMs = now;

        if (hudListener != null && (now % 250) < 18) {
            hudListener.onBreathText(getBreathText(now));
        }

        // If flashing, check if done
        if (flashing) {
            if (now - flashStartMs >= FLASH_DURATION_MS) {
                flashing = false;
                finished = true;
                if (hudListener != null) hudListener.onFinishedReady();
            }
        } else if (!finished) {
            // Spawn comet only while actively playing
            if (comet == null && now >= nextCometMs) {
                spawnComet();
                scheduleNextComet();
            }
        }

        drawScene(canvas, now, dt);
        postInvalidateOnAnimation();
    }

    private void drawScene(Canvas canvas, long now, long dt) {

        // Finger glow only while active
        if (!finished && !flashing && fingerX > 0 && fingerY > 0) {
            RadialGradient glow = new RadialGradient(
                    fingerX, fingerY, 160,
                    new int[]{Color.parseColor("#55C6B7E2"), Color.TRANSPARENT},
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
            );
            glowPaint.setShader(glow);
            canvas.drawCircle(fingerX, fingerY, 160, glowPaint);
            glowPaint.setShader(null);
        }

        // Constellation (FLASH if finishing)
        drawConstellation(canvas, now);

        // Stars (twinkling)
        for (Star s : stars) {
            float px = s.nx * getWidth();
            float py = s.ny * getHeight();

            float tw = (float) (Math.sin((now / 1000f) * s.twinkleSpeed + s.phase) * 0.35f + 0.65f);
            int alpha = (int) (160 + 95 * tw);

            starPaint.setAlpha(alpha);

            Path starPath = createStarPath(px, py, s.size, s.size * 0.5f, 5);
            canvas.drawPath(starPath, starPaint);

            starPaint.setAlpha(Math.min(255, alpha + 30));
            canvas.drawCircle(px, py, 2.6f, starPaint);
        }

        // Comet only while active
        if (!finished && !flashing && comet != null) {
            if (dt > 0) comet.update(dt);

            float cx = comet.x;
            float cy = comet.y;

            cometPaint.setAlpha(120);
            canvas.drawCircle(cx - comet.vx * 0.02f, cy - comet.vy * 0.02f, 40, cometPaint);
            cometPaint.setAlpha(220);
            canvas.drawCircle(cx, cy, 18, cometPaint);

            if (cx < -200 || cx > getWidth() + 200 || cy < -200 || cy > getHeight() + 200) {
                comet = null;
            }
        }

        // Particles
        if (dt > 0) {
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.update(dt);
                if (p.life <= 0) it.remove();
            }
        }

        for (Particle p : particles) {
            starPaint.setAlpha(p.alpha());
            canvas.drawCircle(p.x, p.y, p.r, starPaint);
        }

        // Start flashing when complete
        if (!finished && !flashing && stars.isEmpty()) {
            flashing = true;
            flashStartMs = now;
            fingerX = fingerY = -9999; // lock glow
            comet = null; // stop bonus

            if (hudListener != null) hudListener.onFinishFlashStarted();
        }

        // Finish message (after flash)
        if (finished) {
            Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setColor(Color.WHITE);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(54);
            canvas.drawText("Constellation complete 🌙", getWidth() / 2f, 240, text);

            text.setTextSize(36);
            canvas.drawText("Great job. Tap Play Again.", getWidth() / 2f, 300, text);
        } else if (flashing) {
            Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setColor(Color.WHITE);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(44);
            canvas.drawText("✨ Beautiful ✨", getWidth() / 2f, 240, text);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Lock input during flash + after finish
        if (finished || flashing) return true;

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            fingerX = event.getX();
            fingerY = event.getY();

            boolean clearedAny = false;

            Iterator<Star> it = stars.iterator();
            while (it.hasNext()) {
                Star s = it.next();
                float px = s.nx * getWidth();
                float py = s.ny * getHeight();
                float dx = fingerX - px;
                float dy = fingerY - py;

                if ((dx * dx + dy * dy) <= (90 * 90)) {
                    it.remove();
                    clearedAny = true;

                    addConstellationPoint(px, py, SystemClock.uptimeMillis());
                    spawnSparkles(px, py);
                }
            }

            // Comet bonus touch
            if (comet != null) {
                float dx = fingerX - comet.x;
                float dy = fingerY - comet.y;
                if ((dx * dx + dy * dy) <= (70 * 70)) {
                    comet = null;
                    spawnSparkles(fingerX, fingerY);
                    clearRandomStars(2);
                    clearedAny = true;
                }
            }

            if (clearedAny) {
                vibrateTick();
                if (hudListener != null) hudListener.onStarsLeft(stars.size(), totalStars);
            }

        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            fingerX = fingerY = -9999;
        }

        return true;
    }

    // ----- CONSTELLATION -----
    private void addConstellationPoint(float x, float y, long createdMs) {
        constPoints.add(new ConstPoint(x, y, createdMs));
        if (constPoints.size() > 18) constPoints.remove(0);
    }

    private void drawConstellation(Canvas canvas, long now) {
        if (constPoints.size() < 2) return;

        // Flash factor (only during the 2 seconds)
        float flashFactor = 1f;
        if (flashing) {
            float t = (now - flashStartMs) / 1000f; // seconds
            // oscillates 0..1..0..1 quickly
            flashFactor = (float) (0.35 + 0.65 * (0.5 + 0.5 * Math.sin(t * Math.PI * 6)));
        }

        for (int i = 1; i < constPoints.size(); i++) {
            ConstPoint a = constPoints.get(i - 1);
            ConstPoint b = constPoints.get(i);

            int alpha = (int) (180 * flashFactor);
            linePaint.setAlpha(alpha);
            canvas.drawLine(a.x, a.y, b.x, b.y, linePaint);

            starPaint.setAlpha(Math.min(255, (int) (220 * flashFactor)));
            canvas.drawCircle(b.x, b.y, 6f, starPaint);
        }
    }

    // ----- GAME HELPERS -----
    private void clearRandomStars(int n) {
        for (int i = 0; i < n && !stars.isEmpty(); i++) {
            int idx = random.nextInt(stars.size());
            Star s = stars.remove(idx);
            float px = s.nx * getWidth();
            float py = s.ny * getHeight();
            addConstellationPoint(px, py, SystemClock.uptimeMillis());
            spawnSparkles(px, py);
        }
        if (hudListener != null) hudListener.onStarsLeft(stars.size(), totalStars);
    }

    private void spawnComet() {
        float startX = -120;
        float startY = random.nextFloat() * getHeight() * 0.6f + getHeight() * 0.1f;

        float endX = getWidth() + 120;
        float endY = random.nextFloat() * getHeight() * 0.7f + getHeight() * 0.15f;

        float vx = (endX - startX) / 2400f;
        float vy = (endY - startY) / 2400f;

        comet = new Comet(startX, startY, vx, vy);
    }

    private void spawnSparkles(float x, float y) {
        for (int i = 0; i < 12; i++) {
            float ang = (float) (random.nextFloat() * Math.PI * 2);
            float sp = 0.10f + random.nextFloat() * 0.22f;
            float vx = (float) Math.cos(ang) * sp;
            float vy = (float) Math.sin(ang) * sp;
            float r = 2.5f + random.nextFloat() * 3.5f;
            long life = 520 + random.nextInt(420);
            particles.add(new Particle(x, y, vx, vy, r, life));
        }
    }

    private void vibrateTick() {
        if (vibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(18);
            }
        } catch (Exception ignored) {}
    }

    private String getBreathText(long now) {
        long t = (now - startMs) % 10000;
        if (t < 4000) return "Inhale…";
        if (t < 6000) return "Hold…";
        return "Exhale…";
    }

    private Path createStarPath(float cx, float cy, float outerR, float innerR, int points) {
        Path path = new Path();
        double angle = Math.PI / points;

        for (int i = 0; i < points * 2; i++) {
            double r = (i % 2 == 0) ? outerR : innerR;
            double a = i * angle - Math.PI / 2;

            float x = (float) (cx + Math.cos(a) * r);
            float y = (float) (cy + Math.sin(a) * r);

            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
        return path;
    }

    // ----- DATA -----
    static class Star {
        float nx, ny;
        float size;
        float twinkleSpeed;
        float phase;

        Star(float nx, float ny, float size, float twinkleSpeed, float phase) {
            this.nx = nx;
            this.ny = ny;
            this.size = size;
            this.twinkleSpeed = twinkleSpeed;
            this.phase = phase;
        }
    }

    static class Particle {
        float x, y;
        float vx, vy;
        float r;
        long life;
        long maxLife;

        Particle(float x, float y, float vx, float vy, float r, long life) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.r = r;
            this.life = life;
            this.maxLife = life;
        }

        void update(long dt) {
            x += vx * dt * 1000f;
            y += vy * dt * 1000f;
            life -= dt;
        }

        int alpha() {
            float p = Math.max(0f, Math.min(1f, life / (float) maxLife));
            return (int) (255 * p);
        }
    }

    static class Comet {
        float x, y;
        float vx, vy;

        Comet(float x, float y, float vx, float vy) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
        }

        void update(long dt) {
            x += vx * dt;
            y += vy * dt;
        }
    }

    static class ConstPoint {
        float x, y;
        long createdMs;

        ConstPoint(float x, float y, long createdMs) {
            this.x = x;
            this.y = y;
            this.createdMs = createdMs;
        }
    }
}
