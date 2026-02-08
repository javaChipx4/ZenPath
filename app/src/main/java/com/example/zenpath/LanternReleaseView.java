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
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class LanternReleaseView extends View {

    // ================= UI listener =================
    public interface UiListener {
        void onUpdate(int total, String badge, String message);
    }

    private UiListener uiListener;
    public void setUiListener(UiListener l) { uiListener = l; pushUi(); }

    // ================= Model =================
    private static class Lantern {
        float x, y, w, h, vy;
        boolean released, glowing, messageVisible;
        String message = "";
        long glowEndMs = 0;
        final RectF bounds = new RectF();
        float ribbonPhase = 0f;
    }

    private final List<Lantern> lanterns = new ArrayList<>();
    private final Random rng = new Random();

    private boolean running = false;
    private boolean finished = false;

    private int totalScore = 0;
    private String badge = "—";
    private String hudMessage = "Tap to place a lantern ✨";

    // ================= Background (night + stars) =================
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private LinearGradient bgGrad;

    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static class Star {
        float x, y, r;
        float phase;
    }
    private final ArrayList<Star> stars = new ArrayList<>();
    private int lastW = 0, lastH = 0;

    // ================= Glow paint =================
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ================= Message =================
    private final TextPaint msgPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint msgShadowPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    // ================= PNG Lantern =================
    private Bitmap lanternBmp;
    private final RectF lanternDst = new RectF();

    // ✅ black tint for inactive lantern
    private final Paint lanternBlackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ✅ flame + sparkles
    private final Paint flamePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static class Sparkle {
        float x, y;
        float vx, vy;
        float r;
        float life;   // seconds left
        float maxLife;
    }
    private final ArrayList<Sparkle> sparkles = new ArrayList<>();

    // timing
    private long lastMs = 0;

    // ✅ size
    private static final float LANTERN_W_DP = 130f;
    private static final float LANTERN_H_DP = 200f;

    public LanternReleaseView(Context c){ super(c); init(); }
    public LanternReleaseView(Context c, @Nullable AttributeSet a){ super(c,a); init(); }
    public LanternReleaseView(Context c, @Nullable AttributeSet a, int s){ super(c,a,s); init(); }

    private void init(){
        setClickable(true);

        // stars
        starPaint.setColor(Color.WHITE);
        starPaint.setStyle(Paint.Style.FILL);
        starPaint.setAlpha(180);

        starGlowPaint.setColor(Color.WHITE);
        starGlowPaint.setStyle(Paint.Style.FILL);
        starGlowPaint.setAlpha(60);

        // message paints
        msgPaint.setColor(Color.WHITE);
        msgPaint.setTextSize(sp(12));
        msgPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        msgPaint.setTextAlign(Paint.Align.CENTER);

        msgShadowPaint.set(msgPaint);
        msgShadowPaint.setColor(Color.argb(160, 0, 0, 0));
        msgShadowPaint.setTextAlign(Paint.Align.CENTER);

        glowPaint.setStyle(Paint.Style.FILL);

        // black lantern filter
        lanternBlackPaint.setColorFilter(
                new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP)
        );
        lanternBlackPaint.setAlpha(235);

        // flame/sparkles
        flamePaint.setStyle(Paint.Style.FILL);
        sparklePaint.setStyle(Paint.Style.FILL);

        // load lantern png
        try {
            lanternBmp = BitmapFactory.decodeResource(getResources(), R.drawable.lantern);
        } catch (Exception ignored) {
            lanternBmp = null;
        }
    }

    // ================= Status =================
    public boolean isRunning() { return running; }
    public boolean isFinished() { return finished; }
    public void startRelease() { play(); }

    // ================= Controls =================
    public void play(){
        if(lanterns.isEmpty()){
            hudMessage = "Place a lantern first ✨";
            pushUi();
            return;
        }

        running = true;
        finished = false;
        hudMessage = "Tap a flying lantern to glow ✨";
        pushUi();

        for(Lantern l: lanterns){
            l.released = true;
            l.glowing = false;          // start black
            l.messageVisible = false;
            l.glowEndMs = 0;
            l.vy = -dp(55) - rng.nextFloat()*dp(18);
            l.ribbonPhase = rng.nextFloat() * 6.28f;
        }

        invalidate();
    }

    public void playAgain(){
        running = false;
        finished = false;
        hudMessage = "Tap to place a lantern ✨";
        pushUi();

        for(Lantern l: lanterns){
            l.released = false;
            l.glowing = false;
            l.messageVisible = false;
            l.glowEndMs = 0;
        }
        sparkles.clear();

        invalidate();
    }

    public void resetToday(){
        lanterns.clear();
        sparkles.clear();
        totalScore = 0;
        badge = "—";
        running = false;
        finished = false;
        hudMessage = "Tap to place a lantern ✨";
        pushUi();
        invalidate();
    }

    // ================= Touch =================
    @Override
    public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()!=MotionEvent.ACTION_DOWN) return true;

        float x = e.getX(), y = e.getY();
        Lantern hit = findLanternAt(x, y);

        // BEFORE PLAY: tap lantern = write/edit message; tap empty = place lantern
        if(!running){
            if(hit != null){
                openMessageDialog(hit);
            } else {
                placeLantern(x,y);
                hudMessage = "Tap a lantern to add a message ✨";
                pushUi();
                invalidate();
            }
            return true;
        }

        // DURING FLIGHT: tap lantern = glow + show message inside
        if(hit != null){
            hit.glowing = true;
            hit.messageVisible = true;
            hit.glowEndMs = SystemClock.uptimeMillis() + 2300;

            spawnSparkles(hit.x, hit.y - hit.h * 0.05f);

            totalScore += 10;
            updateBadge();
            hudMessage = "Glow ✨";
            pushUi();
            invalidate();
        }

        return true;
    }

    private void openMessageDialog(Lantern lantern){
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

    private void placeLantern(float x,float y){
        Lantern l = new Lantern();

        l.w = dp(LANTERN_W_DP);
        l.h = dp(LANTERN_H_DP);

        l.x = clamp(x, l.w * 0.6f, getWidth() - l.w * 0.6f);
        l.y = clamp(y, l.h * 0.6f, getHeight() - l.h * 0.6f);

        l.released = false;
        l.glowing = false;
        l.messageVisible = false;

        lanterns.add(l);
    }

    private Lantern findLanternAt(float x,float y){
        for (int i = lanterns.size() - 1; i >= 0; i--){
            Lantern l = lanterns.get(i);
            l.bounds.set(l.x - l.w/2f, l.y - l.h/2f, l.x + l.w/2f, l.y + l.h/2f);
            if(l.bounds.contains(x,y)) return l;
        }
        return null;
    }

    // ================= Draw =================
    @Override
    protected void onDraw(Canvas c){
        super.onDraw(c);

        long now = SystemClock.uptimeMillis();
        if(lastMs==0) lastMs=now;
        float dt=(now-lastMs)/1000f;
        lastMs=now;
        if (dt > 0.05f) dt = 0.05f;

        drawNightBackground(c, now);

        boolean needsMoreFrames = false;

        if(running){
            boolean anyStillOnScreen = false;

            for(Lantern l: lanterns){
                if(!l.released) continue;

                l.y += l.vy * dt;

                if(l.glowing && now > l.glowEndMs){
                    l.glowing = false;      // back to black
                    l.messageVisible = false;
                }

                if (l.y + l.h > -dp(40)) anyStillOnScreen = true;
            }

            if(!anyStillOnScreen){
                running = false;
                finished = true;
                hudMessage = "Done ✨ Press Play";
                pushUi();
            } else {
                needsMoreFrames = true;
            }
        }

        // sparkles update
        if (!sparkles.isEmpty()){
            Iterator<Sparkle> it = sparkles.iterator();
            while (it.hasNext()){
                Sparkle s = it.next();
                s.life -= dt;
                if (s.life <= 0f){
                    it.remove();
                    continue;
                }
                s.x += s.vx * dt;
                s.y += s.vy * dt;
                s.vy += dp(55) * dt;
            }
            needsMoreFrames = true;
        }

        // draw lanterns + inside-message
        for(Lantern l: lanterns){
            drawLanternPng(c, l, now);
        }

        drawSparkles(c);

        if (needsMoreFrames){
            postInvalidateOnAnimation();
        }
    }

    private void drawNightBackground(Canvas c, long now){
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (w != lastW || h != lastH || bgGrad == null || stars.isEmpty()){
            lastW = w;
            lastH = h;

            bgGrad = new LinearGradient(
                    0,0,0,h,
                    new int[]{
                            Color.rgb(5, 7, 20),
                            Color.rgb(9, 12, 36),
                            Color.rgb(10, 16, 48)
                    },
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP
            );
            bgPaint.setShader(bgGrad);

            stars.clear();
            for(int i=0;i<220;i++){
                Star s = new Star();
                s.x = rng.nextFloat()*w;
                s.y = rng.nextFloat()*h;

                float pick = rng.nextFloat();
                float base = (pick < 0.75f) ? dp(0.6f) : dp(1.2f);
                s.r = base + rng.nextFloat()*dp(1.6f);

                s.phase = rng.nextFloat() * 6.28f;
                stars.add(s);
            }
        }

        c.drawRect(0,0,w,h,bgPaint);

        float t = (now % 5000L) / 5000f;
        float tw = (float)Math.sin(t * 6.283f);

        for(Star s: stars){
            float alpha = 115 + 75f * (float)Math.sin(s.phase + tw * 1.5f);
            alpha = clamp(alpha, 45, 220);

            starPaint.setAlpha((int)alpha);
            c.drawCircle(s.x, s.y, s.r, starPaint);

            if (s.r > dp(1.7f)){
                starGlowPaint.setAlpha((int)(alpha * 0.28f));
                c.drawCircle(s.x, s.y, s.r * 2.0f, starGlowPaint);
            }
        }
    }

    private void drawLanternPng(Canvas c, Lantern l, long now){
        if (lanternBmp == null) return;

        float x = l.x;
        float y = l.y;
        float w = l.w;
        float h = l.h;

        // glow behind
        if (l.glowing){
            float pulse = 0.9f + 0.1f * (float)Math.sin(now * 0.008f);
            float r = Math.max(w, h) * 0.80f * pulse;

            glowPaint.setShader(new RadialGradient(
                    x, y + h * 0.15f, r,
                    new int[]{
                            Color.argb(190, 255, 210, 120),
                            Color.argb(70, 255, 170, 70),
                            Color.argb(0, 255, 170, 70)
                    },
                    new float[]{0f, 0.70f, 1f},
                    Shader.TileMode.CLAMP
            ));
            c.drawCircle(x, y, r, glowPaint);
            glowPaint.setShader(null);
        }

        // draw lantern (black if not glowing)
        lanternDst.set(x - w/2f, y - h/2f, x + w/2f, y + h/2f);
        if (!l.glowing) c.drawBitmap(lanternBmp, null, lanternDst, lanternBlackPaint);
        else c.drawBitmap(lanternBmp, null, lanternDst, null);

        // ✅ MESSAGE INSIDE the lantern window (only when glowing)
        if (l.glowing && l.messageVisible && l.message != null && !l.message.trim().isEmpty()){
            RectF window = lanternWindowRect(x, y, w, h);
            drawMessageInsideLantern(c, l.message.trim(), window);
        }
    }

    // ✅ Adjust these numbers if your lantern PNG window is different
    private RectF lanternWindowRect(float cx, float cy, float w, float h){
        // a “glass” area inside the lantern
        float left   = cx - w * 0.23f;
        float right  = cx + w * 0.23f;
        float top    = cy - h * 0.10f;
        float bottom = cy + h * 0.16f;
        return new RectF(left, top, right, bottom);
    }

    private void drawMessageInsideLantern(Canvas c, String msg, RectF window){
        // slightly stronger readability on glow
        msgPaint.setTextSize(sp(11));
        msgShadowPaint.setTextSize(sp(11));

        float padding = dp(6);
        float maxW = Math.max(0, window.width() - padding * 2f);

        String[] lines = wrapTextCentered(msg, maxW, 3);

        float lineH = dp(14);
        int lineCount = 0;
        for (String s : lines) { if (s != null) lineCount++; }
        float totalH = lineCount * lineH;

        float cx = window.centerX();
        float y = window.top + (window.height() - totalH) / 2f + lineH * 0.85f;

        int save = c.save();
        c.clipRect(window);

        for (String line : lines){
            if (line == null) break;
            c.drawText(line, cx + dp(1), y + dp(1), msgShadowPaint);
            c.drawText(line, cx, y, msgPaint);
            y += lineH;
        }

        c.restoreToCount(save);
    }

    // centered wrap (keeps text inside the window)
    private String[] wrapTextCentered(String text, float maxWidth, int maxLines){
        String[] out = new String[maxLines];
        String[] words = text.replace("\n"," ").split("\\s+");

        StringBuilder line = new StringBuilder();
        int idx = 0;

        for (String w : words){
            String test = (line.length()==0) ? w : (line + " " + w);
            if (msgPaint.measureText(test) <= maxWidth){
                line.setLength(0);
                line.append(test);
            } else {
                out[idx++] = line.toString();
                if (idx >= maxLines) break;
                line.setLength(0);
                line.append(w);
            }
        }

        if (idx < maxLines && line.length() > 0) out[idx] = line.toString();

        if (idx >= maxLines){
            String last = out[maxLines-1];
            if (last == null) last = "";
            while (last.length() > 0 && msgPaint.measureText(last + "…") > maxWidth){
                last = last.substring(0, last.length()-1);
            }
            out[maxLines-1] = last + "…";
        }

        return out;
    }

    private void spawnSparkles(float cx, float cy){
        for (int i = 0; i < 14; i++){
            Sparkle s = new Sparkle();
            s.x = cx + (rng.nextFloat() - 0.5f) * dp(10);
            s.y = cy + (rng.nextFloat() - 0.5f) * dp(10);

            float ang = rng.nextFloat() * 6.283f;
            float spd = dp(40) + rng.nextFloat() * dp(70);
            s.vx = (float)Math.cos(ang) * spd;
            s.vy = (float)Math.sin(ang) * spd - dp(30);

            s.r = dp(1.2f) + rng.nextFloat() * dp(2.0f);
            s.maxLife = 0.55f + rng.nextFloat() * 0.35f;
            s.life = s.maxLife;

            sparkles.add(s);
        }
    }

    private void drawSparkles(Canvas c){
        if (sparkles.isEmpty()) return;

        for (Sparkle s : sparkles){
            float a = clamp(s.life / s.maxLife, 0f, 1f);
            int alpha = (int)(200 * a);

            sparklePaint.setColor(Color.WHITE);
            sparklePaint.setAlpha(alpha);
            c.drawCircle(s.x, s.y, s.r, sparklePaint);

            sparklePaint.setAlpha((int)(alpha * 0.35f));
            c.drawCircle(s.x, s.y, s.r * 2.2f, sparklePaint);
        }
    }

    // ================= UI updates =================
    private void updateBadge(){
        if (totalScore >= 300) badge = "Sky Keeper";
        else if (totalScore >= 150) badge = "Soft Glow";
        else if (totalScore >= 60) badge = "First Glow";
        else badge = "—";
    }

    private void pushUi(){
        if(uiListener!=null) uiListener.onUpdate(totalScore, badge, hudMessage);
    }

    // ================= Utils =================
    private float dp(float v){ return v*getResources().getDisplayMetrics().density; }
    private float sp(float v){ return v*getResources().getDisplayMetrics().scaledDensity; }
    private float clamp(float v,float a,float b){ return Math.max(a, Math.min(b, v)); }
}