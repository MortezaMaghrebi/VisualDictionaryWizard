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

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyView, statsText;
    private ProgressBar progressBar;
    private LinearLayout shareButton, clearButton, studyButton;
    private TextView shareButtonText, clearButtonText, studyButtonText;
    private ImageView sortButton, clearButtonImage;
    private LinearLayout statsLayout;

    private DatabaseHelper dbHelper;
    private FavoriteAdapter favoriteAdapter;
    private ArrayList<HashMap<String, String>> favoritesList;

    private String currentSort = "alphabetical";
    private boolean isSelectionMode = false;
    private int currentSelectedCount = 0;  // اضافه کردن متغیر برای ذخیره تعداد انتخاب

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        initViews();
        setupToolbar();
        loadFavorites();
        setupClickListeners();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);
        statsText = findViewById(R.id.statsText);
        statsLayout = findViewById(R.id.statsLayout);
        shareButton = findViewById(R.id.shareButton);
        clearButton = findViewById(R.id.clearButton);
        studyButton = findViewById(R.id.studyButton);
        shareButtonText = findViewById(R.id.shareButtonText);
        clearButtonText = findViewById(R.id.clearButtonText);
        studyButtonText = findViewById(R.id.studyButtonText);
        clearButtonImage = findViewById(R.id.clearButtonImage);
        sortButton = findViewById(R.id.sortButton);

        dbHelper = new DatabaseHelper(this);
        favoritesList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("❤️ علاقه‌مندی‌ها");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadFavorites() {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            favoritesList = dbHelper.getFavoritesWithDetails();
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);

                if (favoritesList.isEmpty()) {
                    showEmptyState();
                } else {
                    showFavoritesList();
                }
            });
        }).start();
    }

    private void showEmptyState() {
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        statsLayout.setVisibility(View.GONE);
        shareButton.setEnabled(false);
        clearButton.setEnabled(false);
        studyButton.setEnabled(false);
        emptyView.setText("💔 هنوز کلمه‌ای به علاقه‌مندی‌ها اضافه نکردید");
        isSelectionMode = false;
        currentSelectedCount = 0;
    }

    private void showFavoritesList() {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        statsLayout.setVisibility(View.VISIBLE);

        favoriteAdapter = new FavoriteAdapter(favoritesList, new FavoriteAdapter.OnFavoriteClickListener() {
            @Override
            public void onItemClick(HashMap<String, String> word, int position) {
                if (isSelectionMode) {
                    favoriteAdapter.toggleSelection(position);
                    // تعداد در onSelectionChanged آپدیت میشه
                } else {
                    openWordDetail(word);
                }
            }

            @Override
            public void onFavoriteClick(HashMap<String, String> word, int position) {
                removeFromFavorites(word, position);
            }

            @Override
            public boolean onLongClick(int position) {
                if (!isSelectionMode) {
                    enterSelectionMode();
                    favoriteAdapter.toggleSelection(position);
                    return true;
                }
                return false;
            }

            @Override
            public void onSelectionChanged(int selectedCount) {
                // وقتی تعداد انتخاب‌ها تغییر کرد، این متد صدا زده میشه
                currentSelectedCount = selectedCount;
                updateStudyButtonText();
                updateShareButtonState();
            }
        });

        recyclerView.setAdapter(favoriteAdapter);
        updateStatsText();

        if (isSelectionMode) {
            exitSelectionMode();
        }
    }

    private void updateStatsText() {
        statsText.setText(favoritesList.size() + " کلمه در لیست علاقه‌مندی‌ها");
    }

    private void updateStudyButtonText() {
        if (isSelectionMode && currentSelectedCount > 0) {
            studyButtonText.setText("مطالعه (" + currentSelectedCount + ")");
        } else if (isSelectionMode) {
            studyButtonText.setText("مطالعه");
        } else {
            studyButtonText.setText("شروع مطالعه");
        }
    }

    private void updateShareButtonState() {
        if (isSelectionMode) {
            shareButton.setEnabled(currentSelectedCount > 0);
            shareButton.setAlpha(currentSelectedCount > 0 ? 1.0f : 0.5f);
        } else {
            shareButton.setEnabled(!favoritesList.isEmpty());
            shareButton.setAlpha(1.0f);
        }
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        if (favoriteAdapter != null) {
            favoriteAdapter.setSelectionMode(true);
            currentSelectedCount = favoriteAdapter.getSelectedCount();
        }
        updateUIForSelectionMode();
        Toast.makeText(this, "کلمات مورد نظر را انتخاب کنید", Toast.LENGTH_SHORT).show();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        currentSelectedCount = 0;
        if (favoriteAdapter != null) {
            favoriteAdapter.setSelectionMode(false);
        }
        updateUIForSelectionMode();
    }

    private void updateUIForSelectionMode() {
        if (isSelectionMode) {
            clearButtonText.setText("بازگشت");
            clearButtonImage.setImageResource(R.drawable.ic_exit);
            updateStudyButtonText();
            updateShareButtonState();
            studyButton.setAlpha(1.0f);
        } else {
            clearButtonText.setText("حذف همه");
            clearButtonImage.setImageResource(R.drawable.ic_delete);
            studyButtonText.setText("شروع مطالعه");
            shareButton.setEnabled(!favoritesList.isEmpty());
            shareButton.setAlpha(1.0f);
            studyButton.setAlpha(1.0f);
        }
    }

    private void removeFromFavorites(HashMap<String, String> word, int position) {
        String wordText = word.get("word");

        new Thread(() -> {
            dbHelper.toggleFavorite(wordText);
            runOnUiThread(() -> {
                favoritesList.remove(position);
                if (favoriteAdapter != null) {
                    favoriteAdapter.notifyItemRemoved(position);
                    updateStatsText();

                    if (favoritesList.isEmpty()) {
                        if (isSelectionMode) {
                            exitSelectionMode();
                        }
                        showEmptyState();
                    } else {
                        currentSelectedCount = favoriteAdapter.getSelectedCount();
                        updateStudyButtonText();
                    }
                }
                //Toast.makeText(this, "❌ " + wordText + " حذف شد", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void clearAllFavorites() {
        if (favoritesList.isEmpty()) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("حذف تمام علاقه‌مندی‌ها")
                .setMessage("آیا از حذف همه " + favoritesList.size() + " کلمه مطمئن هستید؟")
                .setPositiveButton("حذف", (dialog, which) -> {
                    new Thread(() -> {
                        for (HashMap<String, String> word : favoritesList) {
                            dbHelper.toggleFavorite(word.get("word"));
                        }
                        runOnUiThread(() -> {
                            favoritesList.clear();
                            if (isSelectionMode) {
                                exitSelectionMode();
                            }
                            showEmptyState();
                            Toast.makeText(this, "همه علاقه‌مندی‌ها حذف شد", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    private void shareFavorites() {
        ArrayList<HashMap<String, String>> wordsToShare;

        if (isSelectionMode && favoriteAdapter != null && currentSelectedCount > 0) {
            wordsToShare = new ArrayList<>();
            for (int pos : favoriteAdapter.getSelectedPositions()) {
                wordsToShare.add(favoritesList.get(pos));
            }
        } else {
            wordsToShare = favoritesList;
        }

        if (wordsToShare.isEmpty()) {
            Toast.makeText(this, "کلمه‌ای برای اشتراک‌گذاری وجود ندارد", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder shareText = new StringBuilder();
        shareText.append("📚 لیست کلمات مورد علاقه من:\n\n");

        for (HashMap<String, String> word : wordsToShare) {
            shareText.append("• ").append(word.get("word"))
                    .append(": ").append(word.get("persian"))
                    .append(" (").append(word.get("level")).append(")\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری"));
    }

    private void openWordDetail(HashMap<String, String> word) {
        Intent intent = new Intent(this, WordDetailActivity.class);
        intent.putExtra("word", word.get("word"));
        startActivity(intent);
    }

    private void setupClickListeners() {
        sortButton.setOnClickListener(v -> {
            Toast.makeText(this, "مرتب‌سازی", Toast.LENGTH_SHORT).show();
        });

        shareButton.setOnClickListener(v -> shareFavorites());

        clearButton.setOnClickListener(v -> {
            if (isSelectionMode) {
                exitSelectionMode();
            } else {
                clearAllFavorites();
            }
        });

        studyButton.setOnClickListener(v -> startStudySession());
    }

    private void startStudySession() {
        ArrayList<HashMap<String, String>> wordsToStudy;

        if (isSelectionMode && favoriteAdapter != null && currentSelectedCount > 0) {
            wordsToStudy = new ArrayList<>();
            for (int pos : favoriteAdapter.getSelectedPositions()) {
                HashMap<String, String> word = favoritesList.get(pos);
                wordsToStudy.add(word);
            }
        } else {
            wordsToStudy = new ArrayList<>();
            for (HashMap<String, String> word : favoritesList) {
                String currentWord = word.get("word");
                boolean isMastered = dbHelper.isMastered(currentWord);
                if (!isMastered) {
                    wordsToStudy.add(word);
                }
            }
        }

        if (wordsToStudy.isEmpty()) {
            String message;
            if (!isSelectionMode) {
                message = "همه کلمات علاقه‌مندی شما قبلاً یاد گرفته شده‌اند!\nاز صفحه کلمات یادگرفته شده می‌توانید مرور کنید.";
            } else {
                message = "کلمه‌ای برای مطالعه انتخاب نشده";
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            if (!isSelectionMode && favoritesList.isEmpty()) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("🎉 همه کلمات رو یاد گرفتی!")
                        .setMessage("تمام کلمات علاقه‌مندی شما قبلاً یاد گرفته شده‌اند.\nمی‌خواهید کلمات یادگرفته شده را مرور کنید؟")
                        .setPositiveButton("مشاهده", (dialog, which) -> {
                            Intent intent = new Intent(FavoritesActivity.this, MasteredWordsActivity.class);
                            startActivity(intent);
                        })
                        .setNegativeButton("بعداً", null)
                        .show();
            }
            return;
        }

        Intent intent = new Intent(FavoritesActivity.this, StudySessionActivity.class);
        intent.putExtra("study_words", wordsToStudy);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (isSelectionMode) {
            exitSelectionMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (favoritesList.isEmpty() || favoriteAdapter == null) {
            loadFavorites();
        }
    }
}