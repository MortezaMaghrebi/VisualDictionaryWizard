package com.codestoon.visualdictionarywizard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;

public class LimitedFavoriteAdapter extends RecyclerView.Adapter<LimitedFavoriteAdapter.ViewHolder> {

    private ArrayList<HashMap<String, String>> words;
    private OnFavoriteClickListener listener;
    private boolean isSelectionMode = false;
    private ArrayList<Integer> selectedPositions = new ArrayList<>();
    private final int maxFreeFavorites;
    private final boolean isPremium;

    public interface OnFavoriteClickListener {
        void onItemClick(HashMap<String, String> word, int position, boolean isActive);
        void onFavoriteClick(HashMap<String, String> word, int position);
        boolean onLongClick(int position);
        void onSelectionChanged(int selectedCount);
    }

    public LimitedFavoriteAdapter(ArrayList<HashMap<String, String>> words,
                                  OnFavoriteClickListener listener,
                                  int maxFreeFavorites,
                                  boolean isPremium) {
        this.words = words;
        this.listener = listener;
        this.maxFreeFavorites = maxFreeFavorites;
        this.isPremium = isPremium;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_limited, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HashMap<String, String> word = words.get(position);

        // تعیین فعال یا غیرفعال بودن (فقط برای کاربران غیر پریمیوم)
        boolean isActive = isPremium || position < maxFreeFavorites;

        holder.wordText.setText(word.get("word"));
        holder.persianText.setText(word.get("persian"));

        String level = word.get("level");
        holder.levelText.setText(level);
        setLevelColor(holder.levelText, level);

        boolean isSelected = selectedPositions.contains(position);

        // تنظیم ظاهر بر اساس فعال/غیرفعال بودن
        if (!isActive) {
            // کلمه غیرفعال - خاکستری و مات
            holder.cardView.setAlpha(0.5f);
            holder.wordText.setTextColor(0xFF999999);
            holder.persianText.setTextColor(0xFFAAAAAA);
            holder.lockIcon.setVisibility(View.VISIBLE);
            holder.checkIcon.setVisibility(View.GONE);
            holder.favoriteIcon.setEnabled(false);
            holder.favoriteIcon.setAlpha(0.3f);
        } else {
            holder.cardView.setAlpha(1.0f);
            holder.wordText.setTextColor(0xFF2c3e50);
            holder.persianText.setTextColor(0xFF7f8c8d);
            holder.lockIcon.setVisibility(View.GONE);
            holder.favoriteIcon.setEnabled(true);
            holder.favoriteIcon.setAlpha(1.0f);

            if (isSelectionMode && isSelected) {
                holder.cardView.setBackgroundResource(R.drawable.selected_background);
                holder.checkIcon.setVisibility(View.VISIBLE);
            } else {
                holder.cardView.setBackgroundResource(R.drawable.card_background);
                holder.checkIcon.setVisibility(View.GONE);
            }
        }

        holder.favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
        holder.favoriteIcon.setColorFilter(0xFFE74C3C);

        // رویداد کلیک - فقط برای آیتم‌های فعال
        holder.cardView.setOnClickListener(v -> {
            if (isActive) {
                if (isSelectionMode) {
                    toggleSelection(position);
                } else {
                    listener.onItemClick(word, position, isActive);
                }
            }
        });

        holder.cardView.setOnLongClickListener(v -> {
            if (isActive && !isSelectionMode) {
                return listener.onLongClick(position);
            }
            return false;
        });

        // رویداد کلیک روی قلب - فقط برای آیتم‌های فعال
        holder.favoriteIcon.setOnClickListener(v -> {
            if (isActive) {
                listener.onFavoriteClick(word, position);
            }
        });
    }

    private void setLevelColor(TextView textView, String level) {
        int color;
        switch (level) {
            case "A1": color = 0xFF4CAF50; break;
            case "A2": color = 0xFF8BC34A; break;
            case "B1": color = 0xFFFF9800; break;
            case "B2": color = 0xFFFFC107; break;
            case "C1": color = 0xFFF44336; break;
            case "C2": color = 0xFF9C27B0; break;
            default: color = 0xFF757575;
        }
        textView.setBackgroundColor(color);
    }

    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(Integer.valueOf(position));
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);

        if (listener != null) {
            listener.onSelectionChanged(selectedPositions.size());
        }
    }

    public void setSelectionMode(boolean enabled) {
        this.isSelectionMode = enabled;
        if (!enabled) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();

        if (listener != null) {
            listener.onSelectionChanged(selectedPositions.size());
        }
    }

    public int getSelectedCount() {
        return selectedPositions.size();
    }

    public ArrayList<Integer> getSelectedPositions() {
        return selectedPositions;
    }

    public void updateList(ArrayList<HashMap<String, String>> newList) {
        this.words = newList;
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return words.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView wordText, persianText, levelText;
        ImageView favoriteIcon, checkIcon, lockIcon;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            wordText = itemView.findViewById(R.id.wordText);
            persianText = itemView.findViewById(R.id.persianText);
            levelText = itemView.findViewById(R.id.levelText);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
            checkIcon = itemView.findViewById(R.id.checkIcon);
            lockIcon = itemView.findViewById(R.id.lockIcon);
        }
    }
}