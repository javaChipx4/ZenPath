package com.example.zenpath;

import android.text.Editable;
import android.text.TextWatcher;

public class SimpleTextWatcher implements TextWatcher {

    public interface After {
        void run(String text);
    }

    private final After after;

    private SimpleTextWatcher(After after) {
        this.after = after;
    }

    public static SimpleTextWatcher afterChanged(After after) {
        return new SimpleTextWatcher(after);
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    @Override public void afterTextChanged(Editable s) {
        if (after != null) after.run(s == null ? "" : s.toString());
    }
}
