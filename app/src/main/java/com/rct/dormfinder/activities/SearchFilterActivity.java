package com.rct.dormfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.DormitoryAdapter;
import com.rct.dormfinder.database.CachedDormitory;
import com.rct.dormfinder.models.Dormitory;
import com.rct.dormfinder.utils.CacheManager;
import com.rct.dormfinder.utils.NetworkUtil;
import com.rct.dormfinder.utils.OfflineManager;
import java.util.ArrayList;
import java.util.List;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class SearchFilterActivity extends BaseActivity {
    private RecyclerView rvDormitories;
    private EditText etSearch;
    private CardView btnFilter;
    private ImageButton btnBack;
    private ImageView ivSearchIcon, ivClearSearch;
    private ProgressBar searchLoadingIndicator;
    private FloatingActionButton fabMap;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View offlineIndicator, layoutEmptyState;
    private TextView tvOfflineMode, tvResultCount;
    private ProgressBar progressBar;
    
    private DormitoryAdapter adapter;
    private List<Dormitory> allDormitories = new ArrayList<>();
    private List<Dormitory> filteredDormitories = new ArrayList<>();

    private FirebaseFirestore db;
    private CacheManager cacheManager;
    private NetworkUtil networkUtil;
    private OfflineManager offlineManager;

    // Filter variables
    private String selectedCity = "All";
    private double minPrice = 0;
    private double maxPrice = Double.MAX_VALUE;
    private boolean showAvailableOnly = false;
    private List<String> selectedAmenities = new ArrayList<>();
    
    private boolean isOfflineMode = false;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_filter);

        initViews();
        initFirebase();
        setupRecyclerView();
        setupSearch();
        setupNetworkMonitoring();
        loadDormitories();
    }

    private void initViews() {
        rvDormitories = findViewById(R.id.rvDormitories);
        etSearch = findViewById(R.id.etSearch);
        btnFilter = findViewById(R.id.btnFilter);
        btnBack = findViewById(R.id.btnBack);
        fabMap = findViewById(R.id.fabMap);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        offlineIndicator = findViewById(R.id.offlineIndicator);
        tvOfflineMode = findViewById(R.id.tvOfflineMode);
        tvResultCount = findViewById(R.id.tvResultCount);
        progressBar = findViewById(R.id.progressBar);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        
        // New views for modern search
        ivSearchIcon = findViewById(R.id.ivSearchIcon);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        searchLoadingIndicator = findViewById(R.id.searchLoadingIndicator);

        btnFilter.setOnClickListener(v -> showFilterDialog());
        fabMap.setOnClickListener(v -> openMapView());
        
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackPress());
        }
        
        // Clear search button
        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                etSearch.setText("");
                ivClearSearch.setVisibility(View.GONE);
            });
        }
        
        // Setup swipe refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadDormitories);
            swipeRefreshLayout.setColorSchemeResources(R.color.mint_primary);
        }
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        // Apply top insets to the top bar
        View topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            android.view.ViewGroup.MarginLayoutParams params = 
                (android.view.ViewGroup.MarginLayoutParams) topBar.getLayoutParams();
            params.topMargin = insets.top;
            topBar.setLayoutParams(params);
        }
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        cacheManager = new CacheManager(this);
        networkUtil = new NetworkUtil(this);
        offlineManager = OfflineManager.getInstance(this);
    }

    private void setupRecyclerView() {
        adapter = new DormitoryAdapter(filteredDormitories, this);
        // Use GridLayoutManager with 2 columns for professional display
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        rvDormitories.setLayoutManager(gridLayoutManager);
        rvDormitories.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show/hide clear button
                if (ivClearSearch != null) {
                    ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
                
                if (isOfflineMode) {
                    searchOffline(s.toString());
                } else {
                    filterDormitories(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void setupNetworkMonitoring() {
        networkUtil.registerNetworkCallback(new NetworkUtil.OnNetworkStateChangeListener() {
            @Override
            public void onNetworkAvailable() {
                runOnUiThread(() -> {
                    isOfflineMode = false;
                    updateOfflineIndicator();
                    Snackbar.make(findViewById(android.R.id.content), 
                            "Back online! Refreshing data...", Snackbar.LENGTH_SHORT).show();
                    loadDormitories();
                });
            }

            @Override
            public void onNetworkLost() {
                runOnUiThread(() -> {
                    isOfflineMode = true;
                    updateOfflineIndicator();
                    Snackbar.make(findViewById(android.R.id.content), 
                            "You're offline. Showing cached results.", Snackbar.LENGTH_LONG).show();
                    loadFromCache();
                });
            }
        });
    }
    
    private void updateOfflineIndicator() {
        if (offlineIndicator != null) {
            if (isOfflineMode || !networkUtil.isNetworkAvailable()) {
                offlineIndicator.setVisibility(View.VISIBLE);
                if (tvOfflineMode != null) {
                    tvOfflineMode.setText("Offline Mode - Showing cached results");
                }
            } else {
                offlineIndicator.setVisibility(View.GONE);
            }
        }
    }

    private void loadDormitories() {
        if (isLoading) return;
        
        isLoading = true;
        showLoading(true);
        
        isOfflineMode = !networkUtil.isNetworkAvailable();
        updateOfflineIndicator();

        if (isOfflineMode) {
            loadFromCache();
        } else {
            loadFromFirestore();
        }
    }
    
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (swipeRefreshLayout != null && !show) {
            swipeRefreshLayout.setRefreshing(false);
        }
        
        // Toggle search icon and loading indicator
        if (ivSearchIcon != null && searchLoadingIndicator != null) {
            ivSearchIcon.setVisibility(show ? View.GONE : View.VISIBLE);
            searchLoadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void loadFromFirestore() {
        Query query = db.collection("dormitories");

        if (!selectedCity.equals("All")) {
            query = query.whereEqualTo("city", selectedCity);
        }

        query.get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                allDormitories.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Dormitory dorm = document.toObject(Dormitory.class);
                    dorm.setDormId(document.getId());
                    
                    // Skip dummy dormitories
                    if (dorm.getName() != null && dorm.getName().equals("Sunshine Dormitory")) {
                        continue;
                    }
                    
                    allDormitories.add(dorm);
                }

                // Save to cache for offline use
                cacheManager.saveDormitories(allDormitories);
                
                // Also sync to Room database
                offlineManager.syncDormitories((success, count) -> {
                    android.util.Log.d("SearchFilter", "Synced " + count + " dorms to Room DB");
                });

                applyFilters();
                isLoading = false;
                showLoading(false);
                updateResultCount();
                updateEmptyState();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load dormitories", Toast.LENGTH_SHORT).show();
                loadFromCache();
                isLoading = false;
                showLoading(false);
            });
    }

    private void loadFromCache() {
        // Try Room database first
        offlineManager.getAvailableCachedDormitories(cachedDorms -> {
            if (cachedDorms != null && !cachedDorms.isEmpty()) {
                allDormitories.clear();
                for (CachedDormitory cached : cachedDorms) {
                    allDormitories.add(OfflineManager.convertFromCache(cached));
                }
                
                runOnUiThread(() -> {
                    applyFilters();
                    isLoading = false;
                    showLoading(false);
                    updateResultCount();
                    updateEmptyState();
                });
            } else {
                // Fallback to SharedPreferences cache
                allDormitories = cacheManager.getCachedDormitories();
                applyFilters();
                isLoading = false;
                showLoading(false);
                updateResultCount();
                updateEmptyState();
                
                if (allDormitories.isEmpty()) {
                    Toast.makeText(this, "No cached data available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    /**
     * Enhanced offline search using Room database
     */
    private void searchOffline(String searchText) {
        if (searchText.isEmpty()) {
            applyFilters();
            return;
        }
        
        offlineManager.searchCachedDormitories(searchText, cachedDorms -> {
            List<Dormitory> searchResults = new ArrayList<>();
            for (CachedDormitory cached : cachedDorms) {
                Dormitory dorm = OfflineManager.convertFromCache(cached);
                if (matchesFilters(dorm)) {
                    searchResults.add(dorm);
                }
            }
            
            runOnUiThread(() -> {
                filteredDormitories.clear();
                filteredDormitories.addAll(searchResults);
                adapter.notifyDataSetChanged();
                updateResultCount();
                updateEmptyState();
            });
        });
    }

    private void filterDormitories(String searchText) {
        filteredDormitories.clear();

        for (Dormitory dorm : allDormitories) {
            if (matchesSearch(dorm, searchText) && matchesFilters(dorm)) {
                filteredDormitories.add(dorm);
            }
        }

        adapter.notifyDataSetChanged();
        updateResultCount();
        updateEmptyState();
    }

    private boolean matchesSearch(Dormitory dorm, String searchText) {
        if (searchText.isEmpty()) return true;

        String search = searchText.toLowerCase();
        return dorm.getName().toLowerCase().contains(search) ||
                dorm.getAddress().toLowerCase().contains(search) ||
                dorm.getCity().toLowerCase().contains(search);
    }

    private boolean matchesFilters(Dormitory dorm) {
        // Check city
        if (!selectedCity.equals("All") && !dorm.getCity().equals(selectedCity)) {
            return false;
        }
        
        // Check availability - must have available rooms if filter is on
        if (showAvailableOnly) {
            if (!dorm.isAvailable() || dorm.getAvailableRooms() <= 0) {
                return false;
            }
        }
        
        // Check price range
        if (dorm.getMonthlyPrice() < minPrice || dorm.getMonthlyPrice() > maxPrice) {
            return false;
        }

        // Check amenities
        if (!selectedAmenities.isEmpty()) {
            for (String amenity : selectedAmenities) {
                if (!dorm.getAmenities().contains(amenity)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void applyFilters() {
        filterDormitories(etSearch.getText().toString());
    }
    
    private void updateResultCount() {
        if (tvResultCount != null) {
            String countText = filteredDormitories.size() + " dormitor" + 
                    (filteredDormitories.size() == 1 ? "y" : "ies") + " found";
            tvResultCount.setText(countText);
        }
    }
    
    private void updateEmptyState() {
        if (layoutEmptyState != null && rvDormitories != null) {
            if (filteredDormitories.isEmpty()) {
                layoutEmptyState.setVisibility(View.VISIBLE);
                rvDormitories.setVisibility(View.GONE);
            } else {
                layoutEmptyState.setVisibility(View.GONE);
                rvDormitories.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showFilterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        
        // Make dialog background transparent to show rounded corners (just like home page)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Initialize dialog views
        RadioGroup rgCity = dialogView.findViewById(R.id.rgCity);
        EditText etMinPrice = dialogView.findViewById(R.id.etMinPrice);
        EditText etMaxPrice = dialogView.findViewById(R.id.etMaxPrice);
        CheckBox cbWifi = dialogView.findViewById(R.id.cbWifi);
        CheckBox cbAircon = dialogView.findViewById(R.id.cbAircon);
        CheckBox cbParking = dialogView.findViewById(R.id.cbParking);
        CheckBox cbLaundry = dialogView.findViewById(R.id.cbLaundry);
        CheckBox cbAvailableOnly = dialogView.findViewById(R.id.cbAvailableOnly);
        Button btnReset = dialogView.findViewById(R.id.btnReset);
        Button btnApply = dialogView.findViewById(R.id.btnApply);
        
        // Set current filter values
        if (selectedCity.equals("Batangas")) {
            rgCity.check(R.id.rbBatangas);
        } else if (selectedCity.equals("Lipa")) {
            rgCity.check(R.id.rbLipa);
        } else {
            rgCity.check(R.id.rbAll);
        }
        
        if (minPrice > 0) {
            etMinPrice.setText(String.valueOf((int)minPrice));
        }
        if (maxPrice < Double.MAX_VALUE) {
            etMaxPrice.setText(String.valueOf((int)maxPrice));
        }
        
        cbWifi.setChecked(selectedAmenities.contains("WiFi"));
        cbAircon.setChecked(selectedAmenities.contains("Air Conditioning"));
        cbParking.setChecked(selectedAmenities.contains("Parking"));
        cbLaundry.setChecked(selectedAmenities.contains("Laundry"));
        cbAvailableOnly.setChecked(showAvailableOnly);

        // Apply button
        btnApply.setOnClickListener(v -> {
            // Get selected city
            int checkedId = rgCity.getCheckedRadioButtonId();
            if (checkedId == R.id.rbBatangas) {
                selectedCity = "Batangas";
            } else if (checkedId == R.id.rbLipa) {
                selectedCity = "Lipa";
            } else {
                selectedCity = "All";
            }

            // Get price range
            String minPriceStr = etMinPrice.getText().toString();
            String maxPriceStr = etMaxPrice.getText().toString();
            minPrice = minPriceStr.isEmpty() ? 0 : Double.parseDouble(minPriceStr);
            maxPrice = maxPriceStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxPriceStr);

            // Get selected amenities
            selectedAmenities.clear();
            if (cbWifi.isChecked()) selectedAmenities.add("WiFi");
            if (cbAircon.isChecked()) selectedAmenities.add("Air Conditioning");
            if (cbParking.isChecked()) selectedAmenities.add("Parking");
            if (cbLaundry.isChecked()) selectedAmenities.add("Laundry");

            showAvailableOnly = cbAvailableOnly.isChecked();

            // Apply filters locally first for instant response
            applyFilters();
            
            // Then reload from source if online
            if (!isOfflineMode) {
                loadDormitories();
            }
            
            Toast.makeText(this, "Filters applied", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // Reset button
        btnReset.setOnClickListener(v -> {
            rgCity.check(R.id.rbAll);
            etMinPrice.setText("");
            etMaxPrice.setText("");
            cbWifi.setChecked(false);
            cbAircon.setChecked(false);
            cbParking.setChecked(false);
            cbLaundry.setChecked(false);
            cbAvailableOnly.setChecked(false);
            
            // Reset filter variables
            selectedCity = "All";
            minPrice = 0;
            maxPrice = Double.MAX_VALUE;
            selectedAmenities.clear();
            showAvailableOnly = false;
            
            // Apply reset filters
            applyFilters();
            
            Toast.makeText(this, "Filters reset", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void openMapView() {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkUtil != null) {
            networkUtil.unregisterNetworkCallback();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Check network status on resume
        isOfflineMode = !networkUtil.isNetworkAvailable();
        updateOfflineIndicator();
    }

    /**
     * Handle back press - exit directly without confirmation
     */
    private void handleBackPress() {
        finish();
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
