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
    private static final String KEY_CURRENT_USER = "current_user";

    // old prefs keys (fallback)
    private static String moodKey(String d) { return "mood_" + d; }
    private static String noteKey(String d) { return "note_" + d; }

    private SharedPreferences prefs;
    private ZenPathRepository repo;

    private View[] dots = new View[7];               // Mon..Sun
    private TextView[] emojis = new TextView[5];

    private TextView tvMoodNote, tvSelectedDayLabel, tvWeekLabel;
    private TextView tvMoodQuote;

    private View btnPrevWeek, btnNextWeek, noteCard;

    private Calendar weekStartMon;
    private int selectedIndex = 0;

    // must match your MoodActivity mapping
    private static final String[] MOOD_LABELS = new String[]{
            "Sad", "Angry", "Okay", "Good", "Happy"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_mood_history, container, false);

        prefs = requireContext().getSharedPreferences(PREFS, 0);
        repo = new ZenPathRepository(requireContext());

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
        tvMoodQuote = v.findViewById(R.id.tvMoodQuote);

        btnPrevWeek = v.findViewById(R.id.btnPrevWeek);
        btnNextWeek = v.findViewById(R.id.btnNextWeek);
        noteCard = v.findViewById(R.id.noteCard);

        Calendar today = Calendar.getInstance();
        weekStartMon = getMonday(today);
        selectedIndex = mondayIndex(today);

        for (int i = 0; i < 7; i++) {
            final int idx = i;
            if (dots[i] == null) continue;
            dots[i].setOnClickListener(vv -> {
                selectedIndex = idx;
                refresh();
            });
        }

        if (btnPrevWeek != null) btnPrevWeek.setOnClickListener(vv -> {
            weekStartMon.add(Calendar.DAY_OF_MONTH, -7);
            refresh();
        });

        if (btnNextWeek != null) btnNextWeek.setOnClickListener(vv -> {
            weekStartMon.add(Calendar.DAY_OF_MONTH, 7);
            refresh();
        });

        if (noteCard != null) noteCard.setOnClickListener(vv -> openEditor());

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
        if (tvWeekLabel != null) tvWeekLabel.setText(shortDate(weekStartMon) + " – " + shortDate(end));

        // dots state
        for (int i = 0; i < 7; i++) {
            String dk = dateKey(i);
            boolean hasData = hasMoodOrNote(dk);
            boolean sel = (i == selectedIndex);

            if (dots[i] == null) continue;
            if (sel) dots[i].setBackgroundResource(R.drawable.dot_selected);
            else if (hasData) dots[i].setBackgroundResource(R.drawable.dot_filled);
            else dots[i].setBackgroundResource(R.drawable.dot_empty);
        }

        String dk = dateKey(selectedIndex);
        if (tvSelectedDayLabel != null) tvSelectedDayLabel.setText(prettyDate(dk));

        MoodRow row = loadMoodRow(dk);

        // emojis highlight
        for (int i = 0; i < emojis.length; i++) {
            if (emojis[i] == null) continue;
            boolean sel = (i == row.moodIndex);
            emojis[i].setScaleX(sel ? 1.2f : 1f);
            emojis[i].setScaleY(sel ? 1.2f : 1f);
            emojis[i].setAlpha(sel ? 1f : 0.55f);
        }

        if (tvMoodQuote != null) tvMoodQuote.setText(getQuoteForMood(row.moodIndex));

        if (tvMoodNote != null) {
            if (!TextUtils.isEmpty(row.reflection)) tvMoodNote.setText(row.reflection);
            else tvMoodNote.setText("No note yet — tap here to add a reflection.");
        }
    }

    private MoodRow loadMoodRow(String dk) {
        MoodRow out = new MoodRow();

        long userId = currentUserId();
        if (userId > 0) {
            String[] data = repo.getMoodByDate(userId, dk); // [moodText, reflection]
            if (data != null) {
                out.moodIndex = moodTextToIndex(data[0]);
                out.reflection = (data.length > 1 && data[1] != null) ? data[1] : "";
            }
        }

        // fallback to old prefs if DB empty
        if (out.moodIndex == -1 && prefs.contains(moodKey(dk))) {
            out.moodIndex = prefs.getInt(moodKey(dk), -1);
        }
        if (TextUtils.isEmpty(out.reflection)) {
            out.reflection = prefs.getString(noteKey(dk), "");
        }

        return out;
    }

    private boolean hasMoodOrNote(String dk) {
        // DB first
        long userId = currentUserId();
        if (userId > 0) {
            String[] data = repo.getMoodByDate(userId, dk);
            boolean hasMood = data != null && !TextUtils.isEmpty(data[0]);
            boolean hasNote = data != null && data.length > 1 && !TextUtils.isEmpty(data[1]);
            if (hasMood || hasNote) return true;
        }

        // fallback prefs
        return (prefs.contains(moodKey(dk)) && prefs.getInt(moodKey(dk), -1) != -1)
                || !TextUtils.isEmpty(prefs.getString(noteKey(dk), ""));
    }

    private int moodTextToIndex(String moodText) {
        if (moodText == null) return -1;
        for (int i = 0; i < MOOD_LABELS.length; i++) {
            if (moodText.equalsIgnoreCase(MOOD_LABELS[i])) return i;
        }
        return -1;
    }

    // mood → quote (kept your logic)
    private String getQuoteForMood(int moodIndex) {
        switch (moodIndex) {
            case 0: return "Hold on to this light moment.";
            case 1: return "Slow is still progress.";
            case 2: return "It’s okay to feel ‘in between’.";
            case 3: return "You deserve this warmth.";
            case 4: return "One breath at a time. You’re doing your best.";
            default: return "How are you feeling today?";
        }
    }

    private void openEditor() {
        Intent i = new Intent(requireContext(), MoodActivity.class);
        i.putExtra(MoodActivity.EXTRA_DATE_KEY, dateKey(selectedIndex));
        startActivity(i);
    }

    private long currentUserId() {
        String s = prefs.getString(KEY_CURRENT_USER, null);
        if (s == null) return -1;
        try { return Long.parseLong(s); } catch (Exception e) { return -1; }
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

    private static class MoodRow {
        int moodIndex = -1;
        String reflection = "";
    }
}
