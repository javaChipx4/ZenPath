package com.example.zenpath;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class GamePlayAdapter extends RecyclerView.Adapter<GamePlayAdapter.VH> {

    public static class GameRow {
        public final String name;
        public final long minutes;
        @DrawableRes public final int iconRes;

        public GameRow(String name, long minutes, int iconRes) {
            this.name = name;
            this.minutes = minutes;
            this.iconRes = iconRes;
        }
    }

    private final List<GameRow> items = new ArrayList<>();

    public void submit(List<GameRow> rows) {
        items.clear();
        if (rows != null) items.addAll(rows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_game_playtime, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        GameRow r = items.get(position);
        h.img.setImageResource(r.iconRes);
        h.tvName.setText(r.name);
        h.tvMin.setText(r.minutes + " min");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvName, tvMin;

        VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgGame);
            tvName = itemView.findViewById(R.id.tvGameName);
            tvMin = itemView.findViewById(R.id.tvGameMin);
        }
    }
}
