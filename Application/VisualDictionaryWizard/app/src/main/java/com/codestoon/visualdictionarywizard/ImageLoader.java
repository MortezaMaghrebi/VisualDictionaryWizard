package com.codestoon.visualdictionarywizard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;

public class ImageLoader {

    private static final String TAG = "ImageLoader";
    private static final String ENCRYPTION_PASSWORD = "sound113355"; // رمز خود را وارد کنید

    // کش حافظه (Memory Cache)
    private static final HashMap<String, Bitmap> memoryCache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 50; // حداکثر تعداد تصاویر در کش

    // Context
    private Context context;

    // Singleton pattern
    private static ImageLoader instance;

    public static synchronized ImageLoader getInstance(Context context) {
        if (instance == null) {
            instance = new ImageLoader(context.getApplicationContext());
        }
        return instance;
    }

    private ImageLoader(Context context) {
        this.context = context;
    }

    /**
     * بارگذاری تصویر در ImageView
     * @param wordName نام کلمه (بدون پسوند)
     * @param imageView ImageView مقصد
     */
    public void loadImage(String wordName, ImageView imageView, OnImageLoadedListener listener) {
        String key = wordName.toLowerCase();

        // بررسی در کش حافظه
        if (memoryCache.containsKey(key)) {
            Bitmap cachedBitmap = memoryCache.get(key);
            if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                imageView.setImageBitmap(cachedBitmap);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                if (listener != null) listener.onSuccess();
                return;
            } else {
                memoryCache.remove(key);
            }
        }

        // بررسی در کش دیسک
        File diskCacheFile = getDiskCacheFile(key);
        if (diskCacheFile != null && diskCacheFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(diskCacheFile.getAbsolutePath());
            if (bitmap != null) {
                addToMemoryCache(key, bitmap);
                imageView.setImageBitmap(bitmap);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                if (listener != null) listener.onSuccess();
                return;
            }
        }

        // بارگذاری از assets (در ترد جداگانه)
        new Thread(() -> {
            Bitmap bitmap = loadImageFromAssets(key);

            runOnUiThread(() -> {
                if (bitmap != null) {
                    addToMemoryCache(key, bitmap);
                    saveToDiskCache(key, bitmap);
                    imageView.setImageBitmap(bitmap);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    if (listener != null) listener.onSuccess();
                } else {
                    // تصویر پیش‌فرض
                    imageView.setImageResource(R.drawable.ic_no_image);
                    imageView.setScaleType(ImageView.ScaleType.CENTER);
                    if (listener != null) listener.onFailure();
                }
            });
        }).start();
    }

    // متد ساده‌تر بدون listener
    public void loadImage(String wordName, ImageView imageView) {
        loadImage(wordName, imageView, null);
    }

    /**
     * بارگذاری تصویر از assets (با پشتیبانی از فایل‌های انکریپت شده)
     */
    private Bitmap loadImageFromAssets(String wordName) {
        Bitmap bitmap = null;

        String[] extensions = {".jpg_enc", ".png_enc", ".jpg", ".png"};

        for (String ext : extensions) {
            String fileName = wordName + ext;
            try {
                String assetPath = "pictures_enc/" + fileName;

                // بررسی وجود فایل
                InputStream testStream = context.getAssets().open(assetPath);
                testStream.close();

                //if (ext.endsWith("_enc")) {
                    // فایل انکریپت شده
                    bitmap = decryptAndLoadBitmap(assetPath, fileName);
                //} else {
                //    // فایل معمولی
                //    InputStream is = context.getAssets().open(assetPath);
                //    bitmap = BitmapFactory.decodeStream(is);
                //    is.close();
                //}

                if (bitmap != null) {
                    Log.d(TAG, "Image loaded: " + fileName);
                    break;
                }
            } catch (Exception e) {
                // فایل وجود ندارد، ادامه بده
            }
        }

        return bitmap;
    }

    /**
     * دکریپت و بارگذاری تصویر انکریپت شده
     */
    private Bitmap decryptAndLoadBitmap(String assetPath, String fileName) {
        File tempEncryptedFile = null;
        File decryptedFile = null;

        try {
            // کپی فایل از assets به حافظه موقت
            InputStream is = context.getAssets().open(assetPath);
            tempEncryptedFile = new File(context.getCacheDir(), fileName);
            FileOutputStream fos = new FileOutputStream(tempEncryptedFile);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fos.close();
            is.close();

            // دکریپت فایل
            decryptedFile = FileDecryptor.decryptFile(tempEncryptedFile, ENCRYPTION_PASSWORD);

            if (decryptedFile != null && decryptedFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(decryptedFile.getAbsolutePath());

                // ذخیره در کش دیسک
                if (bitmap != null) {
                    String key = fileName.replace(".jpg_enc", "").replace(".png_enc", "");
                    saveToDiskCache(key, bitmap);
                }

                return bitmap;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error decrypting: " + e.getMessage());
        } finally {
            // پاک کردن فایل‌های موقت
            if (tempEncryptedFile != null && tempEncryptedFile.exists()) {
                tempEncryptedFile.delete();
            }
            if (decryptedFile != null && decryptedFile.exists()) {
                decryptedFile.delete();
            }
        }

        return null;
    }

    /**
     * افزودن به کش حافظه (با مدیریت اندازه)
     */
    private void addToMemoryCache(String key, Bitmap bitmap) {
        if (memoryCache.size() >= MAX_CACHE_SIZE) {
            // حذف اولین آیتم (قدیمی‌ترین)
            String firstKey = memoryCache.keySet().iterator().next();
            Bitmap oldBitmap = memoryCache.remove(firstKey);
            if (oldBitmap != null && !oldBitmap.isRecycled()) {
                oldBitmap.recycle();
            }
        }
        memoryCache.put(key, bitmap);
    }

    /**
     * ذخیره در کش دیسک
     */
    private void saveToDiskCache(String key, Bitmap bitmap) {
        try {
            File cacheFile = getDiskCacheFile(key);
            if (cacheFile != null && cacheFile.exists()) {
                cacheFile.delete();
            }

            FileOutputStream fos = new FileOutputStream(cacheFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();

        } catch (Exception e) {
            Log.e(TAG, "Error saving to disk cache: " + e.getMessage());
        }
    }

    /**
     * دریافت فایل کش دیسک
     */
    private File getDiskCacheFile(String key) {
        return new File(context.getCacheDir(), "img_" + key + ".jpg");
    }

    /**
     * حذف همه کش
     */
    public void clearCache() {
        // پاک کردن کش حافظه
        for (Bitmap bitmap : memoryCache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        memoryCache.clear();

        // پاک کردن کش دیسک
        File[] files = context.getCacheDir().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("img_")) {
                    file.delete();
                }
            }
        }

        Log.d(TAG, "Cache cleared");
    }

    /**
     * پاک کردن کش برای یک کلمه خاص
     */
    public void clearCacheForWord(String wordName) {
        String key = wordName.toLowerCase();

        // حذف از کش حافظه
        Bitmap bitmap = memoryCache.remove(key);
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }

        // حذف از کش دیسک
        File cacheFile = getDiskCacheFile(key);
        if (cacheFile != null && cacheFile.exists()) {
            cacheFile.delete();
        }
    }

    /**
     * اجرای کد در UI Thread
     */
    private void runOnUiThread(Runnable runnable) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(runnable);
        } else {
            // fallback
            new android.os.Handler(android.os.Looper.getMainLooper()).post(runnable);
        }
    }

    // Interface برای callback
    public interface OnImageLoadedListener {
        void onSuccess();
        void onFailure();
    }
}