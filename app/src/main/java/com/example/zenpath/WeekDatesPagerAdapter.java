package com.example.zenpath;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class WeekDatesPagerAdapter extends RecyclerView.Adapter<WeekDatesPagerAdapter.VH> {

    public interface OnDateSelected {
        void onSelected(String date); // yyyy-MM-dd
    }

    private static final int CENTER = 500;
    private static final int ITEM_COUNT = 1000;

    private final OnDateSelected callback;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private int selectedPage = CENTER;
    private int selectedIndex = getTodayIndexMonFirst();

    public WeekDatesPagerAdapter(OnDateSelected callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_week_dates_mh, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Calendar monday = getStartOfWeekMonday(position);

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) monday.clone();
            day.add(Calendar.DAY_OF_MONTH, i);

            String dateStr = sdf.format(day.getTime());
            int dayNum = day.get(Calendar.DAY_OF_MONTH);

            TextView tv = h.days[i];
            tv.setText(String.valueOf(dayNum));

            boolean selected = (position == selectedPage && i == selectedIndex);

            if (selected) {
                tv.setBackgroundResource(R.drawable.bg_week_date_selected_mh);
                tv.setTextColor(0xFFFFFFFF);
            } else {
                tv.setBackground(null);
                tv.setTextColor(0xFF1E1E1E);
            }

            final int idx = i;
            tv.setOnClickListener(v -> {
                selectedPage = position;
                selectedIndex = idx;
                notifyDataSetChanged();

                if (callback != null) callback.onSelected(dateStr);
            });
        }
    }

    @Override
    public int getItemCount() {
        return ITEM_COUNT;
    }

    public int getCenter() {
        return CENTER;
    }

    /**
     * Call this after you set the ViewPager current item (center),
     * so your page highlights today AND triggers loading data.
     */
    public void triggerTodaySelection() {
        Calendar monday = getStartOfWeekMonday(selectedPage);
        Calendar day = (Calendar) monday.clone();
        day.add(Calendar.DAY_OF_MONTH, selectedIndex);

        if (callback != null) callback.onSelected(sdf.format(day.getTime()));
        notifyDataSetChanged();
    }

    /**
     * Use this when ViewPager page changes (swipe).
     * It keeps same selectedIndex, moves selectedPage, and triggers callback.
     */
    public void onPageChanged(int newPage) {
        selectedPage = newPage;

        // keep index 0..6 safe
        if (selectedIndex < 0) selectedIndex = 0;
        if (selectedIndex > 6) selectedIndex = 6;

        Calendar monday = getStartOfWeekMonday(selectedPage);
        Calendar day = (Calendar) monday.clone();
        day.add(Calendar.DAY_OF_MONTH, selectedIndex);

        if (callback != null) callback.onSelected(sdf.format(day.getTime()));
        notifyDataSetChanged();
    }

    /**
     * Optional: manually set selection (page + index 0..6).
     */
    public void select(int page, int index) {
        selectedPage = page;
        selectedIndex = Math.max(0, Math.min(6, index));
        notifyDataSetChanged();

        Calendar monday = getStartOfWeekMonday(selectedPage);
        Calendar day = (Calendar) monday.clone();
        day.add(Calendar.DAY_OF_MONTH, selectedIndex);

        if (callback != null) callback.onSelected(sdf.format(day.getTime()));
    }

    // ===================== HELPERS =====================

    private static int getTodayIndexMonFirst() {
        Calendar c = Calendar.getInstance();
        int dow = c.get(Calendar.DAY_OF_WEEK);

        // Mon-first: Mon=0 ... Sat=5, Sun=6
        if (dow == Calendar.SUNDAY) return 6;
        return dow - Calendar.MONDAY;
    }

    /**
     * Returns Monday of the week for a given adapter position.
     */
    private Calendar getStartOfWeekMonday(int position) {
        int weekOffset = position - CENTER;

        Calendar cal = Calendar.getInstance();
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int diff = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);

        cal.add(Calendar.DAY_OF_MONTH, -diff);          // go to Monday of this week
        cal.add(Calendar.DAY_OF_MONTH, weekOffset * 7); // shift by weeks
        return cal;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView[] days = new TextView[7];

        VH(@NonNull View v) {
            super(v);
            days[0] = v.findViewById(R.id.d0);
            days[1] = v.findViewById(R.id.d1);
            days[2] = v.findViewById(R.id.d2);
            days[3] = v.findViewById(R.id.d3);
            days[4] = v.findViewById(R.id.d4);
            days[5] = v.findViewById(R.id.d5);
            days[6] = v.findViewById(R.id.d6);
        }
    }
}
