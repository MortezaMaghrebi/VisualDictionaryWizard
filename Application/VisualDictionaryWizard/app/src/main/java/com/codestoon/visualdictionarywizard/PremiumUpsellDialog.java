package com.codestoon.visualdictionarywizard;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PremiumUpsellDialog {

    public interface OnPurchaseListener {
        void onPurchaseClicked();
        void onCancel();
    }

    public static void show(Activity activity, String message, OnPurchaseListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_premium_upsell, null);

        TextView messageText = view.findViewById(R.id.upsellMessageText);
        Button purchaseButton = view.findViewById(R.id.purchaseButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);

        messageText.setText(message);

        AlertDialog dialog = builder.setView(view)
                .setCancelable(false)
                .create();

        purchaseButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onPurchaseClicked();
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onCancel();
        });

        dialog.show();
    }

    public static void showLimitReached(Activity activity, OnPurchaseListener listener) {
        show(activity,
                "❗ محدودیت روزانه فلش‌کارت به پایان رسید.\n\n"
                        + "نسخه پریمیوم شامل:\n"
                        + "✅ فلش‌کارت نامحدود\n"
                        + "✅ ذخیره نامحدود کلمات در علاقه‌مندی‌ها\n"
                        + "✅ حذف تبلیغات\n"
                        + "✅ دسترسی به تمام تصاویر با کیفیت بالا",
                listener);
    }

    public static void showFavoriteLimitReached(Activity activity, int currentCount, int maxCount, OnPurchaseListener listener) {
        show(activity,
                "❗ شما به " + maxCount + " کلمه در علاقه‌مندی‌ها رسیده‌اید.\n\n"
                        + "نسخه پریمیوم شامل:\n"
                        + "✅ ذخیره نامحدود کلمات\n"
                        + "✅ فلش‌کارت نامحدود\n"
                        + "✅ حذف تبلیغات",
                listener);
    }
}