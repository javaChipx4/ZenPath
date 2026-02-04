package com.example.zenpath;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DiaryPagerAdapter extends RecyclerView.Adapter<DiaryPagerAdapter.PageVH> {

    public interface Listener {
        void onSaved(String date);
    }

    private final Context ctx;
    private final ArrayList<String> dates;
    private final ZenPathRepository repo;
    private final Listener listener;

    // Keep last bound holder to read text from current page reliably
    private PageVH lastBound;

    public DiaryPagerAdapter(Context ctx, ArrayList<String> dates, ZenPathRepository repo, Listener listener) {
        this.ctx = ctx;
        this.dates = dates;
        this.repo = repo;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_diary_page, parent, false);
        return new PageVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageVH holder, int position) {
        String date = dates.get(position);

        String text = repo.getJournalTextByDate(date);

        holder.bind(position + 1, date, text);
        lastBound = holder;
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    public boolean saveCurrentPage(int position) {
        if (position < 0 || position >= dates.size()) return false;
        String date = dates.get(position);

        // Try to read from the currently visible holder
        String text = "";
        if (lastBound != null && lastBound.getAdapterPosition() == position) {
            text = lastBound.etJournal.getText().toString().trim();
        }

        if (TextUtils.isEmpty(text)) return false;

        repo.upsertJournalEntry(date, text);

        if (listener != null) listener.onSaved(date);
        return true;
    }

    static class PageVH extends RecyclerView.ViewHolder {
        TextView tvPageMeta;
        EditText etJournal;

        PageVH(@NonNull View itemView) {
            super(itemView);
            tvPageMeta = itemView.findViewById(R.id.tvPageMeta);
            etJournal = itemView.findViewById(R.id.etJournal);
        }

        void bind(int pageNumber, String date, String text) {
            tvPageMeta.setText("Page " + pageNumber + " • " + date);
            etJournal.setText(text);
        }
    }
}
