package com.codestoon.visualdictionarywizard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private RecyclerView recyclerView;
    private WordAdapter wordAdapter;
    private DatabaseHelper dbHelper;
    private TextView emptyView, recentSearchesLabel;
    private LinearLayout recentSearchesLayout;
    private ArrayList<String> recentSearches;

    // کلید برای ذخیره در SharedPreferences
    private static final String PREFS_NAME = "search_prefs";
    private static final String KEY_RECENT_SEARCHES = "recent_searches";
    private static final int MAX_RECENT_SEARCHES = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupRecyclerView();
        loadRecentSearches();  // بارگذاری جستجوهای اخیر
        setupSearchListener();
        displayRecentSearches();  // نمایش جستجوهای اخیر

        // بررسی فیلتر سطح از Intent
        String filterLevel = getIntent().getStringExtra("filter_level");
        if (filterLevel != null && !filterLevel.isEmpty()) {
            searchEditText.setText(filterLevel);
            performSearch(filterLevel);
        }

        // ✅ نمایش خودکار کیبورد
        showKeyboard();
    }
    // ✅ متد نمایش کیبورد
    private void showKeyboard() {
        searchEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // ✅ متد مخفی کردن کیبورد (اختیاری - می‌توانید در onPause اضافه کنید)
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
    private void initViews() {
        searchEditText = findViewById(R.id.searchEditText);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        recentSearchesLabel = findViewById(R.id.recentSearchesLabel);
        recentSearchesLayout = findViewById(R.id.recentSearchesLayout);

        dbHelper = new DatabaseHelper(this);
        recentSearches = new ArrayList<>();

        searchEditText.setHint("جستجوی انگلیسی یا فارسی...");
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        wordAdapter = new WordAdapter(new ArrayList<>(), word -> {
            openWordDetail(word);
            saveToRecentSearches(word.get("word"));
        });
        recyclerView.setAdapter(wordAdapter);
    }

    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    performSearch(query);
                    recentSearchesLayout.setVisibility(View.GONE);
                } else if (query.isEmpty()) {
                    displayRecentSearches();
                    wordAdapter.updateList(new ArrayList<>());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        ArrayList<HashMap<String, String>> results = dbHelper.searchWords(query);

        if (results.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setText("نتیجه‌ای یافت نشد 😢\nکلمه دیگری جستجو کنید");
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            wordAdapter.updateList(results);
        }
    }

    private void openWordDetail(HashMap<String, String> word) {
        Intent intent = new Intent(SearchActivity.this, WordDetailActivity.class);
        intent.putExtra("word", word.get("word"));
        startActivity(intent);
    }

    // ✅ ذخیره جستجوهای اخیر در SharedPreferences
    private void saveToRecentSearches(String word) {
        if (word == null || word.trim().isEmpty()) return;

        word = word.trim().toLowerCase();

        // دریافت جستجوهای قبلی
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> recentSet = prefs.getStringSet(KEY_RECENT_SEARCHES, new LinkedHashSet<>());

        // استفاده از LinkedHashSet برای حفظ ترتیب و حذف تکراری‌ها
        LinkedHashSet<String> recentList = new LinkedHashSet<>(recentSet);

        // حذف کلمه اگر قبلاً وجود داشت (برای انتقال به اول لیست)
        recentList.remove(word);

        // اضافه کردن کلمه جدید به اول لیست
        LinkedHashSet<String> newRecentList = new LinkedHashSet<>();
        newRecentList.add(word);
        newRecentList.addAll(recentList);

        // محدود کردن به ۱۰ آیتم آخر
        if (newRecentList.size() > MAX_RECENT_SEARCHES) {
            List<String> tempList = new ArrayList<>(newRecentList);
            newRecentList.clear();
            for (int i = 0; i < MAX_RECENT_SEARCHES; i++) {
                newRecentList.add(tempList.get(i));
            }
        }

        // ذخیره در SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_RECENT_SEARCHES, newRecentList);
        editor.apply();

        // به‌روزرسانی لیست و نمایش مجدد
        loadRecentSearches();
        displayRecentSearches();
    }

    // ✅ بارگذاری جستجوهای اخیر از SharedPreferences
    private void loadRecentSearches() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> recentSet = prefs.getStringSet(KEY_RECENT_SEARCHES, new LinkedHashSet<>());

        recentSearches.clear();
        recentSearches.addAll(recentSet);
    }

    // ✅ نمایش جستجوهای اخیر در صفحه
    private void displayRecentSearches() {
        if (recentSearches.isEmpty()) {
            recentSearchesLayout.setVisibility(View.GONE);
            return;
        }

        recentSearchesLayout.setVisibility(View.VISIBLE);
        recentSearchesLayout.removeAllViews();

        // نمایش عنوان
        recentSearchesLabel.setText("🕒 جستجوهای اخیر");

        // ایجاد دکمه‌های جستجوهای اخیر
        for (String searchWord : recentSearches) {
            TextView chip = createSearchChip(searchWord);
            recentSearchesLayout.addView(chip);
        }

        // اضافه کردن دکمه پاک کردن همه
        TextView clearAllChip = createClearAllChip();
        recentSearchesLayout.addView(clearAllChip);
    }

    // ایجاد یک چیپ (دکمه کوچک) برای هر جستجو
    private TextView createSearchChip(final String searchWord) {
        TextView chip = new TextView(this);

        // استایل چیپ
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 16, 8);
        chip.setLayoutParams(params);

        chip.setText("🔍 " + searchWord);
        chip.setTextSize(13f);
        chip.setTextColor(0xFF2c3e50);
        chip.setBackgroundResource(R.drawable.chip_background);
        chip.setPadding(40, 12, 40, 12);

        // رویداد کلیک روی چیپ
        chip.setOnClickListener(v -> {
            searchEditText.setText(searchWord);
            searchEditText.setSelection(searchWord.length());
            performSearch(searchWord);
            recentSearchesLayout.setVisibility(View.GONE);
        });

        // رویداد لمس طولانی برای حذف یک مورد
        chip.setOnLongClickListener(v -> {
            removeFromRecentSearches(searchWord);
            return true;
        });

        return chip;
    }

    // ایجاد دکمه پاک کردن همه
    private TextView createClearAllChip() {
        TextView chip = new TextView(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 16, 8);
        chip.setLayoutParams(params);

        chip.setText("🗑️ پاک کردن همه");
        chip.setTextSize(13f);
        chip.setTextColor(0xFFE74C3C);
        chip.setBackgroundResource(R.drawable.chip_background_clear);
        chip.setPadding(40, 12, 40, 12);

        chip.setOnClickListener(v -> clearAllRecentSearches());

        return chip;
    }

    // حذف یک کلمه از جستجوهای اخیر
    private void removeFromRecentSearches(String word) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> recentSet = prefs.getStringSet(KEY_RECENT_SEARCHES, new LinkedHashSet<>());

        LinkedHashSet<String> recentList = new LinkedHashSet<>(recentSet);
        recentList.remove(word);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_RECENT_SEARCHES, recentList);
        editor.apply();

        loadRecentSearches();
        displayRecentSearches();

        Toast.makeText(this, word + " حذف شد", Toast.LENGTH_SHORT).show();
    }

    // پاک کردن همه جستجوهای اخیر
    private void clearAllRecentSearches() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("پاک کردن جستجوهای اخیر")
                .setMessage("آیا از پاک کردن همه جستجوهای اخیر مطمئن هستید؟")
                .setPositiveButton("پاک کردن", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove(KEY_RECENT_SEARCHES);
                    editor.apply();

                    recentSearches.clear();
                    displayRecentSearches();

                    Toast.makeText(this, "جستجوهای اخیر پاک شد", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    // نمایش جستجوهای اخیر (برای زمانی که جعبه جستجو خالی است)
    private void showRecentSearches() {
        displayRecentSearches();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentSearches();
        if (searchEditText.getText().toString().trim().isEmpty()) {
            displayRecentSearches();
        }
    }
}