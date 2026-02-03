package com.example.zenpath;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class StressHistoryFragment extends Fragment {

    private ZenPathRepository repository;
    private LinearLayout entriesContainer;
    private TextView tvEmptyMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stress_history, container, false);
        
        repository = new ZenPathRepository(requireContext());
        entriesContainer = view.findViewById(R.id.entriesContainer);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        
        loadStressEntries();
        
        return view;
    }

    private void loadStressEntries() {
        ArrayList<String> entries = repository.getStressEntries();
        
        if (entries.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            entriesContainer.setVisibility(View.GONE);
            tvEmptyMessage.setText("No stress entries yet. Track your stress levels to see your history here!");
        } else {
            tvEmptyMessage.setVisibility(View.GONE);
            entriesContainer.setVisibility(View.VISIBLE);
            
            entriesContainer.removeAllViews();
            for (String entry : entries) {
                TextView entryView = new TextView(requireContext());
                entryView.setText(entry);
                entryView.setPadding(16, 16, 16, 16);
                entryView.setTextSize(14);
                entryView.setBackgroundResource(R.drawable.bg_entry_card);
                entryView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                
                LinearLayout.MarginLayoutParams params = (LinearLayout.MarginLayoutParams) entryView.getLayoutParams();
                params.setMargins(0, 0, 0, 8);
                entryView.setLayoutParams(params);
                
                entriesContainer.addView(entryView);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStressEntries();
    }
}
