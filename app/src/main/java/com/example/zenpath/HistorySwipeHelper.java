package com.example.zenpath;

import android.app.Activity;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class HistorySwipeHelper {

    // tuning
    private static final int MIN_SWIPE_DP = 70;     // distance to trigger if slow
    private static final int MIN_FLING_DP = 800;    // velocity to trigger if fast
    private static final long ANIM_MS = 180;        // snap speed
    private static final long COOLDOWN_MS = 280;    // avoid double launch

    public static void attach(Activity activity, View swipeView,
                              Class<?> prevActivity, Class<?> nextActivity) {

        final float density = activity.getResources().getDisplayMetrics().density;
        final float MIN_SWIPE_PX = MIN_SWIPE_DP * density;
        final float MIN_FLING_PX = MIN_FLING_DP * density;

        final long[] lastNav = {0};

        final float[] downX = {0};
        final float[] startTx = {0};
        final boolean[] dragging = {false};

        final VelocityTracker[] vt = {null};

        swipeView.setClickable(true);

        swipeView.setOnTouchListener((v, e) -> {

            switch (e.getActionMasked()) {

                case MotionEvent.ACTION_DOWN: {
                    downX[0] = e.getRawX();
                    startTx[0] = v.getTranslationX();
                    dragging[0] = false;

                    if (vt[0] != null) vt[0].recycle();
                    vt[0] = VelocityTracker.obtain();
                    vt[0].addMovement(e);

                    v.animate().cancel();
                    return true;
                }

                case MotionEvent.ACTION_MOVE: {
                    if (vt[0] != null) vt[0].addMovement(e);

                    float dx = e.getRawX() - downX[0];

                    // start dragging only after small threshold so taps still work
                    if (!dragging[0] && Math.abs(dx) > (10 * density)) {
                        dragging[0] = true;
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }

                    if (dragging[0]) {
                        // Only move the TAB ROW (not whole screen)
                        // Clamp so it doesn't fly too far
                        float max = 80f * density;
                        float clamped = Math.max(-max, Math.min(max, dx));
                        v.setTranslationX(clamped);
                        v.setAlpha(1f - (Math.abs(clamped) / (max * 3f))); // subtle fade
                    }
                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {

                    if (vt[0] != null) {
                        vt[0].addMovement(e);
                        vt[0].computeCurrentVelocity(1000);
                    }

                    float dx = e.getRawX() - downX[0];
                    float vx = (vt[0] != null) ? vt[0].getXVelocity() : 0f;

                    if (vt[0] != null) {
                        vt[0].recycle();
                        vt[0] = null;
                    }

                    // If not dragging, let buttons receive click normally
                    if (!dragging[0]) {
                        v.performClick();
                        return false;
                    }

                    // Always snap tabRow back visually
                    v.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(ANIM_MS)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();

                    // cooldown so it doesn't launch twice
                    long now = System.currentTimeMillis();
                    if (now - lastNav[0] < COOLDOWN_MS) return true;

                    boolean isFling = Math.abs(vx) > MIN_FLING_PX;
                    boolean isSwipe = Math.abs(dx) > MIN_SWIPE_PX;

                    if (!isFling && !isSwipe) return true;

                    // LEFT => next, RIGHT => prev
                    if (dx < 0) {
                        if (nextActivity != null) {
                            lastNav[0] = now;
                            go(activity, nextActivity, true);
                        }
                    } else {
                        if (prevActivity != null) {
                            lastNav[0] = now;
                            go(activity, prevActivity, false);
                        }
                    }
                    return true;
                }
            }

            return false;
        });
    }

    private static void go(Activity activity, Class<?> target, boolean leftToRight) {
        Intent i = new Intent(activity, target);
        activity.startActivity(i);

        if (leftToRight) {
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
        activity.finish();
    }
}
