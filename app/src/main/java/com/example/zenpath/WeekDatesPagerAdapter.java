package com.example.zenpath;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class WeekDatesPagerAdapter extends RecyclerView.Adapter<WeekDatesPagerAdapter.VH> {

    public interface OnDateSelected {
        void onSelected(String date);
    }

    private static final int CENTER = 500;
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
        int weekOffset = position - CENTER;

        Calendar monday = getStartOfWeekMonday();
        monday.add(Calendar.DAY_OF_MONTH, weekOffset * 7);

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) monday.clone();
            day.add(Calendar.DAY_OF_MONTH, i);

            String dateStr = sdf.format(day.getTime());
            int dayNum = day.get(Calendar.DAY_OF_MONTH);

            h.days[i].setText(String.valueOf(dayNum));

            boolean selected = position == selectedPage && i == selectedIndex;

            h.days[i].setBackground(selected ?
                    h.days[i].getContext().getDrawable(R.drawable.bg_week_date_selected_mh)
                    : null);

            h.days[i].setTextColor(selected ? 0xFFFFFFFF : 0xFF1E1E1E);

            final int idx = i;
            h.days[i].setOnClickListener(v -> {
                selectedPage = position;
                selectedIndex = idx;
                notifyDataSetChanged();
                callback.onSelected(dateStr);
            });
        }
    }

    @Override
    public int getItemCount() {
        return 1000;
    }

    public int getCenter() {
        return CENTER;
    }

    public void select(int page, int index) {
        selectedPage = page;
        selectedIndex = index;
        notifyDataSetChanged();
    }

    private static int getTodayIndexMonFirst() {
        Calendar c = Calendar.getInstance();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SUNDAY) return 6;
        return dow - Calendar.MONDAY;
    }

    private Calendar getStartOfWeekMonday() {
        Calendar cal = Calendar.getInstance();
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int diff = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -diff);
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
