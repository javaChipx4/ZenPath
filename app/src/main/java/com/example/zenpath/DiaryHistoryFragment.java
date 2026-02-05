package com.example.zenpath;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DiaryHistoryFragment extends Fragment {

    private RecyclerView rv;
    private DiaryHistoryAdapter adapter;
    private ZenPathRepository repo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_diary_history, container, false);

        repo = new ZenPathRepository(requireContext());

        rv = v.findViewById(R.id.rvDiaryHistory);
        rv.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2));
        rv.setHasFixedSize(true);

        adapter = new DiaryHistoryAdapter(new ArrayList<>(), item -> {
            Intent i = new Intent(requireContext(), DiaryEntryReadActivity.class);
            i.putExtra(DiaryEntryReadActivity.EXTRA_DATE, item.date);
            startActivity(i);
        });

        rv.setAdapter(adapter);

        load();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        load(); // refresh after saving a diary
    }

    private void load() {
        ArrayList<ZenPathRepository.DiaryEntryMeta> items = repo.getSavedDiaryHistory();
        adapter.setItems(items);
    }
}
