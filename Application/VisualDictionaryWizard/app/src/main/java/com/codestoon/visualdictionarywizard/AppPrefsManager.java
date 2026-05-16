package com.codestoon.visualdictionarywizard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;

public class AppPrefsManager {
    private static final String PREFS_NAME = "visual_dict_prefs";
    private static final String KEY_IS_PREMIUM = "is_premium";
    private static final String KEY_FREE_FLASHCARD_COUNT = "free_flashcard_count";
    private static final String KEY_LAST_FLASHCARD_DATE = "last_flashcard_date";
    private static final String KEY_RATING_LAST_REQUEST_DATE = "rating_last_request_date";
    private static final String KEY_RATING_REQUEST_COUNT = "rating_request_count";
    private static final String KEY_USER_RATED = "user_rated";
    private static final String KEY_USER_DECLINED_RATING = "user_declined_rating";

    private static AppPrefsManager instance;
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    private AppPrefsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public static synchronized AppPrefsManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppPrefsManager(context);
        }
        return instance;
    }

    // ========== Premium ==========
    public boolean isPremium() {
        return prefs.getBoolean(KEY_IS_PREMIUM, false);
    }

    public void setPremium(boolean isPremium) {
        editor.putBoolean(KEY_IS_PREMIUM, isPremium);
        editor.apply();
    }

    // ========== Flashcard Limits for Free Users ==========
    private static final int MAX_FREE_FLASHCARDS_PER_DAY = 15;

    public int getRemainingFreeFlashcards() {
        if (isPremium()) {
            return Integer.MAX_VALUE; // unlimited
        }

        String todayDate = getTodayDateString();
        String savedDate = prefs.getString(KEY_LAST_FLASHCARD_DATE, "");

        if (!todayDate.equals(savedDate)) {
            // new day - reset count
            editor.putString(KEY_LAST_FLASHCARD_DATE, todayDate);
            editor.putInt(KEY_FREE_FLASHCARD_COUNT, MAX_FREE_FLASHCARDS_PER_DAY);
            editor.apply();
            return MAX_FREE_FLASHCARDS_PER_DAY;
        }

        return prefs.getInt(KEY_FREE_FLASHCARD_COUNT, MAX_FREE_FLASHCARDS_PER_DAY);
    }

    public boolean consumeFlashcard() {
        if (isPremium()) {
            return true;
        }

        int remaining = getRemainingFreeFlashcards();
        if (remaining <= 0) {
            return false;
        }

        editor.putInt(KEY_FREE_FLASHCARD_COUNT, remaining - 1);
        editor.apply();
        return true;
    }

    public int getUsedFlashcardsToday() {
        if (isPremium()) return 0;
        int remaining = getRemainingFreeFlashcards();
        return MAX_FREE_FLASHCARDS_PER_DAY - remaining;
    }

    private String getTodayDateString() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
    }

    // ========== Rating Management ==========
    private static final int MIN_DAYS_BETWEEN_RATING_REQUESTS = 3;
    private static final int MAX_RATING_REQUESTS = 3;

    public boolean shouldShowRatingDialog() {
        if (prefs.getBoolean(KEY_USER_RATED, false)) {
            return false;
        }

        if (prefs.getBoolean(KEY_USER_DECLINED_RATING, false)) {
            // check if enough time passed
            long lastDeclineDate = prefs.getLong(KEY_RATING_LAST_REQUEST_DATE, 0);
            long daysPassed = (System.currentTimeMillis() - lastDeclineDate) / (24 * 60 * 60 * 1000);
            if (daysPassed < MIN_DAYS_BETWEEN_RATING_REQUESTS) {
                return false;
            }
        }

        int requestCount = prefs.getInt(KEY_RATING_REQUEST_COUNT, 0);
        if (requestCount >= MAX_RATING_REQUESTS) {
            return false;
        }

        return true;
    }

    public void registerRatingRequest() {
        int count = prefs.getInt(KEY_RATING_REQUEST_COUNT, 0);
        editor.putInt(KEY_RATING_REQUEST_COUNT, count + 1);
        editor.putLong(KEY_RATING_LAST_REQUEST_DATE, System.currentTimeMillis());
        editor.apply();
    }

    public void setUserRated() {
        editor.putBoolean(KEY_USER_RATED, true);
        editor.apply();
    }

    public void setUserDeclinedRating() {
        editor.putBoolean(KEY_USER_DECLINED_RATING, true);
        editor.putLong(KEY_RATING_LAST_REQUEST_DATE, System.currentTimeMillis());
        editor.apply();
    }

    // ========== Favorite Limit for Free Users (Optional) ==========
    private static final int MAX_FREE_FAVORITES = 20;

    public boolean canAddToFavorites(int currentFavoritesCount) {
        if (isPremium()) return true;
        return currentFavoritesCount < MAX_FREE_FAVORITES;
    }

    public int getMaxFreeFavorites() {
        return MAX_FREE_FAVORITES;
    }
}