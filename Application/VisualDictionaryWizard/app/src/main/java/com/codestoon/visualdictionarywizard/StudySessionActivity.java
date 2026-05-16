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
    private ImageView speakIcon, nextIcon, prevIcon, flipIcon, favoriteIcon, masteredIcon, wordImage;
    private ProgressBar progressBar;
    private Button exitButton;

    private ArrayList<HashMap<String, String>> studyWords;
    private int currentIndex = 0;
    private boolean isShowingMeaning = false;
    private TextToSpeech textToSpeech;
    private DatabaseHelper dbHelper;
    private RelativeLayout meaningRelativeView;
    private TextView exampleText, exampleTranslationText;
    private boolean isAnimating = false;
    private ImageLoader imageLoader;

    private AppPrefsManager prefsManager;
    private int flashcardsStudiedThisSession = 0;
    private boolean isPremium = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_session);

        imageLoader = ImageLoader.getInstance(this);
        dbHelper = new DatabaseHelper(this);
        prefsManager = AppPrefsManager.getInstance(this);
        isPremium = prefsManager.isPremium();

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
        meaningRelativeView = findViewById(R.id.meaningRelativeView);
        exampleText = findViewById(R.id.exampleText);
        exampleTranslationText = findViewById(R.id.exampleTranslationText);
        levelText = findViewById(R.id.levelText);
        counterText = findViewById(R.id.counterText);
        speakIcon = findViewById(R.id.speakIcon);
        nextIcon = findViewById(R.id.nextIcon);
        prevIcon = findViewById(R.id.prevIcon);
        flipIcon = findViewById(R.id.flipIcon);
        favoriteIcon = findViewById(R.id.favoriteIcon);
        masteredIcon = findViewById(R.id.masteredIcon);
        wordImage = findViewById(R.id.wordImage);
        progressBar = findViewById(R.id.progressBar);
        exitButton = findViewById(R.id.exitButton);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isPremium ? "📖 جلسه مطالعه (نامحدود)" : "📖 جلسه مطالعه");
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

        favoriteIcon.setOnClickListener(v -> {
            String currentWord = studyWords.get(currentIndex).get("word");
            new Thread(() -> {
                dbHelper.toggleFavorite(currentWord);
                boolean isFav = dbHelper.isFavorite(currentWord);
                runOnUiThread(() -> {
                    if (isFav) {
                        favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
                        //Toast.makeText(this, "❤️ به علاقه‌مندی‌ها اضافه شد", Toast.LENGTH_SHORT).show();
                    } else {
                        favoriteIcon.setImageResource(R.drawable.ic_favorite_outline);
                        //Toast.makeText(this, "💔 از علاقه‌مندی‌ها حذف شد", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });

        masteredIcon.setOnClickListener(v -> {
            String currentWord = studyWords.get(currentIndex).get("word");
            new Thread(() -> {
                dbHelper.addToMastered(currentWord);
                runOnUiThread(() -> {
                    //Toast.makeText(this, "🎉 " + currentWord + " یاد گرفتی! به لیست یادگرفته شده‌ها اضافه شد", Toast.LENGTH_SHORT).show();
                    masteredIcon.setEnabled(false);
                    masteredIcon.setAlpha(0.5f);

                    studyWords.remove(currentIndex);
                    if (studyWords.isEmpty()) {
                        Toast.makeText(this, "🎊 تبریک! همه کلمات رو یاد گرفتی!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        if (currentIndex >= studyWords.size()) {
                            currentIndex = studyWords.size() - 1;
                        }
                        updateCard();
                        updateCounter();
                    }
                });
            }).start();
        });

        nextIcon.setOnClickListener(v -> nextWord());
        prevIcon.setOnClickListener(v -> prevWord());
        exitButton.setOnClickListener(v -> finish());
    }

    private void loadImage(String wordName) {
        imageLoader.loadImage(wordName, wordImage, new ImageLoader.OnImageLoadedListener() {
            @Override
            public void onSuccess() {}
            @Override
            public void onFailure() {}
        });
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

    private void showMeaning() {
        HashMap<String, String> currentWord = studyWords.get(currentIndex);
        wordText.setVisibility(View.GONE);
        meaningRelativeView.setVisibility(View.VISIBLE);
        loadImage(currentWord.get("word"));

        String persian = currentWord.get("persian");
        String example = currentWord.get("example");
        String exampleTranslation = currentWord.get("example_translation");

        meaningText.setText(persian);

        if (example != null && !example.isEmpty()) {
            exampleText.setText("📖 " + example);
            exampleText.setVisibility(View.VISIBLE);
        } else {
            exampleText.setVisibility(View.GONE);
        }

        if (exampleTranslation != null && !exampleTranslation.isEmpty()) {
            exampleTranslationText.setText("🇮🇷 " + exampleTranslation);
            exampleTranslationText.setVisibility(View.VISIBLE);
        } else {
            exampleTranslationText.setVisibility(View.GONE);
        }

        isShowingMeaning = true;
    }

    private void showWord() {
        wordText.setVisibility(View.VISIBLE);
        meaningRelativeView.setVisibility(View.GONE);
        isShowingMeaning = false;
    }

    private void updateCard() {
        HashMap<String, String> currentWord = studyWords.get(currentIndex);
        wordText.setText(currentWord.get("word"));

        String persian = currentWord.get("persian");
        String example = currentWord.get("example");
        String exampleTranslation = currentWord.get("example_translation");

        meaningText.setText(persian);

        if (example != null && !example.isEmpty()) {
            exampleText.setText("📖 " + example);
            exampleText.setVisibility(View.VISIBLE);
        } else {
            exampleText.setVisibility(View.GONE);
        }

        if (exampleTranslation != null && !exampleTranslation.isEmpty()) {
            exampleTranslationText.setText("🇮🇷 " + exampleTranslation);
            exampleTranslationText.setVisibility(View.VISIBLE);
        } else {
            exampleTranslationText.setVisibility(View.GONE);
        }

        String level = currentWord.get("level");
        levelText.setText(level);
        setLevelColor(level);

        wordText.setVisibility(View.VISIBLE);
        meaningRelativeView.setVisibility(View.GONE);
        isShowingMeaning = false;

        checkFavoriteStatus(currentWord.get("word"));
        checkMasteredStatus(currentWord.get("word"));
        updateProgress();
    }

    private void checkFavoriteStatus(String word) {
        new Thread(() -> {
            boolean isFavorite = dbHelper.isFavorite(word);
            runOnUiThread(() -> {
                if (isFavorite) {
                    favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
                } else {
                    favoriteIcon.setImageResource(R.drawable.ic_favorite_outline);
                }
            });
        }).start();
    }

    private void checkMasteredStatus(String word) {
        new Thread(() -> {
            boolean isMastered = dbHelper.isMastered(word);
            runOnUiThread(() -> {
                if (isMastered) {
                    masteredIcon.setEnabled(false);
                    masteredIcon.setAlpha(0.5f);
                } else {
                    masteredIcon.setEnabled(true);
                    masteredIcon.setAlpha(1.0f);
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
            // ========== CHECK FLASHCARD LIMIT FOR FREE USERS ==========
            if (!isPremium) {
                if (!prefsManager.consumeFlashcard()) {
                    // limit reached - show upsell dialog
                    PremiumUpsellDialog.showLimitReached(this, new PremiumUpsellDialog.OnPurchaseListener() {
                        @Override
                        public void onPurchaseClicked() {
                            BillingManager billingManager = BillingManager.getInstance(StudySessionActivity.this);
                            if (billingManager.isReady()) {
                                billingManager.purchasePremium();
                            } else {
                                Toast.makeText(StudySessionActivity.this, "در حال اتصال به بازار... لطفاً دوباره تلاش کنید", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onCancel() {
                            finish();
                        }
                    });
                    return;
                }
                flashcardsStudiedThisSession++;

                // show remaining count toast every 5 flashcards
                int remaining = prefsManager.getRemainingFreeFlashcards();
                if (remaining == 5 || remaining == 3 || remaining == 1) {
                    Toast.makeText(this, "⚠️ " + remaining + " فلش‌کارت رایگان امروز باقی ماند. خرید پریمیوم = نامحدود", Toast.LENGTH_LONG).show();
                }
            }

            currentIndex++;
            updateCard();
            updateCounter();
            flashCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
        } else {
            // session completed - show rating dialog
            showRatingIfNeeded();
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

    private void showRatingIfNeeded() {
        RatingManager.showRatingDialogIfNeeded(this);
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