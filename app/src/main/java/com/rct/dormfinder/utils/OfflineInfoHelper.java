package com.rct.dormfinder.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.rct.dormfinder.R;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Helper class for displaying offline cache statistics and information to users
 */
public class OfflineInfoHelper {
    
    private static final SimpleDateFormat DATE_FORMAT = 
            new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
    
    /**
     * Show a dialog with detailed offline cache statistics
     */
    public static void showCacheStatistics(Context context) {
        OfflineManager offlineManager = OfflineManager.getInstance(context);
        
        AlertDialog loadingDialog = new AlertDialog.Builder(context)
                .setTitle("Cache Statistics")
                .setMessage("Loading cache information...")
                .setCancelable(false)
                .create();
        loadingDialog.show();
        
        offlineManager.getCacheStatistics(statistics -> {
            loadingDialog.dismiss();
            
            // Build statistics message
            StringBuilder message = new StringBuilder();
            
            int totalCached = (int) statistics.get("totalCached");
            int availableCount = (int) statistics.get("availableCount");
            int unavailableCount = (int) statistics.get("unavailableCount");
            long lastSyncTime = (long) statistics.get("lastSyncTime");
            double avgPrice = (double) statistics.get("averagePrice");
            
            message.append("📊 Total Dormitories: ").append(totalCached).append("\n\n");
            message.append("✅ Available: ").append(availableCount).append("\n");
            message.append("❌ Unavailable: ").append(unavailableCount).append("\n\n");
            
            if (avgPrice > 0) {
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
                message.append("💰 Average Price: ").append(currencyFormat.format(avgPrice)).append("/month\n\n");
            }
            
            if (lastSyncTime > 0) {
                String lastSyncDate = DATE_FORMAT.format(new Date(lastSyncTime));
                message.append("🔄 Last Synced: ").append(lastSyncDate);
            } else {
                message.append("🔄 Last Synced: Never");
            }
            
            new AlertDialog.Builder(context)
                    .setTitle("📱 Offline Cache Statistics")
                    .setMessage(message.toString())
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Clear Cache", (dialog, which) -> {
                        showClearCacheConfirmation(context);
                    })
                    .show();
        });
    }
    
    /**
     * Show confirmation dialog before clearing cache
     */
    private static void showClearCacheConfirmation(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Clear Cache?")
                .setMessage("This will remove all cached dormitory data. You'll need an internet connection to download data again.\n\nAre you sure?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    clearCache(context);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    
    /**
     * Clear the cache and notify user
     */
    private static void clearCache(Context context) {
        OfflineManager offlineManager = OfflineManager.getInstance(context);
        
        AlertDialog progressDialog = new AlertDialog.Builder(context)
                .setTitle("Clearing Cache")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        progressDialog.show();
        
        offlineManager.clearCache(success -> {
            progressDialog.dismiss();
            
            String message = success ? 
                    "Cache cleared successfully!" : 
                    "Failed to clear cache. Please try again.";
            
            new AlertDialog.Builder(context)
                    .setTitle(success ? "Success" : "Error")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }
    
    /**
     * Format cache size in human-readable format
     */
    public static String formatCacheSize(int itemCount) {
        if (itemCount == 0) {
            return "No cached data";
        } else if (itemCount == 1) {
            return "1 dormitory cached";
        } else {
            return itemCount + " dormitories cached";
        }
    }
    
    /**
     * Get time since last sync in human-readable format
     */
    public static String getTimeSinceSync(long lastSyncTime) {
        if (lastSyncTime == 0) {
            return "Never synced";
        }
        
        long now = System.currentTimeMillis();
        long diff = now - lastSyncTime;
        
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }
    
    /**
     * Show offline mode information dialog
     */
    public static void showOfflineModeInfo(Context context) {
        String message = "📴 You're currently in offline mode.\n\n" +
                "✓ You can browse cached dormitories\n" +
                "✓ Search and filter work offline\n" +
                "✓ View detailed information\n\n" +
                "✗ Cannot make new bookings\n" +
                "✗ Cannot send messages\n" +
                "✗ Data may not be up-to-date\n\n" +
                "Connect to the internet to sync the latest data.";
        
        new AlertDialog.Builder(context)
                .setTitle("Offline Mode")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setNeutralButton("View Cache Stats", (dialog, which) -> {
                    showCacheStatistics(context);
                })
                .show();
    }
    
    /**
     * Check if sync is needed based on time elapsed
     */
    public static boolean isSyncNeeded(long lastSyncTime, long thresholdHours) {
        if (lastSyncTime == 0) {
            return true; // Never synced
        }
        
        long now = System.currentTimeMillis();
        long diff = now - lastSyncTime;
        long hours = diff / (1000 * 60 * 60);
        
        return hours >= thresholdHours;
    }
    
    /**
     * Get sync status message
     */
    public static String getSyncStatusMessage(long lastSyncTime, int cachedCount) {
        if (cachedCount == 0) {
            return "No cached data. Sync needed.";
        }
        
        String timeAgo = getTimeSinceSync(lastSyncTime);
        return "Last synced " + timeAgo + " • " + cachedCount + " dormitories";
    }
    
    /**
     * Show sync recommendation based on cache age
     */
    public static void showSyncRecommendation(Context context, long lastSyncTime) {
        if (isSyncNeeded(lastSyncTime, 24)) { // 24 hours
            new AlertDialog.Builder(context)
                    .setTitle("Sync Recommended")
                    .setMessage("Your cached data is more than 24 hours old. It's recommended to sync for the latest information.\n\nWould you like to sync now?")
                    .setPositiveButton("Sync Now", (dialog, which) -> {
                        // Trigger sync - caller should implement this
                    })
                    .setNegativeButton("Later", null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        }
    }
    
    /**
     * Create a formatted sync status text for UI display
     */
    public static String createSyncStatusText(long lastSyncTime) {
        if (lastSyncTime == 0) {
            return "Never synced";
        }
        
        return "Last synced: " + DATE_FORMAT.format(new Date(lastSyncTime));
    }
    
    /**
     * Get cache health status
     */
    public static String getCacheHealthStatus(long lastSyncTime, int cachedCount) {
        if (cachedCount == 0) {
            return "❌ No cached data";
        }
        
        if (isSyncNeeded(lastSyncTime, 48)) { // 48 hours
            return "⚠️ Cache outdated";
        } else if (isSyncNeeded(lastSyncTime, 24)) { // 24 hours
            return "⚡ Cache aging";
        } else {
            return "✅ Cache fresh";
        }
    }
    
    /**
     * Show detailed sync information
     */
    public static void showSyncInfo(Context context, long lastSyncTime, int cachedCount, boolean isOnline) {
        StringBuilder message = new StringBuilder();
        
        message.append("Connection Status: ").append(isOnline ? "🟢 Online" : "🔴 Offline").append("\n\n");
        message.append("Cache Status: ").append(getCacheHealthStatus(lastSyncTime, cachedCount)).append("\n\n");
        message.append("Cached Items: ").append(formatCacheSize(cachedCount)).append("\n");
        
        if (lastSyncTime > 0) {
            message.append("Last Sync: ").append(getTimeSinceSync(lastSyncTime)).append("\n");
            message.append("Exact Time: ").append(DATE_FORMAT.format(new Date(lastSyncTime)));
        } else {
            message.append("Last Sync: Never");
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("Sync Information")
                .setMessage(message.toString())
                .setPositiveButton("OK", null);
        
        if (isOnline) {
            builder.setNeutralButton("Sync Now", (dialog, which) -> {
                // Trigger sync - caller should implement this
            });
        }
        
        builder.show();
    }
}
