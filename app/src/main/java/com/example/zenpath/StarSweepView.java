package com.example.zenpath;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class StarSweepView extends View {

    // ================= HUD =================
    public interface HudListener {
        void onBreathText(String text);
        void onProgress(int selected, int goal);
        void onFinishFlashStarted();
        void onFinishedReady();
    }

    private HudListener hudListener;
    public void setHudListener(HudListener l) {
        hudListener = l;
        pushHud();
    }

    // ================= PAUSE =================
    private boolean paused = false;
    public void setPaused(boolean p) {
        paused = p;
        if (!paused) postInvalidateOnAnimation();
    }

    // ================= CONFIG =================
    // ✅ NO guide path lines (you asked to remove guide lines)
    private static final boolean SHOW_GUIDE_PATH = false;

    // ✅ NO numbers
    private static final boolean SHOW_GUIDE_NUMBERS = false;

    // ✅ Random shape mode (no repeats until bag empty)
    private static final boolean RANDOM_NO_REPEAT = true;
    private final Random rng = new Random();
    private final int[] bag = new int[Constellations.ALL.length];
    private int bagSize = 0;

    // ================= PAINTS =================
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // constellation line (user-built)
    private final Paint lineGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // guide path (disabled)
    private final Paint guidePathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guideGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // star paints
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starSelectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ✅ NEXT STAR GUIDE (glow pulse)
    private final Paint nextHintGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nextHintRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // (kept but unused)
    private final Paint guideTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guideTextBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // title badge
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titleBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // messages
    private final Paint msgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // universe background
    private static class BgStar {
        final float x, y;   // 0..1
        final float r;      // px
        final float tw;     // twinkle speed
        BgStar(float x, float y, float r, float tw) { this.x = x; this.y = y; this.r = r; this.tw = tw; }
    }
    private final ArrayList<BgStar> bgStars = new ArrayList<>();
    private final Paint bgStarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nebulaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean bgGenerated = false;

    // shooting stars
    private static class ShootingStar {
        float x, y;
        float vx, vy;
        float life;
        float size;
        float tail;
        float curve;
        float wobble;
    }
    private final ArrayList<ShootingStar> shootingStars = new ArrayList<>();
    private final Paint shootingHeadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shootingTailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private long lastFrameMs = 0;
    private long nextBgSpawnMs = 0;

    private long celebrationStartMs = 0;
    private static final long CELEBRATION_MS = 2400;

    // ================= DATA =================
    private final ArrayList<Star> stars = new ArrayList<>();
    private int currentIndex = 0;
    private int GOAL = 0;

    private boolean flashing = false;
    private boolean finished = false;
    private long flashStartMs = 0;
    private static final long FLASH_DURATION_MS = 2000;

    private String constellationName = "Constellation";

    public StarSweepView(Context c) { super(c); init(); }
    public StarSweepView(Context c, AttributeSet a) { super(c, a); init(); }
    public StarSweepView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    // ================= DP + LAYOUT =================
    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private float topInsetPx() { return dp(210); }
    private float bottomInsetPx() { return dp(120); }

    private float mapX(float nx) { return nx * getWidth(); }

    private float mapY(float ny) {
        float top = topInsetPx();
        float bottom = bottomInsetPx();
        float usable = Math.max(1f, getHeight() - top - bottom);
        return top + (ny * usable);
    }

    // ================= INIT =================
    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        Shader sky = new LinearGradient(
                0, 0, 0, 2600,
                Color.parseColor("#07061A"),
                Color.parseColor("#1D1148"),
                Shader.TileMode.CLAMP
        );
        bgPaint.setShader(sky);

        // user-connected line glow
        lineGlowPaint.setColor(Color.parseColor("#66C6B7E2"));
        lineGlowPaint.setStyle(Paint.Style.STROKE);
        lineGlowPaint.setStrokeWidth(dp(9));
        lineGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        lineGlowPaint.setStrokeJoin(Paint.Join.ROUND);
        lineGlowPaint.setMaskFilter(new BlurMaskFilter(dp(10), BlurMaskFilter.Blur.NORMAL));

        // user-connected main line
        linePaint.setColor(Color.parseColor("#C6B7E2"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(3.5f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        // (guide paints, disabled)
        guideGlowPaint.setColor(Color.parseColor("#55FFFFFF"));
        guideGlowPaint.setStyle(Paint.Style.STROKE);
        guideGlowPaint.setStrokeWidth(dp(10));
        guideGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        guideGlowPaint.setStrokeJoin(Paint.Join.ROUND);
        guideGlowPaint.setMaskFilter(new BlurMaskFilter(dp(12), BlurMaskFilter.Blur.NORMAL));

        guidePathPaint.setColor(Color.parseColor("#AAFFFFFF"));
        guidePathPaint.setStyle(Paint.Style.STROKE);
        guidePathPaint.setStrokeWidth(dp(2.8f));
        guidePathPaint.setStrokeCap(Paint.Cap.ROUND);
        guidePathPaint.setStrokeJoin(Paint.Join.ROUND);

        // stars
        starPaint.setColor(Color.WHITE);
        starPaint.setStyle(Paint.Style.FILL);

        starSelectedPaint.setColor(Color.parseColor("#C6B7E2"));
        starSelectedPaint.setStyle(Paint.Style.FILL);

        starGlowPaint.setColor(Color.parseColor("#66FFFFFF"));
        starGlowPaint.setStyle(Paint.Style.FILL);
        starGlowPaint.setMaskFilter(new BlurMaskFilter(dp(14), BlurMaskFilter.Blur.NORMAL));

        starCorePaint.setColor(Color.WHITE);
        starCorePaint.setStyle(Paint.Style.FILL);

        // ✅ NEXT STAR GUIDE GLOW (strong + visible)
        nextHintGlowPaint.setColor(Color.parseColor("#99C6B7E2"));
        nextHintGlowPaint.setStyle(Paint.Style.FILL);
        nextHintGlowPaint.setMaskFilter(new BlurMaskFilter(dp(18), BlurMaskFilter.Blur.NORMAL));

        nextHintRingPaint.setColor(Color.parseColor("#C6B7E2"));
        nextHintRingPaint.setStyle(Paint.Style.STROKE);
        nextHintRingPaint.setStrokeWidth(dp(2.5f));
        nextHintRingPaint.setMaskFilter(new BlurMaskFilter(dp(6), BlurMaskFilter.Blur.NORMAL));

        // unused number paints (disabled)
        guideTextPaint.setColor(Color.WHITE);
        guideTextPaint.setTextSize(dp(12));
        guideTextPaint.setTextAlign(Paint.Align.CENTER);

        guideTextBgPaint.setColor(Color.parseColor("#66000000"));
        guideTextBgPaint.setStyle(Paint.Style.FILL);

        // title badge
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTextSize(dp(16));

        titleBgPaint.setColor(Color.parseColor("#44000000"));
        titleBgPaint.setStyle(Paint.Style.FILL);

        msgPaint.setColor(Color.WHITE);
        msgPaint.setTextAlign(Paint.Align.CENTER);
        msgPaint.setTextSize(dp(18));

        // universe
        bgStarPaint.setStyle(Paint.Style.FILL);
        bgStarPaint.setColor(Color.WHITE);

        nebulaPaint.setStyle(Paint.Style.FILL);
        nebulaPaint.setColor(Color.parseColor("#22C6B7E2"));
        nebulaPaint.setMaskFilter(new BlurMaskFilter(dp(90), BlurMaskFilter.Blur.NORMAL));

        // shooting stars
        shootingHeadPaint.setStyle(Paint.Style.FILL);
        shootingHeadPaint.setColor(Color.WHITE);
        shootingHeadPaint.setMaskFilter(new BlurMaskFilter(dp(10), BlurMaskFilter.Blur.NORMAL));

        shootingTailPaint.setStyle(Paint.Style.STROKE);
        shootingTailPaint.setStrokeCap(Paint.Cap.ROUND);
        shootingTailPaint.setStrokeJoin(Paint.Join.ROUND);
        shootingTailPaint.setColor(Color.WHITE);
        shootingTailPaint.setMaskFilter(new BlurMaskFilter(dp(12), BlurMaskFilter.Blur.NORMAL));

        loadNextShape();
        pushHud();
    }

    // ================= BACKGROUND =================
    private void ensureUniverseBackground() {
        if (bgGenerated || getWidth() <= 0 || getHeight() <= 0) return;

        bgStars.clear();
        int count = Math.min(340, Math.max(200, (getWidth() * getHeight()) / 6000));
        float hudCut = topInsetPx() / Math.max(1f, getHeight());

        for (int i = 0; i < count; i++) {
            float x = rng.nextFloat();
            float y = rng.nextFloat();

            if (y < hudCut * 0.65f) {
                y = hudCut * 0.65f + rng.nextFloat() * (1f - hudCut * 0.65f);
            }

            float r = 0.7f + rng.nextFloat() * 2.4f;
            float tw = 0.6f + rng.nextFloat() * 1.9f;
            bgStars.add(new BgStar(x, y, r, tw));
        }

        bgGenerated = true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bgGenerated = false;
    }

    private void drawUniverseBackground(Canvas canvas, long now) {
        ensureUniverseBackground();

        canvas.drawCircle(getWidth() * 0.25f, getHeight() * 0.55f, dp(220), nebulaPaint);
        canvas.drawCircle(getWidth() * 0.78f, getHeight() * 0.72f, dp(260), nebulaPaint);

        for (int i = 0; i < bgStars.size(); i++) {
            BgStar s = bgStars.get(i);

            float x = s.x * getWidth();
            float y = s.y * getHeight();

            float tw = (float) (0.55 + 0.45 * Math.sin((now * 0.002f * s.tw) + i));
            int a = (int) (40 + 210 * tw);
            bgStarPaint.setAlpha(a);

            canvas.drawCircle(x, y, s.r, bgStarPaint);

            if (s.r > 2.1f && (i % 9 == 0)) {
                float len = s.r * 2.0f;
                canvas.drawLine(x - len, y, x + len, y, bgStarPaint);
                canvas.drawLine(x, y - len, x, y + len, bgStarPaint);
            }
        }

        bgStarPaint.setAlpha(255);
    }

    // ================= SHOOTING STARS =================
    private void maybeSpawnBackgroundShootingStar(long now) {
        if (now < nextBgSpawnMs) return;
        nextBgSpawnMs = now + 3800 + (long) (rng.nextFloat() * 3000);
        spawnShootingStar(false);
    }

    private void spawnShootingStar(boolean celebration) {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        ShootingStar s = new ShootingStar();

        s.x = rng.nextFloat() * getWidth() * 0.85f;
        s.y = -dp(80) - rng.nextFloat() * dp(200);

        float speed = celebration ? dp(520) : dp(320);
        float angle = (float) Math.toRadians(28 + rng.nextFloat() * 18);
        s.vx = (float) (Math.cos(angle) * speed);
        s.vy = (float) (Math.sin(angle) * speed);

        s.life = 1f;
        s.size = celebration ? dp(4.2f) : dp(3.2f);
        s.tail = celebration ? dp(420) : dp(320);

        s.curve = (rng.nextFloat() * 2f - 1f) * dp(40);
        s.wobble = 0.7f + rng.nextFloat() * 1.2f;

        shootingStars.add(s);
        if (shootingStars.size() > 10) shootingStars.remove(0);
    }

    private void updateAndDrawShootingStars(Canvas canvas, long now, float dtSec) {
        for (int i = shootingStars.size() - 1; i >= 0; i--) {
            ShootingStar s = shootingStars.get(i);

            s.x += s.vx * dtSec;
            s.y += s.vy * dtSec;

            s.life -= dtSec * 0.33f;

            if (s.life <= 0f || s.x > getWidth() + dp(520) || s.y > getHeight() + dp(520)) {
                shootingStars.remove(i);
                continue;
            }

            float dx = s.vx;
            float dy = s.vy;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float ux = (len == 0) ? 0 : dx / len;
            float uy = (len == 0) ? 0 : dy / len;

            float tx = s.x - ux * s.tail;
            float ty = s.y - uy * s.tail;

            float px = -uy;
            float py = ux;
            float wob = (float) Math.sin(now * 0.0015f * s.wobble + i) * 0.35f;

            float cx = (s.x + tx) * 0.5f + px * (s.curve * wob);
            float cy = (s.y + ty) * 0.5f + py * (s.curve * wob);

            int aTail = (int) (40 + 155 * s.life);
            int aHead = (int) (150 + 105 * s.life);

            Shader tailShader = new LinearGradient(
                    tx, ty, s.x, s.y,
                    new int[]{
                            Color.argb(0, 255, 255, 255),
                            Color.argb(Math.min(255, aTail), 255, 255, 255),
                            Color.argb(Math.min(255, aHead), 255, 255, 255)
                    },
                    new float[]{0f, 0.65f, 1f},
                    Shader.TileMode.CLAMP
            );
            shootingTailPaint.setShader(tailShader);

            float w = s.size * (1.8f + 0.7f * s.life);
            shootingTailPaint.setStrokeWidth(w);

            Path p = new Path();
            p.moveTo(tx, ty);
            p.quadTo(cx, cy, s.x, s.y);

            canvas.drawPath(p, shootingTailPaint);

            shootingHeadPaint.setAlpha(aHead);
            canvas.drawCircle(s.x, s.y, s.size * 1.9f, shootingHeadPaint);
            canvas.drawCircle(s.x, s.y, s.size * 1.1f, shootingHeadPaint);

            shootingTailPaint.setShader(null);
        }
    }

    // ================= GAME FLOW =================
    public void resetGame() {
        finished = false;
        flashing = false;
        flashStartMs = 0;
        currentIndex = 0;

        loadNextShape();
        pushHud();
        invalidate();
    }

    private int pickRandomShapeIndex() {
        int n = Constellations.ALL.length;

        if (!RANDOM_NO_REPEAT) return rng.nextInt(n);

        if (bagSize <= 0) {
            for (int i = 0; i < n; i++) bag[i] = i;
            bagSize = n;
        }

        int pickPos = rng.nextInt(bagSize);
        int chosen = bag[pickPos];

        bag[pickPos] = bag[bagSize - 1];
        bagSize--;

        return chosen;
    }

    private void loadNextShape() {
        if (Constellations.ALL.length == 0) return;

        int index = pickRandomShapeIndex();
        Constellations.Pattern p = Constellations.ALL[index];

        constellationName = p.name;

        stars.clear();
        for (float[] pt : p.points) stars.add(new Star(pt[0], pt[1]));

        GOAL = stars.size();
        currentIndex = 0;

        finished = false;
        flashing = false;
        flashStartMs = 0;

        celebrationStartMs = 0;
    }

    private void pushHud() {
        if (hudListener != null) {
            hudListener.onBreathText("Shape: " + constellationName + " ✨");
            hudListener.onProgress(currentIndex, GOAL);
        }
    }

    // ================= DRAW =================
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long now = SystemClock.uptimeMillis();
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);

        drawUniverseBackground(canvas, now);

        if (lastFrameMs == 0) lastFrameMs = now;
        float dt = Math.min(0.05f, (now - lastFrameMs) / 1000f);
        lastFrameMs = now;

        maybeSpawnBackgroundShootingStar(now);

        if (celebrationStartMs != 0 && now - celebrationStartMs < CELEBRATION_MS) {
            if (rng.nextFloat() < 0.10f) spawnShootingStar(true);
        }

        updateAndDrawShootingStars(canvas, now, dt);

        drawTitleBadge(canvas);

        if (paused) {
            drawConstellation(canvas, now);
            return;
        }

        if (flashing && now - flashStartMs >= FLASH_DURATION_MS) {
            flashing = false;
            finished = true;
            if (hudListener != null) hudListener.onFinishedReady();
        }

        drawConstellation(canvas, now);
        postInvalidateOnAnimation();
    }

    private void drawConstellation(Canvas canvas, long now) {
        float pulse = (float) (0.5 + 0.5 * Math.sin(now * 0.006));

        // ✅ NO GUIDE LINES (removed)
        if (SHOW_GUIDE_PATH && stars.size() >= 2 && !finished) {
            Path guide = buildCatmullRomPath(stars.size());
            canvas.drawPath(guide, guideGlowPaint);
            canvas.drawPath(guide, guidePathPaint);
        }

        // ✅ USER CONNECTED LINES ONLY
        if (currentIndex >= 2) {
            Path path = buildCatmullRomPath(currentIndex);
            canvas.drawPath(path, lineGlowPaint);
            canvas.drawPath(path, linePaint);
        }

        // ✅ GUIDE: ONLY NEXT STAR GLOWS (NO NUMBERS, NO LINE)
        if (!finished && !flashing && currentIndex < stars.size()) {
            Star h = stars.get(currentIndex);
            float hx = mapX(h.x);
            float hy = mapY(h.y);

            float glowR = dp(26) + dp(14) * pulse;
            float ringR = dp(18) + dp(10) * pulse;

            canvas.drawCircle(hx, hy, glowR, nextHintGlowPaint);
            canvas.drawCircle(hx, hy, ringR, nextHintRingPaint);
        }

        // draw stars
        for (int i = 0; i < stars.size(); i++) {
            Star s = stars.get(i);

            float x = mapX(s.x);
            float y = mapY(s.y);

            boolean connected = (i < currentIndex);
            Paint mainPaint = connected ? starSelectedPaint : starPaint;

            float twinkle = (float) (1.0 + 0.18 * Math.sin(now * 0.006 + i));

            canvas.drawCircle(x, y, dp(18) * twinkle, starGlowPaint);
            drawStar(canvas, x, y, dp(10) * twinkle, mainPaint);
            canvas.drawCircle(x, y, dp(2.8f) * twinkle, starCorePaint);

            // ❌ no numbers
            if (SHOW_GUIDE_NUMBERS) {
                // disabled
            }
        }

        if (finished) {
            canvas.drawText("Completed ✨", getWidth() / 2f, dp(140), msgPaint);
        } else if (flashing) {
            canvas.drawText("✨ Beautiful ✨", getWidth() / 2f, dp(140), msgPaint);
        }
    }

    // spline that passes through all points
    private Path buildCatmullRomPath(int count) {
        Path path = new Path();
        if (count <= 0) return path;

        float[] xs = new float[count];
        float[] ys = new float[count];

        for (int i = 0; i < count; i++) {
            Star s = stars.get(i);
            xs[i] = mapX(s.x);
            ys[i] = mapY(s.y);
        }

        path.moveTo(xs[0], ys[0]);

        final float t = 0.5f;

        for (int i = 0; i < count - 1; i++) {
            float x0 = (i == 0) ? xs[i] : xs[i - 1];
            float y0 = (i == 0) ? ys[i] : ys[i - 1];

            float x1 = xs[i];
            float y1 = ys[i];

            float x2 = xs[i + 1];
            float y2 = ys[i + 1];

            float x3 = (i + 2 < count) ? xs[i + 2] : xs[i + 1];
            float y3 = (i + 2 < count) ? ys[i + 2] : ys[i + 1];

            float c1x = x1 + (x2 - x0) * (t / 3f);
            float c1y = y1 + (y2 - y0) * (t / 3f);

            float c2x = x2 - (x3 - x1) * (t / 3f);
            float c2y = y2 - (y3 - y1) * (t / 3f);

            path.cubicTo(c1x, c1y, c2x, c2y, x2, y2);
        }

        return path;
    }

    // 5-point star
    private void drawStar(Canvas canvas, float cx, float cy, float outerRadius, Paint paint) {
        Path path = new Path();

        int points = 5;
        double angle = -Math.PI / 2.0;
        double step = Math.PI / points;

        float innerRadius = outerRadius * 0.45f;

        path.moveTo(
                (float) (cx + Math.cos(angle) * outerRadius),
                (float) (cy + Math.sin(angle) * outerRadius)
        );

        for (int i = 0; i < points; i++) {
            path.lineTo(
                    (float) (cx + Math.cos(angle) * outerRadius),
                    (float) (cy + Math.sin(angle) * outerRadius)
            );
            angle += step;

            path.lineTo(
                    (float) (cx + Math.cos(angle) * innerRadius),
                    (float) (cy + Math.sin(angle) * innerRadius)
            );
            angle += step;
        }

        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawTitleBadge(Canvas canvas) {
        String title = constellationName;
        float cx = getWidth() / 2f;
        float y = dp(85);

        float w = titlePaint.measureText(title);
        RectF r = new RectF(cx - w / 2f - dp(18), y - dp(26), cx + w / 2f + dp(18), y + dp(10));
        canvas.drawRoundRect(r, dp(18), dp(18), titleBgPaint);
        canvas.drawText(title, cx, y, titlePaint);
    }

    // ================= INPUT =================
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // ✅ Allow HUD buttons (like Settings) to receive touches
        if (event.getY() < topInsetPx()) {
            return false; // let button clicks pass through
        }

        if (finished || flashing || paused) return true;
;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (currentIndex >= stars.size()) return true;

            Star t = stars.get(currentIndex);
            float tx = mapX(t.x);
            float ty = mapY(t.y);

            float dx = event.getX() - tx;
            float dy = event.getY() - ty;

            float tap = dp(36);
            if (dx * dx + dy * dy < tap * tap) {
                currentIndex++;
                if (hudListener != null) hudListener.onProgress(currentIndex, GOAL);

                if (currentIndex >= GOAL) {
                    flashing = true;
                    flashStartMs = SystemClock.uptimeMillis();
                    celebrationStartMs = flashStartMs;
                    if (hudListener != null) hudListener.onFinishFlashStarted();
                }
            }
        }
        return true;
    }

    // ================= DATA TYPES =================
    static class Star {
        final float x, y;
        Star(float x, float y) { this.x = x; this.y = y; }
    }

    // ================= 40 CONSTELLATIONS =================
    static class Constellations {
        static class Pattern {
            final String name;
            final float[][] points;
            Pattern(String name, float[][] points) { this.name = name; this.points = points; }
        }

        static final Pattern[] ALL = new Pattern[]{

                // 1–5
                new Pattern("Cat Head 🐱", new float[][]{
                        {0.35f,0.35f},{0.40f,0.20f},{0.45f,0.32f},
                        {0.55f,0.32f},{0.60f,0.20f},{0.65f,0.35f},
                        {0.70f,0.50f},{0.62f,0.65f},{0.50f,0.72f},
                        {0.38f,0.65f},{0.30f,0.50f},{0.35f,0.35f}
                }),
                new Pattern("Rabbit Head 🐰", new float[][]{
                        {0.42f,0.20f},{0.46f,0.05f},{0.50f,0.18f},
                        {0.54f,0.05f},{0.58f,0.20f},
                        {0.64f,0.38f},{0.60f,0.55f},{0.50f,0.65f},
                        {0.40f,0.55f},{0.36f,0.38f},{0.42f,0.20f}
                }),
                new Pattern("Butterfly 🦋", new float[][]{
                        {0.50f,0.30f},{0.40f,0.20f},{0.30f,0.35f},
                        {0.35f,0.55f},{0.45f,0.65f},{0.50f,0.55f},
                        {0.55f,0.65f},{0.65f,0.55f},{0.70f,0.35f},
                        {0.60f,0.20f},{0.50f,0.30f}
                }),
                new Pattern("Heart 💜", new float[][]{
                        {0.50f,0.35f},{0.42f,0.25f},{0.34f,0.30f},
                        {0.30f,0.40f},{0.36f,0.55f},{0.50f,0.70f},
                        {0.64f,0.55f},{0.70f,0.40f},{0.66f,0.30f},
                        {0.58f,0.25f},{0.50f,0.35f}
                }),
                new Pattern("Moon 🌙", new float[][]{
                        {0.55f,0.20f},{0.65f,0.30f},{0.70f,0.45f},
                        {0.66f,0.60f},{0.55f,0.70f},{0.42f,0.60f},
                        {0.38f,0.45f},{0.42f,0.30f},{0.55f,0.20f}
                }),

                // 6–10
                new Pattern("Dog Head 🐶", new float[][]{
                        {0.32f,0.38f},{0.28f,0.25f},{0.40f,0.30f},
                        {0.50f,0.28f},{0.60f,0.30f},{0.72f,0.25f},{0.68f,0.38f},
                        {0.74f,0.52f},{0.62f,0.66f},{0.50f,0.72f},{0.38f,0.66f},{0.26f,0.52f},
                        {0.32f,0.38f}
                }),
                new Pattern("Fox Head 🦊", new float[][]{
                        {0.30f,0.40f},{0.40f,0.18f},{0.50f,0.32f},{0.60f,0.18f},{0.70f,0.40f},
                        {0.62f,0.58f},{0.50f,0.72f},{0.38f,0.58f},
                        {0.30f,0.40f}
                }),
                new Pattern("Fish 🐟", new float[][]{
                        {0.25f,0.50f},{0.35f,0.40f},{0.50f,0.35f},{0.65f,0.45f},{0.72f,0.55f},
                        {0.65f,0.65f},{0.50f,0.70f},{0.35f,0.60f},
                        {0.25f,0.50f},{0.18f,0.40f},{0.18f,0.60f},{0.25f,0.50f}
                }),
                new Pattern("Flower 🌸", new float[][]{
                        {0.50f,0.20f},{0.62f,0.28f},{0.70f,0.42f},{0.62f,0.55f},{0.50f,0.70f},
                        {0.38f,0.55f},{0.30f,0.42f},{0.38f,0.28f},{0.50f,0.20f},
                        {0.50f,0.40f},{0.56f,0.45f},{0.50f,0.50f},{0.44f,0.45f},{0.50f,0.40f}
                }),
                new Pattern("Crown 👑", new float[][]{
                        {0.25f,0.65f},{0.32f,0.42f},{0.42f,0.62f},
                        {0.50f,0.34f},{0.58f,0.62f},
                        {0.68f,0.42f},{0.75f,0.65f},
                        {0.65f,0.74f},{0.50f,0.76f},{0.35f,0.74f},
                        {0.25f,0.65f}
                }),

                // 11–15
                new Pattern("Panda Head 🐼", new float[][]{
                        {0.32f,0.30f},{0.28f,0.20f},{0.38f,0.22f},
                        {0.50f,0.25f},{0.62f,0.22f},{0.72f,0.20f},{0.68f,0.30f},
                        {0.74f,0.48f},{0.64f,0.64f},{0.50f,0.72f},{0.36f,0.64f},{0.26f,0.48f},
                        {0.32f,0.30f}
                }),
                new Pattern("Frog 🐸", new float[][]{
                        {0.30f,0.42f},{0.28f,0.30f},{0.38f,0.28f},{0.44f,0.38f},
                        {0.50f,0.40f},
                        {0.56f,0.38f},{0.62f,0.28f},{0.72f,0.30f},{0.70f,0.42f},
                        {0.66f,0.60f},{0.50f,0.70f},{0.34f,0.60f},
                        {0.30f,0.42f}
                }),
                new Pattern("Owl 🦉", new float[][]{
                        {0.36f,0.35f},{0.40f,0.20f},{0.46f,0.32f},
                        {0.54f,0.32f},{0.60f,0.20f},{0.64f,0.35f},
                        {0.70f,0.50f},{0.62f,0.66f},{0.50f,0.74f},{0.38f,0.66f},{0.30f,0.50f},
                        {0.36f,0.35f}
                }),
                new Pattern("Tree 🌳", new float[][]{
                        {0.50f,0.20f},{0.62f,0.28f},{0.70f,0.42f},{0.64f,0.56f},{0.50f,0.66f},
                        {0.36f,0.56f},{0.30f,0.42f},{0.38f,0.28f},{0.50f,0.20f},
                        {0.50f,0.66f},{0.50f,0.82f}
                }),
                new Pattern("Rocket 🚀", new float[][]{
                        {0.50f,0.18f},{0.60f,0.30f},{0.62f,0.50f},
                        {0.58f,0.66f},{0.50f,0.78f},{0.42f,0.66f},
                        {0.38f,0.50f},{0.40f,0.30f},{0.50f,0.18f},
                        {0.50f,0.36f},{0.54f,0.42f},{0.50f,0.48f},{0.46f,0.42f},{0.50f,0.36f}
                }),

                // 16–20
                new Pattern("Unicorn 🦄", new float[][]{
                        {0.40f,0.22f},{0.46f,0.05f},{0.52f,0.22f},
                        {0.60f,0.32f},{0.68f,0.44f},{0.62f,0.60f},
                        {0.50f,0.72f},{0.38f,0.62f},{0.32f,0.48f},
                        {0.34f,0.34f},{0.40f,0.22f}
                }),
                new Pattern("Turtle 🐢", new float[][]{
                        {0.40f,0.42f},{0.50f,0.36f},{0.60f,0.42f},
                        {0.66f,0.52f},{0.60f,0.64f},{0.50f,0.68f},{0.40f,0.64f},{0.34f,0.52f},
                        {0.40f,0.42f},
                        {0.30f,0.48f},{0.70f,0.48f},{0.50f,0.76f}
                }),
                new Pattern("Dolphin 🐬", new float[][]{
                        {0.28f,0.56f},{0.36f,0.46f},{0.48f,0.40f},{0.60f,0.44f},{0.70f,0.54f},
                        {0.62f,0.62f},{0.48f,0.68f},{0.34f,0.64f},
                        {0.28f,0.56f}
                }),
                new Pattern("Cloud ☁️", new float[][]{
                        {0.30f,0.56f},{0.34f,0.44f},{0.44f,0.40f},{0.52f,0.32f},{0.62f,0.38f},
                        {0.70f,0.48f},{0.66f,0.62f},{0.52f,0.66f},{0.40f,0.64f},
                        {0.30f,0.56f}
                }),
                new Pattern("Lightning ⚡", new float[][]{
                        {0.56f,0.18f},{0.42f,0.46f},{0.56f,0.46f},
                        {0.38f,0.82f},{0.62f,0.52f},{0.48f,0.52f},{0.56f,0.18f}
                }),

                // 21–25
                new Pattern("Lotus 🌸", new float[][]{
                        {0.50f,0.20f},{0.60f,0.32f},{0.70f,0.48f},{0.62f,0.58f},{0.50f,0.70f},
                        {0.38f,0.58f},{0.30f,0.48f},{0.40f,0.32f},{0.50f,0.20f},
                        {0.50f,0.32f},{0.56f,0.44f},{0.50f,0.54f},{0.44f,0.44f},{0.50f,0.32f}
                }),
                new Pattern("Dove 🕊", new float[][]{
                        {0.30f,0.52f},{0.42f,0.40f},{0.54f,0.38f},{0.68f,0.44f},{0.62f,0.52f},
                        {0.52f,0.60f},{0.44f,0.66f},{0.36f,0.60f},{0.30f,0.52f}
                }),
                new Pattern("Octopus 🐙", new float[][]{
                        {0.50f,0.26f},{0.62f,0.34f},{0.68f,0.48f},{0.62f,0.62f},{0.50f,0.68f},
                        {0.38f,0.62f},{0.32f,0.48f},{0.38f,0.34f},{0.50f,0.26f},
                        {0.40f,0.72f},{0.34f,0.82f},{0.46f,0.74f},{0.44f,0.86f},
                        {0.54f,0.74f},{0.56f,0.86f},{0.60f,0.72f},{0.66f,0.82f}
                }),
                new Pattern("Royal Crown 👑", new float[][]{
                        {0.22f,0.68f},{0.30f,0.44f},{0.40f,0.64f},
                        {0.50f,0.32f},{0.60f,0.64f},
                        {0.70f,0.44f},{0.78f,0.68f},
                        {0.70f,0.76f},{0.50f,0.80f},{0.30f,0.76f},
                        {0.22f,0.68f}
                }),
                new Pattern("Wave 🌊", new float[][]{
                        {0.20f,0.62f},{0.30f,0.52f},{0.40f,0.58f},{0.50f,0.48f},{0.60f,0.54f},
                        {0.70f,0.44f},{0.80f,0.50f},{0.72f,0.66f},{0.56f,0.68f},{0.40f,0.66f},
                        {0.20f,0.62f}
                }),

                // 26–30
                new Pattern("Dragon 🐉", new float[][]{
                        {0.38f,0.28f},{0.44f,0.14f},{0.50f,0.26f},
                        {0.62f,0.34f},{0.72f,0.46f},
                        {0.66f,0.58f},{0.54f,0.70f},{0.42f,0.64f},
                        {0.34f,0.50f},{0.30f,0.38f},
                        {0.38f,0.28f}
                }),
                new Pattern("Swan 🦢", new float[][]{
                        {0.55f,0.20f},{0.50f,0.30f},{0.46f,0.44f},{0.48f,0.60f},
                        {0.60f,0.70f},{0.72f,0.62f},{0.66f,0.50f},
                        {0.54f,0.56f},{0.42f,0.64f},{0.38f,0.54f},
                        {0.48f,0.44f},{0.55f,0.20f}
                }),
                new Pattern("Crescent Star 🌙⭐", new float[][]{
                        {0.56f,0.22f},{0.66f,0.32f},{0.70f,0.48f},{0.64f,0.62f},{0.52f,0.70f},
                        {0.40f,0.62f},{0.38f,0.46f},{0.42f,0.32f},{0.56f,0.22f},
                        {0.74f,0.30f},{0.78f,0.40f},{0.88f,0.40f},{0.80f,0.48f},{0.84f,0.60f},
                        {0.74f,0.52f},{0.64f,0.60f},{0.68f,0.48f},{0.60f,0.40f},{0.70f,0.40f},
                        {0.74f,0.30f}
                }),
                new Pattern("Bow 🎀", new float[][]{
                        {0.50f,0.50f},{0.40f,0.40f},{0.28f,0.50f},{0.40f,0.60f},{0.50f,0.50f},
                        {0.60f,0.40f},{0.72f,0.50f},{0.60f,0.60f},{0.50f,0.50f},
                        {0.46f,0.70f},{0.54f,0.70f}
                }),
                new Pattern("Mushroom 🍄", new float[][]{
                        {0.30f,0.44f},{0.40f,0.32f},{0.50f,0.28f},{0.60f,0.32f},{0.70f,0.44f},
                        {0.30f,0.44f},
                        {0.44f,0.44f},{0.44f,0.72f},{0.56f,0.72f},{0.56f,0.44f}
                }),

                // 31–35
                new Pattern("Big Butterfly 🦋", new float[][]{
                        {0.50f,0.26f},{0.42f,0.18f},{0.32f,0.26f},{0.24f,0.40f},{0.30f,0.56f},{0.40f,0.66f},
                        {0.50f,0.58f},{0.60f,0.66f},{0.70f,0.56f},{0.76f,0.40f},{0.68f,0.26f},{0.58f,0.18f},
                        {0.50f,0.26f}
                }),
                new Pattern("Wolf Head 🐺", new float[][]{
                        {0.30f,0.42f},{0.40f,0.18f},{0.50f,0.32f},{0.60f,0.18f},{0.70f,0.42f},
                        {0.62f,0.60f},{0.50f,0.74f},{0.38f,0.60f},{0.30f,0.42f}
                }),
                new Pattern("Rose 🌹", new float[][]{
                        {0.50f,0.22f},{0.62f,0.30f},{0.70f,0.44f},{0.62f,0.58f},{0.50f,0.70f},
                        {0.38f,0.58f},{0.30f,0.44f},{0.38f,0.30f},{0.50f,0.22f},
                        {0.50f,0.36f},{0.56f,0.44f},{0.50f,0.52f},{0.44f,0.44f},{0.50f,0.36f}
                }),
                new Pattern("Guitar 🎸", new float[][]{
                        {0.42f,0.60f},{0.38f,0.48f},{0.42f,0.38f},{0.50f,0.34f},{0.58f,0.38f},{0.62f,0.48f},
                        {0.58f,0.60f},{0.50f,0.66f},{0.42f,0.60f},
                        {0.50f,0.34f},{0.52f,0.18f},{0.56f,0.10f}
                }),
                new Pattern("Umbrella ☂", new float[][]{
                        {0.30f,0.46f},{0.40f,0.34f},{0.50f,0.30f},{0.60f,0.34f},{0.70f,0.46f},
                        {0.50f,0.46f},
                        {0.50f,0.80f},{0.46f,0.86f},{0.52f,0.88f}
                }),

                // 36–40
                new Pattern("Full Dragon 🐉", new float[][]{
                        {0.30f,0.42f},{0.40f,0.28f},{0.55f,0.30f},{0.68f,0.42f},
                        {0.72f,0.56f},{0.62f,0.66f},{0.48f,0.70f},{0.34f,0.64f},
                        {0.26f,0.54f},{0.32f,0.46f},{0.30f,0.42f}
                }),
                new Pattern("Lion Head 🦁", new float[][]{
                        {0.32f,0.38f},{0.28f,0.28f},{0.38f,0.20f},{0.50f,0.22f},{0.62f,0.20f},{0.72f,0.28f},{0.68f,0.38f},
                        {0.74f,0.50f},{0.66f,0.64f},{0.50f,0.74f},{0.34f,0.64f},{0.26f,0.50f},
                        {0.32f,0.38f}
                }),
                new Pattern("Rainbow 🌈", new float[][]{
                        {0.25f,0.62f},{0.32f,0.46f},{0.42f,0.34f},{0.50f,0.30f},{0.58f,0.34f},{0.68f,0.46f},{0.75f,0.62f},
                        {0.68f,0.58f},{0.58f,0.44f},{0.50f,0.40f},{0.42f,0.44f},{0.32f,0.58f},{0.25f,0.62f}
                }),
                new Pattern("Gift 🎁", new float[][]{
                        {0.34f,0.40f},{0.66f,0.40f},{0.66f,0.70f},{0.34f,0.70f},{0.34f,0.40f},
                        {0.50f,0.40f},{0.50f,0.70f},
                        {0.34f,0.54f},{0.66f,0.54f},
                        {0.46f,0.30f},{0.50f,0.36f},{0.54f,0.30f}
                }),
                new Pattern("Snowflake ❄", new float[][]{
                        {0.50f,0.22f},{0.50f,0.78f},
                        {0.32f,0.34f},{0.68f,0.66f},
                        {0.32f,0.66f},{0.68f,0.34f},
                        {0.22f,0.50f},{0.78f,0.50f},
                        {0.50f,0.22f}
                }),
        };
    }
}
