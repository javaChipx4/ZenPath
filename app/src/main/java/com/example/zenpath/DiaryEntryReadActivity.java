package com.example.zenpath;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DiaryEntryReadActivity extends AppCompatActivity {

    public static final String EXTRA_DATE = "extra_date";

    private ViewPager2 pager;
    private TextView tvTitle;
    private ImageButton btnBack;

    private ZenPathRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_entry_read);

        repo = new ZenPathRepository(this);

        pager = findViewById(R.id.readPager);
        tvTitle = findViewById(R.id.tvReadTitle);
        btnBack = findViewById(R.id.btnReadBack);

        String date = getIntent().getStringExtra(EXTRA_DATE);
        if (date == null) date = "";

        if (tvTitle != null) tvTitle.setText(prettyDate(date));
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ArrayList<String> pages = repo.getDiaryPagesByDate(date);
        DiaryReadPagerAdapter adapter = new DiaryReadPagerAdapter(pages);
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);

        // If you already have PageFlipTransformer class, keep this (looks like a diary/book)
        pager.setPageTransformer(new PageFlipTransformer());
    }

    private String prettyDate(String yyyyMmDd) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(yyyyMmDd);
            return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(d);
        } catch (Exception e) {
            return yyyyMmDd;
        }
    }
}
