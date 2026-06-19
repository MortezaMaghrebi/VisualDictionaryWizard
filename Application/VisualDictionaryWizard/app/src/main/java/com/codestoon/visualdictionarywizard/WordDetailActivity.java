package com.codestoon.visualdictionarywizard;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordDetailActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextView wordText, persianText, pronunciationText, levelText;
    private TextView exampleText, exampleTranslationText, synonymText;
    private ImageView wordImageView, favoriteIcon, speakIcon, speakExampleIcon;
    private CardView exampleCard, synonymCard;
    private ProgressBar imageProgressBar;

    private HashMap<String, String> wordData;
    private DatabaseHelper dbHelper;
    private TextToSpeech textToSpeech;
    private MediaPlayer mediaPlayer;
    private String currentWord;
    private boolean isFavorite = false;
    private ImageLoader imageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_detail);

        imageLoader = ImageLoader.getInstance(this);
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
        setupSpeakExampleButton();

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
        speakExampleIcon = findViewById(R.id.speakExampleIcon);
        exampleCard = findViewById(R.id.exampleCard);
        synonymCard = findViewById(R.id.synonymCard);
        imageProgressBar = findViewById(R.id.imageProgressBar);

        dbHelper = new DatabaseHelper(this);
    }

    private void loadWordData() {
        HashMap<String, String> wordData = dbHelper.getWordByExactMatch(currentWord);

        if (wordData != null) {
            this.wordData = wordData;

            wordText.setText(wordData.get("word"));
            persianText.setText(wordData.get("persian"));
            pronunciationText.setText("/" + wordData.get("pronunciation") + "/");
            levelText.setText(wordData.get("level"));

            // تنظیم مثال با قابلیت کلیک روی کلمات
            setupClickableExample(wordData.get("example"));

            exampleTranslationText.setText(wordData.get("example_translation"));

            // تنظیم مترادف با قابلیت کلیک
            setupClickableSynonyms(wordData.get("synonym"));

            setLevelColor(wordData.get("level"));
            loadImage(wordData.get("word"));
            checkFavoriteStatus();
        } else {
            Toast.makeText(this, "کلمه مورد نظر یافت نشد", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * بررسی وجود کلمه در دیتابیس (با جستجوی مستقیم)
     */
    private boolean wordExistsInDatabase(String word) {
        if (word == null || word.isEmpty()) return false;

        // جستجوی دقیق در دیتابیس
        HashMap<String, String> result = dbHelper.getWordByExactMatch(word);
        return result != null;
    }

    /**
     * پیدا کردن بهترین تطابق برای یک کلمه در دیکشنری با جستجوی دیتابیس
     * مثلاً violently → violent
     */
    private String findBestMatchingWord(String word) {
        if (word == null || word.isEmpty()) {
            return null;
        }

        String lowerWord = word.toLowerCase();

        // 1. بررسی تطابق دقیق
        if (wordExistsInDatabase(lowerWord)) {
            return lowerWord;
        }

        // 2. حذف پسوندهای رایج و بررسی مجدد
        String[] suffixes = {
                "ly", "ness", "tion", "sion", "ment", "ing", "ed", "er", "or",
                "al", "able", "ible", "ous", "ive", "ative", "itive", "ful", "less",
                "ously", "ially", "ually", "fully", "lessly", "ness", "ment"
        };

        for (String suffix : suffixes) {
            if (lowerWord.endsWith(suffix)) {
                String withoutSuffix = lowerWord.substring(0, lowerWord.length() - suffix.length());
                if (withoutSuffix.length() >= 2 && wordExistsInDatabase(withoutSuffix)) {
                    return withoutSuffix;
                }
            }
        }

        // 3. بررسی پسوندهای خاص
        // tion → t/te/ate
        if (lowerWord.endsWith("tion")) {
            String withoutTion = lowerWord.substring(0, lowerWord.length() - 3);
            if (wordExistsInDatabase(withoutTion)) {
                return withoutTion;
            }
            String withTe = lowerWord.substring(0, lowerWord.length() - 4) + "te";
            if (wordExistsInDatabase(withTe)) {
                return withTe;
            }
        }

        // sion → s/ss
        if (lowerWord.endsWith("sion")) {
            String withoutSion = lowerWord.substring(0, lowerWord.length() - 2);
            if (wordExistsInDatabase(withoutSion)) {
                return withoutSion;
            }
        }

        // 4. حذف پیشوندها
        String[] prefixes = {"un", "re", "in", "im", "ir", "il", "dis", "mis", "over", "under"};
        for (String prefix : prefixes) {
            if (lowerWord.startsWith(prefix) && lowerWord.length() > prefix.length() + 1) {
                String withoutPrefix = lowerWord.substring(prefix.length());
                if (wordExistsInDatabase(withoutPrefix)) {
                    return withoutPrefix;
                }
            }
        }

        // 5. بررسی حالت جمع (s یا es)
        if (lowerWord.endsWith("s") && lowerWord.length() > 2) {
            String withoutS = lowerWord.substring(0, lowerWord.length() - 1);
            if (wordExistsInDatabase(withoutS)) {
                return withoutS;
            }
            // برای کلماتی که با es جمع می‌شوند
            if (lowerWord.endsWith("es")) {
                String withoutEs = lowerWord.substring(0, lowerWord.length() - 2);
                if (wordExistsInDatabase(withoutEs)) {
                    return withoutEs;
                }
                // مثل: boxes → box
                if (withoutEs.endsWith("x") || withoutEs.endsWith("s") ||
                        withoutEs.endsWith("ch") || withoutEs.endsWith("sh")) {
                    if (wordExistsInDatabase(withoutEs)) {
                        return withoutEs;
                    }
                }
            }
        }

        // 6. کاهش تدریجی حروف از انتها (برای کلمات بسیار بلند)
        if (lowerWord.length() > 6) {
            for (int i = lowerWord.length() - 2; i >= 3; i--) {
                String subWord = lowerWord.substring(0, i);
                if (wordExistsInDatabase(subWord)) {
                    return subWord;
                }
            }
        }

        return null;
    }

    /**
     * تنظیم متن مثال با قابلیت کلیک روی کلمات موجود در دیکشنری
     */
    private void setupClickableExample(String example) {
        if (example == null || example.isEmpty()) {
            exampleText.setText("");
            return;
        }

        // حذف نقل قول‌ها اگر وجود داشته باشند
        String cleanExample = example.replaceAll("^\"|\"$", "");

        SpannableString spannableString = new SpannableString(cleanExample);

        // الگوی تشخیص کلمات انگلیسی (حداقل 2 حرف)
        Pattern pattern = Pattern.compile("\\b[A-Za-z]{2,}\\b");
        Matcher matcher = pattern.matcher(cleanExample);

        // لیست کلماتی که قبلاً پردازش شده‌اند تا از تداخل جلوگیری شود
        ArrayList<String> processedWords = new ArrayList<>();

        while (matcher.find()) {
            final String originalWord = matcher.group();
            int start = matcher.start();
            int end = matcher.end();

            // اگر کلمه با کلمه اصلی یکی نباشد و قبلاً پردازش نشده باشد
            if (!originalWord.equalsIgnoreCase(currentWord) && !processedWords.contains(originalWord.toLowerCase())) {
                processedWords.add(originalWord.toLowerCase());

                // پیدا کردن بهترین تطابق در دیکشنری با جستجوی دیتابیس
                final String matchedWord = findBestMatchingWord(originalWord);

                if (matchedWord != null) {
                    // کلمه پیدا شد - زیر خط بکش و کلیک‌پذیر کن
                    spannableString.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    spannableString.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View widget) {
                            navigateToWord(matchedWord);
                        }
                    }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        exampleText.setText(spannableString);
        exampleText.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    /**
     * تنظیم مترادف‌ها با قابلیت کلیک
     */
    private void setupClickableSynonyms(String synonym) {
        if (synonym == null || synonym.isEmpty()) {
            synonymText.setText("");
            return;
        }

        // جداسازی مترادف‌ها با کاما یا ویرگول
        String[] synonyms = synonym.split("[,،]");
        StringBuilder displayText = new StringBuilder();

        for (int i = 0; i < synonyms.length; i++) {
            String syn = synonyms[i].trim();
            if (!syn.isEmpty()) {
                if (i > 0) displayText.append("، ");
                displayText.append(syn);
            }
        }

        String synText = displayText.toString();
        SpannableString spannableString = new SpannableString(synText);

        // الگوی تشخیص کلمات انگلیسی
        Pattern pattern = Pattern.compile("\\b[A-Za-z]{2,}\\b");
        Matcher matcher = pattern.matcher(synText);

        while (matcher.find()) {
            final String originalWord = matcher.group();
            int start = matcher.start();
            int end = matcher.end();

            // بررسی وجود کلمه در دیتابیس
            if (wordExistsInDatabase(originalWord.toLowerCase())) {
                spannableString.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                spannableString.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        navigateToWord(originalWord.toLowerCase());
                    }
                }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                // اگر کلمه دقیقاً نبود، بهترین تطابق رو پیدا کن
                final String matchedWord = findBestMatchingWord(originalWord);
                if (matchedWord != null) {
                    spannableString.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    spannableString.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View widget) {
                            navigateToWord(matchedWord);
                        }
                    }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        synonymText.setText(spannableString);
        synonymText.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    /**
     * هدایت به صفحه جزئیات کلمه انتخاب شده
     */
    private void navigateToWord(String word) {
        // بررسی وجود کلمه در دیتابیس
        new Thread(() -> {
            HashMap<String, String> wordData = dbHelper.getWordByExactMatch(word);
            runOnUiThread(() -> {
                if (wordData != null) {
                    Intent intent = new Intent(WordDetailActivity.this, WordDetailActivity.class);
                    intent.putExtra("word", word);
                    startActivity(intent);
                } else {
                    Toast.makeText(WordDetailActivity.this,
                            "کلمه \"" + word + "\" در دیکشنری یافت نشد",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void setLevelColor(String level) {
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
        levelText.setBackgroundColor(color);
    }

    private void loadImage(String wordName) {
        imageProgressBar.setVisibility(View.VISIBLE);
        imageLoader.loadImage(wordName, wordImageView, new ImageLoader.OnImageLoadedListener() {
            @Override
            public void onSuccess() {
                imageProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure() {
                imageProgressBar.setVisibility(View.GONE);
            }
        });
    }

    private void checkFavoriteStatus() {
        new Thread(() -> {
            boolean favorite = dbHelper.isFavorite(wordData.get("word"));
            runOnUiThread(() -> {
                isFavorite = favorite;
                updateFavoriteIcon();
            });
        }).start();
    }

    private void setupFavoriteButton() {
        favoriteIcon.setOnClickListener(v -> {
            dbHelper.toggleFavorite(wordData.get("word"));
            isFavorite = !isFavorite;
            updateFavoriteIcon();
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

    /**
     * دکمه تلفظ مثال
     */
    private void setupSpeakExampleButton() {
        speakExampleIcon.setOnClickListener(v -> {
            String example = wordData.get("example");
            if (example != null && !example.isEmpty()) {
                // حذف نقل قول‌ها
                String cleanExample = example.replaceAll("^\"|\"$", "");
                speakWord(cleanExample);
            } else {
                Toast.makeText(this, "مثالی برای تلفظ وجود ندارد", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void speakWord(String word) {
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