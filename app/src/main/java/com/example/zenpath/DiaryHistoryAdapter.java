package com.example.zenpath;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DiaryHistoryAdapter extends RecyclerView.Adapter<DiaryHistoryAdapter.VH> {

    public interface OnClick {
        void onClick(ZenPathRepository.DiaryEntryMeta item);
    }

    private ArrayList<ZenPathRepository.DiaryEntryMeta> items;
    private final OnClick onClick;

    public DiaryHistoryAdapter(ArrayList<ZenPathRepository.DiaryEntryMeta> items, OnClick onClick) {
        this.items = items;
        this.onClick = onClick;
    }

    public void setItems(ArrayList<ZenPathRepository.DiaryEntryMeta> newItems) {
        this.items = (newItems == null) ? new ArrayList<>() : newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diary_history_entry, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ZenPathRepository.DiaryEntryMeta item = items.get(position);

        h.tvDate.setText(prettyDate(item.date));
        h.tvPreview.setText(item.preview);

        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvPreview;
        VH(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDiaryHistoryDate);
            tvPreview = itemView.findViewById(R.id.tvDiaryHistoryPreview);
        }
    }

    private String prettyDate(String yyyyMmDd) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(yyyyMmDd);
            return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(d);
        } catch (Exception e) {
            return yyyyMmDd;
        }
    }
}
