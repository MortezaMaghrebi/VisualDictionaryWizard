package com.codestoon.visualdictionarywizard;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;

public class LevelWordsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyView, levelTitleText, wordCountText;
    private ProgressBar progressBar;
    private EditText searchEditText;
    private ImageView clearButton, sortButton;
    private LinearLayout searchLayout;

    private DatabaseHelper dbHelper;
    private WordAdapter wordAdapter;
    private ArrayList<HashMap<String, String>> allWords;
    private ArrayList<HashMap<String, String>> filteredWords;

    private String currentLevel;
    private String currentLevelName;
    private String currentSort = "alphabetical"; // alphabetical, search_count, random

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_words);

        // دریافت اطلاعات سطح از Intent
        currentLevel = getIntent().getStringExtra("level");
        currentLevelName = getIntent().getStringExtra("level_name");

        if (currentLevel == null) {
            finish();
            return;
        }

        initViews();
        setupToolbar();
        loadWords();
        setupSearchListener();
        setupClickListeners();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        levelTitleText = findViewById(R.id.levelTitleText);
        wordCountText = findViewById(R.id.wordCountText);
        progressBar = findViewById(R.id.progressBar);
        searchEditText = findViewById(R.id.searchEditText);
        clearButton = findViewById(R.id.clearButton);
        sortButton = findViewById(R.id.sortButton);
        searchLayout = findViewById(R.id.searchLayout);

        dbHelper = new DatabaseHelper(this);
        allWords = new ArrayList<>();
        filteredWords = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // تنظیم عنوان سطح
        levelTitleText.setText("سطح " + currentLevel + " - " + currentLevelName);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📚 کلمات سطح " + currentLevel);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadWords() {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            // دریافت کلمات فقط برای این سطح
            allWords = dbHelper.filterByLevel(currentLevel);
            filteredWords = new ArrayList<>(allWords);

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);

                if (allWords.isEmpty()) {
                    showEmptyState();
                } else {
                    showWordsList();
                }

                updateWordCount();
            });
        }).start();
    }

    private void showEmptyState() {
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        searchLayout.setVisibility(View.GONE);
        emptyView.setText("📭 در سطح " + currentLevel + " کلمه‌ای وجود ندارد");
    }

    private void showWordsList() {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        searchLayout.setVisibility(View.VISIBLE);

        applySort();

        wordAdapter = new WordAdapter(filteredWords, word -> {
            Intent intent = new Intent(LevelWordsActivity.this, WordDetailActivity.class);
            intent.putExtra("word", word.get("word"));
            startActivity(intent);
        });

        recyclerView.setAdapter(wordAdapter);
        updateWordCount();  // ← اینجا صدا زده بشه
    }

    private void updateWordCount() {
        if (wordCountText != null) {
            wordCountText.setText(allWords.size() + " کلمه");
        }
    }

    private void applySort() {
        switch (currentSort) {
            case "alphabetical":
                filteredWords.sort((a, b) ->
                        a.get("word").compareToIgnoreCase(b.get("word")));
                break;
            case "search_count":
                // بر اساس تعداد جستجو (نیاز به فیلد search_count در دیتابیس)
                break;
            case "random":
                // مرتب‌سازی تصادفی
                java.util.Collections.shuffle(filteredWords);
                break;
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            filteredWords = new ArrayList<>(allWords);
            updateWordCount();  // نمایش تعداد کل
        } else {
            filteredWords = new ArrayList<>();
            String lowerQuery = query.toLowerCase();
            for (HashMap<String, String> word : allWords) {
                String wordText = word.get("word").toLowerCase();
                String persianText = word.get("persian").toLowerCase();
                if (wordText.contains(lowerQuery) || persianText.contains(lowerQuery)) {
                    filteredWords.add(word);
                }
            }
            // نمایش تعداد نتایج جستجو
            if (wordCountText != null) {
                wordCountText.setText(filteredWords.size() + " نتیجه برای '" + query + "'");
            }
        }

        applySort();

        if (wordAdapter != null) {
            wordAdapter.updateList(filteredWords);
        }

        if (filteredWords.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            if (query.isEmpty()) {
                emptyView.setText("📭 در سطح " + currentLevel + " کلمه‌ای وجود ندارد");
            } else {
                emptyView.setText("🔍 نتیجه‌ای برای '" + query + "' یافت نشد");
            }
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                performSearch(query);

                // نمایش/مخفی کردن دکمه پاک کردن
                clearButton.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        clearButton.setOnClickListener(v -> {
            searchEditText.setText("");
            performSearch("");
        });

        sortButton.setOnClickListener(v -> showSortOptions());
    }

    private void showSortOptions() {
        String[] sortOptions = {"📖 الفبایی", "🎲 تصادفی"};
        String[] sortValues = {"alphabetical", "random"};

        int checkedItem = currentSort.equals("alphabetical") ? 0 : 1;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("مرتب‌سازی بر اساس")
                .setSingleChoiceItems(sortOptions, checkedItem, (dialog, which) -> {
                    currentSort = sortValues[which];
                    applySort();
                    if (wordAdapter != null) {
                        wordAdapter.updateList(filteredWords);
                    }
                    dialog.dismiss();

                    Toast.makeText(this,
                            currentSort.equals("alphabetical") ? "مرتب‌سازی الفبایی" : "مرتب‌سازی تصادفی",
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // به‌روزرسانی لیست در صورت نیاز
        if (wordAdapter != null) {
            wordAdapter.updateList(filteredWords);
        }
    }
}