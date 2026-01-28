package com.example.zenpath;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class HistoryPagerAdapter extends FragmentStateAdapter {

    public HistoryPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new DiaryHistoryFragment();
            case 1: return new MoodHistoryFragment();
            default: return new StressHistoryFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
