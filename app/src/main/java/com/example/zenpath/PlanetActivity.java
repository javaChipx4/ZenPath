package com.example.zenpath;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;

public class PlanetActivity extends AppCompatActivity {

    private static final String PREFS = "asthera_prefs";
    private static final String KEY_STATE = "space_state_v1";

    private ZoomSpaceView spaceView;

    private Button btnMove, btnStars, btnUndo, btnTool, btnMarkerTool, btnEraserTool;
    private Button btnClearAll, btnClearObject;

    private Button btnPlanets;
    private Button btnPlayPlanets;
    private boolean planetsPlaying = false;

    private View toolsPanel;

    private SeekBar sbR, sbG, sbB, sbMarkerSize;
    private View sw1, sw2, sw3, sw4, sw5, sw6;

    private View instructionsOverlay;
    private Button btnStartPlay;

    // ✅ Pause overlay
    private View pauseOverlay;
    private TextView btnResume, btnRestart, btnBackToSelection;

    // ✅ Volume (inside pauseOverlay if present)
    private SeekBar seekVolume;
    private ImageView imgVolume;

    private final int[] swatchColors = new int[]{
            Color.parseColor("#BFD6FF"),
            Color.parseColor("#C6B7E2"),
            Color.parseColor("#A7C7E7"),
            Color.parseColor("#FFF6EC"),
            Color.parseColor("#FFB7D5"),
            Color.parseColor("#BFFFEA")
    };

    private View[] swatches;
    private int selectedSwatchIndex = 0;

    // Keep animation state when pausing
    private boolean planetsPlayingBeforePause = false;

