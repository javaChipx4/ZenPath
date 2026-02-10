package com.example.zenpath;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class GamePauseOverlay {

    public interface Actions {
        void onResume();
        void onRestart();
        void onBack();
        void onExtra(); // optional
    }

    private View overlay;

    public void show(Activity act,
                     int layoutRes,
                     String title,
                     String subtitle,
                     String restartLabel,
                     String extraLabel, // pass null to hide
                     Actions actions) {

        ViewGroup root = act.findViewById(android.R.id.content);

        overlay = LayoutInflater.from(act).inflate(layoutRes, root, false);
        root.addView(overlay);

        overlay.setOnClickListener(v -> {
            if (actions != null) actions.onResume();
            dismiss(act);
        });

        View card = overlay.findViewById(R.id.pauseCard);
        if (card != null) card.setOnClickListener(v -> {});

        TextView tvTitle = overlay.findViewById(R.id.tvPauseTitle);
        TextView tvSub = overlay.findViewById(R.id.tvPauseSubtitle);

        TextView btnResume = overlay.findViewById(R.id.btnResume);
        TextView btnRestart = overlay.findViewById(R.id.btnRestart);
        TextView btnBack = overlay.findViewById(R.id.btnBackToSelection);
        TextView btnExtra = overlay.findViewById(R.id.btnExtra);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvSub != null) tvSub.setText(subtitle);

        if (btnRestart != null && restartLabel != null) btnRestart.setText(restartLabel);

        if (btnExtra != null) {
            if (extraLabel == null || extraLabel.trim().isEmpty()) {
                btnExtra.setVisibility(View.GONE);
            } else {
                btnExtra.setVisibility(View.VISIBLE);
                btnExtra.setText(extraLabel);
            }
        }

        // ✅ Volume wiring
        ImageView imgVolume = overlay.findViewById(R.id.imgVolume);
        SeekBar seekVolume = overlay.findViewById(R.id.seekVolume);

        if (seekVolume != null) {
            float vol = MusicController.loadVolume(act); // 0..1
            int p = (int) (vol * 100f);
            seekVolume.setMax(100);
            seekVolume.setProgress(p);

            if (imgVolume != null) {
                imgVolume.setImageResource(p <= 1
                        ? android.R.drawable.ic_lock_silent_mode
                        : android.R.drawable.ic_lock_silent_mode_off);
            }

            seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float v = progress / 100f;

                    Intent i = new Intent(act, MusicService.class);
                    i.setAction(MusicService.ACTION_SET_VOLUME);
                    i.putExtra(MusicService.EXTRA_VOLUME, v);
                    act.startService(i);

                    if (imgVolume != null) {
                        imgVolume.setImageResource(progress <= 1
                                ? android.R.drawable.ic_lock_silent_mode
                                : android.R.drawable.ic_lock_silent_mode_off);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (btnResume != null) {
            btnResume.setOnClickListener(v -> {
                if (actions != null) actions.onResume();
                dismiss(act);
            });
        }

        if (btnRestart != null) {
            btnRestart.setOnClickListener(v -> {
                if (actions != null) actions.onRestart();
                dismiss(act);
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (actions != null) actions.onBack();
                dismiss(act);
            });
        }

        if (btnExtra != null) {
            btnExtra.setOnClickListener(v -> {
                if (actions != null) actions.onExtra();
                dismiss(act);
            });
        }
    }

    public void dismiss(Activity act) {
        if (overlay == null) return;
        ViewGroup root = act.findViewById(android.R.id.content);
        root.removeView(overlay);
        overlay = null;
    }

    public boolean isShowing() {
        return overlay != null;
    }
}
