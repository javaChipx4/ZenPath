package com.example.zenpath;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.appcompat.widget.AppCompatSeekBar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class StressHistoryFragment extends Fragment {

    private static final String PREFS = "zen_path_prefs";
    private static String stressKey(String dateKey) { return "stress_" + dateKey; }
    private static String playTotalKey(String dateKey) { return "play_ms_" + dateKey; }
    private static String playLastAppKey(String dateKey) { return "play_topapp_" + dateKey; }

    // ✅ MUST match what you pass into new GameTimeTracker("...")
    private static final String[] GAMES = new String[]{
            "Star Sweep",
            "Lantern Release",
            "Planet"
    };

    private SharedPreferences prefs;

    private View[] dots = new View[7]; // Mon..Sun
    private TextView tvSelectedDate, tvStressPercent, tvAppPlayed;
    private AppCompatSeekBar seekStressPreview;
    private View weekRow;

    private Calendar weekStartMon;
    private int selectedIndex = 0;

    // swipe vars
    private float downX = 0f;
    private boolean dragging = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_stress_history, container, false);
        prefs = requireContext().getSharedPreferences(PREFS, 0);

        dots[0] = v.findViewById(R.id.dot0);
        dots[1] = v.findViewById(R.id.dot1);
        dots[2] = v.findViewById(R.id.dot2);
        dots[3] = v.findViewById(R.id.dot3);
        dots[4] = v.findViewById(R.id.dot4);
        dots[5] = v.findViewById(R.id.dot5);
        dots[6] = v.findViewById(R.id.dot6);

        tvSelectedDate = v.findViewById(R.id.tvSelectedDate);
        tvStressPercent = v.findViewById(R.id.tvStressPercent);
        tvAppPlayed = v.findViewById(R.id.tvAppPlayed);
        seekStressPreview = v.findViewById(R.id.seekStressPreview);

        weekRow = v.findViewById(R.id.weekRow);

        Calendar today = Calendar.getInstance();
        weekStartMon = getMonday(today);
        selectedIndex = mondayIndex(today);

        // tap dot selects day
        for (int i = 0; i < 7; i++) {
            final int idx = i;
            if (dots[i] == null) continue;
            dots[i].setOnClickListener(vv -> {
                selectedIndex = idx;
                refresh();
            });
        }

        installWeekSwipe();
        refresh();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh(); // reflect latest saved stress + playtime
    }

    private void refresh() {
        String dk = dateKey(selectedIndex);

        if (tvSelectedDate != null) tvSelectedDate.setText(prettyDate(dk));

        // --- Stress (day)
        int stress = prefs.getInt(stressKey(dk), 50);
        if (seekStressPreview != null) seekStressPreview.setProgress(stress);
        if (tvStressPercent != null) tvStressPercent.setText(stress + "%");

        // --- Playtime (day)
        long totalMsToday = prefs.getLong(playTotalKey(dk), 0L);

        // --- Top 2 games (day)
        List<GameStat> stats = new ArrayList<>();
        for (String g : GAMES) {
            String perGameKey = "play_ms_" + sanitize(g) + "_" + dk;
            long ms = prefs.getLong(perGameKey, 0L);
            if (ms > 0) stats.add(new GameStat(g, ms));
        }
        stats.sort((a, b) -> Long.compare(b.ms, a.ms)); // desc

        String top1 = null, top2 = null;
        long top1Ms = 0, top2Ms = 0;

        if (stats.size() >= 1) { top1 = stats.get(0).name; top1Ms = stats.get(0).ms; }
        if (stats.size() >= 2) { top2 = stats.get(1).name; top2Ms = stats.get(1).ms; }

        // fallback for old data (before per-game saving existed)
        if (top1 == null && totalMsToday > 0) {
            String last = prefs.getString(playLastAppKey(dk), "");
            if (!TextUtils.isEmpty(last)) top1 = last;
        }

        // --- Weekly total playtime (Mon..Sun)
        long weekTotalMs = 0L;
        for (int i = 0; i < 7; i++) {
            String dayKey = dateKey(i);
            weekTotalMs += prefs.getLong(playTotalKey(dayKey), 0L);
        }

        // --- Trend (this week): stress vs play minutes
        TrendResult trend = computeWeeklyTrend();

        // --- Build text into existing tvAppPlayed (no XML change)
        if (tvAppPlayed != null) {
            StringBuilder sb = new StringBuilder();

            if (totalMsToday <= 0) {
                sb.append("No play data for this day.");
            } else {
                sb.append("Top games:\n");
                if (top1 != null) {
                    sb.append("1) ").append(top1);
                    if (top1Ms > 0) sb.append(" — ").append(formatMinutes(top1Ms));
                    sb.append("\n");
                }
                if (top2 != null) {
                    sb.append("2) ").append(top2).append(" — ").append(formatMinutes(top2Ms)).append("\n");
                } else {
                    sb.append("2) —\n");
                }
                sb.append("Total today: ").append(formatMinutes(totalMsToday));
            }

            sb.append("\n\nThis week: ").append(formatHoursMinutes(weekTotalMs));

            sb.append("\nTrend: ").append(trend.message);

            tvAppPlayed.setText(sb.toString());
        }
    }

    // ---------------- Trend logic (week only, skip missing stress days) ----------------

    private TrendResult computeWeeklyTrend() {
        List<Float> xs = new ArrayList<>(); // stress
        List<Float> ys = new ArrayList<>(); // play minutes

        for (int i = 0; i < 7; i++) {
            String dk = dateKey(i);

            // ✅ skip missing stress days
            if (!prefs.contains(stressKey(dk))) continue;

            // play minutes can be 0 (still valid as "didn't play")
            int stress = prefs.getInt(stressKey(dk), 50);
            long playMs = prefs.getLong(playTotalKey(dk), 0L);

            xs.add((float) stress);
            ys.add((float) (playMs / 60000f)); // minutes
        }

        int n = xs.size();
        if (n < 3) {
            return new TrendResult("Not enough data this week (need 3+ days with stress saved).");
        }

        float r = pearson(xs, ys);

        // interpret
        float abs = Math.abs(r);
        String strength;
        if (abs < 0.25f) strength = "No clear relationship";
        else if (abs < 0.60f) strength = "Mild relationship";
        else strength = "Strong relationship";

        String direction;
        if (abs < 0.25f) direction = "this week";
        else if (r > 0) direction = "higher stress ↔ more playtime";
        else direction = "higher stress ↔ less playtime";

        String msg;
        if (abs < 0.25f) {
            msg = strength + " " + direction + " (" + n + " days).";
        } else {
            msg = strength + ": " + direction + " (" + n + " days).";
        }

        return new TrendResult(msg);
    }

    private float pearson(List<Float> xs, List<Float> ys) {
        int n = xs.size();
        float sumX = 0, sumY = 0;
        for (int i = 0; i < n; i++) {
            sumX += xs.get(i);
            sumY += ys.get(i);
        }
        float meanX = sumX / n;
        float meanY = sumY / n;

        float num = 0, denX = 0, denY = 0;
        for (int i = 0; i < n; i++) {
            float dx = xs.get(i) - meanX;
            float dy = ys.get(i) - meanY;
            num += dx * dy;
            denX += dx * dx;
            denY += dy * dy;
        }

        float den = (float) Math.sqrt(denX * denY);
        if (den == 0) return 0f;
        return num / den;
    }

    // ---------------- Swipe week ----------------

    private void installWeekSwipe() {
        if (weekRow == null) return;

        weekRow.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    dragging = true;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!dragging) return false;
                    dragging = false;

                    float dx = event.getRawX() - downX;
                    float threshold = dp(60);
                    if (Math.abs(dx) < threshold) return true;

                    if (dx < 0) weekStartMon.add(Calendar.DAY_OF_MONTH, 7);   // next week
                    else weekStartMon.add(Calendar.DAY_OF_MONTH, -7);         // prev week

                    refresh();
                    return true;
            }
            return false;
        });
    }

    // ---------------- Date helpers ----------------

    // Monday of week (Mon-first)
    private Calendar getMonday(Calendar c) {
        Calendar cal = (Calendar) c.clone();
        int d = cal.get(Calendar.DAY_OF_WEEK);
        cal.add(Calendar.DAY_OF_MONTH, d == Calendar.SUNDAY ? -6 : Calendar.MONDAY - d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    // 0..6 (Mon..Sun)
    private int mondayIndex(Calendar c) {
        int d = c.get(Calendar.DAY_OF_WEEK);
        return d == Calendar.SUNDAY ? 6 : d - Calendar.MONDAY;
    }

    private String dateKey(int idx) {
        Calendar c = (Calendar) weekStartMon.clone();
        c.add(Calendar.DAY_OF_MONTH, idx);
        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(c.getTime());
    }

    private String prettyDate(String dk) {
        try {
            return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(dk));
        } catch (Exception e) { return dk; }
    }

    private String formatMinutes(long ms) {
        long min = Math.max(1, ms / 60000L);
        return min + " min";
    }

    private String formatHoursMinutes(long ms) {
        long totalMin = ms / 60000L;
        long h = totalMin / 60L;
        long m = totalMin % 60L;

        if (totalMin <= 0) return "0 min";
        if (h <= 0) return m + " min";
        if (m == 0) return h + " hr";
        return h + " hr " + m + " min";
    }

    private int dp(int dp) {
        float d = requireContext().getResources().getDisplayMetrics().density;
        return (int) (dp * d);
    }

    private String sanitize(String s) {
        if (s == null) return "game";
        return s.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    // ---------------- Small structs ----------------

    private static class GameStat {
        final String name;
        final long ms;
        GameStat(String name, long ms) { this.name = name; this.ms = ms; }
    }

    private static class TrendResult {
        final String message;
        TrendResult(String message) { this.message = message; }
    }
}
