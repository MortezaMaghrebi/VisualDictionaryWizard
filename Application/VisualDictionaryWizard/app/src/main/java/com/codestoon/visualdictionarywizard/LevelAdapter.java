package com.codestoon.visualdictionarywizard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.codestoon.visualdictionarywizard.R;
import java.util.ArrayList;
import java.util.HashMap;

public class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.ViewHolder> {

    private String[] levels;
    private String[] levelNames;
    private String[] levelDescriptions;
    private int[] levelColors;
    private int[] levelIcons;
    private OnLevelClickListener listener;

    // آمار تعداد کلمات در هر سطح (اختیاری)
    private HashMap<String, Integer> levelCounts;

    public interface OnLevelClickListener {
        void onLevelClick(String level, String levelName);
    }

    // سازنده اصلی
    public LevelAdapter(String[] levels, String[] levelNames, OnLevelClickListener listener) {
        this.levels = levels;
        this.levelNames = levelNames;
        this.listener = listener;

        // تنظیم توضیحات برای هر سطح
        this.levelDescriptions = new String[]{
                "کلمات پایه و روزمره",
                "کلمات پرکاربرد روزانه",
                "مکالمات ساده و روزمره",
                "مکالمات پیشرفته تر",
                "متون تخصصی و آکادمیک",
                "تسلط کامل بر زبان"
        };

        // تنظیم رنگ‌ها برای هر سطح
        this.levelColors = new int[]{
                0xFF4CAF50,  // A1 - سبز
                0xFF8BC34A,  // A2 - سبز روشن
                0xFFFF9800,  // B1 - نارنجی
                0xFFFFC107,  // B2 - زرد کهربایی
                0xFFF44336,  // C1 - قرمز
                0xFF9C27B0   // C2 - بنفش
        };

        // تنظیم آیکون‌ها برای هر سطح
        this.levelIcons = new int[]{
                R.drawable.ic_level_a1,
                R.drawable.ic_level_a2,
                R.drawable.ic_level_b1,
                R.drawable.ic_level_b2,
                R.drawable.ic_level_c1,
                R.drawable.ic_level_c2
        };

        this.levelCounts = new HashMap<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_level, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String level = levels[position];
        String levelName = levelNames[position];
        String description = levelDescriptions[position];
        int color = levelColors[position];
        int icon = levelIcons[position];

        // تنظیم متن‌ها
        holder.levelText.setText(level);
        holder.levelNameText.setText(levelName);
        holder.levelDescriptionText.setText(description);

        // تنظیم آیکون
        holder.levelIcon.setImageResource(icon);
        holder.levelIcon.setColorFilter(color);

        // تنظیم رنگ‌های کارت
        holder.cardView.setCardBackgroundColor(color);
        holder.cardView.setCardElevation(4f);

        // تنظیم رنگ متن‌ها بر اساس سطح (برای خوانایی)
        if (level.equals("A1") || level.equals("A2") || level.equals("B1")) {
            // سطوح روشن - متن تیره
            holder.levelText.setTextColor(0xFF2c3e50);
            holder.levelNameText.setTextColor(0xFF2c3e50);
            holder.levelDescriptionText.setTextColor(0xFF555555);
        } else {
            // سطوح تیره - متن سفید
            holder.levelText.setTextColor(0xFFFFFFFF);
            holder.levelNameText.setTextColor(0xFFFFFFFF);
            holder.levelDescriptionText.setTextColor(0xFFE0E0E0);
        }

        // نمایش تعداد کلمات (اگر موجود باشد)
        if (levelCounts.containsKey(level)) {
            int count = levelCounts.get(level);
            holder.wordCountText.setVisibility(View.VISIBLE);
            holder.wordCountText.setText("📚 " + count + " کلمه");
            holder.wordCountText.setTextColor(level.equals("A1") || level.equals("A2") || level.equals("B1") ? 0xFF2c3e50 : 0xFFFFFFFF);
        } else {
            holder.wordCountText.setVisibility(View.GONE);
        }

        // انیمیشن پیشرفت (اختیاری - برای نمایش درصد یادگیری کاربر)
        if (holder.progressBar != null) {
            // می‌توانید درصد پیشرفت کاربر در هر سطح را نمایش دهید
            int progress = getLevelProgress(level);
            if (progress > 0) {
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.progressBar.setProgress(progress);
                holder.progressText.setVisibility(View.VISIBLE);
                holder.progressText.setText(progress + "%");
                holder.progressText.setTextColor(level.equals("A1") || level.equals("A2") || level.equals("B1") ? 0xFF2c3e50 : 0xFFFFFFFF);
            } else {
                holder.progressBar.setVisibility(View.GONE);
                holder.progressText.setVisibility(View.GONE);
            }
        }

        // رویداد کلیک
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLevelClick(level, levelName);

                // طبق مقاله: ثبت رویداد برای تحلیل رفتار کاربر
                saveLevelClickEvent(level);
            }
        });

        // انیمیشن ورود (طبق مقاله: تجربه کاربری جذاب)
        animateCard(holder.cardView, position);
    }

    private void animateCard(View card, int position) {
        card.setAlpha(0f);
        card.setTranslationY(50f);
        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(position * 50)
                .start();
    }

    private int getLevelProgress(String level) {
        // این متد باید از SharedPreferences یا دیتابیس
        // درصد کلماتی که کاربر در این سطح مطالعه کرده را برگرداند
        // فعلاً 0 برگشت داده می‌شود
        return 0;
    }

    private void saveLevelClickEvent(String level) {
        // طبق مقاله: ذخیره رویداد برای تحلیل و بهبود ASO
        // می‌توانید در SharedPreferences یا Firebase Analytics ذخیره کنید
    }

    public void setLevelCounts(HashMap<String, Integer> counts) {
        this.levelCounts = counts;
        notifyDataSetChanged();
    }

    public void updateProgress(String level, int progress) {
        // به‌روزرسانی پیشرفت یک سطح خاص
        // نیاز به اضافه کردن فیلد progress در ViewHolder دارد
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return levels.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView levelText;
        TextView levelNameText;
        TextView levelDescriptionText;
        TextView wordCountText;
        TextView progressText;
        ImageView levelIcon;
        ProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            levelText = itemView.findViewById(R.id.levelText);
            levelNameText = itemView.findViewById(R.id.levelNameText);
            levelDescriptionText = itemView.findViewById(R.id.levelDescriptionText);
            wordCountText = itemView.findViewById(R.id.wordCountText);
            progressText = itemView.findViewById(R.id.progressText);
            levelIcon = itemView.findViewById(R.id.levelIcon);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}