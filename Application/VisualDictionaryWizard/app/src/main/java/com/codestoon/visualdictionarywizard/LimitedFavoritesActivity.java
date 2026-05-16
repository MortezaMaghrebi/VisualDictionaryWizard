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

public class LimitedFavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyView, statsText, limitWarningText;
    private ProgressBar progressBar;
    private LinearLayout shareButton, clearButton, studyButton;
    private TextView shareButtonText, clearButtonText, studyButtonText;
    private ImageView sortButton;
    private LinearLayout statsLayout, warningLayout;

    private DatabaseHelper dbHelper;
    private LimitedFavoriteAdapter favoriteAdapter;
    private ArrayList<HashMap<String, String>> favoritesList;

    private String currentSort = "alphabetical";
    private boolean isSelectionMode = false;
    private int currentSelectedCount = 0;

    private AppPrefsManager prefsManager;
    private boolean isPremium;
    private int maxFreeFavorites = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        prefsManager = AppPrefsManager.getInstance(this);
        isPremium = prefsManager.isPremium();

        initViews();
        setupToolbar();
        loadFavorites();
        setupClickListeners();
        showLimitWarning();
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
        sortButton = findViewById(R.id.sortButton);

        // اضافه کردن ویجت هشدار
        warningLayout = findViewById(R.id.warningLayout);
        limitWarningText = findViewById(R.id.limitWarningText);

        dbHelper = new DatabaseHelper(this);
        favoritesList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (isPremium) {
                getSupportActionBar().setTitle("❤️ علاقه‌مندی‌ها (نامحدود)");
            } else {
                getSupportActionBar().setTitle("❤️ علاقه‌مندی‌ها (" + maxFreeFavorites + " عدد رایگان)");
            }
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void showLimitWarning() {
        if (!isPremium && favoritesList.size() >= maxFreeFavorites) {
            warningLayout.setVisibility(View.VISIBLE);
            int lockedCount = favoritesList.size() - maxFreeFavorites;
            if (lockedCount > 0) {
                limitWarningText.setText("⚠️ شما به حداکثر " + maxFreeFavorites +
                        " کلمه رایگان رسیده‌اید.\n" + lockedCount +
                        " کلمه غیرفعال شده‌اند. برای دسترسی به همه کلمات، نسخه پریمیوم را تهیه کنید.");
            } else {
                limitWarningText.setText("⚠️ شما به حداکثر " + maxFreeFavorites +
                        " کلمه رایگان رسیده‌اید.\nبرای اضافه کردن کلمات بیشتر، نسخه پریمیوم را تهیه کنید.");
            }
        } else {
            warningLayout.setVisibility(View.GONE);
        }
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
        warningLayout.setVisibility(View.GONE);
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
        showLimitWarning();

        favoriteAdapter = new LimitedFavoriteAdapter(favoritesList,
                new LimitedFavoriteAdapter.OnFavoriteClickListener() {
                    @Override
                    public void onItemClick(HashMap<String, String> word, int position, boolean isActive) {
                        if (!isActive) {
                            // نمایش دیالوگ خرید پریمیوم
                            showPremiumDialogForLockedWord();
                            return;
                        }

                        if (isSelectionMode) {
                            favoriteAdapter.toggleSelection(position);
                        } else {
                            openWordDetail(word);
                        }
                    }

                    @Override
                    public void onFavoriteClick(HashMap<String, String> word, int position) {
                        // فقط برای کلمات فعال امکان حذف وجود دارد
                        if (position < maxFreeFavorites || isPremium) {
                            removeFromFavorites(word, position);
                        } else {
                            showPremiumDialogForLockedWord();
                        }
                    }

                    @Override
                    public boolean onLongClick(int position) {
                        if (position < maxFreeFavorites || isPremium) {
                            if (!isSelectionMode) {
                                enterSelectionMode();
                                favoriteAdapter.toggleSelection(position);
                                return true;
                            }
                        } else {
                            showPremiumDialogForLockedWord();
                        }
                        return false;
                    }

                    @Override
                    public void onSelectionChanged(int selectedCount) {
                        currentSelectedCount = selectedCount;
                        updateStudyButtonText();
                        updateShareButtonState();
                    }
                }, maxFreeFavorites, isPremium);

        recyclerView.setAdapter(favoriteAdapter);
        updateStatsText();

        if (isSelectionMode) {
            exitSelectionMode();
        }
    }

    private void showPremiumDialogForLockedWord() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔒 کلمه غیرفعال")
                .setMessage("برای دسترسی به این کلمه و تمام کلمات دیگر، باید نسخه پریمیوم را تهیه کنید.\n\n" +
                        "✨ مزایای نسخه پریمیوم:\n" +
                        "• دسترسی نامحدود به تمام کلمات\n" +
                        "• فلش‌کارت نامحدود\n" +
                        "• حذف تبلیغات\n" +
                        "• ذخیره نامحدود کلمات در علاقه‌مندی‌ها")
                .setPositiveButton("خرید پریمیوم", (dialog, which) -> {
                    BillingManager billingManager = BillingManager.getInstance(this);
                    if (billingManager.isReady()) {
                        billingManager.purchasePremium();
                    } else {
                        Toast.makeText(this, "در حال اتصال به بازار... لطفاً دوباره تلاش کنید", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("بعداً", null)
                .show();
    }

    private void updateStatsText() {
        if (isPremium) {
            statsText.setText(favoritesList.size() + " کلمه در لیست علاقه‌مندی‌ها (نامحدود)");
        } else {
            int activeCount = Math.min(favoritesList.size(), maxFreeFavorites);
            int lockedCount = favoritesList.size() - maxFreeFavorites;
            if (lockedCount > 0) {
                statsText.setText(activeCount + " فعال + " + lockedCount + " غیرفعال | مجموع: " + favoritesList.size());
            } else {
                statsText.setText(favoritesList.size() + " از " + maxFreeFavorites + " کلمه رایگان");
            }
        }
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
        Toast.makeText(this, "کلمات فعال را انتخاب کنید", Toast.LENGTH_SHORT).show();
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
            studyButtonText.setText("مطالعه");
            updateStudyButtonText();
            updateShareButtonState();
        } else {
            clearButtonText.setText("حذف همه");
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
                    showLimitWarning();

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
                Toast.makeText(this, "❌ " + wordText + " حذف شد", Toast.LENGTH_SHORT).show();
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
                // فقط کلمات فعال را به اشتراک بگذار
                if (pos < maxFreeFavorites || isPremium) {
                    wordsToShare.add(favoritesList.get(pos));
                }
            }
        } else {
            wordsToShare = new ArrayList<>();
            // فقط کلمات فعال را به اشتراک بگذار
            int limit = isPremium ? favoritesList.size() : Math.min(favoritesList.size(), maxFreeFavorites);
            for (int i = 0; i < limit; i++) {
                wordsToShare.add(favoritesList.get(i));
            }
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

        // فقط کلمات فعال (قابل دسترس) را برای مطالعه انتخاب کن
        wordsToStudy = new ArrayList<>();
        int limit = isPremium ? favoritesList.size() : Math.min(favoritesList.size(), maxFreeFavorites);

        for (int i = 0; i < limit; i++) {
            HashMap<String, String> word = favoritesList.get(i);
            String currentWord = word.get("word");
            boolean isMastered = dbHelper.isMastered(currentWord);
            if (!isMastered) {
                wordsToStudy.add(word);
            }
        }

        if (wordsToStudy.isEmpty()) {
            String message;
            if (!isPremium && favoritesList.size() > maxFreeFavorites) {
                message = "برای مطالعه کلمات بیشتر، لطفاً نسخه پریمیوم را تهیه کنید.\n\n" +
                        "کلمات فعال: " + Math.min(favoritesList.size(), maxFreeFavorites) + "\n" +
                        "کلمات غیرفعال: " + (favoritesList.size() - maxFreeFavorites);
            } else {
                message = "همه کلمات علاقه‌مندی شما قبلاً یاد گرفته شده‌اند!";
            }

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("📖 شروع مطالعه")
                    .setMessage(message)
                    .setPositiveButton("باشه", null)
                    .show();
            return;
        }

        Intent intent = new Intent(LimitedFavoritesActivity.this, StudySessionActivity.class);
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
        // بررسی تغییر وضعیت پریمیوم
        boolean newPremiumStatus = prefsManager.isPremium();
        if (newPremiumStatus != isPremium) {
            isPremium = newPremiumStatus;
            recreate(); // بازآفرینی اکتیویتی برای اعمال تغییرات
        } else if (favoritesList.isEmpty() || favoriteAdapter == null) {
            loadFavorites();
        }
    }
}