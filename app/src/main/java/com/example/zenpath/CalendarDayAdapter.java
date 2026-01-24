package com.example.zenpath;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayVH> {

    public interface OnDayClickListener {
        void onDayClick(int position, String dateKey, int dayNumber);
    }

    private final ArrayList<String> dateKeys;
    private final ArrayList<Integer> dayNums;
    private final OnDayClickListener listener;

    private int selectedPos = -1;

    public CalendarDayAdapter(ArrayList<String> dateKeys,
                              ArrayList<Integer> dayNums,
                              OnDayClickListener listener) {
        this.dateKeys = dateKeys;
        this.dayNums = dayNums;
        this.listener = listener;
    }

    public void setSelectedPos(int pos) {
        selectedPos = pos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DayVH holder, int position) {
        int day = dayNums.get(position);
        String dateKey = dateKeys.get(position);

        if (day == 0 || dateKey == null || dateKey.isEmpty()) {
            // Blank cell
            holder.tvDay.setText("");
            holder.itemView.setClickable(false);
            holder.itemView.setAlpha(0.15f);
        } else {
            holder.tvDay.setText(String.valueOf(day));
            holder.itemView.setClickable(true);
            holder.itemView.setAlpha(1.0f);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDayClick(position, dateKey, day);
            });
        }

        // Optional simple selected effect (bold + slightly bigger)
        if (position == selectedPos && day != 0) {
            holder.tvDay.setScaleX(1.15f);
            holder.tvDay.setScaleY(1.15f);
        } else {
            holder.tvDay.setScaleX(1.0f);
            holder.tvDay.setScaleY(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return dayNums.size(); // should be 42
    }

    static class DayVH extends RecyclerView.ViewHolder {
        TextView tvDay;

        DayVH(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay); // MUST MATCH item_calendar_day.xml
        }
    }
}
