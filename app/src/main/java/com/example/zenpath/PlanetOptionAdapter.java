package com.example.zenpath;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PlanetOptionAdapter extends RecyclerView.Adapter<PlanetOptionAdapter.VH> {

    public interface OnPick {
        void onPick(ZoomSpaceView.Body body);
    }

    private final Context context;
    private final ZoomSpaceView.Body[] bodies;
    private final String[] labels;
    private final OnPick onPick;

    private int selectedPos = RecyclerView.NO_POSITION;

    public PlanetOptionAdapter(Context context,
                               ZoomSpaceView.Body[] bodies,
                               String[] labels,
                               OnPick onPick) {
        this.context = context;
        this.bodies = bodies;
        this.labels = labels;
        this.onPick = onPick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_planet_option, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        h.tvPlanet.setText(labels[position]);

        int icon = getIconRes(bodies[position]);
        if (icon != 0) h.ivPlanet.setImageResource(icon);

        // ✅ selection highlight
        h.root.setSelected(position == selectedPos);

        h.root.setOnClickListener(v -> {
            int old = selectedPos;
            selectedPos = h.getAdapterPosition();

            if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
            notifyItemChanged(selectedPos);

            if (onPick != null) onPick.onPick(bodies[selectedPos]);
        });
    }

    @Override
    public int getItemCount() {
        return bodies.length;
    }

    static class VH extends RecyclerView.ViewHolder {
        View root;
        ImageView ivPlanet;
        TextView tvPlanet;

        VH(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.root);
            ivPlanet = itemView.findViewById(R.id.ivPlanet);
            tvPlanet = itemView.findViewById(R.id.tvPlanet);
        }
    }

    private int getIconRes(ZoomSpaceView.Body body) {
        switch (body) {
            case SUN: return R.drawable.sun;
            case MOON: return R.drawable.moon;
            case MERCURY: return R.drawable.mercury;
            case VENUS: return R.drawable.venus;
            case EARTH: return R.drawable.earth;
            case MARS: return R.drawable.mars;
            case JUPITER: return R.drawable.jupiter;
            case SATURN: return R.drawable.saturn;
            case URANUS: return R.drawable.uranus;
            case NEPTUNE: return R.drawable.neptune;
            default: return 0;
        }
    }
}