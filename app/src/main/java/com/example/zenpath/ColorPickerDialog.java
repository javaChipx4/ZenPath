package com.example.zenpath;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class ColorPickerDialog extends DialogFragment {

    public interface Listener {
        void onColorPicked(int color);
    }

    private static final String ARG_COLOR = "arg_color";

    private Listener listener;

    public static ColorPickerDialog newInstance(int startColor) {
        ColorPickerDialog f = new ColorPickerDialog();
        Bundle b = new Bundle();
        b.putInt(ARG_COLOR, startColor);
        f.setArguments(b);
        return f;
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        int startColor = Color.parseColor("#BFD6FF");
        if (getArguments() != null) startColor = getArguments().getInt(ARG_COLOR, startColor);

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_color_picker, null, false);

        View preview = v.findViewById(R.id.colorPreview);
        TextView hexText = v.findViewById(R.id.tvHex);

        SeekBar sbR = v.findViewById(R.id.sbR);
        SeekBar sbG = v.findViewById(R.id.sbG);
        SeekBar sbB = v.findViewById(R.id.sbB);

        sbR.setMax(255);
        sbG.setMax(255);
        sbB.setMax(255);

        sbR.setProgress(Color.red(startColor));
        sbG.setProgress(Color.green(startColor));
        sbB.setProgress(Color.blue(startColor));

        final int[] current = new int[]{ startColor };

        GradientDrawable bg = (GradientDrawable) preview.getBackground();

        Runnable apply = () -> {
            int r = sbR.getProgress();
            int g = sbG.getProgress();
            int b = sbB.getProgress();
            current[0] = Color.rgb(r, g, b);

            bg.setColor(current[0]);
            hexText.setText(String.format("#%02X%02X%02X", r, g, b));
        };

        SeekBar.OnSeekBarChangeListener change = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { apply.run(); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        sbR.setOnSeekBarChangeListener(change);
        sbG.setOnSeekBarChangeListener(change);
        sbB.setOnSeekBarChangeListener(change);

        // quick palette taps
        View[] swatches = new View[]{
                v.findViewById(R.id.sw1),
                v.findViewById(R.id.sw2),
                v.findViewById(R.id.sw3),
                v.findViewById(R.id.sw4),
                v.findViewById(R.id.sw5),
                v.findViewById(R.id.sw6)
        };

        int[] colors = new int[]{
                Color.parseColor("#BFD6FF"), // icy blue
                Color.parseColor("#C6B7E2"), // lavender
                Color.parseColor("#FFF2C6"), // warm cream
                Color.parseColor("#A7C7E7"), // soft blue
                Color.parseColor("#FFB7D5"), // pink
                Color.parseColor("#BFFFEA")  // mint
        };

        for (int i = 0; i < swatches.length; i++) {
            int c = colors[i];
            GradientDrawable d = (GradientDrawable) swatches[i].getBackground();
            d.setColor(c);

            swatches[i].setOnClickListener(vv -> {
                sbR.setProgress(Color.red(c));
                sbG.setProgress(Color.green(c));
                sbB.setProgress(Color.blue(c));
                apply.run();
            });
        }

        apply.run();

        return new AlertDialog.Builder(requireContext())
                .setTitle("Tools: Ink Color")
                .setView(v)
                .setPositiveButton("Apply", (dialog, which) -> {
                    if (listener != null) listener.onColorPicked(current[0]);
                })
                .setNegativeButton("Cancel", null)
                .create();
    }
}