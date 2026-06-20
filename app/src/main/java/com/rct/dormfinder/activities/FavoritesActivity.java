package com.rct.dormfinder.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.AllDormsAdapter;
import com.rct.dormfinder.models.Dormitory;
import com.rct.dormfinder.utils.FavoritesManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.rct.dormfinder.utils.NavigationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class FavoritesActivity extends BaseActivity {
    private TextView tvTitle;
    private View tvEmptyState;  // Changed from TextView to View since it's a LinearLayout
    private RecyclerView recyclerViewFavorites;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private FavoritesManager favoritesManager;
    private AllDormsAdapter adapter;
    private List<Dormitory> favoriteDormitories;
    private BottomNavigationView bottomNavigation;
    
    // Track loading state to prevent duplicate loads
    private boolean isLoading = false;
    private boolean isInitialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        initializeViews();
        setupFirebase();
        setupRecyclerView();
        setupSwipeRefresh();
        setupListeners();
        setupBottomNavigation();
        loadFavorites();
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        NavigationHelper.setupStudentBottomNavigation(this, bottomNavigation, R.id.nav_favorites);
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        // Apply top insets to header
        View headerLayout = findViewById(R.id.headerLayout);
        if (headerLayout != null) {
            android.view.ViewGroup.MarginLayoutParams params = 
                (android.view.ViewGroup.MarginLayoutParams) headerLayout.getLayoutParams();
            params.topMargin = insets.top;
            headerLayout.setLayoutParams(params);
        }
        
        // Apply bottom insets to bottom navigation
        applyBottomInsets(insets, R.id.bottomNavigation);
        
        // Update swipeRefreshLayout top margin to account for header + system insets
        View swipeRefresh = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefresh != null && headerLayout != null) {
            android.view.ViewGroup.MarginLayoutParams params = 
                (android.view.ViewGroup.MarginLayoutParams) swipeRefresh.getLayoutParams();
            // 56dp (header height) + system inset
            int headerHeight = (int) (56 * getResources().getDisplayMetrics().density);
            params.topMargin = headerHeight + insets.top;
            swipeRefresh.setLayoutParams(params);
        }
    }

    private void initializeViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        recyclerViewFavorites = findViewById(R.id.recyclerViewFavorites);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        favoritesManager = new FavoritesManager(this);
    }

    private void setupRecyclerView() {
        favoriteDormitories = new ArrayList<>();
        adapter = new AllDormsAdapter(this, favoriteDormitories, true); // true = grid layout
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerViewFavorites.setLayoutManager(gridLayoutManager);
        recyclerViewFavorites.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadFavorites();
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.orange_primary);
    }

    private void setupListeners() {
        // No back button - using bottom navigation
    }

    private void loadFavorites() {
        // Prevent multiple simultaneous loads
        if (isLoading) {
            android.util.Log.d("FavoritesActivity", "Already loading, skipping duplicate load");
            return;
        }
        
        isLoading = true;
        swipeRefreshLayout.setRefreshing(true);
        
        // Get current user ID
        com.google.firebase.auth.FirebaseUser currentUser = 
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            android.util.Log.e("FavoritesActivity", "No user logged in");
            favoriteDormitories.clear();
            adapter.updateDormitories(favoriteDormitories);
            updateEmptyState();
            swipeRefreshLayout.setRefreshing(false);
            isLoading = false;
            Toast.makeText(this, "Please login to view favorites", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = currentUser.getUid();
        android.util.Log.d("FavoritesActivity", "Loading favorites for user: " + userId);
        
        // Clear existing list
        favoriteDormitories.clear();
        
        // Load favorites from the subcollection: users/{userId}/favorites
        db.collection("users")
            .document(userId)
            .collection("favorites")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                android.util.Log.d("FavoritesActivity", "Found " + queryDocumentSnapshots.size() + " favorites");
                
                if (queryDocumentSnapshots.isEmpty()) {
                    favoriteDormitories.clear();
                    adapter.updateDormitories(favoriteDormitories);
                    updateEmptyState();
                    swipeRefreshLayout.setRefreshing(false);
                    isLoading = false;
                    return;
                }
                
                // Track how many dormitories we're loading
                final int totalToLoad = queryDocumentSnapshots.size();
                final int[] loadedCount = {0};
                
                // Load each favorite dormitory
                queryDocumentSnapshots.forEach(favoriteDoc -> {
                    String dormId = favoriteDoc.getId();
                    android.util.Log.d("FavoritesActivity", "Loading dormitory: " + dormId);
                    
                    // Get the dormitory details
                    db.collection("dormitories")
                        .document(dormId)
                        .get()
                        .addOnSuccessListener(dormDoc -> {
                            if (dormDoc.exists()) {
                                Dormitory dorm = dormDoc.toObject(Dormitory.class);
                                if (dorm != null) {
                                    dorm.setDormId(dormDoc.getId());
                                    favoriteDormitories.add(dorm);
                                    android.util.Log.d("FavoritesActivity", "Added dormitory: " + dorm.getName());
                                }
                            } else {
                                // Dormitory doesn't exist anymore, remove from favorites
                                android.util.Log.w("FavoritesActivity", "Dormitory not found: " + dormId + ", removing from favorites");
                                db.collection("users")
                                    .document(userId)
                                    .collection("favorites")
                                    .document(dormId)
                                    .delete();
                            }
                            
                            // Increment loaded count
                            loadedCount[0]++;
                            
                            // Update UI when all dormitories are loaded
                            if (loadedCount[0] == totalToLoad) {
                                runOnUiThread(() -> {
                                    android.util.Log.d("FavoritesActivity", "All loaded. Total items: " + favoriteDormitories.size());
                                    adapter.updateDormitories(favoriteDormitories);
                                    updateEmptyState();
                                    swipeRefreshLayout.setRefreshing(false);
                                    isLoading = false;
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("FavoritesActivity", "Failed to load dormitory: " + dormId, e);
                            
                            // Increment loaded count even on failure
                            loadedCount[0]++;
                            
                            // Update UI when all dormitories are loaded
                            if (loadedCount[0] == totalToLoad) {
                                runOnUiThread(() -> {
                                    adapter.updateDormitories(favoriteDormitories);
                                    updateEmptyState();
                                    swipeRefreshLayout.setRefreshing(false);
                                    isLoading = false;
                                });
                            }
                        });
                });
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("FavoritesActivity", "Failed to load favorites collection", e);
                Toast.makeText(this, "Failed to load favorites", Toast.LENGTH_SHORT).show();
                favoriteDormitories.clear();
                adapter.updateDormitories(favoriteDormitories);
                updateEmptyState();
                swipeRefreshLayout.setRefreshing(false);
                isLoading = false;
            });
    }

    private void updateEmptyState() {
        if (favoriteDormitories.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerViewFavorites.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerViewFavorites.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // CRITICAL: Re-select Favorites ONLY after view is rendered to prevent race conditions
        if (bottomNavigation != null) {
            bottomNavigation.post(() -> {
                bottomNavigation.setSelectedItemId(R.id.nav_favorites);
                android.util.Log.d("FavoritesActivity", "✅ Bottom nav set to Favorites in onResume");
            });
        }
        
        // Only reload if this is not the initial load (already loaded in onCreate)
        if (!isInitialLoad) {
            android.util.Log.d("FavoritesActivity", "onResume: Reloading favorites");
            loadFavorites();
        } else {
            android.util.Log.d("FavoritesActivity", "onResume: Skipping initial load");
            isInitialLoad = false;
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Reset the initial load flag when leaving the activity
        // so it will reload when coming back
        android.util.Log.d("FavoritesActivity", "onPause: Activity paused");
    }
}
