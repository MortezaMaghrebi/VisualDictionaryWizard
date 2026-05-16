package com.codestoon.visualdictionarywizard;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adivery.sdk.Adivery;
import com.adivery.sdk.AdiveryBannerAdView;
import com.adivery.sdk.AdiveryListener;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private RecyclerView levelRecyclerView, recentWordsRecyclerView;
    private DatabaseHelper dbHelper;
    private TextView welcomeText, wordCountText;
    private LinearLayout searchButton, favoritesButton, randomButton, masteredButton, otherAppsButton;
    private ProgressBar progressBar;
    private LinearLayout bannerAdLayout;
    private TextView bannerAdText;

    private String[] levels = {"A1", "A2", "B1", "B2", "C1", "C2"};
    private String[] levelNames = {"مبتدی 1", "مبتدی 2", "متوسط 1", "متوسط 2", "پیشرفته 1", "پیشرفته 2"};

    private AppPrefsManager prefsManager;
    private boolean isPremium = false;
    private BillingManager billingManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsManager = AppPrefsManager.getInstance(this);
        isPremium = prefsManager.isPremium();

        initViews();
        setupDatabase();
        setupClickListeners();
        setupLevelRecyclerView();
        setupBannerAd();

        displayWordCount();
        displayRecentFavorites();
        initializeBilling();
        setupAdivery();
    }
    private void initializeBilling(){
        billingManager = BillingManager.getInstance(this);
        billingManager.initializeBilling();
    }


    final String ADDIVERY_APP_ID = "779dbd87-6ba4-4cdd-9868-a3f0018af0f6";
    final String ADDIVERY_REWARD_ID = "32f45500-4ffe-4c60-afdc-f6255ea451e7";
    final String ADDIVERY_APPOPEN_ID = "0ea304f9-6d55-4971-92b0-fb246f28927a";
    final String ADDIVERY_BANNER_ID = "e354955a-a82c-418f-80cb-735ef2ecea85";

    private void setupAdivery() {
        Adivery.configure(getApplication(), ADDIVERY_APP_ID);
        loadBannerAdd();
        Adivery.prepareRewardedAd(MainActivity.this, ADDIVERY_REWARD_ID);
        addAdiveryGlobalListener();
    }

    private void loadBannerAdd() {
        AdiveryBannerAdView bannerAd = findViewById(R.id.banner_ad);
        if (bannerAd != null) bannerAd.loadAd(ADDIVERY_BANNER_ID);
    }

    boolean addAlertShown=false;
    private void addAdiveryGlobalListener() {
        Adivery.addGlobalListener(new AdiveryListener() {
            @Override public void onAppOpenAdLoaded(String placementId) {}
            @Override public void onInterstitialAdLoaded(String placementId) {}
            @Override public void onRewardedAdLoaded(String placementId) {}
            @Override public void onRewardedAdClosed(String placementId, boolean isRewarded) {
                if (!isRewarded) {

                } else {

                }
            }
            @Override public void log(String placementId, String log) {}
        });
    }



    public boolean showRewardAdd() {

        // ✅ بررسی پریمیوم بودن کاربر
        if (billingManager != null && billingManager.isPremiumActivated()) {
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
        return true; // بدون تبلیغ پخش کن
    }

    private int rewardedCountToday() {
        SharedPreferences prefs = getSharedPreferences("ad_prefs", MODE_PRIVATE);
        String today = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(new java.util.Date());
        return prefs.getInt("rewarded_" + today, 0);
    }

    private void incrementRewardedCountToday() {
        SharedPreferences prefs = getSharedPreferences("ad_prefs", MODE_PRIVATE);
        String today = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(new java.util.Date());
        int count = prefs.getInt("rewarded_" + today, 0);
        prefs.edit().putInt("rewarded_" + today, count + 1).apply();
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
        otherAppsButton = findViewById(R.id.otherAppsButton);
        bannerAdLayout = findViewById(R.id.bannerAdLayout);
        bannerAdText = findViewById(R.id.bannerAdText);

        dbHelper = new DatabaseHelper(this);
    }

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
                showWelcomeMessage();
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

    private void setupClickListeners() {
        searchButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
        });

        favoritesButton.setOnClickListener(v -> {
            // بررسی محدودیت علاقه‌مندی برای کاربران رایگان
            if (!isPremium) {
                new Thread(() -> {
                    int favoriteCount = dbHelper.getFavorites().size();
                    int maxFavorites = prefsManager.getMaxFreeFavorites();
                    runOnUiThread(() -> {
                        if (favoriteCount >= maxFavorites) {
                            // اگر به محدودیت رسیده، اکتیویتی محدود شده را باز کن
                            startActivity(new Intent(MainActivity.this, LimitedFavoritesActivity.class));
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

        otherAppsButton.setOnClickListener(v -> {
            StoreIntents.openDeveloperPage(this);
        });

        // Banner ad click - show upsell for premium
        bannerAdLayout.setOnClickListener(v -> {
            if (!isPremium) {
                PremiumUpsellDialog.show(MainActivity.this,
                        "✨ نسخه پریمیوم: حذف تبلیغات + فلش‌کارت نامحدود + ذخیره نامحدود کلمات",
                        new PremiumUpsellDialog.OnPurchaseListener() {
                            @Override
                            public void onPurchaseClicked() {
                                BillingManager billingManager = BillingManager.getInstance(MainActivity.this);
                                if (billingManager.isReady()) {
                                    billingManager.purchasePremium();
                                } else {
                                    Toast.makeText(MainActivity.this, "در حال اتصال به بازار...", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onCancel() {}
                        });
            }
        });
    }

    private void setupBannerAd() {
        if (isPremium) {
            bannerAdLayout.setVisibility(View.GONE);
        } else {
            bannerAdLayout.setVisibility(View.VISIBLE);
            bannerAdText.setText("✨ نسخه پریمیوم: فلش‌کارت نامحدود + حذف تبلیغات");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if premium status changed after returning from purchase
        boolean newPremiumStatus = prefsManager.isPremium();
        if (newPremiumStatus != isPremium) {
            isPremium = newPremiumStatus;
            setupBannerAd();
        }
    }
}