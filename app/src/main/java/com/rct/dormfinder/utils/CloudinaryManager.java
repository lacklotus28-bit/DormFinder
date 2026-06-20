package com.rct.dormfinder.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudinaryManager {
    private static final String TAG = "CloudinaryManager";
    private Context context;
    private static CloudinaryManager instance;
    private OnUploadCompleteListener uploadListener;
    private OnUploadProgressListener progressListener;
    private int totalImages = 0;
    private int uploadedCount = 0;

    public interface OnUploadCompleteListener {
        void onUploadComplete(List<String> imageUrls);
        void onUploadError(String error);
    }

    public interface OnUploadProgressListener {
        void onProgress(int current, int total, String message);
    }

    private CloudinaryManager(Context context) {
        this.context = context.getApplicationContext();
        initializeCloudinary();
    }

    public static CloudinaryManager getInstance(Context context) {
        if (instance == null) {
            instance = new CloudinaryManager(context);
        }
        return instance;
    }

    private void initializeCloudinary() {
        try {
            // Initialize Cloudinary with your credentials
            Map config = new HashMap();
            config.put("cloud_name", "dwxjfvv1g");
            config.put("api_key", "767744389793541");
            config.put("api_secret", "1y2IoScT4Cx_azpdBh94KAcvoRk");

            MediaManager.init(context, config);
            Log.d(TAG, "✅ Cloudinary initialized successfully");
            Log.d(TAG, "Cloud Name: dwxjfvv1g");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error initializing Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Upload images to Cloudinary
     * @param imageUris List of image URIs to upload
     * @param dormitoryId Dormitory ID for organization
     * @param listener Callback listener
     */
    public void uploadImages(List<Uri> imageUris, String dormitoryId, OnUploadCompleteListener listener) {
        this.uploadListener = listener;
        
        if (imageUris == null || imageUris.isEmpty()) {
            Log.w(TAG, "No images to upload");
            listener.onUploadComplete(new ArrayList<>());
            return;
        }

        totalImages = imageUris.size();
        uploadedCount = 0;
        
        Log.d(TAG, "📤 Starting upload of " + totalImages + " images for dormitory: " + dormitoryId);
        
        List<String> uploadedUrls = new ArrayList<>();
        uploadImageRecursively(imageUris, dormitoryId, 0, uploadedUrls);
    }

    /**
     * Set progress listener for upload tracking
     */
    public void setProgressListener(OnUploadProgressListener listener) {
        this.progressListener = listener;
    }

    private void uploadImageRecursively(List<Uri> imageUris, String dormitoryId, 
                                       int currentIndex, List<String> uploadedUrls) {
        
        // Base case: all images uploaded
        if (currentIndex >= imageUris.size()) {
            Log.d(TAG, "✅ All images uploaded successfully! Total: " + uploadedUrls.size());
            if (uploadListener != null) {
                uploadListener.onUploadComplete(uploadedUrls);
            }
            return;
        }

        Uri imageUri = imageUris.get(currentIndex);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String publicId = dormitoryId + "_" + timestamp + "_" + currentIndex;

        Log.d(TAG, "📸 Uploading image " + (currentIndex + 1) + "/" + imageUris.size());
        Log.d(TAG, "   URI: " + imageUri.toString());
        Log.d(TAG, "   Public ID: " + publicId);

        // Notify progress
        if (progressListener != null) {
            progressListener.onProgress(currentIndex + 1, totalImages, 
                "Uploading image " + (currentIndex + 1) + " of " + totalImages);
        }

        try {
            MediaManager.get().upload(imageUri)
                    .option("folder", "dormfinder/dormitories/" + dormitoryId)
                    .option("public_id", publicId)
                    .option("resource_type", "auto")
                    .option("quality", "auto:good") // Optimize image quality
                    .option("fetch_format", "auto") // Auto format selection
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "   🔄 Upload started - Request ID: " + requestId);
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            // Get the secure URL
                            String imageUrl = (String) resultData.get("secure_url");
                            
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                uploadedUrls.add(imageUrl);
                                uploadedCount++;
                                
                                Log.d(TAG, "   ✅ Upload successful!");
                                Log.d(TAG, "   📍 URL: " + imageUrl);
                                Log.d(TAG, "   Progress: " + uploadedCount + "/" + totalImages);
                                
                                // Upload next image
                                uploadImageRecursively(imageUris, dormitoryId, currentIndex + 1, uploadedUrls);
                            } else {
                                String error = "Upload returned empty URL";
                                Log.e(TAG, "   ❌ " + error);
                                if (uploadListener != null) {
                                    uploadListener.onUploadError(error);
                                }
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            String errorMsg = "Upload failed for image " + (currentIndex + 1) + ": " + error.getDescription();
                            Log.e(TAG, "   ❌ " + errorMsg);
                            Log.e(TAG, "   Error Code: " + error.getCode());
                            
                            // Try to provide more helpful error messages based on error description
                            String userFriendlyError = errorMsg;
                            String errorDescription = error.getDescription().toLowerCase();
                            
                            if (errorDescription.contains("credential") || errorDescription.contains("authentication")) {
                                userFriendlyError = "Invalid Cloudinary credentials. Please check your configuration.";
                            } else if (errorDescription.contains("network") || errorDescription.contains("connection")) {
                                userFriendlyError = "Network error. Please check your internet connection.";
                            } else if (errorDescription.contains("parameter") || errorDescription.contains("invalid")) {
                                userFriendlyError = "Invalid upload parameters. Please try again.";
                            }
                            
                            if (uploadListener != null) {
                                uploadListener.onUploadError(userFriendlyError);
                            }
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, "   ⚠️ Upload rescheduled: " + error.getDescription());
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            int percentage = (int) ((bytes * 100) / totalBytes);
                            Log.d(TAG, "   📊 Progress: " + percentage + "% (" + bytes + "/" + totalBytes + " bytes)");
                            
                            if (progressListener != null) {
                                String message = "Uploading image " + (currentIndex + 1) + ": " + percentage + "%";
                                progressListener.onProgress(currentIndex + 1, totalImages, message);
                            }
                        }
                    })
                    .dispatch();
        } catch (Exception e) {
            String errorMsg = "Exception during upload: " + e.getMessage();
            Log.e(TAG, "   ❌ " + errorMsg, e);
            
            if (uploadListener != null) {
                uploadListener.onUploadError(errorMsg);
            }
        }
    }

    /**
     * Check if Cloudinary is properly initialized
     */
    public boolean isInitialized() {
        try {
            boolean initialized = MediaManager.get() != null;
            if (initialized) {
                Log.d(TAG, "✅ Cloudinary is initialized and ready");
            } else {
                Log.e(TAG, "❌ Cloudinary MediaManager is null");
            }
            return initialized;
        } catch (Exception e) {
            Log.e(TAG, "❌ Cloudinary not initialized: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Test Cloudinary connection
     */
    public void testConnection() {
        Log.d(TAG, "🔍 Testing Cloudinary connection...");
        try {
            if (MediaManager.get() != null) {
                Log.d(TAG, "✅ MediaManager is available");
                Log.d(TAG, "   Cloud Name: dwxjfvv1g");
                Log.d(TAG, "   API Key configured: Yes");
            } else {
                Log.e(TAG, "❌ MediaManager is null - initialization failed");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Connection test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get upload statistics
     */
    public String getUploadStats() {
        return "Uploaded: " + uploadedCount + "/" + totalImages;
    }
}
