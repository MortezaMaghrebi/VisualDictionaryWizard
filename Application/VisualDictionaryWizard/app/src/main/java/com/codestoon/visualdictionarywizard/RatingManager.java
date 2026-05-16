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
                    openStoreForRating(activity);
                    if (onComplete != null) onComplete.run();
                })
                .setNegativeButton("نه، فعلاً", (dialog, which) -> {
                    prefs.setUserDeclinedRating();
                    if (onComplete != null) onComplete.run();
                })
                .setNeutralButton("بعداً یادآوری کن", (dialog, which) -> {
                    prefs.registerRatingRequest();
                    if (onComplete != null) onComplete.run();
                })
                .setCancelable(false)
                .show();

        prefs.registerRatingRequest();
    }

    private static void openStoreForRating(Activity activity) {
        // اولویت: کافه بازار > گوگل پلی
        String packageName = activity.getPackageName();

        try {
            // تلاش برای باز کردن کافه بازار
            Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setData(android.net.Uri.parse("bazaar://details?id=" + packageName));
            intent.setPackage("com.farsitel.bazaar");
            activity.startActivity(intent);
        } catch (Exception e) {
            try {
                // تلاش برای باز کردن گوگل پلی
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.parse("market://details?id=" + packageName));
                activity.startActivity(intent);
            } catch (Exception e2) {
                // آخرین راه: لینک وب
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.parse("https://cafebazaar.ir/app/" + packageName));
                activity.startActivity(intent);
            }
        }
    }

    // نسخه ساده بدون callback
    public static void showRatingDialogIfNeeded(Activity activity) {
        showRatingDialogIfNeeded(activity, null);
    }
}