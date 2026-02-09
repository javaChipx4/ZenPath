package com.example.zenpath;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ZoomSpaceView extends View {

    public enum Body {
        SUN, MOON,
        MERCURY, VENUS, EARTH, MARS, JUPITER, SATURN, URANUS, NEPTUNE
    }

    public enum Mode { MOVE, STARS, SUN, MOON, MARKER, ERASER, PLANET }
    private Mode mode = Mode.MOVE;

    private Body selectedBody = Body.MERCURY;

    private Bitmap sunBmp, moonBmp, starBmp;

    // ✅ NOW: true orbit animation (revolve around sun)
    private boolean planetAnimationEnabled = false;

    private Bitmap mercuryBmp, venusBmp, earthBmp, marsBmp, jupiterBmp, saturnBmp, uranusBmp, neptuneBmp;

    private static final float SPACE_W = 2200f;
    private static final float SPACE_H = 3800f;

    private final Matrix worldToScreen = new Matrix();
    private final Matrix screenToWorld = new Matrix();

    private float scale = 1f;
    private float minScale = 1f;
    private float maxScale = 4.0f;

    private float offsetX = 0f;
    private float offsetY = 0f;

    private ScaleGestureDetector scaleDetector;
    private float lastX, lastY;
    private boolean isPanning = false;

    private final Paint pLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<PointF> stars = new ArrayList<>();
    private final List<Line> links = new ArrayList<>();
    private int selectedStar = -1;

    private final Paint pBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private LinearGradient bgGrad = null;

    private final Paint pBgStar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<BgStar> bgStars = new ArrayList<>();
    private long lastFrameTime = 0L;

    private PointF sunPos = null;
    private PointF moonPos = null;

    private final ArrayList<PlanetInstance> planets = new ArrayList<>();

    private final Paint pMoonGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSunGlow  = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float sunPhaseA, sunPhaseB, moonPhaseA, moonPhaseB;

    private int inkColor = Color.parseColor("#BFD6FF");
    private int markerSizeProgress = 35;

    private Bitmap dustBitmap;
    private Canvas dustCanvas;

    private final Paint pDustCore = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pDustGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pDustClear = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final ArrayList<Stroke> strokes = new ArrayList<>();
    private final ArrayList<Action> undo = new ArrayList<>();

    private final Random brushRand = new Random();
    private PointF lastBrush = null;

    private enum DragTarget { NONE, SUN, MOON, STAR, PLANET }
    private DragTarget dragTarget = DragTarget.NONE;
    private int activeStarIndex = -1;
    private int activePlanetIndex = -1;

    public ZoomSpaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // ✅ SAFE bitmap loading by name
        sunBmp = loadBitmapByName("sun");
        moonBmp = loadBitmapByName("moon");
        mercuryBmp = loadBitmapByName("mercury");
        venusBmp   = loadBitmapByName("venus");
        earthBmp   = loadBitmapByName("earth");
        marsBmp    = loadBitmapByName("mars");
        jupiterBmp = loadBitmapByName("jupiter");
        saturnBmp  = loadBitmapByName("saturn");
        uranusBmp  = loadBitmapByName("uranus");
        neptuneBmp = loadBitmapByName("neptune");

        Bitmap rawStar = loadBitmapByName("star");
        starBmp = trimTransparent(rawStar);

        pLine.setStrokeWidth(dp(2.2f));
        pLine.setAlpha(220);
        applyInkColorToPaints();

        pRing.setStyle(Paint.Style.STROKE);
        pRing.setStrokeWidth(dp(2));
        pRing.setColor(Color.WHITE);
        pRing.setAlpha(230);

        pBgStar.setStyle(Paint.Style.FILL);
        pBgStar.setColor(Color.WHITE);

        pMoonGlow.setStyle(Paint.Style.FILL);
        pMoonGlow.setMaskFilter(new BlurMaskFilter(dp(38), BlurMaskFilter.Blur.NORMAL));

        pSunGlow.setStyle(Paint.Style.FILL);
        pSunGlow.setMaskFilter(new BlurMaskFilter(dp(38), BlurMaskFilter.Blur.NORMAL));

        pDustCore.setStyle(Paint.Style.FILL);
        pDustCore.setFilterBitmap(true);

        pDustGlow.setStyle(Paint.Style.FILL);
        pDustGlow.setMaskFilter(new BlurMaskFilter(dp(10), BlurMaskFilter.Blur.NORMAL));

        pDustClear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        pDustClear.setAntiAlias(true);

        setLayerType(LAYER_TYPE_SOFTWARE, null);

        Random r = new Random();
        sunPhaseA = r.nextFloat() * 10f;
        sunPhaseB = r.nextFloat() * 10f;
        moonPhaseA = r.nextFloat() * 10f;
        moonPhaseB = r.nextFloat() * 10f;

        scaleDetector = new ScaleGestureDetector(getContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float newScale = scale * detector.getScaleFactor();
                        newScale = clamp(newScale, minScale, maxScale);

                        float fx = detector.getFocusX();
                        float fy = detector.getFocusY();

                        PointF before = screenToWorld(fx, fy);

                        scale = newScale;
                        clampOffsetsToSpace();
                        rebuildMatrices();

                        PointF after = screenToWorld(fx, fy);

                        offsetX += (after.x - before.x);
                        offsetY += (after.y - before.y);

                        clampOffsetsToSpace();
                        rebuildMatrices();
                        invalidate();
                        return true;
                    }
                });

        seedBackgroundStars();
        lastFrameTime = SystemClock.uptimeMillis();
        setClickable(true);
    }

    // ✅ SAFE drawable lookup (returns 0 if missing)
    private int getDrawableIdByName(String name) {
        return getResources().getIdentifier(name, "drawable", getContext().getPackageName());
    }

    // ✅ SAFE bitmap decode (returns null if missing)
    private Bitmap loadBitmapByName(String name) {
        int id = getDrawableIdByName(name);
        if (id == 0) return null;
        try {
            return BitmapFactory.decodeResource(getResources(), id);
        } catch (Exception e) {
            return null;
        }
    }

    // -------- public API --------
    public void setMode(Mode m) {
        mode = m;
        lastBrush = null;
        dragTarget = DragTarget.NONE;
        activeStarIndex = -1;
        activePlanetIndex = -1;
        invalidate();
    }

    // ✅ Orbit animation toggle
    public void setPlanetAnimationEnabled(boolean enabled) {
        planetAnimationEnabled = enabled;

        if (enabled) {
            // Initialize orbit parameters for all planets based on current sun position (or space center)
            PointF c = getOrbitCenter();
            for (int i = 0; i < planets.size(); i++) {
                initOrbitForPlanet(planets.get(i), c, i);
            }
        }

        invalidate();
    }

    public void setSelectedBody(Body body) {
        selectedBody = body;
    }

    public Body getSelectedBody() {
        return selectedBody;
    }

    public int getInkColor() { return inkColor; }

    public void setInkColor(int color) {
        inkColor = color;
        applyInkColorToPaints();
        invalidate();
    }

    public void setMarkerSize(int progress) {
        markerSizeProgress = clampInt(progress, 5, 100);
        invalidate();
    }

    public int getMarkerSizeProgress() { return markerSizeProgress; }

    public Bitmap exportBitmap() {
        Bitmap b = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        draw(c);
        return b;
    }

    public String exportStateJson() throws Exception {
        JSONObject root = new JSONObject();

        root.put("inkColor", inkColor);
        root.put("markerSizeProgress", markerSizeProgress);

        if (sunPos != null) {
            JSONObject s = new JSONObject();
            s.put("x", sunPos.x);
            s.put("y", sunPos.y);
            root.put("sun", s);
        } else root.put("sun", JSONObject.NULL);

        if (moonPos != null) {
            JSONObject m = new JSONObject();
            m.put("x", moonPos.x);
            m.put("y", moonPos.y);
            root.put("moon", m);
        } else root.put("moon", JSONObject.NULL);

        JSONArray planetArr = new JSONArray();
        for (PlanetInstance p : planets) {
            JSONObject o = new JSONObject();
            o.put("body", p.body.name());
            // Save the user's base position (not the animated orbital pose)
            o.put("x", p.pos.x);
            o.put("y", p.pos.y);
            planetArr.put(o);
        }
        root.put("planets", planetArr);

        JSONArray starArr = new JSONArray();
        for (PointF p : stars) {
            JSONObject o = new JSONObject();
            o.put("x", p.x);
            o.put("y", p.y);
            starArr.put(o);
        }
        root.put("stars", starArr);

        JSONArray linkArr = new JSONArray();
        for (Line l : links) {
            JSONObject o = new JSONObject();
            o.put("a", l.a);
            o.put("b", l.b);
            linkArr.put(o);
        }
        root.put("links", linkArr);

        JSONArray strokeArr = new JSONArray();
        for (Stroke s : strokes) {
            JSONObject so = new JSONObject();
            so.put("isErase", s.isErase);

            JSONArray dustArr = new JSONArray();
            for (Dust d : s.dust) {
                JSONObject o = new JSONObject();
                o.put("x", d.x);
                o.put("y", d.y);
                o.put("radius", d.radius);
                o.put("coreColor", d.coreColor);
                o.put("glowColor", d.glowColor);
                o.put("coreAlpha", d.coreAlpha);
                o.put("glowAlpha", d.glowAlpha);
                dustArr.put(o);
            }
            so.put("dust", dustArr);

            JSONArray erArr = new JSONArray();
            for (EraseDot e : s.erasers) {
                JSONObject o = new JSONObject();
                o.put("x", e.x);
                o.put("y", e.y);
                o.put("r", e.r);
                erArr.put(o);
            }
            so.put("erasers", erArr);

            strokeArr.put(so);
        }
        root.put("strokes", strokeArr);

        return root.toString();
    }

    public void importStateJson(String json) throws Exception {
        JSONObject root = new JSONObject(json);

        inkColor = root.optInt("inkColor", inkColor);
        markerSizeProgress = root.optInt("markerSizeProgress", markerSizeProgress);
        applyInkColorToPaints();

        stars.clear();
        links.clear();
        strokes.clear();
        planets.clear();
        selectedStar = -1;
        sunPos = null;
        moonPos = null;

        Object sunObj = root.opt("sun");
        if (sunObj instanceof JSONObject) {
            JSONObject s = (JSONObject) sunObj;
            sunPos = new PointF((float) s.optDouble("x", 0), (float) s.optDouble("y", 0));
        }

        Object moonObj = root.opt("moon");
        if (moonObj instanceof JSONObject) {
            JSONObject m = (JSONObject) moonObj;
            moonPos = new PointF((float) m.optDouble("x", 0), (float) m.optDouble("y", 0));
        }

        JSONArray planetArr = root.optJSONArray("planets");
        if (planetArr != null) {
            for (int i = 0; i < planetArr.length(); i++) {
                JSONObject o = planetArr.getJSONObject(i);
                String b = o.optString("body", "");
                Body body = safeBodyFromName(b);
                float x = (float) o.optDouble("x", 0);
                float y = (float) o.optDouble("y", 0);
                if (body != null) planets.add(new PlanetInstance(body, new PointF(x, y)));
            }
        }

        JSONArray starArr = root.optJSONArray("stars");
        if (starArr != null) {
            for (int i = 0; i < starArr.length(); i++) {
                JSONObject o = starArr.getJSONObject(i);
                stars.add(new PointF((float) o.optDouble("x", 0), (float) o.optDouble("y", 0)));
            }
        }

        JSONArray linkArr = root.optJSONArray("links");
        if (linkArr != null) {
            for (int i = 0; i < linkArr.length(); i++) {
                JSONObject o = linkArr.getJSONObject(i);
                links.add(new Line(o.optInt("a", 0), o.optInt("b", 0)));
            }
        }

        JSONArray strokeArr = root.optJSONArray("strokes");
        if (strokeArr != null) {
            for (int i = 0; i < strokeArr.length(); i++) {
                JSONObject so = strokeArr.getJSONObject(i);
                boolean isErase = so.optBoolean("isErase", false);
                Stroke s = new Stroke(isErase);

                JSONArray dustArr = so.optJSONArray("dust");
                if (dustArr != null) {
                    for (int j = 0; j < dustArr.length(); j++) {
                        JSONObject o = dustArr.getJSONObject(j);
                        s.dust.add(new Dust(
                                (float) o.optDouble("x", 0),
                                (float) o.optDouble("y", 0),
                                (float) o.optDouble("radius", 0),
                                o.optInt("coreColor", Color.WHITE),
                                o.optInt("glowColor", Color.WHITE),
                                o.optInt("coreAlpha", 20),
                                o.optInt("glowAlpha", 10)
                        ));
                    }
                }

                JSONArray erArr = so.optJSONArray("erasers");
                if (erArr != null) {
                    for (int j = 0; j < erArr.length(); j++) {
                        JSONObject o = erArr.getJSONObject(j);
                        s.erasers.add(new EraseDot(
                                (float) o.optDouble("x", 0),
                                (float) o.optDouble("y", 0),
                                (float) o.optDouble("r", 0)
                        ));
                    }
                }

                strokes.add(s);
            }
        }

        ensureDustBitmap();
        rebuildDustBitmapFromStrokes();

        undo.clear();
        lastBrush = null;

        // If play is ON, re-init orbits for loaded planets
        if (planetAnimationEnabled) {
            PointF c = getOrbitCenter();
            for (int i = 0; i < planets.size(); i++) initOrbitForPlanet(planets.get(i), c, i);
        }

        invalidate();
    }

    private Body safeBodyFromName(String n) {
        try { return Body.valueOf(n); }
        catch (Exception ignored) { return null; }
    }

    // ✅ Undo (supports planets)
    public void undo() {
        if (undo.isEmpty()) return;

        Action a = undo.remove(undo.size() - 1);
        switch (a.type) {
            case ADD_STAR:
                if (a.starIndex >= 0 && a.starIndex < stars.size()) {
                    removeStarAndLinks(a.starIndex);
                }
                selectedStar = -1;
                break;

            case ADD_LINK:
                if (a.linkIndex >= 0 && a.linkIndex < links.size()) {
                    links.remove(a.linkIndex);
                }
                selectedStar = -1;
                break;

            case SET_SUN:
                sunPos = a.prevPos;
                // If orbit is ON, re-init orbits because center changed
                if (planetAnimationEnabled) reinitAllOrbits();
                break;

            case SET_MOON:
                moonPos = a.prevPos;
                break;

            case ADD_PLANET:
                if (a.planetIndex >= 0 && a.planetIndex < planets.size()) {
                    planets.remove(a.planetIndex);
                } else if (!planets.isEmpty()) {
                    planets.remove(planets.size() - 1);
                }
                break;

            case ADD_STROKE:
                if (!strokes.isEmpty()) {
                    strokes.remove(strokes.size() - 1);
                    rebuildDustBitmapFromStrokes();
                }
                break;

            case CLEAR_OBJECTS:
            case CLEAR_ALL:
                restoreSnapshot(a.snapshot);
                break;
        }
        invalidate();
    }

    public void clearObjectsOnly() {
        Snapshot snap = makeSnapshot(true);
        Action clear = Action.clearObjects(snap);

        stars.clear();
        links.clear();
        planets.clear();
        selectedStar = -1;
        sunPos = null;
        moonPos = null;

        undo.clear();
        undo.add(clear);

        invalidate();
    }

    public void clearAllObjects() {
        Snapshot snap = makeSnapshot(true);
        Action clear = Action.clearAll(snap);

        stars.clear();
        links.clear();
        planets.clear();
        selectedStar = -1;
        sunPos = null;
        moonPos = null;

        strokes.clear();
        if (dustCanvas != null) {
            dustCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        lastBrush = null;

        undo.clear();
        undo.add(clear);

        invalidate();
    }

    private void restoreSnapshot(Snapshot s) {
        if (s == null) return;

        stars.clear();
        links.clear();
        strokes.clear();
        planets.clear();

        if (s.sunPos != null) sunPos = new PointF(s.sunPos.x, s.sunPos.y);
        else sunPos = null;

        if (s.moonPos != null) moonPos = new PointF(s.moonPos.x, s.moonPos.y);
        else moonPos = null;

        for (PlanetInstance p : s.planets) planets.add(new PlanetInstance(p.body, new PointF(p.pos.x, p.pos.y)));

        for (PointF p : s.stars) stars.add(new PointF(p.x, p.y));
        for (Line l : s.links) links.add(new Line(l.a, l.b));

        for (Stroke st : s.strokes) strokes.add(st.deepCopy());

        ensureDustBitmap();
        rebuildDustBitmapFromStrokes();

        selectedStar = -1;
        lastBrush = null;

        if (planetAnimationEnabled) reinitAllOrbits();
    }

    private Snapshot makeSnapshot(boolean includeMarker) {
        Snapshot s = new Snapshot();

        s.sunPos = (sunPos == null) ? null : new PointF(sunPos.x, sunPos.y);
        s.moonPos = (moonPos == null) ? null : new PointF(moonPos.x, moonPos.y);

        for (PlanetInstance p : planets) s.planets.add(new PlanetInstance(p.body, new PointF(p.pos.x, p.pos.y)));

        for (PointF p : stars) s.stars.add(new PointF(p.x, p.y));
        for (Line l : links) s.links.add(new Line(l.a, l.b));

        if (includeMarker) {
            for (Stroke st : strokes) s.strokes.add(st.deepCopy());
        }

        return s;
    }

    private void applyInkColorToPaints() {
        pLine.setColor(softenColor(inkColor, 0.78f));
    }

    // -------- matrices --------
    private void computeCoverMinScale() {
        if (getWidth() == 0 || getHeight() == 0) return;
        float cover = Math.max(getWidth() / SPACE_W, getHeight() / SPACE_H);
        minScale = cover;
        if (scale < minScale) scale = minScale;
    }

    private void clampOffsetsToSpace() {
        if (getWidth() == 0 || getHeight() == 0) return;

        float halfW = SPACE_W * 0.5f;
        float halfH = SPACE_H * 0.5f;

        float viewHalfWWorld = (getWidth() * 0.5f) / scale;
        float viewHalfHWorld = (getHeight() * 0.5f) / scale;

        float maxX = Math.max(0, halfW - viewHalfWWorld);
        float maxY = Math.max(0, halfH - viewHalfHWorld);

        offsetX = clamp(offsetX, -maxX, maxX);
        offsetY = clamp(offsetY, -maxY, maxY);
    }

    private void rebuildMatrices() {
        worldToScreen.reset();
        float cx = getWidth() * 0.5f;
        float cy = getHeight() * 0.5f;

        worldToScreen.postTranslate(-offsetX, -offsetY);
        worldToScreen.postScale(scale, scale);
        worldToScreen.postTranslate(cx, cy);

        worldToScreen.invert(screenToWorld);
    }

    private PointF screenToWorld(float sx, float sy) {
        float[] pts = new float[]{sx, sy};
        screenToWorld.mapPoints(pts);
        return new PointF(pts[0], pts[1]);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        computeCoverMinScale();
        clampOffsetsToSpace();
        rebuildMatrices();

        bgGrad = new LinearGradient(
                0, 0, 0, h,
                Color.parseColor("#070716"),
                Color.parseColor("#1A1030"),
                Shader.TileMode.CLAMP
        );
        pBg.setShader(bgGrad);

        ensureDustBitmap();
        rebuildDustBitmapFromStrokes();
    }

    private void ensureDustBitmap() {
        int bw = (int) SPACE_W;
        int bh = (int) SPACE_H;

        if (dustBitmap == null || dustBitmap.getWidth() != bw || dustBitmap.getHeight() != bh) {
            dustBitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
            dustCanvas = new Canvas(dustBitmap);
            dustCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
    }

    // -------- background stars --------
    private void seedBackgroundStars() {
        bgStars.clear();
        Random r = new Random();

        for (int i = 0; i < 170; i++) {
            BgStar s = new BgStar();
            s.x = (r.nextFloat() - 0.5f) * SPACE_W;
            s.y = (r.nextFloat() - 0.5f) * SPACE_H;
            s.vx = (r.nextFloat() - 0.5f) * 5f;
            s.vy = (r.nextFloat() - 0.5f) * 5f;
            s.r = 1.0f + r.nextFloat() * 2.0f;
            s.a = 70 + r.nextInt(170);
            s.tw = r.nextFloat() * 10f;
            bgStars.add(s);
        }
    }

    private void updateBackgroundStars(float dtSec, float t) {
        float halfW = SPACE_W * 0.5f;
        float halfH = SPACE_H * 0.5f;

        for (BgStar s : bgStars) {
            s.x += s.vx * dtSec;
            s.y += s.vy * dtSec;

            if (s.x < -halfW) s.x = halfW;
            if (s.x >  halfW) s.x = -halfW;
            if (s.y < -halfH) s.y = halfH;
            if (s.y >  halfH) s.y = -halfH;

            float tw = 0.5f + 0.5f * (float)Math.sin(t * 0.9f + s.tw);
            s.curA = clampInt((int)(s.a * (0.55f + 0.45f * tw)), 30, 255);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long now = SystemClock.uptimeMillis();
        float dt = Math.min(0.05f, (now - lastFrameTime) / 1000f);
        lastFrameTime = now;

        float t = now / 1000f;
        updateBackgroundStars(dt, t);

        if (bgGrad != null) canvas.drawRect(0, 0, getWidth(), getHeight(), pBg);
        else canvas.drawColor(Color.parseColor("#070716"));

        // ✅ update orbit angles
        if (planetAnimationEnabled) {
            PointF c = getOrbitCenter();
            for (int i = 0; i < planets.size(); i++) {
                PlanetInstance p = planets.get(i);
                // If sun moved while animating, keep orbit around current sun
                p.orbitCx = c.x;
                p.orbitCy = c.y;
                p.angleRad += p.angularSpeedRad * dt;
            }
        }

        canvas.save();
        canvas.concat(worldToScreen);

        for (BgStar s : bgStars) {
            pBgStar.setAlpha(s.curA);
            canvas.drawCircle(s.x, s.y, (s.r / scale), pBgStar);
        }

        if (dustBitmap != null) {
            float halfW = SPACE_W * 0.5f;
            float halfH = SPACE_H * 0.5f;
            canvas.drawBitmap(dustBitmap, -halfW, -halfH, null);
        }

        for (Line l : links) {
            PointF a = stars.get(l.a);
            PointF b = stars.get(l.b);
            canvas.drawLine(a.x, a.y, b.x, b.y, pLine);
        }

        for (int i = 0; i < stars.size(); i++) {
            PointF s = stars.get(i);

            float starHalf = dp(34) / scale;
            Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            starPaint.setFilterBitmap(true);
            starPaint.setShadowLayer(dp(3) / scale, 0, dp(1) / scale, Color.argb(120, 0, 0, 0));
            drawBitmapCenteredKeepAspect(canvas, starBmp, s.x, s.y, starHalf, starPaint);

            if (i == selectedStar) {
                canvas.drawCircle(s.x, s.y, starHalf * 0.95f, pRing);
            }
        }

        // ✅ draw planets (orbiting when Play is ON)
        for (int i = 0; i < planets.size(); i++) {
            PlanetInstance p = planets.get(i);

            Bitmap bmp = bitmapForBody(p.body);
            if (bmp == null) continue;

            float half = planetHalfSizeDp(p.body) / scale;

            float px = p.pos.x;
            float py = p.pos.y;

            if (planetAnimationEnabled) {
                // Elliptical orbit for nicer feel (more "solar system" vibes)
                float ox = (float) Math.cos(p.angleRad) * p.orbitRx;
                float oy = (float) Math.sin(p.angleRad) * p.orbitRy;

                px = p.orbitCx + ox;
                py = p.orbitCy + oy;

                // subtle "float" wobble (small)
                float drift = dp(6) / scale;
                px += drift * (float) Math.sin(t * 0.7f + p.phaseA);
                py += drift * (float) Math.cos(t * 0.6f + p.phaseB);
            } else {
                // old idle drift (tiny)
                float driftRadius = dp(12) / scale;
                float dx = driftRadius * (float) Math.sin(t * 0.18f + p.phaseA);
                float dy = driftRadius * (float) Math.cos(t * 0.14f + p.phaseB);
                px += dx;
                py += dy;
            }

            Paint planetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            planetPaint.setFilterBitmap(true);
            planetPaint.setShadowLayer(dp(4) / scale, 0, dp(2) / scale, Color.argb(70, 0, 0, 0));

            drawBitmapCenteredKeepAspect(canvas, bmp, px, py, half, planetPaint);
        }

        if (sunPos != null && sunBmp != null) {
            float sunHalf = dp(135) / scale;

            float dx = 18f * (float) Math.sin(t * 0.18f + sunPhaseA);
            float dy = 12f * (float) Math.sin(t * 0.14f + sunPhaseB);

            float sx = sunPos.x + dx;
            float sy = sunPos.y + dy;

            float glowR = sunHalf * 0.55f;
            int glowA = (int) (70 + 20 * (0.5f + 0.5f * Math.sin(t * 0.45f + sunPhaseA)));
            pSunGlow.setColor(Color.argb(glowA, 255, 185, 90));
            canvas.drawCircle(sx, sy, glowR, pSunGlow);

            pSunGlow.setColor(Color.argb((int)(glowA * 0.5f), 255, 170, 70));
            canvas.drawCircle(sx, sy, glowR * 1.35f, pSunGlow);

            Paint sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            sunPaint.setFilterBitmap(true);
            sunPaint.setShadowLayer(dp(7) / scale, 0, dp(2) / scale, Color.argb(90, 0, 0, 0));
            drawBitmapCenteredKeepAspect(canvas, sunBmp, sx, sy, sunHalf, sunPaint);
        }

        if (moonPos != null && moonBmp != null) {
            float moonHalf = dp(125) / scale;

            float dx = 14f * (float) Math.sin(t * 0.16f + moonPhaseA);
            float dy = 10f * (float) Math.sin(t * 0.12f + moonPhaseB);

            float mx = moonPos.x + dx;
            float my = moonPos.y + dy;

            float haloR = moonHalf * 0.50f;
            int haloA = (int) (60 + 18 * (0.5f + 0.5f * Math.sin(t * 0.35f + moonPhaseA)));
            pMoonGlow.setColor(Color.argb(haloA, 140, 170, 255));
            canvas.drawCircle(mx, my, haloR, pMoonGlow);

            pMoonGlow.setColor(Color.argb((int)(haloA * 0.45f), 100, 140, 240));
            canvas.drawCircle(mx, my, haloR * 1.25f, pMoonGlow);

            Paint moonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            moonPaint.setFilterBitmap(true);
            moonPaint.setShadowLayer(dp(7) / scale, 0, dp(2) / scale, Color.argb(90, 0, 0, 0));
            drawBitmapCenteredKeepAspect(canvas, moonBmp, mx, my, moonHalf, moonPaint);
        }

        canvas.restore();

        // ✅ keep animating (background twinkle + orbits)
        postInvalidateOnAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        float sx = event.getX();
        float sy = event.getY();
        PointF w = screenToWorld(sx, sy);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                lastX = sx;
                lastY = sy;
                isPanning = false;

                dragTarget = DragTarget.NONE;
                activeStarIndex = -1;
                activePlanetIndex = -1;

                if (mode == Mode.MOVE && !scaleDetector.isInProgress()) {
                    if (hitSun(w)) { dragTarget = DragTarget.SUN; return true; }
                    if (hitMoon(w)) { dragTarget = DragTarget.MOON; return true; }

                    int hitP = findNearestPlanetIndex(w);
                    if (hitP != -1) {
                        dragTarget = DragTarget.PLANET;
                        activePlanetIndex = hitP;
                        return true;
                    }

                    int hit = findNearestStarIndex(w, dp(70) / scale);
                    if (hit != -1) { dragTarget = DragTarget.STAR; activeStarIndex = hit; return true; }

                    isPanning = true;
                    return true;
                }

                if (mode == Mode.MARKER && !scaleDetector.isInProgress()) {
                    strokes.add(new Stroke(false));
                    undo.add(Action.addStroke());
                    lastBrush = new PointF(w.x, w.y);
                    stampMarker(w.x, w.y, true);
                    invalidate();
                    return true;
                }

                if (mode == Mode.ERASER && !scaleDetector.isInProgress()) {
                    strokes.add(new Stroke(true));
                    undo.add(Action.addStroke());
                    lastBrush = new PointF(w.x, w.y);
                    stampEraser(w.x, w.y, true);
                    invalidate();
                    return true;
                }

                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                if (scaleDetector.isInProgress()) return true;

                if (mode == Mode.MARKER && lastBrush != null) {
                    drawBrushPath(w, false);
                    return true;
                }

                if (mode == Mode.ERASER && lastBrush != null) {
                    drawBrushPath(w, true);
                    return true;
                }

                if (mode == Mode.MOVE) {
                    if (dragTarget == DragTarget.SUN) {
                        sunPos = w;
                        if (planetAnimationEnabled) reinitAllOrbits();
                        invalidate();
                        return true;
                    }
                    if (dragTarget == DragTarget.MOON) {
                        moonPos = w; invalidate(); return true;
                    }
                    if (dragTarget == DragTarget.PLANET && activePlanetIndex >= 0 && activePlanetIndex < planets.size()) {
                        PlanetInstance p = planets.get(activePlanetIndex);
                        p.pos = new PointF(w.x, w.y);

                        // If anim ON, update orbit params based on new position
                        if (planetAnimationEnabled) {
                            PointF c = getOrbitCenter();
                            initOrbitForPlanet(p, c, activePlanetIndex);
                        }

                        invalidate();
                        return true;
                    }
                    if (dragTarget == DragTarget.STAR && activeStarIndex >= 0 && activeStarIndex < stars.size()) {
                        stars.set(activeStarIndex, w); invalidate(); return true;
                    }
                }

                float dx = sx - lastX;
                float dy = sy - lastY;

                offsetX -= dx / scale;
                offsetY -= dy / scale;

                clampOffsetsToSpace();
                rebuildMatrices();
                invalidate();

                lastX = sx;
                lastY = sy;
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (!scaleDetector.isInProgress()) {

                    if (mode == Mode.SUN) {
                        undo.add(Action.setSun(sunPos));
                        sunPos = w;
                        if (planetAnimationEnabled) reinitAllOrbits();
                        invalidate();
                        return true;
                    }

                    if (mode == Mode.MOON) {
                        undo.add(Action.setMoon(moonPos));
                        moonPos = w;
                        invalidate();
                        return true;
                    }

                    if (mode == Mode.PLANET) {
                        if (selectedBody == Body.SUN) {
                            undo.add(Action.setSun(sunPos));
                            sunPos = w;
                            if (planetAnimationEnabled) reinitAllOrbits();
                            invalidate();
                            return true;
                        }
                        if (selectedBody == Body.MOON) {
                            undo.add(Action.setMoon(moonPos));
                            moonPos = w;
                            invalidate();
                            return true;
                        }

                        PlanetInstance inst = new PlanetInstance(selectedBody, new PointF(w.x, w.y));
                        planets.add(inst);
                        undo.add(Action.addPlanet(planets.size() - 1));

                        if (planetAnimationEnabled) {
                            PointF c = getOrbitCenter();
                            initOrbitForPlanet(inst, c, planets.size() - 1);
                        }

                        invalidate();
                        return true;
                    }

                    if (mode == Mode.STARS) {
                        int hit = findNearestStarIndex(w, dp(70) / scale);
                        if (hit != -1) {
                            onStarTapped(hit);
                            return true;
                        }

                        stars.add(w);
                        undo.add(Action.addStar(stars.size() - 1));
                        invalidate();
                        return true;
                    }
                }

                lastBrush = null;
                dragTarget = DragTarget.NONE;
                activeStarIndex = -1;
                activePlanetIndex = -1;
                return true;
            }
        }

        return super.onTouchEvent(event);
    }

    private Bitmap bitmapForBody(Body b) {
        switch (b) {
            case MERCURY: return mercuryBmp;
            case VENUS: return venusBmp;
            case EARTH: return earthBmp;
            case MARS: return marsBmp;
            case JUPITER: return jupiterBmp;
            case SATURN: return saturnBmp;
            case URANUS: return uranusBmp;
            case NEPTUNE: return neptuneBmp;
            default: return null;
        }
    }

    private float planetHalfSizeDp(Body b) {
        switch (b) {
            case MERCURY: return dp(32);
            case VENUS:   return dp(40);
            case EARTH:   return dp(42);
            case MARS:    return dp(36);

            case JUPITER: return dp(62);
            case SATURN:  return dp(58);

            case URANUS:  return dp(52);
            case NEPTUNE: return dp(50);

            default:      return dp(42);
        }
    }

    private int findNearestPlanetIndex(PointF w) {
        for (int i = planets.size() - 1; i >= 0; i--) {
            PlanetInstance p = planets.get(i);
            float r = planetHalfSizeDp(p.body) / scale;
            if (dist2(w, p.pos) <= (r * 1.15f) * (r * 1.15f)) return i;
        }
        return -1;
    }

    private void drawBrushPath(PointF w, boolean erase) {
        if (lastBrush == null) lastBrush = new PointF(w.x, w.y);

        float dx = w.x - lastBrush.x;
        float dy = w.y - lastBrush.y;
        float dist = (float) Math.sqrt(dx*dx + dy*dy);

        float step = 18f;
        int steps = Math.max(1, (int)(dist / step));

        for (int i = 1; i <= steps; i++) {
            float tt = i / (float) steps;
            float px = lastBrush.x + dx * tt;
            float py = lastBrush.y + dy * tt;

            if (erase) stampEraser(px, py, false);
            else stampMarker(px, py, false);
        }

        lastBrush.set(w.x, w.y);
        invalidate();
    }

    private void onStarTapped(int idx) {
        if (selectedStar == -1) {
            selectedStar = idx;
        } else if (selectedStar == idx) {
            selectedStar = -1;
        } else {
            if (!linkExists(selectedStar, idx)) {
                links.add(new Line(selectedStar, idx));
                undo.add(Action.addLink(links.size() - 1));
            }
            selectedStar = -1;
        }
        invalidate();
    }

    private void removeStarAndLinks(int idx) {
        for (int i = links.size() - 1; i >= 0; i--) {
            Line l = links.get(i);
            if (l.a == idx || l.b == idx) {
                links.remove(i);
            }
        }

        for (Line l : links) {
            if (l.a > idx) l.a--;
            if (l.b > idx) l.b--;
        }

        stars.remove(idx);
    }

    private void stampMarker(float x, float y, boolean heavier) {
        ensureDustBitmap();
        if (dustCanvas == null) return;

        if (strokes.isEmpty()) strokes.add(new Stroke(false));
        Stroke stroke = strokes.get(strokes.size() - 1);

        float sizeMul = 0.45f + (markerSizeProgress / 100f) * 1.35f;

        float base = 55f * sizeMul;
        float spread = heavier ? 1.2f : 1.0f;
        int count = (int)((heavier ? 70 : 35) * (0.75f + sizeMul * 0.55f));

        float[] hsv = new float[3];
        Color.colorToHSV(inkColor, hsv);

        float halfW = SPACE_W * 0.5f;
        float halfH = SPACE_H * 0.5f;

        for (int i = 0; i < count; i++) {
            float ox = (float) (brushRand.nextGaussian() * base * spread);
            float oy = (float) (brushRand.nextGaussian() * base * 0.55f * spread);

            float px = x + ox;
            float py = y + oy;

            float r = (6f + brushRand.nextFloat() * (heavier ? 20f : 14f)) * sizeMul;

            float hue = (hsv[0] + (brushRand.nextFloat() * 10f - 5f) + 360f) % 360f;
            float sat = clamp(hsv[1] * (0.35f + brushRand.nextFloat() * 0.45f), 0f, 1f);
            float val = clamp(0.80f + brushRand.nextFloat() * 0.18f, 0f, 1f);

            int core = Color.HSVToColor(new float[]{hue, sat, val});
            int glow = Color.HSVToColor(new float[]{hue, clamp(sat * 0.85f, 0f, 1f), clamp(val * 0.90f, 0f, 1f)});

            int coreA = 18 + brushRand.nextInt(40);
            int glowA = 10 + brushRand.nextInt(22);

            Dust d = new Dust(px, py, r, core, glow, coreA, glowA);
            stroke.dust.add(d);

            float bx = px + halfW;
            float by = py + halfH;

            pDustGlow.setColor(d.glowColor);
            pDustGlow.setAlpha(d.glowAlpha);
            dustCanvas.drawCircle(bx, by, r * 1.35f, pDustGlow);

            pDustCore.setColor(d.coreColor);
            pDustCore.setAlpha(d.coreAlpha);
            dustCanvas.drawCircle(bx, by, r, pDustCore);
        }
    }

    private void stampEraser(float x, float y, boolean heavier) {
        ensureDustBitmap();
        if (dustCanvas == null) return;

        if (strokes.isEmpty()) strokes.add(new Stroke(true));
        Stroke stroke = strokes.get(strokes.size() - 1);

        float sizeMul = 0.55f + (markerSizeProgress / 100f) * 1.65f;
        float er = (heavier ? 55f : 38f) * sizeMul;

        float halfW = SPACE_W * 0.5f;
        float halfH = SPACE_H * 0.5f;

        stroke.erasers.add(new EraseDot(x, y, er));

        float bx = x + halfW;
        float by = y + halfH;

        dustCanvas.drawCircle(bx, by, er, pDustClear);
    }

    private void rebuildDustBitmapFromStrokes() {
        ensureDustBitmap();
        if (dustCanvas == null) return;

        dustCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        float halfW = SPACE_W * 0.5f;
        float halfH = SPACE_H * 0.5f;

        for (Stroke s : strokes) {
            if (!s.isErase) {
                for (Dust d : s.dust) {
                    float bx = d.x + halfW;
                    float by = d.y + halfH;

                    pDustGlow.setColor(d.glowColor);
                    pDustGlow.setAlpha(d.glowAlpha);
                    dustCanvas.drawCircle(bx, by, d.radius * 1.35f, pDustGlow);

                    pDustCore.setColor(d.coreColor);
                    pDustCore.setAlpha(d.coreAlpha);
                    dustCanvas.drawCircle(bx, by, d.radius, pDustCore);
                }
            } else {
                for (EraseDot e : s.erasers) {
                    float bx = e.x + halfW;
                    float by = e.y + halfH;
                    dustCanvas.drawCircle(bx, by, e.r, pDustClear);
                }
            }
        }
    }

    private boolean hitSun(PointF w) {
        if (sunPos == null) return false;
        float r = dp(150) / scale;
        return dist2(w, sunPos) <= r * r;
    }

    private boolean hitMoon(PointF w) {
        if (moonPos == null) return false;
        float r = dp(145) / scale;
        return dist2(w, moonPos) <= r * r;
    }

    private int findNearestStarIndex(PointF w, float radiusWorld) {
        for (int i = stars.size() - 1; i >= 0; i--) {
            PointF s = stars.get(i);
            float dx = w.x - s.x;
            float dy = w.y - s.y;
            if (dx * dx + dy * dy <= radiusWorld * radiusWorld) return i;
        }
        return -1;
    }

    private boolean linkExists(int a, int b) {
        for (Line l : links) {
            if ((l.a == a && l.b == b) || (l.a == b && l.b == a)) return true;
        }
        return false;
    }

    private float dist2(PointF a, PointF b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private float clamp(float v, float mn, float mx) {
        return Math.max(mn, Math.min(mx, v));
    }

    private int clampInt(int v, int mn, int mx) {
        return Math.max(mn, Math.min(mx, v));
    }

    private int softenColor(int c, float mixToWhite) {
        int r = Color.red(c), g = Color.green(c), b = Color.blue(c);
        int rr = (int) (r + (255 - r) * mixToWhite);
        int gg = (int) (g + (255 - g) * mixToWhite);
        int bb = (int) (b + (255 - b) * mixToWhite);
        return Color.rgb(clamp255(rr), clamp255(gg), clamp255(bb));
    }

    private int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private void drawBitmapCenteredKeepAspect(Canvas c, Bitmap bmp, float x, float y, float halfSize, Paint paintOpt) {
        if (bmp == null) return;

        float bw = bmp.getWidth();
        float bh = bmp.getHeight();
        if (bw <= 0 || bh <= 0) return;

        float aspect = bw / bh;

        float halfW, halfH;
        if (aspect >= 1f) {
            halfW = halfSize;
            halfH = halfSize / aspect;
        } else {
            halfH = halfSize;
            halfW = halfSize * aspect;
        }

        Paint paint = (paintOpt != null) ? paintOpt : new Paint(Paint.ANTI_ALIAS_FLAG);
        if (paintOpt == null) paint.setFilterBitmap(true);

        c.drawBitmap(bmp, null,
                new android.graphics.RectF(x - halfW, y - halfH, x + halfW, y + halfH),
                paint);
    }

    private Bitmap trimTransparent(Bitmap src) {
        if (src == null) return null;

        int w = src.getWidth();
        int h = src.getHeight();

        int minX = w, minY = h, maxX = -1, maxY = -1;
        final int alphaThreshold = 10;

        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            src.getPixels(row, 0, w, 0, y, w, 1);
            for (int x = 0; x < w; x++) {
                int alpha = (row[x] >>> 24);
                if (alpha > alphaThreshold) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) return src;

        int pad = 6;
        minX = Math.max(0, minX - pad);
        minY = Math.max(0, minY - pad);
        maxX = Math.min(w - 1, maxX + pad);
        maxY = Math.min(h - 1, maxY + pad);

        return Bitmap.createBitmap(src, minX, minY, (maxX - minX + 1), (maxY - minY + 1));
    }

    // =========================
    // ✅ ORBIT HELPERS
    // =========================
    private PointF getOrbitCenter() {
        // Orbit around sun if placed. Otherwise orbit around space center (0,0 world).
        if (sunPos != null) return sunPos;
        return new PointF(0f, 0f);
    }

    private void reinitAllOrbits() {
        PointF c = getOrbitCenter();
        for (int i = 0; i < planets.size(); i++) {
            initOrbitForPlanet(planets.get(i), c, i);
        }
    }

    private void initOrbitForPlanet(PlanetInstance p, PointF center, int index) {
        float dx = p.pos.x - center.x;
        float dy = p.pos.y - center.y;

        float r = (float) Math.sqrt(dx * dx + dy * dy);

        // If user dropped planet too close to center, push it out to a nicer orbit
        float minR = dp(220);
        if (r < minR) {
            r = minR + index * dp(70);
            // keep original direction if possible
            float ang = (float) Math.atan2(dy, dx);
            if (Float.isNaN(ang)) ang = (float) (Math.random() * Math.PI * 2);
            p.pos = new PointF(center.x + (float)Math.cos(ang) * r,
                    center.y + (float)Math.sin(ang) * r);
            dx = p.pos.x - center.x;
            dy = p.pos.y - center.y;
        }

        p.orbitCx = center.x;
        p.orbitCy = center.y;

        p.angleRad = (float) Math.atan2(dy, dx);

        // Ellipse (slightly flattened) so it looks more “solar system”
        float ellipse = 0.78f + (index % 4) * 0.05f; // 0.78..0.93
        p.orbitRx = r;
        p.orbitRy = r * ellipse;

        // Speed: inner faster, outer slower
        // base ~ 0.65 rad/s then scaled down by radius
        float rNorm = Math.max(dp(180), r);
        float base = 0.75f; // rad/s-ish
        float speed = base * (dp(280) / rNorm);
        speed = clamp(speed, 0.10f, 0.90f);

        // Alternate direction a bit (looks cute + less uniform)
        if (index % 2 == 1) speed *= -1f;

        p.angularSpeedRad = speed;

        // keep subtle float phases
        p.phaseA = (float) (Math.random() * 10f);
        p.phaseB = (float) (Math.random() * 10f);
    }

    // =========================
    // Models
    // =========================
    private static class Line {
        int a, b;
        Line(int a, int b) { this.a = a; this.b = b; }
    }

    private static class BgStar {
        float x, y;
        float vx, vy;
        float r;
        int a;
        float tw;
        int curA;
    }

    private static class PlanetInstance {
        Body body;
        PointF pos;

        // idle drift phases
        float phaseA;
        float phaseB;

        // ✅ orbit state
        float orbitCx, orbitCy;
        float orbitRx, orbitRy;
        float angleRad;
        float angularSpeedRad;

        PlanetInstance(Body body, PointF pos) {
            this.body = body;
            this.pos = pos;
            phaseA = (float) (Math.random() * 10f);
            phaseB = (float) (Math.random() * 10f);

            orbitCx = 0f;
            orbitCy = 0f;
            orbitRx = 0f;
            orbitRy = 0f;
            angleRad = 0f;
            angularSpeedRad = 0f;
        }
    }

    private static class Dust {
        float x, y;
        float radius;
        int coreColor, glowColor;
        int coreAlpha, glowAlpha;

        Dust(float x, float y, float radius, int coreColor, int glowColor, int coreAlpha, int glowAlpha) {
            this.x = x; this.y = y; this.radius = radius;
            this.coreColor = coreColor; this.glowColor = glowColor;
            this.coreAlpha = coreAlpha; this.glowAlpha = glowAlpha;
        }
    }

    private static class EraseDot {
        float x, y, r;
        EraseDot(float x, float y, float r) { this.x = x; this.y = y; this.r = r; }
    }

    private static class Stroke {
        boolean isErase;
        ArrayList<Dust> dust = new ArrayList<>();
        ArrayList<EraseDot> erasers = new ArrayList<>();
        Stroke(boolean erase) { isErase = erase; }

        Stroke deepCopy() {
            Stroke s = new Stroke(isErase);
            for (Dust d : dust) {
                s.dust.add(new Dust(d.x, d.y, d.radius, d.coreColor, d.glowColor, d.coreAlpha, d.glowAlpha));
            }
            for (EraseDot e : erasers) {
                s.erasers.add(new EraseDot(e.x, e.y, e.r));
            }
            return s;
        }
    }

    private static class Snapshot {
        PointF sunPos;
        PointF moonPos;
        ArrayList<PlanetInstance> planets = new ArrayList<>();
        ArrayList<PointF> stars = new ArrayList<>();
        ArrayList<Line> links = new ArrayList<>();
        ArrayList<Stroke> strokes = new ArrayList<>();
    }

    private enum ActionType { ADD_STAR, ADD_LINK, SET_SUN, SET_MOON, ADD_PLANET, ADD_STROKE, CLEAR_OBJECTS, CLEAR_ALL }

    private static class Action {
        ActionType type;
        int starIndex;
        int linkIndex;
        int planetIndex;
        PointF prevPos;
        Snapshot snapshot;

        static Action addStar(int idx) {
            Action a = new Action();
            a.type = ActionType.ADD_STAR;
            a.starIndex = idx;
            return a;
        }

        static Action addLink(int idx) {
            Action a = new Action();
            a.type = ActionType.ADD_LINK;
            a.linkIndex = idx;
            return a;
        }

        static Action setSun(PointF prev) {
            Action a = new Action();
            a.type = ActionType.SET_SUN;
            a.prevPos = (prev == null) ? null : new PointF(prev.x, prev.y);
            return a;
        }

        static Action setMoon(PointF prev) {
            Action a = new Action();
            a.type = ActionType.SET_MOON;
            a.prevPos = (prev == null) ? null : new PointF(prev.x, prev.y);
            return a;
        }

        static Action addPlanet(int idx) {
            Action a = new Action();
            a.type = ActionType.ADD_PLANET;
            a.planetIndex = idx;
            return a;
        }

        static Action addStroke() {
            Action a = new Action();
            a.type = ActionType.ADD_STROKE;
            return a;
        }

        static Action clearObjects(Snapshot snap) {
            Action a = new Action();
            a.type = ActionType.CLEAR_OBJECTS;
            a.snapshot = snap;
            return a;
        }

        static Action clearAll(Snapshot snap) {
            Action a = new Action();
            a.type = ActionType.CLEAR_ALL;
            a.snapshot = snap;
            return a;
        }
    }
}
