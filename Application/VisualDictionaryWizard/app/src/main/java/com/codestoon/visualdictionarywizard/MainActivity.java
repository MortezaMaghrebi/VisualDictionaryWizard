package com.codestoon.visualdictionarywizard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private RecyclerView levelRecyclerView, recentWordsRecyclerView;
    private DatabaseHelper dbHelper;
    private TextView welcomeText, wordCountText;
    private Button searchButton, favoritesButton, randomButton;
    private ProgressBar progressBar;
    private Button masteredButton;
    private String[] levels = {"A1", "A2", "B1", "B2", "C1", "C2"};
    private String[] levelNames = {"مبتدی 1", "مبتدی 2", "متوسط 1", "متوسط 2", "پیشرفته 1", "پیشرفته 2"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupDatabase();
        setupClickListeners();
        setupLevelRecyclerView();  // ← این رو جدا کردم

        displayWordCount();
        displayRecentFavorites();
    }

    private void initViews() {
        levelRecyclerView = findViewById(R.id.levelRecyclerView);
        recentWordsRecyclerView = findViewById(R.id.recentWordsRecyclerView);
        welcomeText = findViewById(R.id.welcomeText);
        wordCountText = findViewById(R.id.wordCountText);
        searchButton = findViewById(R.id.searchButton);
        favoritesButton = findViewById(R.id.favoritesButton);
        randomButton = findViewById(R.id.randomButton);
        progressBar = findViewById(R.id.progressBar);
        masteredButton = findViewById(R.id.masteredButton);
        dbHelper = new DatabaseHelper(this);
    }

    // ✅ اصلاح شده: این متد رو جداگانه نوشتم
    private void setupLevelRecyclerView() {
        LevelAdapter levelAdapter = new LevelAdapter(levels, levelNames, (level, levelName) -> {
            // رفتن به صفحه کلمات سطح
            Intent intent = new Intent(MainActivity.this, LevelWordsActivity.class);
            intent.putExtra("level", level);
            intent.putExtra("level_name", levelName);
            startActivity(intent);
        });

        new Thread(() -> {
            HashMap<String, Integer> levelCounts = dbHelper.getLevelWordCounts();
            runOnUiThread(() -> {
                levelAdapter.setLevelCounts(levelCounts);
            });
        }).start();

        levelRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        levelRecyclerView.setAdapter(levelAdapter);
    }

    private void setupDatabase() {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            dbHelper.loadDataFromAssetsIfNeeded();
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                showWelcomeMessage();
            });
        }).start();
    }

    private void showWelcomeMessage() {
        String[] greetings = {"سلام! وقت بخیر", "به دیکشنری تصویری خوش آمدید", "آماده یادگیری ای؟"};
        int randomIndex = (int) (Math.random() * greetings.length);
        welcomeText.setText(greetings[randomIndex]);
    }

    private void displayWordCount() {
        new Thread(() -> {
            int count = dbHelper.getWordCount();
            runOnUiThread(() -> {
                wordCountText.setText("📚 " + count + " کلمه");
            });
        }).start();
    }

    private void displayRecentFavorites() {
        new Thread(() -> {
            ArrayList<HashMap<String, String>> favorites = dbHelper.getFavorites();
            int recentCount = Math.min(favorites.size(), 5);
            ArrayList<String> recentWords = new ArrayList<>();
            for (int i = 0; i < recentCount; i++) {
                recentWords.add(favorites.get(i).get("word"));
            }
            runOnUiThread(() -> {
                if (!recentWords.isEmpty()) {
                    // می‌تونید اینجا کلمات رو در RecyclerView نمایش بدید
                }
            });
        }).start();
    }

    private void setupClickListeners() {
        searchButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
        });

        favoritesButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
        });

        randomButton.setOnClickListener(v -> {
            new Thread(() -> {
                String randomWord = dbHelper.getRandomWord();
                runOnUiThread(() -> {
                    Intent intent = new Intent(MainActivity.this, WordDetailActivity.class);
                    intent.putExtra("word", randomWord);
                    startActivity(intent);
                });
            }).start();
        });
        masteredButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MasteredWordsActivity.class));
        });
    }
}