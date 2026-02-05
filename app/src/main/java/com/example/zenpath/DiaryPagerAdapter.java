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
    private final String date;                 // ✅ one date
    private final ArrayList<String> pages;     // ✅ multiple pages
    private final ZenPathRepository repo;
    private final Listener listener;

    // Keep last bound holder (for current visible page typing)
    private PageVH lastBound;

    public DiaryPagerAdapter(Context ctx, String date, ArrayList<String> pages,
                             ZenPathRepository repo, Listener listener) {
        this.ctx = ctx;
        this.date = date;
        this.pages = pages;
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
        String text = pages.get(position);
        holder.bind(position + 1, text);
        lastBound = holder;
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    // ✅ Add a new blank page
    public void addNewPage() {
        // save current page text into list before adding, so nothing is lost
        syncVisibleToList();

        pages.add("");
        notifyItemInserted(pages.size() - 1);
    }

    // ✅ Save ALL pages as one entry for the date
    public boolean saveAllPages() {
        syncVisibleToList();

        // Remove empty pages at the end (but keep at least 1)
        while (pages.size() > 1) {
            String last = pages.get(pages.size() - 1);
            if (!TextUtils.isEmpty(safeTrim(last))) break;
            pages.remove(pages.size() - 1);
        }

        // Check if there's any real content
        boolean hasAny = false;
        for (String p : pages) {
            if (!TextUtils.isEmpty(safeTrim(p))) { hasAny = true; break; }
        }
        if (!hasAny) return false;

        repo.upsertDiaryPages(date, pages);

        if (listener != null) listener.onSaved(date);
        return true;
    }

    private void syncVisibleToList() {
        if (lastBound == null) return;
        int pos = lastBound.getAdapterPosition();
        if (pos == RecyclerView.NO_POSITION) return;
        if (pos < 0 || pos >= pages.size()) return;

        pages.set(pos, lastBound.etJournal.getText().toString());
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    static class PageVH extends RecyclerView.ViewHolder {
        TextView tvPageMeta;
        EditText etJournal;

        PageVH(@NonNull View itemView) {
            super(itemView);
            tvPageMeta = itemView.findViewById(R.id.tvPageMeta);
            etJournal = itemView.findViewById(R.id.etJournal);
        }

        void bind(int pageNumber, String text) {
            // ✅ no date here (history already has date)
            tvPageMeta.setText("Page " + pageNumber);
            etJournal.setText(text);
        }
    }
}
