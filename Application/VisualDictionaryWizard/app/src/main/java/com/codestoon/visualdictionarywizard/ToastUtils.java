package com.codestoon.visualdictionarywizard;

import static android.widget.Toast.LENGTH_SHORT;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ToastUtils {
    public static void showSafeToast(Context context, String message) {
        // inflate custom layout
        Toast.makeText(context,message,LENGTH_SHORT).show();
    }
}
