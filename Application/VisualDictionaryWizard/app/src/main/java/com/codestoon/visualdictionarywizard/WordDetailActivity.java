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
    private TextView synonymPersianText;
    private ImageView wordImageView, favoriteIcon, speakIcon, speakExampleIcon, speakSynonymIcon, speakPersianIcon;
    private CardView exampleCard, synonymCard;
    private ProgressBar imageProgressBar;
    private LinearLayout synonymPersianLayout;

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
        setupSpeakSynonymButton();
        setupSpeakPersianButton();

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
        synonymPersianText = findViewById(R.id.synonymPersianText);
        wordImageView = findViewById(R.id.wordImageView);
        favoriteIcon = findViewById(R.id.favoriteIcon);
        speakIcon = findViewById(R.id.speakIcon);
        speakExampleIcon = findViewById(R.id.speakExampleIcon);
        speakSynonymIcon = findViewById(R.id.speakSynonymIcon);
        speakPersianIcon = findViewById(R.id.speakPersianIcon);
        exampleCard = findViewById(R.id.exampleCard);
        synonymCard = findViewById(R.id.synonymCard);
        imageProgressBar = findViewById(R.id.imageProgressBar);
        synonymPersianLayout = findViewById(R.id.synonymPersianLayout);

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

            setupClickableExample(wordData.get("example"));
            exampleTranslationText.setText(wordData.get("example_translation"));
            setupClickableSynonyms(wordData.get("synonym"));

            setLevelColor(wordData.get("level"));
            loadImage(wordData.get("word"));
            checkFavoriteStatus();
        } else {
            Toast.makeText(this, "کلمه مورد نظر یافت نشد", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean wordExistsInDatabase(String word) {
        if (word == null || word.isEmpty()) return false;
        HashMap<String, String> result = dbHelper.getWordByExactMatch(word);
        return result != null;
    }

    /**
     * پیدا کردن بهترین تطابق و موقعیت آن در کلمه اصلی
     * مثلاً برای "important" برمیگرداند: "import" با موقعیت 0 تا 6
     */
    private String[] findBestMatchingWithPosition(String word) {
        if (word == null || word.isEmpty()) {
            return null;
        }

        String lowerWord = word.toLowerCase();

        // 1. بررسی تطابق دقیق
        if (wordExistsInDatabase(lowerWord)) {
            return new String[]{lowerWord, "0", String.valueOf(lowerWord.length())};
        }

        // 2. حذف پسوندهای رایج
        String[] suffixes = {
                "ly", "ness", "tion", "sion", "ment", "ing", "ed", "er", "or",
                "al", "able", "ible", "ous", "ive", "ative", "itive", "ful", "less",
                "ously", "ially", "ually", "fully", "lessly", "ness", "ment",
                "ant", "ent", "ance", "ence", "ancy", "ency", "ism", "ist",
                "ate", "ute", "ify", "ize", "ise"
        };

        for (String suffix : suffixes) {
            if (lowerWord.endsWith(suffix)) {
                String withoutSuffix = lowerWord.substring(0, lowerWord.length() - suffix.length());
                if (withoutSuffix.length() >= 2 && wordExistsInDatabase(withoutSuffix)) {
                    return new String[]{withoutSuffix, "0", String.valueOf(withoutSuffix.length())};
                }
            }
        }

        // 3. پسوندهای خاص
        if (lowerWord.endsWith("tion")) {
            String withoutTion = lowerWord.substring(0, lowerWord.length() - 3);
            if (wordExistsInDatabase(withoutTion)) {
                return new String[]{withoutTion, "0", String.valueOf(withoutTion.length())};
            }
            String withTe = lowerWord.substring(0, lowerWord.length() - 4) + "te";
            if (wordExistsInDatabase(withTe)) {
                return new String[]{withTe, "0", String.valueOf(withTe.length())};
            }
        }

        if (lowerWord.endsWith("sion")) {
            String withoutSion = lowerWord.substring(0, lowerWord.length() - 2);
            if (wordExistsInDatabase(withoutSion)) {
                return new String[]{withoutSion, "0", String.valueOf(withoutSion.length())};
            }
        }

        // 4. حذف پیشوندها
        String[] prefixes = {"un", "re", "in", "im", "ir", "il", "dis", "mis", "over", "under", "pre", "post", "anti"};
        for (String prefix : prefixes) {
            if (lowerWord.startsWith(prefix) && lowerWord.length() > prefix.length() + 1) {
                String withoutPrefix = lowerWord.substring(prefix.length());
                if (wordExistsInDatabase(withoutPrefix)) {
                    return new String[]{withoutPrefix, String.valueOf(prefix.length()), String.valueOf(lowerWord.length())};
                }
            }
        }

        // 5. حالت جمع
        if (lowerWord.endsWith("s") && lowerWord.length() > 2) {
            String withoutS = lowerWord.substring(0, lowerWord.length() - 1);
            if (wordExistsInDatabase(withoutS)) {
                return new String[]{withoutS, "0", String.valueOf(withoutS.length())};
            }
            if (lowerWord.endsWith("es")) {
                String withoutEs = lowerWord.substring(0, lowerWord.length() - 2);
                if (wordExistsInDatabase(withoutEs)) {
                    return new String[]{withoutEs, "0", String.valueOf(withoutEs.length())};
                }
            }
        }

        return null;
    }

    /**
     * تنظیم متن مثال با قابلیت کلیک - فقط زیر بخش تطابق‌یافته خط کشیده میشه
     */
    private void setupClickableExample(String example) {
        if (example == null || example.isEmpty()) {
            exampleText.setText("");
            return;
        }

        String cleanExample = example.replaceAll("^\"|\"$", "");
        SpannableString spannableString = new SpannableString(cleanExample);

        Pattern pattern = Pattern.compile("\\b[A-Za-z]{2,}\\b");
        Matcher matcher = pattern.matcher(cleanExample);

        ArrayList<String> processedWords = new ArrayList<>();

        while (matcher.find()) {
            final String originalWord = matcher.group();
            int wordStart = matcher.start();
            int wordEnd = matcher.end();

            if (!originalWord.equalsIgnoreCase(currentWord) && !processedWords.contains(originalWord.toLowerCase())) {
                processedWords.add(originalWord.toLowerCase());

                String[] matchResult = findBestMatchingWithPosition(originalWord);

                if (matchResult != null) {
                    String matchedWord = matchResult[0];
                    int matchStartInWord = Integer.parseInt(matchResult[1]);
                    int matchEndInWord = Integer.parseInt(matchResult[2]);

                    // محاسبه موقعیت دقیق در متن اصلی
                    int globalStart = wordStart + matchStartInWord;
                    int globalEnd = wordStart + matchEndInWord;

                    final String finalMatchedWord = matchedWord;

                    // زیر بخش تطابق‌یافته خط بکش
                    spannableString.setSpan(new UnderlineSpan(), globalStart, globalEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannableString.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View widget) {
                            navigateToWord(finalMatchedWord);
                        }
                    }, globalStart, globalEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        exampleText.setText(spannableString);
        exampleText.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    /**
     * تنظیم مترادف‌ها با قابلیت کلیک و نمایش معنی فارسی
     */
    private void setupClickableSynonyms(String synonym) {
        if (synonym == null || synonym.isEmpty()) {
            synonymText.setText("");
            synonymPersianLayout.setVisibility(View.GONE);
            return;
        }

        String[] synonyms = synonym.split("[,،]");
        StringBuilder displayText = new StringBuilder();
        StringBuilder persianText = new StringBuilder();

        for (int i = 0; i < synonyms.length; i++) {
            String syn = synonyms[i].trim();
            if (!syn.isEmpty()) {
                if (i > 0) {
                    displayText.append("، ");
                    persianText.append("، ");
                }
                displayText.append(syn);

                HashMap<String, String> synData = dbHelper.getWordByExactMatch(syn.toLowerCase());
                if (synData != null && synData.containsKey("persian")) {
                    persianText.append(synData.get("persian"));
                } else {
                    String[] matchResult = findBestMatchingWithPosition(syn);
                    if (matchResult != null) {
                        String matchedWord = matchResult[0];
                        HashMap<String, String> matchedData = dbHelper.getWordByExactMatch(matchedWord);
                        if (matchedData != null && matchedData.containsKey("persian")) {
                            persianText.append(matchedData.get("persian"));
                        } else {
                            persianText.append("—");
                        }
                    } else {
                        persianText.append("—");
                    }
                }
            }
        }

        String synText = displayText.toString();
        SpannableString spannableString = new SpannableString(synText);

        Pattern pattern = Pattern.compile("\\b[A-Za-z]{2,}\\b");
        Matcher matcher = pattern.matcher(synText);

        while (matcher.find()) {
            final String originalWord = matcher.group();
            int wordStart = matcher.start();
            int wordEnd = matcher.end();

            String[] matchResult = findBestMatchingWithPosition(originalWord);

            if (matchResult != null) {
                String matchedWord = matchResult[0];
                int matchStartInWord = Integer.parseInt(matchResult[1]);
                int matchEndInWord = Integer.parseInt(matchResult[2]);

                int globalStart = wordStart + matchStartInWord;
                int globalEnd = wordStart + matchEndInWord;

                final String finalMatchedWord = matchedWord;

                spannableString.setSpan(new UnderlineSpan(), globalStart, globalEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        navigateToWord(finalMatchedWord);
                    }
                }, globalStart, globalEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        synonymText.setText(spannableString);
        synonymText.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());

        if (persianText.length() > 0) {
            synonymPersianText.setText(persianText.toString());
            synonymPersianLayout.setVisibility(View.VISIBLE);
        } else {
            synonymPersianLayout.setVisibility(View.GONE);
        }
    }

    private void navigateToWord(String word) {
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

    private void setupSpeakExampleButton() {
        speakExampleIcon.setOnClickListener(v -> {
            String example = wordData.get("example");
            if (example != null && !example.isEmpty()) {
                String cleanExample = example.replaceAll("^\"|\"$", "");
                speakWord(cleanExample);
            } else {
                Toast.makeText(this, "مثالی برای تلفظ وجود ندارد", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpeakSynonymButton() {
        speakSynonymIcon.setOnClickListener(v -> {
            String synonym = wordData.get("synonym");
            if (synonym != null && !synonym.isEmpty()) {
                String[] synonyms = synonym.split("[,،]");
                if (synonyms.length > 0) {
                    String firstSynonym = synonyms[0].trim();
                    if (!firstSynonym.isEmpty()) {
                        speakWord(firstSynonym);
                    }
                }
            } else {
                Toast.makeText(this, "مترادفی برای تلفظ وجود ندارد", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpeakPersianButton() {
        speakPersianIcon.setOnClickListener(v -> {
            String persian = wordData.get("persian");
            if (persian != null && !persian.isEmpty()) {
                speakPersianWord(persian);
            } else {
                Toast.makeText(this, "معنی فارسی برای تلفظ وجود ندارد", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void speakWord(String word) {
        if (textToSpeech != null) {
            textToSpeech.setLanguage(Locale.US);
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void speakPersianWord(String persian) {
        if (textToSpeech != null) {
            textToSpeech.setLanguage(new Locale("fa", "IR"));
            textToSpeech.speak(persian, TextToSpeech.QUEUE_FLUSH, null, null);
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