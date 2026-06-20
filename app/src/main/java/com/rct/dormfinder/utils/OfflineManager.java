package com.rct.dormfinder.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rct.dormfinder.database.AppDatabase;
import com.rct.dormfinder.database.BookingDao;
import com.rct.dormfinder.database.CachedBooking;
import com.rct.dormfinder.database.CachedDormitory;
import com.rct.dormfinder.database.CachedPayment;
import com.rct.dormfinder.database.CachedReview;
import com.rct.dormfinder.database.DormitoryDao;
import com.rct.dormfinder.database.PaymentDao;
import com.rct.dormfinder.database.ReviewDao;
import com.rct.dormfinder.models.Booking;
import com.rct.dormfinder.models.Dormitory;
import com.rct.dormfinder.models.Payment;
import com.rct.dormfinder.models.Review;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OfflineManager {
    
    private static final String TAG = "OfflineManager";
    private static final String PREFS_NAME = "OfflineManagerPrefs";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";
    private static final String KEY_CACHED_COUNT = "cached_count";
    
    private static OfflineManager instance;
    
    private Context context;
    private AppDatabase database;
    private DormitoryDao dormitoryDao;
    private BookingDao bookingDao;
    private ReviewDao reviewDao;
    private PaymentDao paymentDao;
    private FirebaseFirestore firestore;
    private ExecutorService executorService;
    private SharedPreferences prefs;
    private ImageCacheManager imageCacheManager;
    
    private OfflineManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.dormitoryDao = database.dormitoryDao();
        this.bookingDao = database.bookingDao();
        this.reviewDao = database.reviewDao();
        this.paymentDao = database.paymentDao();
        this.firestore = FirebaseFirestore.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.imageCacheManager = ImageCacheManager.getInstance(context);
    }
    
    public static synchronized OfflineManager getInstance(Context context) {
        if (instance == null) {
            instance = new OfflineManager(context);
        }
        return instance;
    }
    
    /**
     * Sync data from Firestore to local database
     * Enhanced version with progress tracking and better error handling
     */
    public void syncDormitories(OnSyncCompleteListener listener) {
        long syncStartTime = System.currentTimeMillis();
        
        firestore.collection("dormitories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CachedDormitory> cachedDormitories = new ArrayList<>();
                    int totalDocs = queryDocumentSnapshots.size();
                    int successCount = 0;
                    int errorCount = 0;
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Dormitory dormitory = document.toObject(Dormitory.class);
                            dormitory.setDormId(document.getId());
                            
                            CachedDormitory cachedDorm = convertToCache(dormitory);
                            cachedDormitories.add(cachedDorm);
                            successCount++;
                        } catch (Exception e) {
                            errorCount++;
                            Log.e(TAG, "Error converting document " + document.getId() + ": " + e.getMessage());
                        }
                    }
                    
                    // Save to local database in background thread
                    final int finalSuccessCount = successCount;
                    final int finalErrorCount = errorCount;
                    
                    executorService.execute(() -> {
                        try {
                            // Use transaction for atomic operation
                            database.runInTransaction(() -> {
                                dormitoryDao.deleteAll();
                                dormitoryDao.insertAll(cachedDormitories);
                            });
                            
                            // Save sync metadata
                            long syncTime = System.currentTimeMillis();
                            prefs.edit()
                                    .putLong(KEY_LAST_SYNC_TIME, syncTime)
                                    .putInt(KEY_CACHED_COUNT, cachedDormitories.size())
                                    .apply();
                            
                            if (listener != null) {
                                listener.onSyncComplete(true, cachedDormitories.size());
                            }
                            
                            long duration = System.currentTimeMillis() - syncStartTime;
                            Log.d(TAG, String.format("Sync completed: %d items synced, %d errors in %d ms", 
                                    finalSuccessCount, finalErrorCount, duration));
                        } catch (Exception e) {
                            Log.e(TAG, "Database error during sync: " + e.getMessage(), e);
                            if (listener != null) {
                                listener.onSyncComplete(false, 0);
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Sync failed: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onSyncComplete(false, 0);
                    }
                });
    }
    
    /**
     * Get sync statistics
     */
    public SyncStats getSyncStats() {
        SyncStats stats = new SyncStats();
        stats.lastSyncTime = prefs.getLong(KEY_LAST_SYNC_TIME, 0);
        stats.cachedCount = prefs.getInt(KEY_CACHED_COUNT, 0);
        
        // Get actual count from database
        executorService.execute(() -> {
            stats.actualCount = dormitoryDao.getCount();
        });
        
        return stats;
    }
    
    /**
     * Get cached dormitories (offline)
     */
    public void getCachedDormitories(OnDataLoadedListener listener) {
        executorService.execute(() -> {
            List<CachedDormitory> dormitories = dormitoryDao.getAllDormitoriesSync();
            if (listener != null) {
                listener.onDataLoaded(dormitories);
            }
        });
    }
    
    /**
     * Get available dormitories (offline)
     */
    public void getAvailableCachedDormitories(OnDataLoadedListener listener) {
        executorService.execute(() -> {
            List<CachedDormitory> dormitories = dormitoryDao.getAvailableDormitoriesSync();
            if (listener != null) {
                listener.onDataLoaded(dormitories);
            }
        });
    }
    
    /**
     * Search dormitories offline
     */
    public void searchCachedDormitories(String query, OnDataLoadedListener listener) {
        executorService.execute(() -> {
            List<CachedDormitory> dormitories = dormitoryDao.searchDormitories(query);
            if (listener != null) {
                listener.onDataLoaded(dormitories);
            }
        });
    }
    
    /**
     * Filter by price offline
     */
    public void filterByPrice(double minPrice, double maxPrice, OnDataLoadedListener listener) {
        executorService.execute(() -> {
            List<CachedDormitory> dormitories = dormitoryDao.filterByPrice(minPrice, maxPrice);
            if (listener != null) {
                listener.onDataLoaded(dormitories);
            }
        });
    }
    
    /**
     * Filter by location offline
     */
    public void filterByLocation(String location, OnDataLoadedListener listener) {
        executorService.execute(() -> {
            List<CachedDormitory> dormitories = dormitoryDao.filterByLocation(location);
            if (listener != null) {
                listener.onDataLoaded(dormitories);
            }
        });
    }
    
    /**
     * Advanced filter with multiple criteria
     */
    public void filterDormitories(FilterCriteria criteria, OnDataLoadedListener listener) {
        executorService.execute(() -> {
            List<CachedDormitory> allDorms = dormitoryDao.getAllDormitoriesSync();
            List<CachedDormitory> filtered = new ArrayList<>();
            
            for (CachedDormitory dorm : allDorms) {
                if (matchesCriteria(dorm, criteria)) {
                    filtered.add(dorm);
                }
            }
            
            if (listener != null) {
                listener.onDataLoaded(filtered);
            }
        });
    }
    
    private boolean matchesCriteria(CachedDormitory dorm, FilterCriteria criteria) {
        // Check location
        if (criteria.location != null && !criteria.location.isEmpty() && 
            !criteria.location.equals("All")) {
            if (!dorm.getLocation().equalsIgnoreCase(criteria.location)) {
                return false;
            }
        }
        
        // Check price range
        if (dorm.getPrice() < criteria.minPrice || dorm.getPrice() > criteria.maxPrice) {
            return false;
        }
        
        // Check availability
        if (criteria.availableOnly && (!dorm.isAvailable() || dorm.getAvailableRooms() <= 0)) {
            return false;
        }
        
        // Check amenities
        if (criteria.requiredAmenities != null && !criteria.requiredAmenities.isEmpty()) {
            List<String> dormAmenities = dorm.getAmenities();
            if (dormAmenities == null) return false;
            
            for (String required : criteria.requiredAmenities) {
                if (!dormAmenities.contains(required)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Get cached dormitory count
     */
    public void getCachedCount(OnCountLoadedListener listener) {
        executorService.execute(() -> {
            int count = dormitoryDao.getCount();
            if (listener != null) {
                listener.onCountLoaded(count);
            }
        });
    }
    
    /**
     * Check if data is available offline
     */
    public void isDataAvailableOffline(OnAvailabilityCheckedListener listener) {
        executorService.execute(() -> {
            int count = dormitoryDao.getCount();
            if (listener != null) {
                listener.onAvailabilityChecked(count > 0);
            }
        });
    }
    
    /**
     * Get statistics about cached data
     */
    public void getCacheStatistics(OnStatisticsLoadedListener listener) {
        executorService.execute(() -> {
            Map<String, Object> stats = new HashMap<>();
            
            int totalCount = dormitoryDao.getCount();
            List<CachedDormitory> available = dormitoryDao.getAvailableDormitoriesSync();
            
            stats.put("totalCached", totalCount);
            stats.put("availableCount", available.size());
            stats.put("unavailableCount", totalCount - available.size());
            stats.put("lastSyncTime", prefs.getLong(KEY_LAST_SYNC_TIME, 0));
            
            // Calculate average price
            double totalPrice = 0;
            for (CachedDormitory dorm : available) {
                totalPrice += dorm.getPrice();
            }
            double avgPrice = available.isEmpty() ? 0 : totalPrice / available.size();
            stats.put("averagePrice", avgPrice);
            
            if (listener != null) {
                listener.onStatisticsLoaded(stats);
            }
        });
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache(OnCacheClearedListener listener) {
        executorService.execute(() -> {
            try {
                dormitoryDao.deleteAll();
                prefs.edit().clear().apply();
                
                if (listener != null) {
                    listener.onCacheCleared(true);
                }
                Log.d(TAG, "Cache cleared successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing cache: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onCacheCleared(false);
                }
            }
        });
    }
    
    /**
     * Convert Dormitory to CachedDormitory
     */
    private CachedDormitory convertToCache(Dormitory dormitory) {
        CachedDormitory cached = new CachedDormitory();
        cached.setDormId(dormitory.getDormId());
        cached.setName(dormitory.getName());
        cached.setDescription(dormitory.getDescription());
        cached.setAddress(dormitory.getAddress());
        cached.setLocation(dormitory.getCity());
        cached.setLatitude(dormitory.getLatitude());
        cached.setLongitude(dormitory.getLongitude());
        cached.setPrice(dormitory.getMonthlyPrice());
        cached.setTotalRooms(dormitory.getTotalRooms());
        cached.setAvailableRooms(dormitory.getAvailableRooms());
        cached.setLandlordId(dormitory.getLandlordId());
        cached.setLandlordName("");
        cached.setAmenities(dormitory.getAmenities());
        cached.setImageUrls(dormitory.getImages());
        cached.setRules("");
        cached.setLastSyncTime(System.currentTimeMillis());
        cached.setAvailable(dormitory.isAvailable());
        cached.setAverageRating(dormitory.getAverageRating());
        cached.setTotalReviews(dormitory.getTotalReviews());
        return cached;
    }
    
    /**
     * Convert CachedDormitory to Dormitory
     */
    public static Dormitory convertFromCache(CachedDormitory cached) {
        Dormitory dormitory = new Dormitory();
        dormitory.setDormId(cached.getDormId());
        dormitory.setName(cached.getName());
        dormitory.setDescription(cached.getDescription());
        dormitory.setAddress(cached.getAddress());
        dormitory.setCity(cached.getLocation());
        dormitory.setLatitude(cached.getLatitude());
        dormitory.setLongitude(cached.getLongitude());
        dormitory.setMonthlyPrice(cached.getPrice());
        dormitory.setTotalRooms(cached.getTotalRooms());
        dormitory.setAvailableRooms(cached.getAvailableRooms());
        dormitory.setLandlordId(cached.getLandlordId());
        dormitory.setAmenities(cached.getAmenities());
        dormitory.setImages(cached.getImageUrls());
        dormitory.setAvailable(cached.isAvailable());
        dormitory.setAverageRating(cached.getAverageRating());
        dormitory.setTotalReviews(cached.getTotalReviews());
        return dormitory;
    }
    
    /**
     * Sync bookings from Firestore to local database
     */
    public void syncBookings(String userId, boolean isLandlord, OnSyncCompleteListener listener) {
        String fieldName = isLandlord ? "landlordId" : "studentId";
        
        firestore.collection("bookings")
                .whereEqualTo(fieldName, userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CachedBooking> cachedBookings = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Booking booking = document.toObject(Booking.class);
                            booking.setBookingId(document.getId());
                            
                            CachedBooking cachedBooking = convertBookingToCache(booking);
                            cachedBookings.add(cachedBooking);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting booking: " + e.getMessage());
                        }
                    }
                    
                    executorService.execute(() -> {
                        try {
                            bookingDao.insertAll(cachedBookings);
                            
                            if (listener != null) {
                                listener.onSyncComplete(true, cachedBookings.size());
                            }
                            Log.d(TAG, "Bookings synced: " + cachedBookings.size());
                        } catch (Exception e) {
                            Log.e(TAG, "Database error during booking sync: " + e.getMessage(), e);
                            if (listener != null) {
                                listener.onSyncComplete(false, 0);
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Booking sync failed: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onSyncComplete(false, 0);
                    }
                });
    }
    
    /**
     * Sync reviews from Firestore to local database
     */
    public void syncReviews(OnSyncCompleteListener listener) {
        firestore.collection("reviews")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CachedReview> cachedReviews = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Review review = document.toObject(Review.class);
                            review.setReviewId(document.getId());
                            
                            CachedReview cachedReview = convertReviewToCache(review);
                            cachedReviews.add(cachedReview);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting review: " + e.getMessage());
                        }
                    }
                    
                    executorService.execute(() -> {
                        try {
                            database.runInTransaction(() -> {
                                reviewDao.deleteAll();
                                reviewDao.insertAll(cachedReviews);
                            });
                            
                            if (listener != null) {
                                listener.onSyncComplete(true, cachedReviews.size());
                            }
                            Log.d(TAG, "Reviews synced: " + cachedReviews.size());
                        } catch (Exception e) {
                            Log.e(TAG, "Database error during review sync: " + e.getMessage(), e);
                            if (listener != null) {
                                listener.onSyncComplete(false, 0);
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Review sync failed: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onSyncComplete(false, 0);
                    }
                });
    }
    
    /**
     * Sync payments from Firestore to local database
     */
    public void syncPayments(String userId, OnSyncCompleteListener listener) {
        firestore.collection("payments")
                .whereEqualTo("studentId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CachedPayment> cachedPayments = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Payment payment = document.toObject(Payment.class);
                            payment.setPaymentId(document.getId());
                            
                            CachedPayment cachedPayment = convertPaymentToCache(payment);
                            cachedPayments.add(cachedPayment);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting payment: " + e.getMessage());
                        }
                    }
                    
                    executorService.execute(() -> {
                        try {
                            paymentDao.insertAll(cachedPayments);
                            
                            if (listener != null) {
                                listener.onSyncComplete(true, cachedPayments.size());
                            }
                            Log.d(TAG, "Payments synced: " + cachedPayments.size());
                        } catch (Exception e) {
                            Log.e(TAG, "Database error during payment sync: " + e.getMessage(), e);
                            if (listener != null) {
                                listener.onSyncComplete(false, 0);
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Payment sync failed: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onSyncComplete(false, 0);
                    }
                });
    }
    
    /**
     * Convert Booking to CachedBooking
     */
    private CachedBooking convertBookingToCache(Booking booking) {
        CachedBooking cached = new CachedBooking();
        cached.setBookingId(booking.getBookingId());
        cached.setStudentId(booking.getStudentId());
        cached.setDormId(booking.getDormitoryId());
        cached.setDormName(booking.getDormitoryName() != null ? booking.getDormitoryName() : "");
        cached.setCheckInDate(String.valueOf(booking.getMoveInDate())); // Convert long to String
        cached.setCheckOutDate(""); // Not in Booking model
        cached.setNumberOfRooms(1); // Not in Booking model, default to 1
        cached.setTotalPrice(booking.getMonthlyPrice());
        cached.setStatus(booking.getStatus());
        cached.setPaymentStatus(booking.getPaymentStatus() != null ? booking.getPaymentStatus() : "UNPAID");
        cached.setSpecialRequests(booking.getMessage() != null ? booking.getMessage() : "");
        cached.setBookingDate(booking.getRequestDate());
        cached.setLastSyncTime(System.currentTimeMillis());
        return cached;
    }
    
    /**
     * Convert Review to CachedReview
     */
    private CachedReview convertReviewToCache(Review review) {
        CachedReview cached = new CachedReview();
        cached.setReviewId(review.getReviewId());
        cached.setDormId(review.getDormId());
        cached.setDormName(""); // Not in Review model
        cached.setStudentId(review.getStudentId());
        cached.setStudentName(review.getStudentName());
        cached.setRating((int) review.getRating()); // Convert float to int
        cached.setComment(review.getComment());
        cached.setReviewDate(review.getDatePosted());
        cached.setLandlordReply(review.getLandlordReply());
        cached.setReplyDate(review.getReplyDate());
        cached.setHasReply(review.getLandlordReply() != null && !review.getLandlordReply().isEmpty());
        cached.setLastSyncTime(System.currentTimeMillis());
        return cached;
    }
    
    /**
     * Convert Payment to CachedPayment
     */
    private CachedPayment convertPaymentToCache(Payment payment) {
        CachedPayment cached = new CachedPayment();
        cached.setPaymentId(payment.getPaymentId());
        cached.setBookingId(payment.getBookingId());
        cached.setStudentId(payment.getStudentId());
        cached.setDormName(payment.getDormitoryName() != null ? payment.getDormitoryName() : "");
        cached.setAmount(payment.getAmount());
        cached.setPaymentMethod(payment.getPaymentMethod());
        cached.setStatus(payment.getStatus());
        cached.setReferenceNumber(payment.getReferenceNumber());
        cached.setPaymentDate(payment.getTimestamp());
        cached.setDueDate(0); // Not in Payment model
        cached.setRemarks(payment.getDescription() != null ? payment.getDescription() : "");
        cached.setLastSyncTime(System.currentTimeMillis());
        return cached;
    }
    
    /**
     * Sync dormitory images for offline access
     */
    public void syncDormitoryImages(List<String> imageUrls, ImageCacheManager.OnImagesCachedListener listener) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            if (listener != null) {
                listener.onImagesCached(new ArrayList<>(), 0, 0);
            }
            return;
        }
        
        imageCacheManager.cacheImages(imageUrls, listener);
    }
    
    /**
     * Get image cache manager instance
     */
    public ImageCacheManager getImageCacheManager() {
        return imageCacheManager;
    }
    
    // Data classes
    public static class FilterCriteria {
        public String location;
        public double minPrice = 0;
        public double maxPrice = Double.MAX_VALUE;
        public boolean availableOnly = true;
        public List<String> requiredAmenities = new ArrayList<>();
    }
    
    public static class SyncStats {
        public long lastSyncTime;
        public int cachedCount;
        public int actualCount;
    }
    
    // Interfaces
    public interface OnSyncCompleteListener {
        void onSyncComplete(boolean success, int itemCount);
    }
    
    public interface OnDataLoadedListener {
        void onDataLoaded(List<CachedDormitory> dormitories);
    }
    
    public interface OnCountLoadedListener {
        void onCountLoaded(int count);
    }
    
    public interface OnAvailabilityCheckedListener {
        void onAvailabilityChecked(boolean isAvailable);
    }
    
    public interface OnStatisticsLoadedListener {
        void onStatisticsLoaded(Map<String, Object> statistics);
    }
    
    public interface OnCacheClearedListener {
        void onCacheCleared(boolean success);
    }
}
