package com.example.zenpath;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class MusicService extends Service {

    // Track constants
    public static final int TRACK_MAIN = 0;
    public static final int TRACK_CONSTELLA = 1;
    public static final int TRACK_LANTELLE = 2;
    public static final int TRACK_ASTHERA = 3;

    // Intent actions
    public static final String ACTION_PLAY = "com.example.zenpath.MUSIC_PLAY";
    public static final String ACTION_STOP = "com.example.zenpath.MUSIC_STOP";
    public static final String ACTION_SET_VOLUME = "com.example.zenpath.MUSIC_SET_VOLUME";

    public static final String EXTRA_TRACK = "track";
    public static final String EXTRA_VOLUME = "volume"; // 0f..1f

    private MediaPlayer player;
    private int currentTrack = -1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopPlayer();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_SET_VOLUME.equals(action)) {
            float vol = intent.getFloatExtra(EXTRA_VOLUME, 0.6f);
            applyVolume(vol);
            // also persist
            MusicController.saveVolume(this, vol);
            return START_STICKY;
        }

        if (ACTION_PLAY.equals(action)) {
            int track = intent.getIntExtra(EXTRA_TRACK, TRACK_MAIN);
            playTrack(track);
            return START_STICKY;
        }

        return START_STICKY;
    }

    private void playTrack(int track) {
        if (track == currentTrack && player != null) {
            // just re-apply volume (in case settings changed)
            applyVolume(MusicController.loadVolume(this));
            return;
        }

        currentTrack = track;
        stopPlayer();

        int resId = trackToRes(track);
        if (resId == 0) return;

        player = MediaPlayer.create(this, resId);
        if (player == null) return;

        player.setLooping(true);
        applyVolume(MusicController.loadVolume(this));
        player.start();
    }

    private int trackToRes(int track) {
        switch (track) {
            case TRACK_MAIN: return R.raw.bg_main;
            case TRACK_CONSTELLA: return R.raw.bg_constella;
            case TRACK_LANTELLE: return R.raw.bg_lantelle;
            case TRACK_ASTHERA: return R.raw.bg_asthera;
            default: return R.raw.bg_main;
        }
    }

    private void applyVolume(float vol) {
        if (player == null) return;
        if (vol < 0f) vol = 0f;
        if (vol > 1f) vol = 1f;
        player.setVolume(vol, vol);
    }

    private void stopPlayer() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
            }
        } catch (Exception ignored) {}
        player = null;
    }

    @Override
    public void onDestroy() {
        stopPlayer();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
