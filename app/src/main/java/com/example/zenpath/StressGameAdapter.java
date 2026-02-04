package com.example.zenpath;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StressGameAdapter
        extends RecyclerView.Adapter<StressGameAdapter.VH> {

    public static class Item {
        public final String gameName;
        public final int iconRes;
        public final long minutes;

        public Item(String gameName, int iconRes, long minutes) {
            this.gameName = gameName;
            this.iconRes = iconRes;
            this.minutes = minutes;
        }
    }

    private final List<Item> items;

    public StressGameAdapter(List<Item> items) {
        this.items = items;
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
        Item it = items.get(position);
        h.imgGame.setImageResource(it.iconRes);
        h.tvGameName.setText(it.gameName);
        h.tvMinutes.setText(it.minutes + " min");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgGame;
        TextView tvGameName, tvMinutes;

        VH(@NonNull View v) {
            super(v);
            imgGame = v.findViewById(R.id.imgGame);
            tvGameName = v.findViewById(R.id.tvGameName);
            tvMinutes = v.findViewById(R.id.tvMinutes);
        }
    }
}
