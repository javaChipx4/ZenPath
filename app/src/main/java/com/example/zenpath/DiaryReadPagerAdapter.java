package com.example.zenpath;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DiaryReadPagerAdapter extends RecyclerView.Adapter<DiaryReadPagerAdapter.VH> {

    private final ArrayList<String> pages;

    public DiaryReadPagerAdapter(ArrayList<String> pages) {
        this.pages = (pages == null) ? new ArrayList<>() : pages;
        if (this.pages.isEmpty()) this.pages.add("");
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diary_read_page, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String text = pages.get(position);
        if (TextUtils.isEmpty(text)) text = "(Empty page)";
        int chars = text == null ? 0 : text.length();
        h.tvMeta.setText(chars + " characters");
        h.tvBody.setText(text);
        h.tvPageNum.setText("Page " + (position + 1) + " / " + pages.size());
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvBody, tvPageNum;
        TextView tvMeta;
        TextView tvTitleGhost;

        VH(@NonNull View itemView) {
            super(itemView);
            tvBody = itemView.findViewById(R.id.tvReadBody);
            tvPageNum = itemView.findViewById(R.id.tvReadPageNum);
            tvTitleGhost = itemView.findViewById(R.id.tvReadTitleGhost);
            tvMeta = itemView.findViewById(R.id.tvReadMeta);

        }
    }
}
