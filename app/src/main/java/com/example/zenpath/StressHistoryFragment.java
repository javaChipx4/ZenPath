package com.example.zenpath;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatSeekBar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    private TextView tvSelectedDate, tvStressPercent;

    // ✅ Week nav
    private TextView tvWeekLabel;
    private View btnPrevWeek, btnNextWeek;

    private AppCompatSeekBar seekStressPreview;

    // ✅ NEW: game rows UI
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

        dots[0] = v.findViewById(R.id.dot0);
        dots[1] = v.findViewById(R.id.dot1);
        dots[2] = v.findViewById(R.id.dot2);
        dots[3] = v.findViewById(R.id.dot3);
        dots[4] = v.findViewById(R.id.dot4);
        dots[5] = v.findViewById(R.id.dot5);
        dots[6] = v.findViewById(R.id.dot6);

        tvSelectedDate = v.findViewById(R.id.tvSelectedDate);
        tvStressPercent = v.findViewById(R.id.tvStressPercent);
        seekStressPreview = v.findViewById(R.id.seekStressPreview);

        // week nav
        btnPrevWeek = v.findViewById(R.id.btnPrevWeek);
        btnNextWeek = v.findViewById(R.id.btnNextWeek);
        tvWeekLabel = v.findViewById(R.id.tvWeekLabel);

        // ✅ NEW: bind game list views
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

        // set static labels (safe)
        if (tvStarName != null) tvStarName.setText("Star Sweep");
        if (tvLanternName != null) tvLanternName.setText("Lantern Release");
        if (tvPlanetName != null) tvPlanetName.setText("Planet");

        Calendar today = Calendar.getInstance();
        weekStartMon = getMonday(today);
        selectedIndex = mondayIndex(today);

        // dot click
        for (int i = 0; i < 7; i++) {
            final int idx = i;
            if (dots[i] == null) continue;
            dots[i].setOnClickListener(vv -> {
                selectedIndex = idx;
                refresh();
            });
        }

        // week nav buttons
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

        // week label
        if (tvWeekLabel != null) {
            Calendar end = (Calendar) weekStartMon.clone();
            end.add(Calendar.DAY_OF_MONTH, 6);
            tvWeekLabel.setText(shortDate(weekStartMon) + " – " + shortDate(end));
        }

        // dot states (selected / filled / empty)
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

        // selected date text
        if (tvSelectedDate != null) tvSelectedDate.setText(prettyDate(dk));

        // stress preview
        int stress = prefs.getInt(stressKey(dk), 50);
        if (seekStressPreview != null) seekStressPreview.setProgress(stress);
        if (tvStressPercent != null) tvStressPercent.setText(stress + "%");

        // play totals (day)
        long totalMsToday = prefs.getLong(playTotalKey(dk), 0L);

        // per-game ms (day)
        long starMs = prefs.getLong(perGameKey("Star Sweep", dk), 0L);
        long lanternMs = prefs.getLong(perGameKey("Lantern Release", dk), 0L);
        long planetMs = prefs.getLong(perGameKey("Planet", dk), 0L);

        // fallback (old data): if total exists but no per-game keys, at least show top app text somewhere
        // (we won’t break UI if older prefs exist)
        if (totalMsToday > 0 && starMs == 0 && lanternMs == 0 && planetMs == 0) {
            String last = prefs.getString(playLastAppKey(dk), "");
            // put it into trend line as a tiny hint (optional)
            if (!TextUtils.isEmpty(last) && tvTrend != null) {
                // do nothing heavy; trend will be set below anyway
            }
        }

        // update rows (with icons)
        bindGameRow(
                rowStar, ivStar, tvStarTime,
                R.drawable.bg_constella, starMs
        );
        bindGameRow(
                rowLantern, ivLantern, tvLanternTime,
                R.drawable.bg_lantelle, lanternMs
        );
        bindGameRow(
                rowPlanet, ivPlanet, tvPlanetTime,
                R.drawable.bg_asthera, planetMs
        );

        // weekly total (Mon..Sun)
        long weekTotalMs = 0L;
        for (int i = 0; i < 7; i++) {
            weekTotalMs += prefs.getLong(playTotalKey(dateKey(i)), 0L);
        }

        // trend (week, skip missing stress days)
        TrendResult trend = computeWeeklyTrend();

        // bottom summary
        if (tvTotalToday != null) tvTotalToday.setText("Total today: " + formatHoursMinutes(totalMsToday));
        if (tvThisWeek != null) tvThisWeek.setText("This week: " + formatHoursMinutes(weekTotalMs));
        if (tvTrend != null) tvTrend.setText("Trend: " + trend.message);

        // divider visibility (optional)
        if (dividerGames != null) dividerGames.setVisibility(View.VISIBLE);
    }

    private void bindGameRow(View row, ImageView icon, TextView tvTime, int drawableRes, long ms) {
        if (row == null) return;
        if (icon != null) icon.setImageResource(drawableRes);
        if (tvTime != null) tvTime.setText(formatMinutes0Ok(ms));
    }

    // ✅ dot “filled” if stress saved OR any playtime exists
    private boolean hasStressOrPlay(String dk) {
        boolean hasStress = prefs.contains(stressKey(dk));
        boolean hasPlay = prefs.getLong(playTotalKey(dk), 0L) > 0L;
        return hasStress || hasPlay;
    }

    private String perGameKey(String gameName, String dk) {
        return "play_ms_" + sanitize(gameName) + "_" + dk;
    }

    // ---------------- Trend logic (week only, skip missing stress days) ----------------

    private TrendResult computeWeeklyTrend() {
        List<Float> xs = new ArrayList<>(); // stress
        List<Float> ys = new ArrayList<>(); // play minutes

        for (int i = 0; i < 7; i++) {
            String dk = dateKey(i);

            // skip missing stress days
            if (!prefs.contains(stressKey(dk))) continue;

            int stress = prefs.getInt(stressKey(dk), 50);
            long playMs = prefs.getLong(playTotalKey(dk), 0L);

            xs.add((float) stress);
            ys.add((float) (playMs / 60000f));
        }

        int n = xs.size();
        if (n < 3) {
            return new TrendResult("Not enough data this week (need 3+ days with stress saved).");
        }

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

        String msg;
        if (abs < 0.25f) msg = strength + " " + direction + " (" + n + " days).";
        else msg = strength + ": " + direction + " (" + n + " days).";

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

    private String sanitize(String s) {
        if (s == null) return "game";
        return s.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    // ---------------- Small structs ----------------

    private static class TrendResult {
        final String message;
        TrendResult(String message) { this.message = message; }
    }
}
