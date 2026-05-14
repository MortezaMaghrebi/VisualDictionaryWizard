package com.codestoon.visualdictionarywizard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;

public class MasteredWordsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView statsText,emptyView;

    private ProgressBar progressBar;
    private Button resetButton, studyButton;
    private LinearLayout statsLayout;
    private ImageView sortButton;

    private DatabaseHelper dbHelper;
    private WordAdapter wordAdapter;
    private ArrayList<HashMap<String, String>> masteredWords;
    private ArrayList<HashMap<String, String>> filteredWords;

    private EditText searchEditText;
    private ImageView clearButton;
    private String currentSort = "alphabetical";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mastered_words);

        initViews();
        setupToolbar();
        loadMasteredWords();
        setupClickListeners();
        setupSearchListener();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        statsText = findViewById(R.id.statsText);
        statsLayout = findViewById(R.id.statsLayout);
        progressBar = findViewById(R.id.progressBar);
        resetButton = findViewById(R.id.resetButton);
        studyButton = findViewById(R.id.studyButton);
        sortButton = findViewById(R.id.sortButton);
        searchEditText = findViewById(R.id.searchEditText);
        clearButton = findViewById(R.id.clearButton);

        dbHelper = new DatabaseHelper(this);
        masteredWords = new ArrayList<>();
        filteredWords = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("✅ کلمات یاد گرفته شده");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadMasteredWords() {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            masteredWords = dbHelper.getMasteredWordsWithDetails();
            filteredWords = new ArrayList<>(masteredWords);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);

                if (masteredWords.isEmpty()) {
                    showEmptyState();
                } else {
                    showMasteredList();
                }
                updateStats();
            });
        }).start();
    }

    private void showEmptyState() {
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        statsLayout.setVisibility(View.GONE);
        resetButton.setEnabled(false);
        studyButton.setEnabled(false);
        emptyView.setText("🎯 هنوز کلمه‌ای یاد نگرفته‌اید\n\nدر جلسه مطالعه، روی دکمه سبز ✅ بزنید تا کلمات را به این لیست اضافه کنید");
    }

    private void showMasteredList() {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        statsLayout.setVisibility(View.VISIBLE);

        applySort();

        wordAdapter = new WordAdapter(filteredWords, word -> {
            Intent intent = new Intent(MasteredWordsActivity.this, WordDetailActivity.class);
            intent.putExtra("word", word.get("word"));
            startActivity(intent);
        });

        recyclerView.setAdapter(wordAdapter);
        updateStats();
    }

    private void updateStats() {
        statsText.setText(masteredWords.size() + " کلمه یاد گرفته شده");
    }

    private void applySort() {
        switch (currentSort) {
            case "alphabetical":
                filteredWords.sort((a, b) ->
                        a.get("word").compareToIgnoreCase(b.get("word")));
                break;
            case "level":
                String[] levelOrder = {"A1", "A2", "B1", "B2", "C1", "C2"};
                filteredWords.sort((a, b) -> {
                    int indexA = getLevelIndex(a.get("level"), levelOrder);
                    int indexB = getLevelIndex(b.get("level"), levelOrder);
                    return Integer.compare(indexA, indexB);
                });
                break;
            case "date":
                // بر اساس تاریخ یادگیری (در صورت اضافه شدن)
                break;
        }
    }

    private int getLevelIndex(String level, String[] order) {
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(level)) return i;
        }
        return order.length;
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            filteredWords = new ArrayList<>(masteredWords);
        } else {
            filteredWords = new ArrayList<>();
            String lowerQuery = query.toLowerCase();
            for (HashMap<String, String> word : masteredWords) {
                String wordText = word.get("word").toLowerCase();
                String persianText = word.get("persian").toLowerCase();
                if (wordText.contains(lowerQuery) || persianText.contains(lowerQuery)) {
                    filteredWords.add(word);
                }
            }
        }

        applySort();

        if (wordAdapter != null) {
            wordAdapter.updateList(filteredWords);
        }

        if (filteredWords.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText("🔍 نتیجه‌ای برای '" + query + "' یافت نشد");
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        updateStatsTextForSearch(query);
    }

    private void updateStatsTextForSearch(String query) {
        if (query.isEmpty()) {
            statsText.setText(masteredWords.size() + " کلمه یاد گرفته شده");
        } else {
            statsText.setText(filteredWords.size() + " نتیجه برای '" + query + "'");
        }
    }

    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                performSearch(query);
                clearButton.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void resetAllMastered() {
        if (masteredWords.isEmpty()) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("بازنشانی کلمات یاد گرفته شده")
                .setMessage("آیا از حذف همه " + masteredWords.size() + " کلمه از لیست یادگرفته شده مطمئن هستید؟")
                .setPositiveButton("بازنشانی", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    new Thread(() -> {
                        for (HashMap<String, String> word : masteredWords) {
                            dbHelper.removeFromMastered(word.get("word"));
                        }
                        runOnUiThread(() -> {
                            masteredWords.clear();
                            filteredWords.clear();
                            progressBar.setVisibility(View.GONE);
                            showEmptyState();
                            Toast.makeText(this, "لیست کلمات یاد گرفته شده بازنشانی شد", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    private void startStudySession() {
        if (filteredWords.isEmpty()) {
            Toast.makeText(this, "کلمه‌ای برای مطالعه وجود ندارد", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MasteredWordsActivity.this, StudySessionActivity.class);
        intent.putExtra("study_words", filteredWords);
        startActivity(intent);
    }

    private void showSortOptions() {
        String[] sortOptions = {"📖 الفبایی", "🎯 سطح (A1 تا C2)"};
        String[] sortValues = {"alphabetical", "level"};

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
                })
                .show();
    }

    private void setupClickListeners() {
        sortButton.setOnClickListener(v -> showSortOptions());

        clearButton.setOnClickListener(v -> {
            searchEditText.setText("");
            performSearch("");
        });

        resetButton.setOnClickListener(v -> resetAllMastered());
        studyButton.setOnClickListener(v -> startStudySession());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMasteredWords();
    }
}