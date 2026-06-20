package com.rct.dormfinder.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.view.ViewTreeObserver;
import android.graphics.Rect;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rct.dormfinder.R;
import com.rct.dormfinder.models.Dormitory;
import com.rct.dormfinder.adapters.MapDormitoryAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MapActivity extends BaseActivity implements OnMapReadyCallback {
    private static final String TAG = "MapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    private GoogleMap mMap;
    private SearchView searchView;
    private ProgressBar progressBar;
    private RecyclerView nearbyList;
    private LinearLayout emptyStateLayout;
    private LinearLayout fabContainer;
    private BottomSheetBehavior resultsBottomSheetBehavior;
    private View resultsBottomSheet;
    private TextView tvDormCount, radiusInfo;
    private FloatingActionButton fabMyLocation, fabRefresh, fabFilter;
    private ImageView btnMapType, btnToggleSearch;
    private MaterialCardView searchPanelCard;
    private boolean isSearchPanelVisible = false;
    
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private List<Dormitory> allDormitories;
    private List<Dormitory> filteredDormitories;
    private MapDormitoryAdapter adapter;
    private Circle radiusCircle;
    private com.google.android.gms.maps.model.Marker centerMarker;
    
    // Radius search components
    private SeekBar radiusSeekBar;
    private TextView radiusValue;
    private CheckBox radiusSearchEnabled;
    private double customSearchRadius = 5.0;
    private final double MAX_RADIUS_KM = 25.0;
    private final double MIN_RADIUS_KM = 1.0;
    private LatLng searchCenter;
    private LatLng userLocation;
    
    // Handler for delayed operations
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        Log.d(TAG, "=== MapActivity onCreate called ===");

        mainHandler = new Handler(Looper.getMainLooper());
        
        initializeViews();
        setupToolbar();
        setupRadiusSearch();
        setupFirestore();
        setupMap();
        setupListeners();
        setupRecyclerView();
        setupLocationClient();
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        // Apply top insets to header layout (same as StudentHomeActivity)
        applyTopInsets(insets, R.id.headerLayout);
        
        // Set top margin on bottom sheet to prevent it from going under header
        View headerCard = findViewById(R.id.headerCard);
        View bottomSheetCard = findViewById(R.id.bottomSheetCard);
        
        if (headerCard != null && bottomSheetCard != null) {
            // Wait for header to be measured
            headerCard.post(() -> {
                int headerHeight = headerCard.getHeight();
                
                // Set top margin on bottom sheet card
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                    (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) 
                    bottomSheetCard.getLayoutParams();
                params.topMargin = headerHeight;
                bottomSheetCard.setLayoutParams(params);
                
                Log.d(TAG, "Bottom sheet top margin set to: " + headerHeight + "px");
            });
        }
    }

    private void initializeViews() {
        searchView = findViewById(R.id.searchView);
        progressBar = findViewById(R.id.progressBar);
        nearbyList = findViewById(R.id.nearbyList);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        tvDormCount = findViewById(R.id.tvDormCount);
        radiusInfo = findViewById(R.id.radiusInfo);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        fabRefresh = findViewById(R.id.fabRefresh);
        fabFilter = findViewById(R.id.fabFilter);
        fabContainer = findViewById(R.id.fabContainer);
        btnMapType = findViewById(R.id.btnMapType);
        btnToggleSearch = findViewById(R.id.btnToggleSearch);
        searchPanelCard = findViewById(R.id.searchPanelCard);
        resultsBottomSheet = findViewById(R.id.resultsBottomSheet);
        
        // Setup bottom sheet behavior
        if (resultsBottomSheet != null) {
            resultsBottomSheetBehavior = BottomSheetBehavior.from(resultsBottomSheet);
        }
        
        // Configure SearchView text color after view is laid out
        if (searchView != null) {
            searchView.post(new Runnable() {
                @Override
                public void run() {
                    configureSearchViewTextColor();
                }
            });
        }
        
        Log.d(TAG, "Views initialized successfully");
    }

    private void setupToolbar() {
        // No back button - using bottom navigation
    }

    private void setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setupRadiusSearch() {
        radiusSeekBar = findViewById(R.id.radiusSeekBar);
        radiusValue = findViewById(R.id.radiusValue);
        radiusSearchEnabled = findViewById(R.id.radiusSearchEnabled);

        if (radiusSeekBar == null || radiusValue == null) {
            Log.w(TAG, "Radius search controls not found in layout");
            return;
        }

        // Initialize with 5km
        customSearchRadius = 5.0;
        updateRadiusDisplay();

        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Convert progress (0-20) to radius (1-25 km)
                customSearchRadius = MIN_RADIUS_KM + (progress * (MAX_RADIUS_KM - MIN_RADIUS_KM) / 20.0);
                updateRadiusDisplay();
                
                // Redraw circle with new radius if radius search is enabled
                if (radiusSearchEnabled.isChecked() && radiusCircle != null && searchCenter != null) {
                    redrawRadiusCircle();
                    applyRadiusFilter();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        if (radiusSearchEnabled != null) {
            radiusSearchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    Toast.makeText(this, "Tap on map to set search location or tap a marker", Toast.LENGTH_LONG).show();
                } else {
                    clearRadiusSearch();
                }
            });
        }
    }

    private void updateRadiusDisplay() {
        if (radiusValue != null) {
            radiusValue.setText(String.format("%.1f km", customSearchRadius));
        }
        if (radiusInfo != null) {
            radiusInfo.setText(String.format("Within %.1f km", customSearchRadius));
        }
    }

    private void redrawRadiusCircle() {
        if (radiusCircle != null) {
            radiusCircle.remove();
        }
        
        if (searchCenter != null && mMap != null) {
            radiusCircle = mMap.addCircle(new CircleOptions()
                    .center(searchCenter)
                    .radius(customSearchRadius * 1000) // Convert to meters
                    .strokeColor(0xFF4CAF50)
                    .fillColor(0x334CAF50)
                    .strokeWidth(3));
            
            Log.d(TAG, "Radius circle redrawn with radius " + String.format("%.1f", customSearchRadius) + " km");
        }
    }

    private void applyRadiusFilter() {
        if (searchCenter == null) return;
        
        List<Dormitory> nearby = allDormitories.stream()
                .filter(dorm -> {
                    double distance = calculateDistanceFromLatLng(searchCenter, dorm);
                    return distance <= customSearchRadius;
                })
                .collect(Collectors.toList());

        Log.d(TAG, "Radius filter: " + nearby.size() + " dormitories within " + 
                String.format("%.1f", customSearchRadius) + " km");
        updateDormitoryList(nearby);
    }

    private void clearRadiusSearch() {
        if (radiusCircle != null) {
            radiusCircle.remove();
            radiusCircle = null;
        }
        if (centerMarker != null) {
            centerMarker.remove();
            centerMarker = null;
        }
        searchCenter = null;
        updateDormitoryList(filteredDormitories);
    }

    private void setupFirestore() {
        db = FirebaseFirestore.getInstance();
        allDormitories = new ArrayList<>();
        filteredDormitories = new ArrayList<>();
        Log.d(TAG, "Firestore initialized");
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            Log.d(TAG, "Map fragment found, requesting async");
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "❌ CRITICAL: Map fragment NOT found!");
            Toast.makeText(this, "Error: Map fragment not found", Toast.LENGTH_LONG).show();
        }
    }

    private void setupRecyclerView() {
        adapter = new MapDormitoryAdapter(filteredDormitories, this, dormitory -> {
            Intent intent = new Intent(this, DormitoryDetailActivity.class);
            intent.putExtra("dormitory_id", dormitory.getDormId());
            startActivity(intent);
        });
        
        // Use GridLayoutManager for full-screen view with 2 columns
        androidx.recyclerview.widget.GridLayoutManager layoutManager = 
            new androidx.recyclerview.widget.GridLayoutManager(this, 2);
        nearbyList.setLayoutManager(layoutManager);
        nearbyList.setAdapter(adapter);
        Log.d(TAG, "RecyclerView setup complete with grid layout");
    }

    private void configureSearchViewTextColor() {
        try {
            // Method 1: Using androidx.appcompat.widget.SearchView
            int searchTextId = androidx.appcompat.R.id.search_src_text;
            android.widget.EditText searchEditText = searchView.findViewById(searchTextId);
            
            if (searchEditText != null) {
                // Set text color to pure black for maximum visibility
                searchEditText.setTextColor(0xFF000000); // Pure black
                // Set hint color to dark gray
                searchEditText.setHintTextColor(0xFF757575); // Dark gray
                // Set text size
                searchEditText.setTextSize(16);
                Log.d(TAG, "SearchView text color configured successfully using Method 1");
            } else {
                // Method 2: Fallback using resource identifier
                Log.d(TAG, "Trying fallback method...");
                int searchTextId2 = searchView.getContext().getResources()
                        .getIdentifier("android:id/search_src_text", null, null);
                
                if (searchTextId2 != 0) {
                    android.widget.TextView searchText = searchView.findViewById(searchTextId2);
                    if (searchText != null) {
                        searchText.setTextColor(0xFF000000); // Pure black
                        searchText.setHintTextColor(0xFF757575); // Dark gray
                        searchText.setTextSize(16);
                        Log.d(TAG, "SearchView text color configured successfully using Method 2");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error configuring SearchView text color", e);
        }
    }
    
    private void setupListeners() {
        // Configure SearchView text colors programmatically
        configureSearchViewTextColor();
        
        // Search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Search submitted: " + query);
                filterDormitories(query);
                
                // Expand results when search is submitted
                if (resultsBottomSheetBehavior != null) {
                    resultsBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
                
                // Hide keyboard after search
                searchView.clearFocus();
                android.view.inputmethod.InputMethodManager imm = 
                    (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                }
                
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "Search text changed: " + newText);
                filterDormitories(newText);
                
                // Only expand if there are results and user has typed something
                if (!newText.isEmpty() && !filteredDormitories.isEmpty()) {
                    if (resultsBottomSheetBehavior != null && 
                        resultsBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                        resultsBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                    }
                }
                
                return true;
            }
        });

        // FAB listeners with animations
        fabMyLocation.setOnClickListener(v -> {
            // Pulse animation
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start());
            moveToMyLocation();
        });
        
        fabRefresh.setOnClickListener(v -> {
            // Rotation animation
            v.animate().rotation(v.getRotation() + 360f).setDuration(500).start();
            loadDormitories();
        });
        
        fabFilter.setOnClickListener(v -> {
            // Bounce animation
            v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100)
                .withEndAction(() -> openFilterDialog()).start());
        });
        
        // Map type toggle
        btnMapType.setOnClickListener(v -> toggleMapType());
        
        // Toggle search panel
        btnToggleSearch.setOnClickListener(v -> toggleSearchPanel());
    }

    private void toggleMapType() {
        if (mMap == null) return;
        
        int currentType = mMap.getMapType();
        if (currentType == GoogleMap.MAP_TYPE_NORMAL) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            Toast.makeText(this, "Satellite View", Toast.LENGTH_SHORT).show();
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            Toast.makeText(this, "Default View", Toast.LENGTH_SHORT).show();
        }
        
        // Animate the icon
        btnMapType.animate().rotationBy(180f).setDuration(300).start();
    }
    
    private void toggleSearchPanel() {
        isSearchPanelVisible = !isSearchPanelVisible;
        
        if (isSearchPanelVisible) {
            // Show search panel with slide down animation
            searchPanelCard.setVisibility(View.VISIBLE);
            searchPanelCard.setTranslationY(-searchPanelCard.getHeight());
            searchPanelCard.animate()
                .translationY(0)
                .setDuration(300)
                .start();
            
            // Rotate icon
            btnToggleSearch.animate().rotation(180f).setDuration(300).start();
        } else {
            // Hide search panel with slide up animation
            searchPanelCard.animate()
                .translationY(-searchPanelCard.getHeight())
                .setDuration(300)
                .withEndAction(() -> searchPanelCard.setVisibility(View.GONE))
                .start();
            
            // Reset icon rotation
            btnToggleSearch.animate().rotation(0f).setDuration(300).start();
        }
    }
    
    private void openFilterDialog() {
        // Navigate to filter activity
        Intent intent = new Intent(this, SearchFilterActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    
    private void moveToMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null && mMap != null) {
                            userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                            Toast.makeText(MapActivity.this, "Moved to your location", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MapActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                moveToMyLocation();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "✓ onMapReady called - Map is now ready!");
        mMap = googleMap;

        // Professional map styling
        try {
            // Apply custom map style for better aesthetics
            boolean success = mMap.setMapStyle(
                com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.map_style));
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            } else {
                Log.d(TAG, "✓ Custom map style applied successfully");
            }
        } catch (android.content.res.Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        // Enable map controls with professional settings
        mMap.getUiSettings().setZoomControlsEnabled(false); // Use FAB instead
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false); // Use custom FAB
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Enable my location layer if permission granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            Log.d(TAG, "✓ My location layer enabled");
        }

        // Set initial camera position to Batangas with smooth animation
        LatLng batangas = new LatLng(13.7565, 121.0583);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(batangas, 12), 1000, null);
        Log.d(TAG, "✓ Map camera set to Batangas (13.7565, 121.0583) with zoom 12");

        // Get user's location for distance calculations
        getUserLocationForDistance();

        // Load dormitories and add markers
        Log.d(TAG, ">>> About to call loadDormitories() <<<");
        loadDormitories();

        // Set marker click listener for dormitory markers
        mMap.setOnMarkerClickListener(marker -> {
            Log.d(TAG, "Marker clicked: " + marker.getTitle());
            
            // Check if it's the center marker for radius search
            if (marker.equals(centerMarker)) {
                return false; // Allow marker to be dragged
            }
            
            Dormitory dormitory = (Dormitory) marker.getTag();
            if (dormitory != null) {
                marker.showInfoWindow();
                
                // If radius search enabled, set this as search center
                if (radiusSearchEnabled != null && radiusSearchEnabled.isChecked()) {
                    setSearchCenter(marker.getPosition());
                } else {
                    updateNearbyList(dormitory);
                }
            }
            return true;
        });

        // Set map click listener to set search center
        mMap.setOnMapClickListener(latLng -> {
            if (radiusSearchEnabled != null && radiusSearchEnabled.isChecked()) {
                setSearchCenter(latLng);
            }
        });

        // Handle marker drag for center marker
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(com.google.android.gms.maps.model.Marker marker) {
                if (marker.equals(centerMarker)) {
                    Log.d(TAG, "Started dragging center marker");
                }
            }

            @Override
            public void onMarkerDrag(com.google.android.gms.maps.model.Marker marker) {
                if (marker.equals(centerMarker)) {
                    searchCenter = marker.getPosition();
                    redrawRadiusCircle();
                }
            }

            @Override
            public void onMarkerDragEnd(com.google.android.gms.maps.model.Marker marker) {
                if (marker.equals(centerMarker)) {
                    searchCenter = marker.getPosition();
                    redrawRadiusCircle();
                    applyRadiusFilter();
                    Log.d(TAG, "Finished dragging center marker");
                }
            }
        });
    }

    private void setSearchCenter(LatLng location) {
        searchCenter = location;
        
        // Remove old center marker if exists
        if (centerMarker != null) {
            centerMarker.remove();
        }
        
        // Add draggable center marker
        centerMarker = mMap.addMarker(new MarkerOptions()
                .position(location)
                .title("Search Center")
                .snippet("Drag to move • Radius: " + String.format("%.1f", customSearchRadius) + " km")
                .draggable(true)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        
        // Draw radius circle
        redrawRadiusCircle();
        
        // Apply filter
        applyRadiusFilter();
        
        Log.d(TAG, "Search center set");
        Toast.makeText(this, "Drag marker to adjust search location", Toast.LENGTH_SHORT).show();
    }

    private void loadDormitories() {
        Log.d(TAG, "════════════════════════════════════════");
        Log.d(TAG, ">>> loadDormitories() CALLED <<<");
        Log.d(TAG, "════════════════════════════════════════");
        
        showLoadingAnimation();
        
        Log.d(TAG, "Firestore instance: " + (db != null ? "OK" : "NULL"));
        Log.d(TAG, "Attempting to query 'dormitories' collection...");
        
        db.collection("dormitories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "════════════════════════════════════════");
                    Log.d(TAG, "✓✓✓ FIRESTORE QUERY SUCCESSFUL! ✓✓✓");
                    Log.d(TAG, "Total documents found: " + queryDocumentSnapshots.size());
                    Log.d(TAG, "════════════════════════════════════════");
                    
                    allDormitories.clear();
                    filteredDormitories.clear();
                    
                    if (mMap != null) {
                        Log.d(TAG, "Clearing existing markers from map...");
                        mMap.clear();
                        // Re-add center marker if exists
                        if (centerMarker != null && searchCenter != null) {
                            centerMarker = mMap.addMarker(new MarkerOptions()
                                    .position(searchCenter)
                                    .title("Search Center")
                                    .snippet("Drag to move")
                                    .draggable(true)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                            redrawRadiusCircle();
                        }
                    } else {
                        Log.e(TAG, "❌ mMap is NULL - cannot add markers!");
                    }
                    
                    int markerCount = 0;
                    double minLat = Double.MAX_VALUE, maxLat = Double.MIN_VALUE;
                    double minLng = Double.MAX_VALUE, maxLng = Double.MIN_VALUE;
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Dormitory dormitory = document.toObject(Dormitory.class);
                            dormitory.setDormId(document.getId());
                            
                            // Validate coordinates
                            double lat = dormitory.getLatitude();
                            double lng = dormitory.getLongitude();
                            
                            if (lat == 0.0 && lng == 0.0) {
                                Log.w(TAG, "⚠ Skipping " + dormitory.getName() + " - Invalid coordinates (0,0)");
                                continue;
                            }
                            
                            allDormitories.add(dormitory);
                            filteredDormitories.add(dormitory);
                            markerCount++;
                            
                            // Track bounds
                            minLat = Math.min(minLat, lat);
                            maxLat = Math.max(maxLat, lat);
                            minLng = Math.min(minLng, lng);
                            maxLng = Math.max(maxLng, lng);
                            
                            Log.d(TAG, String.format("✓ [%d] Added: %s at (%.6f, %.6f)", 
                                    markerCount, dormitory.getName(), lat, lng));

                            // Add marker to map
                            addDormitoryMarker(dormitory);
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error parsing dormitory: " + document.getId(), e);
                        }
                    }
                    
                    Log.d(TAG, "════════════════════════════════════════");
                    Log.d(TAG, "SUMMARY:");
                    Log.d(TAG, "  Total documents in Firestore: " + queryDocumentSnapshots.size());
                    Log.d(TAG, "  Valid dormitories loaded: " + allDormitories.size());
                    Log.d(TAG, "  Markers added to map: " + markerCount);
                    Log.d(TAG, "  Coordinate Range:");
                    Log.d(TAG, "    Lat: " + minLat + " to " + maxLat);
                    Log.d(TAG, "    Lng: " + minLng + " to " + maxLng);
                    Log.d(TAG, "════════════════════════════════════════");
                    
                    updateDormitoryList(filteredDormitories);
                    hideLoadingAnimation();
                    
                    // Center map on all markers using bounds
                    if (!allDormitories.isEmpty()) {
                        fitAllMarkersInView();
                    } else {
                        Log.w(TAG, "⚠ No valid dormitories to display!");
                        Toast.makeText(MapActivity.this, "No dormitories with valid locations found",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "════════════════════════════════════════");
                    Log.e(TAG, "❌❌❌ FIRESTORE QUERY FAILED! ❌❌❌");
                    Log.e(TAG, "Error message: " + e.getMessage());
                    Log.e(TAG, "Error type: " + e.getClass().getSimpleName());
                    Log.e(TAG, "════════════════════════════════════════");
                    e.printStackTrace();
                    
                    Toast.makeText(MapActivity.this, "Failed to load dormitories: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    hideLoadingAnimation();
                });
    }

    private void addDormitoryMarker(Dormitory dormitory) {
        if (mMap == null) {
            Log.e(TAG, "❌ Cannot add marker - mMap is NULL");
            return;
        }
        
        LatLng position = new LatLng(dormitory.getLatitude(), dormitory.getLongitude());
        
        // Validate position
        if (dormitory.getLatitude() == 0.0 && dormitory.getLongitude() == 0.0) {
            Log.w(TAG, "⚠ Invalid coordinates for: " + dormitory.getName());
            return;
        }
        
        // Create custom marker with price tag
        String priceText = "₱" + String.format("%.0f", dormitory.getMonthlyPrice());
        
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title(dormitory.getName())
                .snippet(priceText + "/month • " + dormitory.getAvailableRooms() + " rooms available")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        com.google.android.gms.maps.model.Marker marker = mMap.addMarker(markerOptions);
        if (marker != null) {
            marker.setTag(dormitory);
            Log.d(TAG, "  ✓ Marker added successfully on map for: " + dormitory.getName());
        } else {
            Log.e(TAG, "  ❌ Failed to add marker for: " + dormitory.getName());
        }
    }

    private void filterDormitories(String query) {
        Log.d(TAG, "Filtering with query: " + query);
        if (query.isEmpty()) {
            filteredDormitories.clear();
            filteredDormitories.addAll(allDormitories);
        } else {
            String lowerQuery = query.toLowerCase();
            filteredDormitories.clear();
            
            for (Dormitory dorm : allDormitories) {
                if (dorm.getName().toLowerCase().contains(lowerQuery) ||
                    dorm.getAddress().toLowerCase().contains(lowerQuery) ||
                    dorm.getCity().toLowerCase().contains(lowerQuery)) {
                    filteredDormitories.add(dorm);
                }
            }
            
            Log.d(TAG, "Filter results: " + filteredDormitories.size() + " dormitories");
            
            // Clear old markers and add only filtered ones
            if (mMap != null) {
                mMap.clear();
                // Re-add center marker if it exists
                if (centerMarker != null && searchCenter != null) {
                    centerMarker = mMap.addMarker(new MarkerOptions()
                            .position(searchCenter)
                            .title("Search Center")
                            .snippet("Drag to move")
                            .draggable(true)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                    redrawRadiusCircle();
                }
                for (Dormitory dorm : filteredDormitories) {
                    addDormitoryMarker(dorm);
                }
            }
        }
        
        updateDormitoryList(filteredDormitories);
    }

    private void updateNearbyList(Dormitory selectedDorm) {
        // Show nearby dormitories based on default 5km distance
        List<Dormitory> nearby = filteredDormitories.stream()
                .filter(dorm -> calculateDistance(selectedDorm, dorm) <= 5.0)
                .collect(Collectors.toList());

        Log.d(TAG, "Nearby dormitories: " + nearby.size());
        updateDormitoryList(nearby);
    }

    private void updateDormitoryList(List<Dormitory> dormitories) {
        adapter.updateDormitories(dormitories);
        
        // Update count
        if (tvDormCount != null) {
            String countText = dormitories.size() + " dormitor" + (dormitories.size() == 1 ? "y" : "ies") + " found";
            tvDormCount.setText(countText);
        }
        
        // Show/hide empty state
        if (emptyStateLayout != null) {
            if (dormitories.isEmpty()) {
                emptyStateLayout.setVisibility(View.VISIBLE);
                nearbyList.setVisibility(View.GONE);
            } else {
                emptyStateLayout.setVisibility(View.GONE);
                nearbyList.setVisibility(View.VISIBLE);
            }
        }
    }

    private double calculateDistance(Dormitory dorm1, Dormitory dorm2) {
        // Haversine formula to calculate distance between two points
        double lat1 = Math.toRadians(dorm1.getLatitude());
        double lon1 = Math.toRadians(dorm1.getLongitude());
        double lat2 = Math.toRadians(dorm2.getLatitude());
        double lon2 = Math.toRadians(dorm2.getLongitude());

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dlon / 2) * Math.sin(dlon / 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        double R = 6371; // Earth's radius in km

        return R * c;
    }

    private double calculateDistanceFromLatLng(LatLng point1, Dormitory dorm2) {
        double lat1 = Math.toRadians(point1.latitude);
        double lon1 = Math.toRadians(point1.longitude);
        double lat2 = Math.toRadians(dorm2.getLatitude());
        double lon2 = Math.toRadians(dorm2.getLongitude());

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dlon / 2) * Math.sin(dlon / 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        double R = 6371; // Earth's radius in km

        return R * c;
    }

    /**
     * FIXED: Properly fits all markers using LatLngBounds
     * Ensures ALL markers are visible on the map
     */
    private void fitAllMarkersInView() {
        if (allDormitories.isEmpty() || mMap == null) {
            Log.w(TAG, "Cannot fit markers - no dormitories or map is null");
            return;
        }

        try {
            Log.d(TAG, "Fitting " + allDormitories.size() + " markers in view...");
            
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Dormitory dorm : allDormitories) {
                builder.include(new LatLng(dorm.getLatitude(), dorm.getLongitude()));
            }

            final LatLngBounds bounds = builder.build();
            
            // Use larger padding to ensure markers aren't at edges
            final int padding = 200; // pixels
            
            // Wait for map to be laid out before moving camera
            mainHandler.postDelayed(() -> {
                if (mMap != null) {
                    try {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding), 1500, null);
                        Log.d(TAG, "✓ Successfully fitted all " + allDormitories.size() + " markers in view");
                    } catch (Exception e) {
                        Log.e(TAG, "Error animating to bounds: " + e.getMessage());
                        // Fallback: move without animation
                        try {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                        } catch (Exception e2) {
                            Log.e(TAG, "Error moving to bounds: " + e2.getMessage());
                        }
                    }
                }
            }, 800); // Longer delay to ensure markers are fully rendered
            
        } catch (Exception e) {
            Log.e(TAG, "Error in fitAllMarkersInView: " + e.getMessage(), e);
        }
    }
    
    // Professional loading animations
    private void showLoadingAnimation() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setAlpha(0f);
            progressBar.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
        }
    }
    
    private void hideLoadingAnimation() {
        if (progressBar != null) {
            progressBar.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> progressBar.setVisibility(View.GONE))
                .start();
        }
    }

    private void getUserLocationForDistance() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            adapter.setUserLocation(location);
                            Log.d(TAG, "✓ User location obtained for distance calculations");
                        }
                    });
        }
    }

}
