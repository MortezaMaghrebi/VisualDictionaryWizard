package com.codestoon.visualdictionarywizard;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class StudySessionActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private CardView flashCard;
    private TextView wordText, meaningText, levelText, counterText;
    private ImageView speakIcon, nextIcon, prevIcon, flipIcon;
    private ProgressBar progressBar;
    private Button exitButton;

    private ArrayList<HashMap<String, String>> studyWords;
    private int currentIndex = 0;
    private boolean isShowingMeaning = false;
    private TextToSpeech textToSpeech;

    private boolean isAnimating = false;  // جلوگیری از کلیک همزمان

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_session);

        studyWords = (ArrayList<HashMap<String, String>>) getIntent().getSerializableExtra("study_words");
        if (studyWords == null || studyWords.isEmpty()) {
            Toast.makeText(this, "کلمه‌ای برای مطالعه وجود ندارد", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupClickListeners();
        updateCard();
        updateCounter();

        textToSpeech = new TextToSpeech(this, this);
    }

    private void initViews() {
        flashCard = findViewById(R.id.flashCard);
        wordText = findViewById(R.id.wordText);
        meaningText = findViewById(R.id.meaningText);
        levelText = findViewById(R.id.levelText);
        counterText = findViewById(R.id.counterText);
        speakIcon = findViewById(R.id.speakIcon);
        nextIcon = findViewById(R.id.nextIcon);
        prevIcon = findViewById(R.id.prevIcon);
        flipIcon = findViewById(R.id.flipIcon);
        progressBar = findViewById(R.id.progressBar);
        exitButton = findViewById(R.id.exitButton);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📖 جلسه مطالعه");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        flashCard.setOnClickListener(v -> {
            if (!isAnimating) {
                flipCard();
            }
        });

        flipIcon.setOnClickListener(v -> {
            if (!isAnimating) {
                flipCard();
            }
        });

        speakIcon.setOnClickListener(v -> {
            String word = studyWords.get(currentIndex).get("word");
            speakWord(word);
        });

        nextIcon.setOnClickListener(v -> nextWord());
        prevIcon.setOnClickListener(v -> prevWord());
        exitButton.setOnClickListener(v -> finish());
    }

    private void flipCard() {
        if (isAnimating) return;
        isAnimating = true;

        Animation flipOut = AnimationUtils.loadAnimation(this, R.anim.flip_out);
        Animation flipIn = AnimationUtils.loadAnimation(this, R.anim.flip_in);

        flashCard.startAnimation(flipOut);

        flipOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // تغییر محتوا
                if (isShowingMeaning) {
                    showWord();
                } else {
                    showMeaning();
                }

                flashCard.startAnimation(flipIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        flipIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                isAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    private void showWord() {
        HashMap<String, String> currentWord = studyWords.get(currentIndex);
        wordText.setVisibility(View.VISIBLE);
        meaningText.setVisibility(View.GONE);
        wordText.setText(currentWord.get("word"));
        isShowingMeaning = false;
    }

    private void showMeaning() {
        HashMap<String, String> currentWord = studyWords.get(currentIndex);
        wordText.setVisibility(View.GONE);
        meaningText.setVisibility(View.VISIBLE);

        String persian = currentWord.get("persian");
        String example = currentWord.get("example");
        String exampleTranslation = currentWord.get("example_translation");

        StringBuilder meaning = new StringBuilder();
        meaning.append(persian);

        if (example != null && !example.isEmpty()) {
            meaning.append("\n\n📖 ").append(example);
        }

        if (exampleTranslation != null && !exampleTranslation.isEmpty()) {
            meaning.append("\n\n🇮🇷 ").append(exampleTranslation);
        }

        meaningText.setText(meaning.toString());
        isShowingMeaning = true;
    }

    private void updateCard() {
        HashMap<String, String> currentWord = studyWords.get(currentIndex);

        // نمایش کلمه
        wordText.setText(currentWord.get("word"));

        // ذخیره معنی در meaningText برای استفاده بعدی
        String persian = currentWord.get("persian");
        String example = currentWord.get("example");
        String exampleTranslation = currentWord.get("example_translation");

        StringBuilder meaning = new StringBuilder();
        meaning.append(persian);

        if (example != null && !example.isEmpty()) {
            meaning.append("\n\n📖 ").append(example);
        }

        if (exampleTranslation != null && !exampleTranslation.isEmpty()) {
            meaning.append("\n\n🇮🇷 ").append(exampleTranslation);
        }

        meaningText.setText(meaning.toString());

        // سطح
        String level = currentWord.get("level");
        levelText.setText(level);
        setLevelColor(level);

        // وضعیت اولیه (نمایش کلمه)
        wordText.setVisibility(View.VISIBLE);
        meaningText.setVisibility(View.GONE);
        isShowingMeaning = false;

        // به‌روزرسانی نوار پیشرفت
        updateProgress();
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

    private void updateCounter() {
        counterText.setText((currentIndex + 1) + " / " + studyWords.size());

        prevIcon.setEnabled(currentIndex > 0);
        prevIcon.setAlpha(currentIndex > 0 ? 1.0f : 0.5f);

        nextIcon.setEnabled(currentIndex < studyWords.size() - 1);
        nextIcon.setAlpha(currentIndex < studyWords.size() - 1 ? 1.0f : 0.5f);
    }

    private void updateProgress() {
        int progress = (int) (((float) (currentIndex + 1) / studyWords.size()) * 100);
        progressBar.setProgress(progress);
    }

    private void nextWord() {
        if (currentIndex < studyWords.size() - 1) {
            currentIndex++;
            updateCard();
            updateCounter();
            flashCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
        } else {
            Toast.makeText(this, "🎉 تبریک! جلسه مطالعه به پایان رسید", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void prevWord() {
        if (currentIndex > 0) {
            currentIndex--;
            updateCard();
            updateCounter();
            flashCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
        }
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
        super.onDestroy();
    }
}