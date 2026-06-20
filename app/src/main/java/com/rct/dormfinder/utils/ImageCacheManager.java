package com.rct.dormfinder.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages image caching for offline access
 * Downloads images from URLs and stores them locally
 */
public class ImageCacheManager {
    
    private static final String TAG = "ImageCacheManager";
    private static final String CACHE_DIR = "dorm_images";
    private static ImageCacheManager instance;
    
    private Context context;
    private File cacheDirectory;
    private ExecutorService executorService;
    
    private ImageCacheManager(Context context) {
        this.context = context.getApplicationContext();
        this.cacheDirectory = new File(context.getCacheDir(), CACHE_DIR);
        
        // Create cache directory if it doesn't exist
        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdirs();
        }
        
        this.executorService = Executors.newFixedThreadPool(3); // Max 3 concurrent downloads
    }
    
    public static synchronized ImageCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new ImageCacheManager(context);
        }
        return instance;
    }
    
    /**
     * Pre-cache images from URLs for offline access
     */
    public void cacheImages(List<String> imageUrls, OnImagesCachedListener listener) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            if (listener != null) {
                listener.onImagesCached(new ArrayList<>(), 0, 0);
            }
            return;
        }
        
        executorService.execute(() -> {
            List<String> cachedPaths = new ArrayList<>();
            int successCount = 0;
            int errorCount = 0;
            
            for (String url : imageUrls) {
                try {
                    // Use Glide to download and cache image
                    File imageFile = Glide.with(context)
                            .downloadOnly()
                            .load(url)
                            .submit()
                            .get();
                    
                    if (imageFile != null && imageFile.exists()) {
                        // Copy to our cache directory for guaranteed offline access
                        String cachedPath = copyToCache(imageFile, url);
                        if (cachedPath != null) {
                            cachedPaths.add(cachedPath);
                            successCount++;
                        } else {
                            // If copy failed, still use Glide's cache
                            cachedPaths.add(url);
                            errorCount++;
                        }
                    } else {
                        errorCount++;
                        cachedPaths.add(url); // Keep original URL as fallback
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to cache image: " + url, e);
                    errorCount++;
                    cachedPaths.add(url); // Keep original URL as fallback
                }
            }
            
            final int finalSuccess = successCount;
            final int finalError = errorCount;
            
            if (listener != null) {
                listener.onImagesCached(cachedPaths, finalSuccess, finalError);
            }
            
            Log.d(TAG, String.format("Image caching completed: %d success, %d errors", 
                    finalSuccess, finalError));
        });
    }
    
    /**
     * Copy image file to our cache directory
     */
    private String copyToCache(File sourceFile, String originalUrl) {
        try {
            // Generate cache filename from URL hash
            String filename = getCacheFilename(originalUrl);
            File destFile = new File(cacheDirectory, filename);
            
            // Copy file
            if (sourceFile.renameTo(destFile) || copyFile(sourceFile, destFile)) {
                return destFile.getAbsolutePath();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy image to cache", e);
        }
        return null;
    }
    
    /**
     * Copy file from source to destination
     */
    private boolean copyFile(File source, File dest) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inSampleSize = 2; // Reduce size to save space
            
            Bitmap bitmap = BitmapFactory.decodeFile(source.getAbsolutePath(), options);
            if (bitmap != null) {
                FileOutputStream out = new FileOutputStream(dest);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
                out.flush();
                out.close();
                bitmap.recycle();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error copying file", e);
        }
        return false;
    }
    
    /**
     * Generate cache filename from URL
     */
    private String getCacheFilename(String url) {
        // Use URL hash as filename
        int hash = url.hashCode();
        return "img_" + Math.abs(hash) + ".jpg";
    }
    
    /**
     * Check if image is cached
     */
    public boolean isImageCached(String url) {
        String filename = getCacheFilename(url);
        File cachedFile = new File(cacheDirectory, filename);
        return cachedFile.exists();
    }
    
    /**
     * Get cached image path
     */
    public String getCachedImagePath(String url) {
        String filename = getCacheFilename(url);
        File cachedFile = new File(cacheDirectory, filename);
        return cachedFile.exists() ? cachedFile.getAbsolutePath() : url;
    }
    
    /**
     * Clear all cached images
     */
    public void clearImageCache(OnCacheClearedListener listener) {
        executorService.execute(() -> {
            try {
                deleteDirectory(cacheDirectory);
                cacheDirectory.mkdirs(); // Recreate directory
                
                // Also clear Glide cache
                Glide.get(context).clearDiskCache();
                
                if (listener != null) {
                    listener.onCacheCleared(true);
                }
                
                Log.d(TAG, "Image cache cleared successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear image cache", e);
                if (listener != null) {
                    listener.onCacheCleared(false);
                }
            }
        });
    }
    
    /**
     * Delete directory recursively
     */
    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
    }
    
    /**
     * Get cache size in bytes
     */
    public long getCacheSize() {
        return getDirectorySize(cacheDirectory);
    }
    
    /**
     * Get directory size recursively
     */
    private long getDirectorySize(File directory) {
        long size = 0;
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }
    
    /**
     * Format cache size for display
     */
    public String getFormattedCacheSize() {
        long bytes = getCacheSize();
        
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    /**
     * Get number of cached images
     */
    public int getCachedImageCount() {
        File[] files = cacheDirectory.listFiles();
        return files != null ? files.length : 0;
    }
    
    /**
     * Preload images using Glide for better offline performance
     */
    public void preloadImagesWithGlide(List<String> imageUrls) {
        for (String url : imageUrls) {
            try {
                Glide.with(context)
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original & resized
                        .preload();
            } catch (Exception e) {
                Log.e(TAG, "Failed to preload image: " + url, e);
            }
        }
    }
    
    // Interfaces
    public interface OnImagesCachedListener {
        void onImagesCached(List<String> cachedPaths, int successCount, int errorCount);
    }
    
    public interface OnCacheClearedListener {
        void onCacheCleared(boolean success);
    }
}
