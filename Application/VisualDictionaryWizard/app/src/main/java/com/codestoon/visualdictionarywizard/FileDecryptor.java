package com.codestoon.visualdictionarywizard;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class FileDecryptor {
    private static final String TAG = "FileDecryptor";
    private static final int SALT_LENGTH = 32;
    private static final int IV_LENGTH = 16;
    private static final int ITERATION_COUNT = 10000;
    private static final int KEY_LENGTH = 256;

    public static File decryptFile(File encryptedFile, String password) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        CipherInputStream cis = null;

        try {
            // بررسی وجود فایل
            if (!encryptedFile.exists()) {
                Log.e(TAG, "فایل انکریپت شده یافت نشد");
                return null;
            }

            fis = new FileInputStream(encryptedFile);

            // خواندن salt از ابتدای فایل (32 بایت اول)
            byte[] salt = new byte[SALT_LENGTH];
            int bytesRead = fis.read(salt);
            if (bytesRead != SALT_LENGTH) {
                Log.e(TAG, "خطا در خواندن salt از فایل");
                return null;
            }

            // خواندن IV بعد از salt (16 بایت بعدی)
            byte[] iv = new byte[IV_LENGTH];
            bytesRead = fis.read(iv);
            if (bytesRead != IV_LENGTH) {
                Log.e(TAG, "خطا در خواندن IV از فایل");
                return null;
            }

            // تولید key از پسورد و salt
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
            SecretKey secretKey = factory.generateSecret(spec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");

            // آماده‌سازی برای دکریپت
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);

            // نام فایل خروجی (حذف _enc)
            String encryptedFileName = encryptedFile.getName();
            String decryptedFileName;
            if (encryptedFileName.contains("_enc")) {
                decryptedFileName = encryptedFileName.replace("_enc", "");
            } else {
                decryptedFileName = "decrypted_" + encryptedFileName;
            }

            File outputFile = new File(encryptedFile.getParent(), decryptedFileName);
            fos = new FileOutputStream(outputFile);

            // استفاده از CipherInputStream برای دکریپت ساده‌تر
            cis = new CipherInputStream(fis, cipher);

            // کپی داده‌های دکریپت شده
            byte[] buffer = new byte[8192];
            int count;
            while ((count = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, count);
            }

            Log.i(TAG, "فایل با موفقیت دکریپت شد: " + outputFile.getAbsolutePath());
            return outputFile;

        } catch (Exception e) {
            Log.e(TAG, "خطا در دکریپت کردن فایل: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (cis != null) cis.close();
                if (fos != null) fos.close();
                if (fis != null) fis.close();
            } catch (IOException e) {
                Log.e(TAG, "خطا در بستن فایل‌ها: " + e.getMessage());
            }
        }
    }

    // متد جایگزین با استفاده از Cipher.doFinal() برای فایل‌های کوچک
    public static File decryptFileAlternative(File encryptedFile, String password) {
        try {
            if (!encryptedFile.exists()) {
                Log.e(TAG, "فایل انکریپت شده یافت نشد");
                return null;
            }

            // خواندن تمام محتوای فایل
            FileInputStream fis = new FileInputStream(encryptedFile);
            byte[] fileData = new byte[(int) encryptedFile.length()];
            fis.read(fileData);
            fis.close();

            // جدا کردن salt و IV و داده‌های انکریپت شده
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_LENGTH];
            byte[] encryptedData = new byte[fileData.length - SALT_LENGTH - IV_LENGTH];

            System.arraycopy(fileData, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(fileData, SALT_LENGTH, iv, 0, IV_LENGTH);
            System.arraycopy(fileData, SALT_LENGTH + IV_LENGTH, encryptedData, 0, encryptedData.length);

            // تولید key
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
            SecretKey secretKey = factory.generateSecret(spec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");

            // دکریپت
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);
            byte[] decryptedData = cipher.doFinal(encryptedData);

            // نام فایل خروجی
            String encryptedFileName = encryptedFile.getName();
            String decryptedFileName = encryptedFileName.replace("_enc", "");
            File outputFile = new File(encryptedFile.getParent(), decryptedFileName);

            // نوشتن فایل دکریپت شده
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(decryptedData);
            fos.close();

            Log.i(TAG, "فایل با موفقیت دکریپت شد: " + outputFile.getAbsolutePath());
            return outputFile;

        } catch (Exception e) {
            Log.e(TAG, "خطا در دکریپت کردن فایل: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}