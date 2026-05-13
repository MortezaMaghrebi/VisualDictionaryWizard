package com.codestoon.visualdictionarywizard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "visual_dictionary.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_WORDS = "words";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_WORD = "word";
    private static final String COLUMN_PERSIAN = "persian";
    private static final String COLUMN_EXAMPLE = "example";
    private static final String COLUMN_SYNONYM = "synonym";
    private static final String COLUMN_LEVEL = "level";
    private static final String COLUMN_PRONUNCIATION = "pronunciation";
    private static final String COLUMN_EXAMPLE_TRANSLATION = "example_translation";
    private static final String COLUMN_FAVORITE = "is_favorite";
    private static final String COLUMN_SEARCH_COUNT = "search_count";

    private Context context;
    private boolean isFirstRun = false;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // ایجاد جدول با ایندکس برای جستجوی سریع
        String createTable = "CREATE TABLE " + TABLE_WORDS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_WORD + " TEXT UNIQUE NOT NULL, " +
                COLUMN_PERSIAN + " TEXT NOT NULL, " +
                COLUMN_EXAMPLE + " TEXT, " +
                COLUMN_SYNONYM + " TEXT, " +
                COLUMN_LEVEL + " TEXT, " +
                COLUMN_PRONUNCIATION + " TEXT, " +
                COLUMN_EXAMPLE_TRANSLATION + " TEXT, " +
                COLUMN_FAVORITE + " INTEGER DEFAULT 0, " +
                COLUMN_SEARCH_COUNT + " INTEGER DEFAULT 0);";

        db.execSQL(createTable);

        // ایجاد ایندکس برای جستجوی سریع‌تر (طبق مقاله: عملکرد فنی مهمه!)
        db.execSQL("CREATE INDEX idx_word ON " + TABLE_WORDS + "(" + COLUMN_WORD + ")");
        db.execSQL("CREATE INDEX idx_level ON " + TABLE_WORDS + "(" + COLUMN_LEVEL + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORDS);
        onCreate(db);
    }

    // بارگذاری داده از فایل assets (در اولین اجرا)
    public void loadDataFromAssetsIfNeeded() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_WORDS, new String[]{COLUMN_ID}, null, null, null, null, null);

        if (cursor.getCount() == 0) {
            // دیتابیس خالی است - نیاز به بارگذاری
            loadDataFromAssets();
        }
        cursor.close();
    }

    private void loadDataFromAssets() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            InputStream is = context.getAssets().open("my_dictionary.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("#");
                if (parts.length >= 7) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_WORD, parts[0].trim().toLowerCase());
                    values.put(COLUMN_PERSIAN, parts[1].trim());
                    values.put(COLUMN_EXAMPLE, parts[2].trim());
                    values.put(COLUMN_SYNONYM, parts[3].trim());
                    values.put(COLUMN_LEVEL, parts[4].trim());
                    values.put(COLUMN_PRONUNCIATION, parts[5].trim());
                    values.put(COLUMN_EXAMPLE_TRANSLATION, parts[6].trim());

                    db.insertWithOnConflict(TABLE_WORDS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                    count++;
                }
            }

            db.setTransactionSuccessful();
            Log.d("Database", count + " words loaded successfully!");

        } catch (IOException e) {
            Log.e("Database", "Error loading data: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    // جستجوی کلمات (با اولویت بندی - طبق مقاله سفر کاربر)
    public ArrayList<HashMap<String, String>> searchWords(String query) {
        ArrayList<HashMap<String, String>> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String searchQuery = "SELECT * FROM " + TABLE_WORDS +
                " WHERE " + COLUMN_WORD + " LIKE ? OR " +
                COLUMN_PERSIAN + " LIKE ? " +
                "ORDER BY " + COLUMN_SEARCH_COUNT + " DESC, " + COLUMN_WORD + " ASC " +
                "LIMIT 50";

        String[] args = {"%" + query.toLowerCase() + "%", "%" + query + "%"};
        Cursor cursor = db.rawQuery(searchQuery, args);

        while (cursor.moveToNext()) {
            HashMap<String, String> word = new HashMap<>();
            word.put("word", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORD)));
            word.put("persian", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PERSIAN)));
            word.put("level", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LEVEL)));
            word.put("example", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXAMPLE)));
            word.put("synonym", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SYNONYM)));
            word.put("pronunciation", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRONUNCIATION)));
            word.put("example_translation", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXAMPLE_TRANSLATION)));
            results.add(word);

            // به‌روزرسانی تعداد جستجو (برای رتبه‌بندی نتایج بعدی)
            updateSearchCount(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORD)));
        }
        cursor.close();

        return results;
    }

    private void updateSearchCount(String word) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_WORDS + " SET " + COLUMN_SEARCH_COUNT +
                        " = " + COLUMN_SEARCH_COUNT + " + 1 WHERE " + COLUMN_WORD + " = ?",
                new String[]{word});
    }

    // فیلتر بر اساس سطح (A1 تا C2)
    public ArrayList<HashMap<String, String>> filterByLevel(String level) {
        ArrayList<HashMap<String, String>> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_WORDS, null, COLUMN_LEVEL + "=?",
                new String[]{level}, null, null, COLUMN_WORD + " ASC");

        while (cursor.moveToNext()) {
            HashMap<String, String> word = new HashMap<>();
            word.put("word", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORD)));
            word.put("persian", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PERSIAN)));
            word.put("level", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LEVEL)));
            results.add(word);
        }
        cursor.close();

        return results;
    }

    // افزودن به علاقه‌مندی‌ها (برای افزایش ریتنشن - طبق مقاله)
    public void toggleFavorite(String word) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_WORDS + " SET " + COLUMN_FAVORITE +
                " = CASE WHEN " + COLUMN_FAVORITE + " = 0 THEN 1 ELSE 0 END " +
                "WHERE " + COLUMN_WORD + " = ?", new String[]{word});
    }

    public ArrayList<HashMap<String, String>> getFavorites() {
        ArrayList<HashMap<String, String>> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_WORDS, null, COLUMN_FAVORITE + "=1",
                null, null, null, COLUMN_WORD + " ASC");

        while (cursor.moveToNext()) {
            HashMap<String, String> word = new HashMap<>();
            word.put("word", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORD)));
            word.put("persian", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PERSIAN)));
            results.add(word);
        }
        cursor.close();

        return results;
    }

    // دریافت کلمات بر اساس حروف الفبا (برای صفحه اصلی)
    public ArrayList<String> getAllWordsAlphabetically() {
        ArrayList<String> words = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_WORDS, new String[]{COLUMN_WORD}, null,
                null, null, null, COLUMN_WORD + " ASC", "100");

        while (cursor.moveToNext()) {
            words.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORD)));
        }
        cursor.close();

        return words;
    }

    // تعداد کل کلمات
    public int getWordCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WORDS, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    // کلمه تصادفی
    public String getRandomWord() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_WORD + " FROM " + TABLE_WORDS +
                " ORDER BY RANDOM() LIMIT 1", null);
        String word = "";
        if (cursor.moveToFirst()) {
            word = cursor.getString(0);
        }
        cursor.close();
        return word;
    }
    // دریافت جزئیات کامل علاقه‌مندی‌ها
    public ArrayList<HashMap<String, String>> getFavoritesWithDetails() {
        ArrayList<HashMap<String, String>> favorites = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_WORDS,
                new String[]{COLUMN_WORD, COLUMN_PERSIAN, COLUMN_LEVEL, COLUMN_EXAMPLE},
                COLUMN_FAVORITE + "=1",
                null, null, null, COLUMN_WORD + " ASC");

        while (cursor.moveToNext()) {
            HashMap<String, String> word = new HashMap<>();
            word.put("word", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORD)));
            word.put("persian", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PERSIAN)));
            word.put("level", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LEVEL)));
            word.put("example", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXAMPLE)));
            favorites.add(word);
        }
        cursor.close();

        return favorites;
    }
    // دریافت تعداد کلمات در هر سطح
    public HashMap<String, Integer> getLevelWordCounts() {
        HashMap<String, Integer> counts = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] levels = {"A1", "A2", "B1", "B2", "C1", "C2"};

        for (String level : levels) {
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WORDS +
                            " WHERE " + COLUMN_LEVEL + " = ?",
                    new String[]{level});
            cursor.moveToFirst();
            counts.put(level, cursor.getInt(0));
            cursor.close();
        }

        return counts;
    }
}