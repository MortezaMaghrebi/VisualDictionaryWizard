package com.codestoon.visualdictionarywizard;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class WordDetailActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextView wordText, persianText, pronunciationText, levelText;
    private TextView exampleText, exampleTranslationText, synonymText;
    private ImageView wordImageView, favoriteIcon, speakIcon;
    private CardView exampleCard, synonymCard;
    private ProgressBar imageProgressBar;

    private HashMap<String, String> wordData;
    private DatabaseHelper dbHelper;
    private TextToSpeech textToSpeech;
    private MediaPlayer mediaPlayer;
    private String currentWord;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_detail);

        // دریافت داده از Intent
        currentWord = getIntent().getStringExtra("word");
        if (currentWord == null) {
            finish();
            return;
        }

        initViews();
        loadWordData();
        setupFavoriteButton();
        setupSpeakButton();

        // طبق مقاله: تجربه کاربری شخصی‌سازی شده = افزایش نرخ بازگشت
        textToSpeech = new TextToSpeech(this, this);
    }

    private void initViews() {
        wordText = findViewById(R.id.wordText);
        persianText = findViewById(R.id.persianText);
        pronunciationText = findViewById(R.id.pronunciationText);
        levelText = findViewById(R.id.levelText);
        exampleText = findViewById(R.id.exampleText);
        exampleTranslationText = findViewById(R.id.exampleTranslationText);
        synonymText = findViewById(R.id.synonymText);
        wordImageView = findViewById(R.id.wordImageView);
        favoriteIcon = findViewById(R.id.favoriteIcon);
        speakIcon = findViewById(R.id.speakIcon);
        exampleCard = findViewById(R.id.exampleCard);
        synonymCard = findViewById(R.id.synonymCard);
        imageProgressBar = findViewById(R.id.imageProgressBar);

        dbHelper = new DatabaseHelper(this);
    }

    private void loadWordData() {
        // ✅ استفاده از جستجوی دقیق به جای searchWords
        HashMap<String, String> wordData = dbHelper.getWordByExactMatch(currentWord);

        if (wordData != null) {
            this.wordData = wordData;

            wordText.setText(wordData.get("word"));
            persianText.setText(wordData.get("persian"));
            pronunciationText.setText("/" + wordData.get("pronunciation") + "/");
            levelText.setText(wordData.get("level"));
            exampleText.setText("\"" + wordData.get("example") + "\"");
            exampleTranslationText.setText(wordData.get("example_translation"));
            synonymText.setText(wordData.get("synonym"));

            setLevelColor(wordData.get("level"));
            loadImage(wordData.get("word"));
            checkFavoriteStatus();
        } else {
            // اگر کلمه پیدا نشد
            Toast.makeText(this, "کلمه مورد نظر یافت نشد", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setLevelColor(String level) {
        int color;
        switch (level) {
            case "A1": color = 0xFF4CAF50; break; // مبتدی - سبز
            case "A2": color = 0xFF8BC34A; break;
            case "B1": color = 0xFFFF9800; break; // متوسط - نارنجی
            case "B2": color = 0xFFFFC107; break;
            case "C1": color = 0xFFF44336; break; // پیشرفته - قرمز
            case "C2": color = 0xFF9C27B0; break; // حرفه‌ای - بنفش
            default: color = 0xFF757575;
        }
        levelText.setBackgroundColor(color);
    }

    private void loadImage(String wordName) {
        imageProgressBar.setVisibility(View.VISIBLE);

        try {
            // تلاش برای بارگذاری تصویر از assets/pictures/
            String imagePath = "pictures/" + wordName.toLowerCase() + ".jpg";
            InputStream is = getAssets().open(imagePath);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            wordImageView.setImageBitmap(bitmap);
            imageProgressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            // اگر تصویر نبود، تصویر پیش‌فرض نمایش بده
            try {
                InputStream is = getAssets().open("pictures/default.jpg");
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                wordImageView.setImageBitmap(bitmap);
            } catch (Exception e2) {
                wordImageView.setImageResource(R.drawable.ic_no_image);
            }
            imageProgressBar.setVisibility(View.GONE);
        }
    }

    private void checkFavoriteStatus() {
        // طبق مقاله: بررسی وضعیت علاقه‌مندی از دیتابیس
        // این متد باید در DatabaseHelper اضافه شود
        isFavorite = false; // بعداً از دیتابیس بخوانید
        updateFavoriteIcon();
    }

    private void setupFavoriteButton() {
        favoriteIcon.setOnClickListener(v -> {
            dbHelper.toggleFavorite(wordData.get("word"));
            isFavorite = !isFavorite;
            updateFavoriteIcon();

            // طبق مقاله: بازخورد لمسی برای تجربه کاربری بهتر
            Toast.makeText(this,
                    isFavorite ? "به علاقه‌مندی‌ها اضافه شد" : "از علاقه‌مندی‌ها حذف شد",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void updateFavoriteIcon() {
        if (isFavorite) {
            favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            favoriteIcon.setImageResource(R.drawable.ic_favorite_outline);
        }
    }

    private void setupSpeakButton() {
        speakIcon.setOnClickListener(v -> {
            String word = wordData.get("word");
            speakWord(word);
        });
    }

    private void speakWord(String word) {
        // طبق مقاله: قابلیت تلفظ = افزایش تعامل کاربر
        if (textToSpeech != null) {
            textToSpeech.setLanguage(Locale.US);
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.US);
            textToSpeech.setSpeechRate(0.9f);
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        super.onDestroy();
    }
}