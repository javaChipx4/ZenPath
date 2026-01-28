package com.example.zenpath;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayVH> {

    public interface Listener {
        void onDayClick(int position, String dateKey, int dayNumber);
    }

    private ArrayList<String> dateKeys;   // yyyyMMdd or ""
    private ArrayList<Integer> dayNums;   // 0 for blank
    private final Listener listener;

    private int selectedPos = -1;

    public CalendarDayAdapter(ArrayList<String> dateKeys,
                              ArrayList<Integer> dayNums,
                              Listener listener) {
        this.dateKeys = dateKeys;
        this.dayNums = dayNums;
        this.listener = listener;
    }

    public void submit(ArrayList<String> newKeys, ArrayList<Integer> newNums) {
        this.dateKeys = newKeys;
        this.dayNums = newNums;
        selectedPos = -1;
        notifyDataSetChanged();
    }

    public void setSelectedPos(int pos) {
        int old = selectedPos;
        selectedPos = pos;
        if (old >= 0) notifyItemChanged(old);
        if (selectedPos >= 0) notifyItemChanged(selectedPos);
    }

    public int findPosByDateKey(String key) {
        if (key == null) return -1;
        for (int i = 0; i < dateKeys.size(); i++) {
            if (key.equals(dateKeys.get(i))) return i;
        }
        return -1;
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
        String key = dateKeys.get(position);

        if (day <= 0 || key == null || key.isEmpty()) {
            holder.tvDay.setText("");
            holder.itemView.setClickable(false);
            holder.itemView.setAlpha(0.25f);
            holder.selBg.setBackgroundColor(0x00000000);
            return;
        }

        holder.itemView.setClickable(true);
        holder.itemView.setAlpha(1.0f);
        holder.tvDay.setText(String.valueOf(day));

        boolean selected = (position == selectedPos);
        holder.selBg.setBackgroundResource(selected ? R.drawable.bg_day_selected : android.R.color.transparent);

        holder.itemView.setOnClickListener(v -> {
            setSelectedPos(position);
            if (listener != null) listener.onDayClick(position, key, day);
        });
    }

    @Override
    public int getItemCount() {
        return dayNums.size();
    }

    static class DayVH extends RecyclerView.ViewHolder {
        TextView tvDay;
        View selBg;

        DayVH(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            selBg = itemView.findViewById(R.id.selBg);
        }
    }
}