    // ✅ Play time tracker
    private GameTimeTracker playTracker;
    private boolean playTimeSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planet);

        bindViews();
        setupOverlays();
        setupBackHandling();
        setupTopButtons();
        setupModeButtons();
        setupToolButtons();
        setupPlanetButtons();
        setupSwatchesAndSeekBars();

        loadStateFromPrefs();

        // Show instructions first time
        if (savedInstanceState == null && instructionsOverlay != null) {
            if (spaceView != null) spaceView.postDelayed(() -> showInstructionsOverlay(false), 250);
            else showInstructionsOverlay(false);
        }
    }

    private void bindViews() {
        spaceView = findViewById(R.id.spaceView);

        ImageButton btnInfo = findViewById(R.id.btnInfo);
        ImageButton btnSave = findViewById(R.id.btnSave);
        ImageButton menuIcon = findViewById(R.id.menuIcon);

        btnMove = findViewById(R.id.btnMove);
        btnStars = findViewById(R.id.btnStars);
        btnUndo = findViewById(R.id.btnUndo);
        btnTool = findViewById(R.id.btnTool);

        btnPlanets = findViewById(R.id.btnPlanets);
        btnPlayPlanets = findViewById(R.id.btnPlayPlanets);

        btnClearAll = findViewById(R.id.btnClearAll);
        btnClearObject = findViewById(R.id.btnClearObject);

        toolsPanel = findViewById(R.id.toolsPanel);
        btnMarkerTool = findViewById(R.id.btnMarkerTool);
        btnEraserTool = findViewById(R.id.btnEraserTool);

        sw1 = findViewById(R.id.sw1);
        sw2 = findViewById(R.id.sw2);
        sw3 = findViewById(R.id.sw3);
        sw4 = findViewById(R.id.sw4);
        sw5 = findViewById(R.id.sw5);
        sw6 = findViewById(R.id.sw6);
        swatches = new View[]{sw1, sw2, sw3, sw4, sw5, sw6};

        sbR = findViewById(R.id.sbR);
        sbG = findViewById(R.id.sbG);
        sbB = findViewById(R.id.sbB);
        sbMarkerSize = findViewById(R.id.sbMarkerSize);

        instructionsOverlay = findViewById(R.id.instructionsOverlay);
        pauseOverlay = findViewById(R.id.pauseOverlay);

        if (instructionsOverlay != null) btnStartPlay = instructionsOverlay.findViewById(R.id.btnStartPlay);
        else btnStartPlay = findViewById(R.id.btnStartPlay);

        if (btnInfo != null) btnInfo.setOnClickListener(v -> showInstructionsOverlay(true));
        if (btnSave != null) btnSave.setOnClickListener(v -> {
            saveStateToPrefs(false);
            saveDesignToGallery();
        });
        if (menuIcon != null) menuIcon.setOnClickListener(v -> showPause());
    }

    private void setupOverlays() {
        if (btnStartPlay != null) btnStartPlay.setOnClickListener(v -> hideInstructionsOverlay());

        if (pauseOverlay != null) {
            View pauseCard = pauseOverlay.findViewById(R.id.pauseCard);
            btnResume = pauseOverlay.findViewById(R.id.btnResume);
            btnRestart = pauseOverlay.findViewById(R.id.btnRestart);
            btnBackToSelection = pauseOverlay.findViewById(R.id.btnBackToSelection);

            // ✅ Volume controls (optional if your pause overlay xml includes them)
            seekVolume = pauseOverlay.findViewById(R.id.seekVolume);
            imgVolume = pauseOverlay.findViewById(R.id.imgVolume);
            setupVolumeSeekIfPresent();

            pauseOverlay.setOnClickListener(v -> hidePause());
            if (pauseCard != null) pauseCard.setOnClickListener(v -> {});
        }

        if (btnResume != null) btnResume.setOnClickListener(v -> hidePause());
        if (btnRestart != null) btnRestart.setOnClickListener(v -> {
            hidePause();
            recreate();
        });
        if (btnBackToSelection != null) btnBackToSelection.setOnClickListener(v -> {
            hidePause();
            savePlayTimeOnce();
            playMusic(MusicService.TRACK_MAIN);
            startActivity(new Intent(PlanetActivity.this, SelectionGamesActivity.class));
            finish();
        });
    }

    private void setupVolumeSeekIfPresent() {
        if (seekVolume == null) return;

        // init from saved volume
        float vol = MusicController.loadVolume(this); // 0..1
        int progress = (int) (vol * 100f);
        seekVolume.setMax(100);
        seekVolume.setProgress(progress);
        updateVolumeIcon(progress);

        seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                float v = p / 100f;
                setMusicVolume(v);
                updateVolumeIcon(p);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateVolumeIcon(int p) {
        if (imgVolume == null) return;
        // simple icon swap (optional)
        if (p <= 1) imgVolume.setImageResource(android.R.drawable.ic_lock_silent_mode);
        else imgVolume.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
    }

    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (pauseOverlay != null && pauseOverlay.getVisibility() == View.VISIBLE) {
                    hidePause();
                    return;
                }
                if (instructionsOverlay != null && instructionsOverlay.getVisibility() == View.VISIBLE) {
                    hideInstructionsOverlay();
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private void setupTopButtons() {
        setActive(btnMove);
        if (spaceView != null) spaceView.setMode(ZoomSpaceView.Mode.MOVE);

        refreshSwatchViews();
        selectSwatch(0);
    }

    private void setupModeButtons() {
        if (btnMove != null) {
            btnMove.setOnClickListener(v -> {
                setActive(btnMove);
                hideToolsPanel();
                if (spaceView != null) spaceView.setMode(ZoomSpaceView.Mode.MOVE);
            });
        }

        if (btnStars != null) {
            btnStars.setOnClickListener(v -> {
                setActive(btnStars);
                hideToolsPanel();
                if (spaceView != null) spaceView.setMode(ZoomSpaceView.Mode.STARS);
            });
        }

        if (btnUndo != null) {
            btnUndo.setOnClickListener(v -> {
                hideToolsPanel();
                if (spaceView != null) spaceView.undo();
            });
        }

        if (btnTool != null) {
            btnTool.setOnClickListener(v -> toggleToolsPanel());
        }
    }

    private void setupToolButtons() {
        if (btnMarkerTool != null) {
            btnMarkerTool.setOnClickListener(v -> {
                setActive(btnMarkerTool);
                if (spaceView != null) spaceView.setMode(ZoomSpaceView.Mode.MARKER);
            });
        }

        if (btnEraserTool != null) {
            btnEraserTool.setOnClickListener(v -> {
                setActive(btnEraserTool);
                if (spaceView != null) spaceView.setMode(ZoomSpaceView.Mode.ERASER);
            });
        }

        if (btnClearObject != null) {
            btnClearObject.setOnClickListener(v -> {
                if (spaceView != null) spaceView.clearObjectsOnly();
            });
        }

        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> {
                if (spaceView != null) spaceView.clearAllObjects();
            });
        }
    }

    private void setupPlanetButtons() {
        if (btnPlanets != null) btnPlanets.setOnClickListener(v -> showPlanetPickerDialog());

        planetsPlaying = false;
        if (spaceView != null) spaceView.setPlanetAnimationEnabled(false);

        if (btnPlayPlanets != null) {
            btnPlayPlanets.setText(R.string.play);
            btnPlayPlanets.setOnClickListener(v -> {
                planetsPlaying = !planetsPlaying;
                if (spaceView != null) spaceView.setPlanetAnimationEnabled(planetsPlaying);
                btnPlayPlanets.setText(planetsPlaying ? R.string.stop : R.string.play);
            });
        }
    }

    private void setupSwatchesAndSeekBars() {
        if (sw1 != null) sw1.setOnClickListener(v -> selectSwatch(0));
        if (sw2 != null) sw2.setOnClickListener(v -> selectSwatch(1));
        if (sw3 != null) sw3.setOnClickListener(v -> selectSwatch(2));
        if (sw4 != null) sw4.setOnClickListener(v -> selectSwatch(3));
        if (sw5 != null) sw5.setOnClickListener(v -> selectSwatch(4));
        if (sw6 != null) sw6.setOnClickListener(v -> selectSwatch(5));

        if (sbR != null) sbR.setMax(255);
        if (sbG != null) sbG.setMax(255);
        if (sbB != null) sbB.setMax(255);

        SeekBar.OnSeekBarChangeListener rgbListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (sbR == null || sbG == null || sbB == null) return;
                int c = Color.rgb(sbR.getProgress(), sbG.getProgress(), sbB.getProgress());
                swatchColors[selectedSwatchIndex] = c;
                refreshSwatchViews();
                if (spaceView != null) spaceView.setInkColor(c);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        if (sbR != null) sbR.setOnSeekBarChangeListener(rgbListener);
        if (sbG != null) sbG.setOnSeekBarChangeListener(rgbListener);
        if (sbB != null) sbB.setOnSeekBarChangeListener(rgbListener);

        if (sbMarkerSize != null && spaceView != null) {
            sbMarkerSize.setMax(100);
            sbMarkerSize.setProgress(spaceView.getMarkerSizeProgress());
            sbMarkerSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (spaceView != null) spaceView.setMarkerSize(progress);
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    // =========================
    // Play time saving
    // =========================
    private void savePlayTimeOnce() {
        if (playTimeSaved) return;
        playTimeSaved = true;
        if (playTracker != null) playTracker.stopAndSave(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        playTimeSaved = false;
        if (playTracker == null) playTracker = new GameTimeTracker("Planet");
        playTracker.start();

        playMusic(MusicService.TRACK_ASTHERA);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveStateToPrefs(false);
        savePlayTimeOnce();

        // ⚠️ don’t always force main here if you're just opening overlays.
        // But onPause means you're leaving the activity, so OK:
        playMusic(MusicService.TRACK_MAIN);
    }

    // =========================
    // Pause UX
    // =========================
    private void setUiEnabled(boolean enabled) {
        if (btnMove != null) btnMove.setEnabled(enabled);
        if (btnStars != null) btnStars.setEnabled(enabled);
        if (btnUndo != null) btnUndo.setEnabled(enabled);
        if (btnTool != null) btnTool.setEnabled(enabled);
        if (btnMarkerTool != null) btnMarkerTool.setEnabled(enabled);
        if (btnEraserTool != null) btnEraserTool.setEnabled(enabled);
        if (btnPlanets != null) btnPlanets.setEnabled(enabled);
        if (btnPlayPlanets != null) btnPlayPlanets.setEnabled(enabled);
        if (btnClearAll != null) btnClearAll.setEnabled(enabled);
        if (btnClearObject != null) btnClearObject.setEnabled(enabled);
        if (toolsPanel != null) toolsPanel.setEnabled(enabled);
    }

    private void showPause() {
        if (pauseOverlay == null) return;

        planetsPlayingBeforePause = planetsPlaying;

        planetsPlaying = false;
        if (spaceView != null) spaceView.setPlanetAnimationEnabled(false);
        if (btnPlayPlanets != null) btnPlayPlanets.setText(R.string.play);

        // refresh volume UI each open
        if (seekVolume != null) {
            float vol = MusicController.loadVolume(this);
            int progress = (int) (vol * 100f);
            seekVolume.setProgress(progress);
            updateVolumeIcon(progress);
        }

        setUiEnabled(false);
        pauseOverlay.setVisibility(View.VISIBLE);
    }

    private void hidePause() {
        if (pauseOverlay == null) return;

        pauseOverlay.setVisibility(View.GONE);
        setUiEnabled(true);

        planetsPlaying = planetsPlayingBeforePause;
        if (spaceView != null) spaceView.setPlanetAnimationEnabled(planetsPlaying);
        if (btnPlayPlanets != null) btnPlayPlanets.setText(planetsPlaying ? R.string.stop : R.string.play);
    }

    private void showInstructionsOverlay(boolean fromInfo) {
        if (instructionsOverlay == null) return;
        setUiEnabled(false);
        instructionsOverlay.setVisibility(View.VISIBLE);
        if (btnStartPlay != null) btnStartPlay.setText(fromInfo ? R.string.close : R.string.play);
    }

    private void hideInstructionsOverlay() {
        if (instructionsOverlay == null) return;
        instructionsOverlay.setVisibility(View.GONE);
        setUiEnabled(true);
    }

    // =========================
    // Tools panel helpers
    // =========================
    private void toggleToolsPanel() {
        if (toolsPanel == null) return;

        if (toolsPanel.getVisibility() == View.VISIBLE) {
            hideToolsPanel();
        } else {
            toolsPanel.clearAnimation();
            toolsPanel.setVisibility(View.VISIBLE);

            if (btnTool != null) {
                btnTool.setEnabled(false);
                btnTool.postDelayed(() -> btnTool.setEnabled(true), 180);
            }

            Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            toolsPanel.startAnimation(anim);
        }
        if (btnTool != null) {
            btnTool.setEnabled(false);
            btnTool.postDelayed(() -> btnTool.setEnabled(true), 200);
        }
    }

    private void hideToolsPanel() {
        if (toolsPanel == null) return;
        if (toolsPanel.getVisibility() != View.VISIBLE) return;

        toolsPanel.clearAnimation();

        Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
                toolsPanel.setVisibility(View.GONE);
            }
            @Override public void onAnimationRepeat(Animation animation) {}
        });

        toolsPanel.startAnimation(anim);
    }

    // =========================
    // Planet Picker
    // =========================
    private void showPlanetPickerDialog() {
        final ZoomSpaceView.Body[] bodies = new ZoomSpaceView.Body[]{
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

        final String[] labels = new String[]{
                getString(R.string.sun),
                getString(R.string.moon),
                getString(R.string.mercury),
                getString(R.string.venus),
                getString(R.string.earth),
                getString(R.string.mars),
                getString(R.string.jupiter),
                getString(R.string.saturn),
                getString(R.string.uranus),
                getString(R.string.neptune)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.choose)
                .setItems(labels, (d, which) -> {
                    setActive(btnPlanets);
                    if (spaceView != null) {
                        spaceView.setSelectedBody(bodies[which]);
                        spaceView.setMode(ZoomSpaceView.Mode.PLANET);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // =========================
    // State persistence
    // =========================
    private void saveStateToPrefs(boolean showToast) {
        try {
            if (spaceView == null) return;
            String state = spaceView.exportStateJson();
            SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            sp.edit().putString(KEY_STATE, state).apply();
            if (showToast) Toast.makeText(this, R.string.work_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.save_state_error, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void loadStateFromPrefs() {
        try {
            SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            String state = sp.getString(KEY_STATE, null);
            if (state != null && !state.trim().isEmpty() && spaceView != null) {
                spaceView.importStateJson(state);
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.load_state_error, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    // =========================
    // Selection states
    // =========================
    private void setActive(Button active) {
        if (btnMove != null) btnMove.setSelected(false);
        if (btnStars != null) btnStars.setSelected(false);
        if (btnMarkerTool != null) btnMarkerTool.setSelected(false);
        if (btnEraserTool != null) btnEraserTool.setSelected(false);
        if (btnPlanets != null) btnPlanets.setSelected(false);
        if (active != null) active.setSelected(true);
    }

    private void refreshSwatchViews() {
        if (swatches == null) return;
        for (int i = 0; i < swatches.length; i++) {
            if (swatches[i] != null) swatches[i].setBackgroundColor(swatchColors[i]);
        }
        applySwatchSelectionBorder();
    }

    private void selectSwatch(int index) {
        selectedSwatchIndex = index;
        int c = swatchColors[index];

        if (sbR != null) sbR.setProgress(Color.red(c));
        if (sbG != null) sbG.setProgress(Color.green(c));
        if (sbB != null) sbB.setProgress(Color.blue(c));

        if (spaceView != null) spaceView.setInkColor(c);
        applySwatchSelectionBorder();
    }

    private void applySwatchSelectionBorder() {
        if (swatches == null) return;
        int borderW = dp(2);
        for (int i = 0; i < swatches.length; i++) {
            View v = swatches[i];
            if (v == null) continue;

            v.setPadding(0, 0, 0, 0);
            v.setBackground(StyleUtils.makeSwatchDrawable(
                    swatchColors[i],
                    (i == selectedSwatchIndex) ? Color.WHITE : Color.TRANSPARENT,
                    borderW,
                    dp(8)
            ));
        }
    }

    // =========================
    // Save image
    // =========================
    private void saveDesignToGallery() {
        try {
            if (spaceView == null) return;
            Bitmap bmp = spaceView.exportBitmap();

            String name = "Asthera_" + System.currentTimeMillis() + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/Asthera");

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os == null) {
                    Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
            }

            Toast.makeText(this, R.string.saved_to_gallery, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.save_error, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // =========================
    // ✅ Music controls (match MusicService)
    // =========================
    private void playMusic(int track) {
        Intent i = new Intent(this, MusicService.class);
        i.setAction(MusicService.ACTION_PLAY);
        i.putExtra(MusicService.EXTRA_TRACK, track);
        startService(i);
    }

    private void setMusicVolume(float volume0to1) {
        Intent i = new Intent(this, MusicService.class);
        i.setAction(MusicService.ACTION_SET_VOLUME);
        i.putExtra(MusicService.EXTRA_VOLUME, volume0to1);
        startService(i);
    }
}
