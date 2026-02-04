package com.example.zenpath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MoodHistoryFragment extends Fragment {

    private static final String PREFS = "zen_path_prefs";

    private static String moodKey(String d) { return "mood_" + d; }
    private static String noteKey(String d) { return "note_" + d; }

    private SharedPreferences prefs;

    private View[] dots = new View[7];               // Mon..Sun
    private TextView[] emojis = new TextView[5];

    private TextView tvMoodNote, tvSelectedDayLabel, tvWeekLabel;
    private View btnPrevWeek, btnNextWeek, noteCard;

    private Calendar weekStartMon;
    private int selectedIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_mood_history, container, false);
        prefs = requireContext().getSharedPreferences(PREFS, 0);

        dots[0] = v.findViewById(R.id.dot0);
        dots[1] = v.findViewById(R.id.dot1);
        dots[2] = v.findViewById(R.id.dot2);
        dots[3] = v.findViewById(R.id.dot3);
        dots[4] = v.findViewById(R.id.dot4);
        dots[5] = v.findViewById(R.id.dot5);
        dots[6] = v.findViewById(R.id.dot6);

        emojis[0] = v.findViewById(R.id.emoji0);
        emojis[1] = v.findViewById(R.id.emoji1);
        emojis[2] = v.findViewById(R.id.emoji2);
        emojis[3] = v.findViewById(R.id.emoji3);
        emojis[4] = v.findViewById(R.id.emoji4);

        tvMoodNote = v.findViewById(R.id.tvMoodNote);
        tvSelectedDayLabel = v.findViewById(R.id.tvSelectedDayLabel);
        tvWeekLabel = v.findViewById(R.id.tvWeekLabel);

        btnPrevWeek = v.findViewById(R.id.btnPrevWeek);
        btnNextWeek = v.findViewById(R.id.btnNextWeek);
        noteCard = v.findViewById(R.id.noteCard);

        Calendar today = Calendar.getInstance();
        weekStartMon = getMonday(today);
        selectedIndex = mondayIndex(today);

        for (int i = 0; i < 7; i++) {
            final int idx = i;
            dots[i].setOnClickListener(vv -> {
                selectedIndex = idx;
                refresh();
            });
        }

        btnPrevWeek.setOnClickListener(vv -> {
            weekStartMon.add(Calendar.DAY_OF_MONTH, -7);
            refresh();
        });

        btnNextWeek.setOnClickListener(vv -> {
            weekStartMon.add(Calendar.DAY_OF_MONTH, 7);
            refresh();
        });

        noteCard.setOnClickListener(vv -> openEditor());

        refresh();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        Calendar end = (Calendar) weekStartMon.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);
        tvWeekLabel.setText(shortDate(weekStartMon) + " – " + shortDate(end));

        for (int i = 0; i < 7; i++) {
            String dk = dateKey(i);
            boolean hasData = hasMoodOrNote(dk);
            boolean sel = (i == selectedIndex);

            if (sel) dots[i].setBackgroundResource(R.drawable.dot_selected);
            else if (hasData) dots[i].setBackgroundResource(R.drawable.dot_filled);
            else dots[i].setBackgroundResource(R.drawable.dot_empty);
        }

        String dk = dateKey(selectedIndex);
        tvSelectedDayLabel.setText(prettyDate(dk));

        int mood = prefs.getInt(moodKey(dk), -1);
        String note = prefs.getString(noteKey(dk), "");

        for (int i = 0; i < emojis.length; i++) {
            boolean sel = (i == mood);
            emojis[i].setScaleX(sel ? 1.2f : 1f);
            emojis[i].setScaleY(sel ? 1.2f : 1f);
            emojis[i].setAlpha(sel ? 1f : 0.55f);
        }

        if (!TextUtils.isEmpty(note)) {
            tvMoodNote.setText(note);
        } else {
            tvMoodNote.setText("No note for this day.");
        }
    }

    private void openEditor() {
        Intent i = new Intent(requireContext(), MoodActivity.class);
        i.putExtra(MoodActivity.EXTRA_DATE_KEY, dateKey(selectedIndex));
        startActivity(i);
    }

    private boolean hasMoodOrNote(String dk) {
        return (prefs.contains(moodKey(dk)) && prefs.getInt(moodKey(dk), -1) != -1)
                || !TextUtils.isEmpty(prefs.getString(noteKey(dk), ""));
    }

    private Calendar getMonday(Calendar c) {
        Calendar cal = (Calendar) c.clone();
        int d = cal.get(Calendar.DAY_OF_WEEK);
        cal.add(Calendar.DAY_OF_MONTH, d == Calendar.SUNDAY ? -6 : Calendar.MONDAY - d);
        return cal;
    }

    private int mondayIndex(Calendar c) {
        int d = c.get(Calendar.DAY_OF_WEEK);
        return d == Calendar.SUNDAY ? 6 : d - Calendar.MONDAY;
    }

    private String dateKey(int idx) {
        Calendar c = (Calendar) weekStartMon.clone();
        c.add(Calendar.DAY_OF_MONTH, idx);
        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(c.getTime());
    }

    private String prettyDate(String dk) {
        try {
            return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(dk));
        } catch (Exception e) { return dk; }
    }

    private String shortDate(Calendar c) {
        return new SimpleDateFormat("MMM dd", Locale.getDefault()).format(c.getTime());
    }
}
