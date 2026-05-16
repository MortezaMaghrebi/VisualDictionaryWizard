package com.codestoon.visualdictionarywizard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adivery.sdk.Adivery;
import com.adivery.sdk.AdiveryBannerAdView;
import com.adivery.sdk.AdiveryListener;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Views
    private RecyclerView levelRecyclerView;
    private RecyclerView recentWordsRecyclerView;
    private TextView welcomeText;
    private TextView wordCountText;
    private TextView tvPremiumPurchase;
    private LinearLayout searchButton;
    private LinearLayout favoritesButton;
    private LinearLayout randomButton;
    private LinearLayout masteredButton;
    private LinearLayout otherAppsButton;
    private ProgressBar progressBar;
    private LinearLayout bannerAdContainer;
    private AdiveryBannerAdView bannerAd;

    // Database
    private DatabaseHelper dbHelper;

    // Managers
    private AppPrefsManager prefsManager;
    private BillingManager billingManager;
    private boolean isPremium = false;

    // Level data
    private String[] levels = {"A1", "A2", "B1", "B2", "C1", "C2"};
    private String[] levelNames = {"مبتدی 1", "مبتدی 2", "متوسط 1", "متوسط 2", "پیشرفته 1", "پیشرفته 2"};

    // Adivery IDs
    private static final String ADDIVERY_APP_ID = "779dbd87-6ba4-4cdd-9868-a3f0018af0f6";
    private static final String ADDIVERY_REWARD_ID = "32f45500-4ffe-4c60-afdc-f6255ea451e7";
    private static final String ADDIVERY_BANNER_ID = "e354955a-a82c-418f-80cb-735ef2ecea85";

    // برای ردیابی رویدادهای نظرخواهی
    private int studySessionCount = 0;
    private int favoriteAddedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize managers
        prefsManager = AppPrefsManager.getInstance(this);

        // Initialize views
        initViews();

        // Initialize database
        dbHelper = new DatabaseHelper(this);

        // Initialize billing
        initializeBilling();


        // Initialize adivery
        setupAdivery();

        // Setup UI
        setupDatabase();
        setupClickListeners();
        setupLevelRecyclerView();
        setupBannerAdVisibility();

        // Load data
        displayWordCount();
        displayRecentFavorites();

        // Show welcome message
        showWelcomeMessage();
    }

    // ==================== INITIALIZATION METHODS ====================

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
        otherAppsButton = findViewById(R.id.otherAppsButton);
        bannerAdContainer = findViewById(R.id.bannerAdContainer);
        tvPremiumPurchase = findViewById(R.id.tvPremiumPurchase);
        bannerAd = findViewById(R.id.banner_ad);
    }

    private void initializeBilling() {
        billingManager = BillingManager.getInstance(this);
        billingManager.initializeBilling();
        isPremium = billingManager.isPremiumActivated();
        // بررسی وضعیت پریمیوم بعد از اتصال
        new Thread(() -> {
            runOnUiThread(() -> {
                setupBannerAdVisibility();
            });
        }).start();
    }

    private void setupAdivery() {
        try {
            Adivery.configure(getApplication(), ADDIVERY_APP_ID);

            // فقط برای کاربران غیر پریمیوم بنر را آماده کن
            if (!isPremium) {
                if (bannerAd != null) {
                    bannerAd.loadAd(ADDIVERY_BANNER_ID);
                }
                Adivery.prepareRewardedAd(MainActivity.this, ADDIVERY_REWARD_ID);
            }

            addAdiveryGlobalListener();
            Log.d(TAG, "Adivery initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Adivery initialization error: " + e.getMessage());
        }
    }

    private void addAdiveryGlobalListener() {
        Adivery.addGlobalListener(new AdiveryListener() {
            @Override
            public void onAppOpenAdLoaded(String placementId) {
            }

            @Override
            public void onInterstitialAdLoaded(String placementId) {
            }

            @Override
            public void onRewardedAdLoaded(String placementId) {
                Log.d(TAG, "Rewarded ad loaded: " + placementId);
            }

            @Override
            public void onRewardedAdClosed(String placementId, boolean isRewarded) {
                Log.d(TAG, "Rewarded ad closed, rewarded: " + isRewarded);
            }

            @Override
            public void log(String placementId, String log) {
            }
        });
    }

    // ==================== UI SETUP METHODS ====================

    private void setupLevelRecyclerView() {
        LevelAdapter levelAdapter = new LevelAdapter(levels, levelNames, (level, levelName) -> {
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
            });
        }).start();
    }

    private void showWelcomeMessage() {
        String[] greetings = {"به دیکشنری تصویری خوش آمدید", "آماده یادگیری هستی؟"};
        int randomIndex = (int) (Math.random() * greetings.length);
        welcomeText.setText(greetings[randomIndex]);
    }

    private void displayWordCount() {
        new Thread(() -> {
            int count = dbHelper.getWordCount();
            runOnUiThread(() -> {
                if (count > 0) {
                    wordCountText.setText("📚 " + count + " کلمه");
                } else {
                    wordCountText.setText("📚 34,520 کلمه");
                }
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
                    // می‌توانید اینجا کلمات را در RecyclerView نمایش بدید
                }
            });
        }).start();
    }

    private void setupBannerAdVisibility() {
        if (bannerAdContainer == null) return;
        if (isPremium) {
            bannerAdContainer.setVisibility(View.GONE);
        } else {
            bannerAdContainer.setVisibility(View.VISIBLE);
        }
    }

    // ==================== CLICK LISTENERS ====================

    private void setupClickListeners() {
        // دکمه جستجو
        searchButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
        });

        // دکمه علاقه‌مندی‌ها (با محدودیت برای کاربران رایگان)
        favoritesButton.setOnClickListener(v -> {
            if (!isPremium) {
                new Thread(() -> {
                    int favoriteCount = dbHelper.getFavorites().size();
                    int maxFavorites = prefsManager.getMaxFreeFavorites();
                    runOnUiThread(() -> {
                        if (favoriteCount >= maxFavorites) {
                            // اگر به محدودیت رسیده، اکتیویتی محدود شده را باز کن
                            Intent intent = new Intent(MainActivity.this, LimitedFavoritesActivity.class);
                            startActivity(intent);
                        } else {
                            startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
                        }
                    });
                }).start();
            } else {
                // کاربر پریمیوم - اکتیویتی عادی
                startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
            }
        });

        // دکمه کلمات یادگرفته شده
        masteredButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MasteredWordsActivity.class));
        });

        // دکمه کلمه تصادفی
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

        // دکمه سایر اپلیکیشن‌های ما
        otherAppsButton.setOnClickListener(v -> {
            StoreIntents.openDeveloperPage(this);
        });

        tvPremiumPurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                billingManager.purchasePremium();
            }
        });
    }

    // ==================== ADVERTISING METHODS ====================

    public boolean showRewardedAd() {
        // بررسی پریمیوم بودن کاربر
        if (isPremium) {
            Log.d(TAG, "User is PREMIUM, no ads shown");
            return true; // بدون تبلیغ
        }

        // فقط اگر تعداد تبلیغات امروز کمتر از 3 است، نشان بده
        if (rewardedCountToday() < 3) {
            if (Adivery.isLoaded(ADDIVERY_REWARD_ID)) {
                Adivery.showAd(ADDIVERY_REWARD_ID);
                incrementRewardedCountToday();
                return true;
            } else {
                Adivery.prepareRewardedAd(MainActivity.this, ADDIVERY_REWARD_ID);
                return false;
            }
        }

        Toast.makeText(this, "امروز از حد مجاز تبلیغات استفاده کردید", Toast.LENGTH_SHORT).show();
        return true; // بدون تبلیغ پخش کن
    }

    private int rewardedCountToday() {
        SharedPreferences prefs = getSharedPreferences("ad_prefs", MODE_PRIVATE);
        String today = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                .format(new java.util.Date());
        return prefs.getInt("rewarded_" + today, 0);
    }

    private void incrementRewardedCountToday() {
        SharedPreferences prefs = getSharedPreferences("ad_prefs", MODE_PRIVATE);
        String today = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                .format(new java.util.Date());
        int count = prefs.getInt("rewarded_" + today, 0);
        prefs.edit().putInt("rewarded_" + today, count + 1).apply();
    }

    // ==================== RATING & FEEDBACK METHODS ====================

    public void onStudySessionCompleted() {
        studySessionCount++;
        checkAndShowRatingDialog();
    }

    public void onFavoriteAdded() {
        favoriteAddedCount++;
        checkAndShowRatingDialog();
    }

    private void checkAndShowRatingDialog() {
        // هر 3 جلسه مطالعه یا هر 5 کلمه اضافه شده به علاقه‌مندی‌ها
        if (studySessionCount >= 3 || favoriteAddedCount >= 5) {
            studySessionCount = 0;
            favoriteAddedCount = 0;
            RatingManager.showRatingDialogIfNeeded(this);
        }
    }

    // ==================== LIFE CYCLE METHODS ====================

    @Override
    protected void onResume() {
        super.onResume();

        // بررسی تغییر وضعیت پریمیوم بعد از بازگشت از خرید
        boolean newPremiumStatus = prefsManager.isPremium();
        if (newPremiumStatus != isPremium) {
            isPremium = newPremiumStatus;
            setupBannerAdVisibility();

            // اگر پریمیوم فعال شده، تبلیغات جایزه‌دار را غیرفعال کن
            if (isPremium) {
                Adivery.prepareRewardedAd(MainActivity.this, ADDIVERY_REWARD_ID);
            }
        }

        // به‌روزرسانی تعداد کلمات در صورت نیاز
        displayWordCount();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingManager != null) {
            billingManager.onDestroy();
        }
    }

    // ==================== GETTERS ====================

    public BillingManager getBillingManager() {
        return billingManager;
    }

    public boolean isPremium() {
        return isPremium;
    }
}