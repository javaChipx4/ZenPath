package com.example.zenpath;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class SettingsDialogFragment extends DialogFragment {

    public interface Listener {
        void onHome();
        void onHistory();
        void onBack();
        void onMood();
    }

    private final Listener listener;

    public SettingsDialogFragment(Listener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.dialog_settings, container, false);

        // Tap outside the card to close
        View overlay = v.findViewById(R.id.overlayRoot);
        View card = v.findViewById(R.id.settingsCard);

        overlay.setOnClickListener(view -> dismiss());
        card.setOnClickListener(view -> { /* stop dismiss when tapping inside */ });

        v.findViewById(R.id.btnHome).setOnClickListener(view -> {
            dismiss();
            if (listener != null) listener.onHome();
        });

        v.findViewById(R.id.btnHistory).setOnClickListener(view -> {
            dismiss();
            if (listener != null) listener.onHistory();
        });

        v.findViewById(R.id.btnBack).setOnClickListener(view -> {
            dismiss();
            if (listener != null) listener.onBack();
        });

        v.findViewById(R.id.btnMood).setOnClickListener(view -> {
            dismiss();
            if (listener != null) listener.onMood();
        });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null && getDialog().getWindow() != null) {
            Window w = getDialog().getWindow();
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
