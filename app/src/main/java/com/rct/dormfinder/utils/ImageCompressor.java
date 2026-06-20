package com.rct.dormfinder.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageCompressor {
    private static final String TAG = "ImageCompressor";
    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;
    private static final int QUALITY = 85; // 85% quality

    /**
     * Compress image from URI and save to temporary file
     * @param context Application context
     * @param imageUri URI of the image to compress
     * @return URI of compressed image file, or original URI if compression fails
     */
    public static Uri compressImage(Context context, Uri imageUri) {
        try {
            // Open input stream from URI
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream");
                return imageUri;
            }

            // Decode bitmap with inSampleSize
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);
            options.inJustDecodeBounds = false;

            // Decode bitmap with calculated inSampleSize
            inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap");
                return imageUri;
            }

            // Scale bitmap if still too large
            bitmap = scaleBitmap(bitmap, MAX_WIDTH, MAX_HEIGHT);

            // Compress and save to file
            File compressedFile = createTempFile(context);
            FileOutputStream outputStream = new FileOutputStream(compressedFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, outputStream);
            outputStream.flush();
            outputStream.close();
            bitmap.recycle();

            // Get file size
            long originalSize = getFileSize(context, imageUri);
            long compressedSize = compressedFile.length();
            float compressionRatio = ((float) (originalSize - compressedSize) / originalSize) * 100;

            Log.d(TAG, String.format("Image compressed: %.2f%% reduction (%.2f KB -> %.2f KB)", 
                compressionRatio, originalSize / 1024f, compressedSize / 1024f));

            return Uri.fromFile(compressedFile);
        } catch (IOException e) {
            Log.e(TAG, "Error compressing image: " + e.getMessage());
            return imageUri; // Return original URI on error
        }
    }

    /**
     * Calculate appropriate inSampleSize for bitmap decoding
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Scale bitmap to fit within max dimensions while maintaining aspect ratio
     */
    private static Bitmap scaleBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Create temporary file for compressed image
     */
    private static File createTempFile(Context context) throws IOException {
        File cacheDir = new File(context.getCacheDir(), "compressed_images");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return File.createTempFile("compressed_", ".jpg", cacheDir);
    }

    /**
     * Get file size from URI
     */
    private static long getFileSize(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                long size = inputStream.available();
                inputStream.close();
                return size;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting file size: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Delete compressed image files older than 24 hours
     */
    public static void cleanupOldFiles(Context context) {
        File cacheDir = new File(context.getCacheDir(), "compressed_images");
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                long now = System.currentTimeMillis();
                long twentyFourHours = 24 * 60 * 60 * 1000;
                
                for (File file : files) {
                    if (now - file.lastModified() > twentyFourHours) {
                        if (file.delete()) {
                            Log.d(TAG, "Deleted old compressed file: " + file.getName());
                        }
                    }
                }
            }
        }
    }
}
