package com.codestoon.visualdictionarywizard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.codestoon.visualdictionarywizard.R;
import java.util.ArrayList;
import java.util.HashMap;

public class WordAdapter extends RecyclerView.Adapter<WordAdapter.ViewHolder> {

    private ArrayList<HashMap<String, String>> words;
    private OnWordClickListener listener;

    private int[] levelColors;


    public interface OnWordClickListener {
        void onWordClick(HashMap<String, String> word);
    }

    public WordAdapter(ArrayList<HashMap<String, String>> words, OnWordClickListener listener) {
        this.words = words;
        this.listener = listener;
        this.levelColors = new int[]{
                0xFF4CAF50,  // A1 - سبز
                0xFF8BC34A,  // A2 - سبز روشن
                0xFFFF9800,  // B1 - نارنجی
                0xFFFFC107,  // B2 - زرد کهربایی
                0xFFF44336,  // C1 - قرمز
                0xFF9C27B0   // C2 - بنفش
        };
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_word, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HashMap<String, String> word = words.get(position);

        holder.wordText.setText(word.get("word"));
        holder.persianText.setText(word.get("persian"));

        // نمایش سطح (A1, B2, C1 و...) با رنگ متفاوت - طبق مقاله برای تجربه کاربری بهتر
        String level = word.get("level");
        holder.levelText.setText(level);

        // رنگ‌بندی سطح طبق CEFR
        if(level.equals("A1")) holder.levelText.setBackgroundColor(this.levelColors[0]);
        else if(level.equals("A2")) holder.levelText.setBackgroundColor(this.levelColors[1]);
        else if(level.equals("B1")) holder.levelText.setBackgroundColor(this.levelColors[2]);
        else if(level.equals("B2")) holder.levelText.setBackgroundColor(this.levelColors[3]);
        else if(level.equals("C1")) holder.levelText.setBackgroundColor(this.levelColors[4]);
        else if(level.equals("C2")) holder.levelText.setBackgroundColor(this.levelColors[5]);
        holder.cardView.setOnClickListener(v -> listener.onWordClick(word));
    }

    @Override
    public int getItemCount() {
        return words.size();
    }

    public void updateList(ArrayList<HashMap<String, String>> newWords) {
        this.words = newWords;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView wordText, persianText, levelText;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            wordText = itemView.findViewById(R.id.wordText);
            persianText = itemView.findViewById(R.id.persianText);
            levelText = itemView.findViewById(R.id.levelText);
        }
    }
}