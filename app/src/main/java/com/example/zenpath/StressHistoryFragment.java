package com.example.zenpath;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StressHistoryFragment extends Fragment {

    private static final String PREFS = "zen_path_prefs";
    private static final String KEY_CURRENT_USER = "current_user";

    // old prefs keys (fallback)
    private static String stressKey(String dateKey) { return "stress_" + dateKey; }
    private static String playTotalKey(String dateKey) { return "play_ms_" + dateKey; }
    private static String playLastAppKey(String dateKey) { return "play_topapp_" + dateKey; }
    private static String perGameKey(String gameName, String dk) {
        return "play_ms_" + sanitize(gameName) + "_" + dk;
    }

    private SharedPreferences prefs;
    private ZenPathRepository repo;

    private View[] dots = new View[7]; // Mon..Sun
    private TextView tvSelectedDate, tvStressPercent;

    private TextView tvWeekLabel;
    private View btnPrevWeek, btnNextWeek;

    private ProgressBar progStressPreview;

    private View rowStar, rowLantern, rowPlanet;
    private ImageView ivStar, ivLantern, ivPlanet;
    private TextView tvStarName, tvLanternName, tvPlanetName;
    private TextView tvStarTime, tvLanternTime, tvPlanetTime;

    private View dividerGames;
    private TextView tvTotalToday, tvThisWeek, tvTrend;

    private Calendar weekStartMon;
    private int selectedIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_stress_history, container, false);

        prefs = requireContext().getSharedPreferences(PREFS, 0);
        repo = new ZenPathRepository(requireContext());

        dots[0] = v.findViewById(R.id.dot0);
        dots[1] = v.findViewById(R.id.dot1);
        dots[2] = v.findViewById(R.id.dot2);
        dots[3] = v.findViewById(R.id.dot3);
        dots[4] = v.findViewById(R.id.dot4);
        dots[5] = v.findViewById(R.id.dot5);
        dots[6] = v.findViewById(R.id.dot6);

        tvSelectedDate = v.findViewById(R.id.tvSelectedDate);
        tvStressPercent = v.findViewById(R.id.tvStressPercent);
        progStressPreview = v.findViewById(R.id.progStressPreview);

        btnPrevWeek = v.findViewById(R.id.btnPrevWeek);
        btnNextWeek = v.findViewById(R.id.btnNextWeek);
        tvWeekLabel = v.findViewById(R.id.tvWeekLabel);

        rowStar = v.findViewById(R.id.rowStar);
        rowLantern = v.findViewById(R.id.rowLantern);
        rowPlanet = v.findViewById(R.id.rowPlanet);

        ivStar = v.findViewById(R.id.ivStar);
        ivLantern = v.findViewById(R.id.ivLantern);
        ivPlanet = v.findViewById(R.id.ivPlanet);

        tvStarName = v.findViewById(R.id.tvStarName);
        tvLanternName = v.findViewById(R.id.tvLanternName);
        tvPlanetName = v.findViewById(R.id.tvPlanetName);

        tvStarTime = v.findViewById(R.id.tvStarTime);
        tvLanternTime = v.findViewById(R.id.tvLanternTime);
        tvPlanetTime = v.findViewById(R.id.tvPlanetTime);

        dividerGames = v.findViewById(R.id.dividerGames);
        tvTotalToday = v.findViewById(R.id.tvTotalToday);
        tvThisWeek = v.findViewById(R.id.tvThisWeek);
        tvTrend = v.findViewById(R.id.tvTrend);

        if (tvStarName != null) tvStarName.setText("Star Sweep");
        if (tvLanternName != null) tvLanternName.setText("Lantern Release");
        if (tvPlanetName != null) tvPlanetName.setText("Planet");

        Calendar today = Calendar.getInstance();
        weekStartMon = getMonday(today);
        selectedIndex = mondayIndex(today);

        for (int i = 0; i < 7; i++) {
            final int idx = i;
            if (dots[i] == null) continue;
            dots[i].setOnClickListener(vv -> {
                selectedIndex = idx;
                refresh();
            });
        }

        if (btnPrevWeek != null) btnPrevWeek.setOnClickListener(vv -> {
            weekStartMon.add(Calendar.DAY_OF_MONTH, -7);
            refresh();
        });

        if (btnNextWeek != null) btnNextWeek.setOnClickListener(vv -> {
            weekStartMon.add(Calendar.DAY_OF_MONTH, 7);
            refresh();
        });

        refresh();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (tvWeekLabel != null) {
            Calendar end = (Calendar) weekStartMon.clone();
            end.add(Calendar.DAY_OF_MONTH, 6);
            tvWeekLabel.setText(shortDate(weekStartMon) + " – " + shortDate(end));
        }

        for (int i = 0; i < 7; i++) {
            String dk = dateKey(i);
            boolean hasData = hasStressOrPlay(dk);
            boolean sel = (i == selectedIndex);

            if (dots[i] == null) continue;
            if (sel) dots[i].setBackgroundResource(R.drawable.dot_selected);
            else if (hasData) dots[i].setBackgroundResource(R.drawable.dot_filled);
            else dots[i].setBackgroundResource(R.drawable.dot_empty);
        }

        String dk = dateKey(selectedIndex);
        if (tvSelectedDate != null) tvSelectedDate.setText(prettyDate(dk));

        StressRow row = loadStressRow(dk);

        int stress = row.level;

        if (progStressPreview != null) {
            progStressPreview.setProgress(stress);

            int resId;
            if (stress <= 33) resId = R.drawable.progress_stress_low;
            else if (stress <= 66) resId = R.drawable.progress_stress_med;
            else resId = R.drawable.progress_stress_high;

            progStressPreview.setProgressDrawable(
                    ContextCompat.getDrawable(requireContext(), resId)
            );
        }

        if (tvStressPercent != null) tvStressPercent.setText(stress + "%");

        // --- show per-game times ---
        bindGameRow(rowStar, ivStar, tvStarTime, R.drawable.bg_constella, row.starMs);
        bindGameRow(rowLantern, ivLantern, tvLanternTime, R.drawable.bg_lantelle, row.lanternMs);
        bindGameRow(rowPlanet, ivPlanet, tvPlanetTime, R.drawable.bg_asthera, row.planetMs);

        // totals
        long totalMsToday = row.totalMs;

        long weekTotalMs = 0L;
        for (int i = 0; i < 7; i++) {
            weekTotalMs += loadStressRow(dateKey(i)).totalMs;
        }

        TrendResult trend = computeWeeklyTrend();

        if (tvTotalToday != null) tvTotalToday.setText("Total today: " + formatHoursMinutes(totalMsToday));
        if (tvThisWeek != null) tvThisWeek.setText("This week: " + formatHoursMinutes(weekTotalMs));
        if (tvTrend != null) tvTrend.setText("Trend: " + trend.message);

        if (dividerGames != null) dividerGames.setVisibility(View.VISIBLE);
    }

    private StressRow loadStressRow(String dk) {
        StressRow out = new StressRow();

        long userId = currentUserId();

        // --- DB first ---
        if (userId > 0) {
            ZenPathDbHelper helper = new ZenPathDbHelper(requireContext());
            try (android.database.sqlite.SQLiteDatabase db = helper.getReadableDatabase()) {

                String sql =
                        "SELECT " +
                                ZenPathDbHelper.S_LEVEL + ", " +
                                ZenPathDbHelper.S_PLAY_STAR + ", " +
                                ZenPathDbHelper.S_PLAY_LANTERN + ", " +
                                ZenPathDbHelper.S_PLAY_PLANET +
                                " FROM " + ZenPathDbHelper.T_STRESS +
                                " WHERE " + ZenPathDbHelper.COL_USER_ID + "=? AND " + ZenPathDbHelper.S_DATE + "=? " +
                                " ORDER BY " + ZenPathDbHelper.S_CREATED_AT + " DESC LIMIT 1";

                try (android.database.Cursor c = db.rawQuery(sql,
                        new String[]{ String.valueOf(userId), dk })) {

                    if (c.moveToFirst()) {
                        out.level = c.getInt(0);

                        // DB stores seconds -> convert to ms for your UI formatting
                        int starSec = c.getInt(1);
                        int lanternSec = c.getInt(2);
                        int planetSec = c.getInt(3);

                        out.starMs = starSec * 1000L;
                        out.lanternMs = lanternSec * 1000L;
                        out.planetMs = planetSec * 1000L;

                        out.totalMs = out.starMs + out.lanternMs + out.planetMs;
                        out.hasDb = true;
                    }
                }

            } catch (Exception ignored) {}
        }

        // --- fallback to old prefs ---
        if (!out.hasDb) {
            out.level = prefs.contains(stressKey(dk)) ? prefs.getInt(stressKey(dk), 0) : 0;

            out.totalMs = prefs.getLong(playTotalKey(dk), 0L);
            out.starMs = prefs.getLong(perGameKey("Star Sweep", dk), 0L);
            out.lanternMs = prefs.getLong(perGameKey("Lantern Release", dk), 0L);
            out.planetMs = prefs.getLong(perGameKey("Planet", dk), 0L);

            // old fallback: if only total exists
            if (out.totalMs > 0 && out.starMs == 0 && out.lanternMs == 0 && out.planetMs == 0) {
                String last = prefs.getString(playLastAppKey(dk), "");
                // we keep UI stable; no extra action needed
                if (!TextUtils.isEmpty(last)) { /* optional hint */ }
            }
        }

        return out;
    }

    private void bindGameRow(View row, ImageView icon, TextView tvTime, int drawableRes, long ms) {
        if (row == null) return;
        if (icon != null) icon.setImageResource(drawableRes);
        if (tvTime != null) tvTime.setText(formatMinutes0Ok(ms));
    }

    private boolean hasStressOrPlay(String dk) {
        // DB quick check
        long userId = currentUserId();
        if (userId > 0) {
            ZenPathDbHelper helper = new ZenPathDbHelper(requireContext());
            try (android.database.sqlite.SQLiteDatabase db = helper.getReadableDatabase()) {
                String sql = "SELECT 1 FROM " + ZenPathDbHelper.T_STRESS +
                        " WHERE " + ZenPathDbHelper.COL_USER_ID + "=? AND " + ZenPathDbHelper.S_DATE + "=? LIMIT 1";
                try (android.database.Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(userId), dk })) {
                    if (c.moveToFirst()) return true;
                }
            } catch (Exception ignored) {}
        }

        // prefs fallback
        boolean hasStress = prefs.contains(stressKey(dk));
        boolean hasPlay = prefs.getLong(playTotalKey(dk), 0L) > 0L;
        return hasStress || hasPlay;
    }

    // ---------------- Trend logic (week only, skip missing stress days) ----------------

    private TrendResult computeWeeklyTrend() {
        List<Float> xs = new ArrayList<>(); // stress
        List<Float> ys = new ArrayList<>(); // play minutes

        for (int i = 0; i < 7; i++) {
            String dk = dateKey(i);
            StressRow row = loadStressRow(dk);

            // skip missing stress days (stress not saved)
            // DB: we treat "hasDb" as saved; Prefs: contains(stressKey)
            boolean stressSaved = row.hasDb || prefs.contains(stressKey(dk));
            if (!stressSaved) continue;

            xs.add((float) row.level);
            ys.add((float) (row.totalMs / 60000f));
        }

        int n = xs.size();
        if (n < 3) return new TrendResult("Not enough data this week (need 3+ days with stress saved).");

        float r = pearson(xs, ys);

        float abs = Math.abs(r);
        String strength;
        if (abs < 0.25f) strength = "No clear relationship";
        else if (abs < 0.60f) strength = "Mild relationship";
        else strength = "Strong relationship";

        String direction;
        if (abs < 0.25f) direction = "this week";
        else if (r > 0) direction = "higher stress ↔ more playtime";
        else direction = "higher stress ↔ less playtime";

        String msg = (abs < 0.25f)
                ? (strength + " " + direction + " (" + n + " days).")
                : (strength + ": " + direction + " (" + n + " days).");

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

    // ---------------- Date helpers ----------------

    private long currentUserId() {
        String s = prefs.getString(KEY_CURRENT_USER, null);
        if (s == null) return -1;
        try { return Long.parseLong(s); } catch (Exception e) { return -1; }
    }

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

    private String shortDate(Calendar c) {
        return new SimpleDateFormat("MMM dd", Locale.getDefault()).format(c.getTime());
    }

    private String formatMinutes0Ok(long ms) {
        long min = (ms / 60000L);
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

    private static String sanitize(String s) {
        if (s == null) return "game";
        return s.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private static class StressRow {
        boolean hasDb = false;
        int level = 0;
        long starMs = 0L;
        long lanternMs = 0L;
        long planetMs = 0L;
        long totalMs = 0L;
    }

    private static class TrendResult {
        final String message;
        TrendResult(String message) { this.message = message; }
    }
}
