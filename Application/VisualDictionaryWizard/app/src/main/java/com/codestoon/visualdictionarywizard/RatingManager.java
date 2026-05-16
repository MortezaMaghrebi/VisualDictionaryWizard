package com.codestoon.visualdictionarywizard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;

public class RatingManager {

    public interface RatingCallback {
        void onRatingRequested();
    }

    public static void showRatingDialogIfNeeded(Activity activity, Runnable onComplete) {
        AppPrefsManager prefs = AppPrefsManager.getInstance(activity);

        // اگر کاربر پریمیوم باشد، درخواست نظر نمایش داده نمی‌شود
        if (prefs.isPremium()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        if (!prefs.shouldShowRatingDialog()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("⭐ از دیکشنری تصویری راضی هستید؟")
                .setMessage("نظر شما به ما کمک می‌کند تا اپلیکیشن را بهتر کنیم.\n\n"
                        + "اگر از اپلیکیشن لذت می‌برید، لطفاً با دادن امتیاز ۵ ستاره از ما حمایت کنید.")
                .setPositiveButton("⭐ امتیاز بده", (dialog, which) -> {
                    prefs.setUserRated();
                    StoreIntents.openStoreForComment(activity);
                    if (onComplete != null) onComplete.run();
                })
                .setNegativeButton("نه، فعلاً", (dialog, which) -> {
                    prefs.setUserDeclinedRating();
                    if (onComplete != null) onComplete.run();
                })
                .setNeutralButton("بازخورد بدهم", (dialog, which) -> {
                    // رفتن به صفحه نظر منفی
                    Intent intent = new Intent(activity, CommentNegativeActivity.class);
                    activity.startActivity(intent);
                    if (onComplete != null) onComplete.run();
                })
                .setCancelable(false)
                .show();

        prefs.registerRatingRequest();
    }



    public static void showRatingDialogIfNeeded(Activity activity) {
        showRatingDialogIfNeeded(activity, null);
    }
}