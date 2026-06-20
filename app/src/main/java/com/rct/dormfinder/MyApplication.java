package com.rct.dormfinder;

import android.app.Application;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.cloudinary.android.MediaManager;
import com.rct.dormfinder.services.SyncWorker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Cloudinary
        initializeCloudinary();
        
        // Initialize Background Sync
        initializeBackgroundSync();
    }

    private void initializeCloudinary() {
        try {
            // Initialize with cloud name using a Map
            // Replace YOUR_CLOUD_NAME with your actual Cloudinary Cloud Name
            Map config = new HashMap();
            config.put("cloud_name", "dwxjfvv1g");
            config.put("api_key", "767744389793541");
            config.put("api_secret", "1y2IoScT4Cx_azpdBh94KAcvoRk");
            
            MediaManager.init(this, config);
            Log.d(TAG, "Cloudinary initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Cloudinary: " + e.getMessage(), e);
            Log.w(TAG, "Images will not work properly. Please configure Cloudinary Cloud Name in MyApplication.java");
        }
    }
    
    private void initializeBackgroundSync() {
        try {
            // Schedule periodic background sync for dormitories
            // Syncs every 6 hours when device has network connectivity
            PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                    SyncWorker.class,
                    6,
                    TimeUnit.HOURS
            )
            .addTag("dorm_sync")
            .build();
            
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "sync_dormitories",
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncWorkRequest
            );
            
            Log.d(TAG, "Background sync initialized - will sync every 6 hours");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize background sync: " + e.getMessage(), e);
        }
    }
}
