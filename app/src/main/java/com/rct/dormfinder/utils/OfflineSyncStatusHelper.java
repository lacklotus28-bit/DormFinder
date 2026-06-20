package com.rct.dormfinder.utils;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.rct.dormfinder.R;

/**
 * Helper class to manage offline sync status display in activities
 * Shows connection status, last sync time, and provides quick sync access
 */
public class OfflineSyncStatusHelper {
    
    private static final String TAG = "OfflineSyncStatus";
    
    private Context context;
    private View statusView;
    private ImageView ivConnectionStatus;
    private TextView tvSyncStatus;
    private TextView tvSyncDetails;
    private ImageView btnSync;
    private ImageView btnInfo;
    
    private OfflineManager offlineManager;
    private NetworkUtil networkUtil;
    private ObjectAnimator syncAnimation;
    
    private OnSyncRequestedListener syncRequestedListener;
    
    public OfflineSyncStatusHelper(Context context) {
        this.context = context;
        this.offlineManager = OfflineManager.getInstance(context);
        this.networkUtil = new NetworkUtil(context);
    }
    
    /**
     * Inflate and add the sync status view to an activity
     * Call this in your activity's onCreate after setContentView
     */
    public View addToActivity(Activity activity, ViewGroup container) {
        LayoutInflater inflater = LayoutInflater.from(context);
        statusView = inflater.inflate(R.layout.view_offline_sync_status, container, false);
        
        initializeViews(statusView);
        setupClickListeners();
        updateStatus();
        
        container.addView(statusView, 0); // Add at top
        return statusView;
    }
    
    /**
     * Create a standalone view (for fragments or custom layouts)
     */
    public View createView(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        statusView = inflater.inflate(R.layout.view_offline_sync_status, parent, false);
        
        initializeViews(statusView);
        setupClickListeners();
        updateStatus();
        
        return statusView;
    }
    
    private void initializeViews(View view) {
        ivConnectionStatus = view.findViewById(R.id.ivConnectionStatus);
        tvSyncStatus = view.findViewById(R.id.tvSyncStatus);
        tvSyncDetails = view.findViewById(R.id.tvSyncDetails);
        btnSync = view.findViewById(R.id.btnSync);
        btnInfo = view.findViewById(R.id.btnInfo);
    }
    
    private void setupClickListeners() {
        btnSync.setOnClickListener(v -> {
            if (networkUtil.isNetworkAvailable()) {
                startSync();
            } else {
                Toast.makeText(context, "No internet connection. Cannot sync now.", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnInfo.setOnClickListener(v -> {
            OfflineInfoHelper.showCacheStatistics(context);
        });
        
        statusView.setOnClickListener(v -> {
            // Show detailed sync info when tapping the status bar
            OfflineManager.SyncStats stats = offlineManager.getSyncStats();
            boolean isOnline = networkUtil.isNetworkAvailable();
            OfflineInfoHelper.showSyncInfo(context, stats.lastSyncTime, stats.cachedCount, isOnline);
        });
    }
    
    /**
     * Update the status display based on current state
     */
    public void updateStatus() {
        boolean isOnline = networkUtil.isNetworkAvailable();
        OfflineManager.SyncStats stats = offlineManager.getSyncStats();
        
        if (isOnline) {
            // Online mode
            ivConnectionStatus.setImageResource(R.drawable.ic_wifi_off); // You may want to create ic_wifi_on
            ivConnectionStatus.setColorFilter(context.getResources().getColor(android.R.color.holo_green_dark));
            tvSyncStatus.setText("Online");
            btnSync.setEnabled(true);
            btnSync.setAlpha(1.0f);
        } else {
            // Offline mode
            ivConnectionStatus.setImageResource(R.drawable.ic_wifi_off);
            ivConnectionStatus.setColorFilter(context.getResources().getColor(android.R.color.holo_red_dark));
            tvSyncStatus.setText("Offline Mode");
            btnSync.setEnabled(false);
            btnSync.setAlpha(0.5f);
        }
        
        // Update sync details
        String syncDetails = OfflineInfoHelper.getSyncStatusMessage(stats.lastSyncTime, stats.cachedCount);
        tvSyncDetails.setText(syncDetails);
        
        // Check if sync is recommended
        if (OfflineInfoHelper.isSyncNeeded(stats.lastSyncTime, 24) && isOnline) {
            tvSyncDetails.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            tvSyncDetails.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
        }
    }
    
    /**
     * Start syncing data
     */
    private void startSync() {
        if (syncRequestedListener != null) {
            // Let the activity/fragment handle the sync
            syncRequestedListener.onSyncRequested();
        } else {
            // Default behavior: sync dormitories only
            performDormitoriesSync();
        }
    }
    
    /**
     * Perform default dormitories sync
     */
    private void performDormitoriesSync() {
        showSyncInProgress();
        
        offlineManager.syncDormitories((success, itemCount) -> {
            hideSyncInProgress();
            
            if (success) {
                Toast.makeText(context, "Synced " + itemCount + " dormitories", Toast.LENGTH_SHORT).show();
                updateStatus();
            } else {
                Toast.makeText(context, "Sync failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Show sync animation
     */
    private void showSyncInProgress() {
        btnSync.setEnabled(false);
        
        // Rotate animation
        syncAnimation = ObjectAnimator.ofFloat(btnSync, "rotation", 0f, 360f);
        syncAnimation.setDuration(1000);
        syncAnimation.setRepeatCount(ObjectAnimator.INFINITE);
        syncAnimation.setInterpolator(new LinearInterpolator());
        syncAnimation.start();
        
        tvSyncStatus.setText("Syncing...");
    }
    
    /**
     * Hide sync animation
     */
    private void hideSyncInProgress() {
        btnSync.setEnabled(true);
        
        if (syncAnimation != null) {
            syncAnimation.cancel();
            btnSync.setRotation(0f);
        }
        
        updateStatus();
    }
    
    /**
     * Show or hide the status view
     */
    public void setVisibility(int visibility) {
        if (statusView != null) {
            statusView.setVisibility(visibility);
        }
    }
    
    /**
     * Hide the status view (convenience method)
     */
    public void hide() {
        setVisibility(View.GONE);
    }
    
    /**
     * Show the status view (convenience method)
     */
    public void show() {
        setVisibility(View.VISIBLE);
        updateStatus();
    }
    
    /**
     * Set listener for sync requests
     */
    public void setOnSyncRequestedListener(OnSyncRequestedListener listener) {
        this.syncRequestedListener = listener;
    }
    
    /**
     * Check and show sync recommendation if needed
     */
    public void checkAndShowSyncRecommendation() {
        OfflineManager.SyncStats stats = offlineManager.getSyncStats();
        if (networkUtil.isNetworkAvailable()) {
            OfflineInfoHelper.showSyncRecommendation(context, stats.lastSyncTime);
        }
    }
    
    /**
     * Get the status view
     */
    public View getView() {
        return statusView;
    }
    
    /**
     * Interface for handling sync requests
     */
    public interface OnSyncRequestedListener {
        void onSyncRequested();
    }
}
