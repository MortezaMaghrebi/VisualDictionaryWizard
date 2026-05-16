package com.codestoon.visualdictionarywizard;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import java.io.InputStream;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_session);
        imageLoader = ImageLoader.getInstance(this);
        studyWords = (ArrayList<HashMap<String, String>>) getIntent().getSerializableExtra("study_words");
        if (studyWords == null || studyWords.isEmpty()) {
            Toast.makeText(this, "کلمه‌ای برای مطالعه وجود ندارد", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

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
            getSupportActionBar().setTitle("📖 جلسه مطالعه");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        // کلیک روی خود کارت برای چرخاندن
        flashCard.setOnClickListener(v -> {
            //android.util.Log.d("StudySession", "FlashCard clicked!"); // برای دیباگ
            if (!isAnimating) {
                flipCard();
            }
        });

        // دکمه flip هم برای چرخاندن
        flipIcon.setOnClickListener(v -> {
            //android.util.Log.d("StudySession", "Flip icon clicked!");
            if (!isAnimating) {
                flipCard();
            }
        });

        speakIcon.setOnClickListener(v -> {
            String word = studyWords.get(currentIndex).get("word");
            speakWord(word);
        });

        // حذف از علاقه‌مندی‌ها
        favoriteIcon.setOnClickListener(v -> {
            String currentWord = studyWords.get(currentIndex).get("word");
            new Thread(() -> {
                dbHelper.toggleFavorite(currentWord);
                isFavorite = dbHelper.isFavorite(currentWord);
                runOnUiThread(() -> {
                    //Toast.makeText(this, "❌ " + currentWord + " از علاقه‌مندی‌ها حذف شد", Toast.LENGTH_SHORT).show();
                     if (isFavorite) {
                        favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
                    } else {
                        favoriteIcon.setImageResource(R.drawable.ic_empty_favorites);
                    }
                });
            }).start();
        });

        masteredIcon.setOnClickListener(v -> {
            String currentWord = studyWords.get(currentIndex).get("word");
            new Thread(() -> {
                dbHelper.addToMastered(currentWord);
                runOnUiThread(() -> {
                    //Toast.makeText(this, "🎉 " + currentWord + " به کلمات یاد گرفته شده اضافه شد", Toast.LENGTH_SHORT).show();
                    masteredIcon.setEnabled(false);
                    masteredIcon.setAlpha(0.5f);

                    // ✅ این قسمت کلمه رو از لیست مطالعه حذف میکنه
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
            public void onSuccess() {
                //imageProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure() {
                //imageProgressBar.setVisibility(View.GONE);
            }
        });
        //try {
        //    String imagePath = "pictures/" + wordName.toLowerCase() + ".jpg";
        //    InputStream is = getAssets().open(imagePath);
        //    Bitmap bitmap = BitmapFactory.decodeStream(is);
        //    wordImage.setImageBitmap(bitmap);
        //} catch (Exception e) {
        //    try {
        //        String imagePath = "pictures/" + wordName.toLowerCase() + ".png";
        //        InputStream is = getAssets().open(imagePath);
        //        Bitmap bitmap = BitmapFactory.decodeStream(is);
        //        wordImage.setImageBitmap(bitmap);
        //    } catch (Exception e2) {
        //        wordImage.setImageResource(R.drawable.ic_no_image);
        //    }
        //}
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

        // نمایش تصویر در سمت معنی
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
        HashMap<String, String> currentWord = studyWords.get(currentIndex);
        wordText.setVisibility(View.VISIBLE);
        meaningRelativeView.setVisibility(View.GONE);
        isShowingMeaning = false;
    }

    private void updateCard() {
        HashMap<String, String> currentWord = studyWords.get(currentIndex);

        // نمایش کلمه در سمت اول
        wordText.setText(currentWord.get("word"));

        // ذخیره اطلاعات برای سمت دوم
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

        // سطح
        String level = currentWord.get("level");
        levelText.setText(level);
        setLevelColor(level);

        // وضعیت اولیه (نمایش کلمه)
        wordText.setVisibility(View.VISIBLE);
        meaningRelativeView.setVisibility(View.GONE);
        isShowingMeaning = false;

        // بررسی وضعیت علاقه‌مندی
        checkFavoriteStatus(currentWord.get("word"));
        checkMasteredStatus(currentWord.get("word"));

        updateProgress();
    }
    boolean isFavorite;
    private void checkFavoriteStatus(String word) {
        new Thread(() -> {
            isFavorite = dbHelper.isFavorite(word);
            runOnUiThread(() -> {
                if (isFavorite) {
                    favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
                } else {
                    favoriteIcon.setImageResource(R.drawable.ic_empty_favorites);
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