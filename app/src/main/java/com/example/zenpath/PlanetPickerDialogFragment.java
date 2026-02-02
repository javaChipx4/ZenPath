package com.example.zenpath;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PlanetPickerDialogFragment extends DialogFragment {

    public interface Callback {
        void onPlanetPicked(ZoomSpaceView.Body body);
    }

    private final Callback callback;

    public PlanetPickerDialogFragment(Callback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_planet_picker, null, false);

        RecyclerView rv = v.findViewById(R.id.rvPlanets);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 4));

        ZoomSpaceView.Body[] bodies = new ZoomSpaceView.Body[]{
                ZoomSpaceView.Body.SUN,
                ZoomSpaceView.Body.MOON,
                ZoomSpaceView.Body.MERCURY,
                ZoomSpaceView.Body.VENUS,
                ZoomSpaceView.Body.EARTH,
                ZoomSpaceView.Body.MARS,
                ZoomSpaceView.Body.JUPITER,
                ZoomSpaceView.Body.SATURN,
                ZoomSpaceView.Body.URANUS,
                ZoomSpaceView.Body.NEPTUNE
        };

        String[] labels = new String[]{
                "Sun",
                "Moon",
                "Mercury",
                "Venus",
                "Earth",
                "Mars",
                "Jupiter",
                "Saturn",
                "Uranus",
                "Neptune"
        };

        PlanetOptionAdapter adapter = new PlanetOptionAdapter(
                requireContext(),
                bodies,
                labels,
                body -> {
                    if (callback != null) callback.onPlanetPicked(body);
                    dismiss();
                }
        );

        rv.setAdapter(adapter);

        return new AlertDialog.Builder(requireContext())
                .setView(v)
                .setNegativeButton("Cancel", null)
                .create();
    }
}
