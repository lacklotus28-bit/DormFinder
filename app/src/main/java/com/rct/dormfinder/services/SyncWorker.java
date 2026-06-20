package com.rct.dormfinder.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rct.dormfinder.utils.NetworkUtil;
import com.rct.dormfinder.utils.OfflineManager;

/**
 * Background worker for syncing data from Firestore to local database
 * Runs periodically to keep offline cache up to date
 */
public class SyncWorker extends Worker {
    
    private static final String TAG = "SyncWorker";
    private static final String PREFS_NAME = "SyncWorkerPrefs";
    private static final String KEY_LAST_SYNC = "last_sync_time";
    private static final String KEY_SYNC_COUNT = "sync_count";
    
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting background sync...");
        
        Context context = getApplicationContext();
        NetworkUtil networkUtil = new NetworkUtil(context);
        
        // Check if network is available
        if (!networkUtil.isNetworkAvailable()) {
            Log.d(TAG, "No network available, skipping sync");
            return Result.retry();
        }
        
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user logged in, skipping sync");
            return Result.success();
        }
        
        String userId = currentUser.getUid();
        
        // Sync all data types
        return performComprehensiveSync(context, userId);
    }
    
    /**
     * Perform comprehensive sync of all data types
     */
    private Result performComprehensiveSync(Context context, String userId) {
        final boolean[] allSuccess = {true};
        final int[] totalSynced = {0};
        final int[] completedSyncs = {0};
        final Object lock = new Object();
        
        OfflineManager offlineManager = OfflineManager.getInstance(context);
        
        // Sync dormitories
        offlineManager.syncDormitories((success, count) -> {
            synchronized (lock) {
                if (success) {
                    totalSynced[0] += count;
                    Log.d(TAG, "Dormitories synced: " + count);
                } else {
                    allSuccess[0] = false;
                    Log.e(TAG, "Dormitories sync failed");
                }
                completedSyncs[0]++;
                lock.notify();
            }
        });
        
        // Wait for dormitories sync
        synchronized (lock) {
            try {
                lock.wait(30000); // Wait max 30 seconds
            } catch (InterruptedException e) {
                Log.e(TAG, "Sync interrupted", e);
                return Result.failure();
            }
        }
        
        // Sync reviews (available to all users)
        completedSyncs[0] = 0;
        offlineManager.syncReviews((success, count) -> {
            synchronized (lock) {
                if (success) {
                    totalSynced[0] += count;
                    Log.d(TAG, "Reviews synced: " + count);
                } else {
                    Log.w(TAG, "Reviews sync failed");
                }
                completedSyncs[0]++;
                lock.notify();
            }
        });
        
        // Wait for reviews sync
        synchronized (lock) {
            try {
                lock.wait(30000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Sync interrupted", e);
            }
        }
        
        // Note: User-specific data (bookings, payments) should be synced on-demand
        // when user opens respective activities, not in background
        
        // Save sync statistics
        saveSyncStats(context, totalSynced[0]);
        
        Log.d(TAG, "Background sync completed: " + totalSynced[0] + " total items");
        
        return allSuccess[0] ? Result.success() : Result.retry();
    }
    
    /**
     * Save sync statistics for tracking
     */
    private void saveSyncStats(Context context, int syncCount) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .putInt(KEY_SYNC_COUNT, syncCount)
                .apply();
    }
    
    /**
     * Get last sync time
     */
    public static long getLastSyncTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_SYNC, 0);
    }
    
    /**
     * Get last sync count
     */
    public static int getLastSyncCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_SYNC_COUNT, 0);
    }
}
