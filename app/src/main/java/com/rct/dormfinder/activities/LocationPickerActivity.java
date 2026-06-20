package com.rct.dormfinder.activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.rct.dormfinder.R;
import com.rct.dormfinder.utils.ConfirmationDialogHelper;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends BaseActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private ImageView ivBack;
    private EditText etSearchLocation;
    private Button btnSearch, btnConfirmLocation;
    
    private LatLng selectedLocation;
    private com.google.android.gms.maps.model.Marker selectedMarker;
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        initializeViews();
        setupMap();
        setupListeners();
        loadCurrentLocation();
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        // Apply top insets to the header layout
        android.view.View headerLayout = findViewById(R.id.headerLayout);
        if (headerLayout != null) {
            android.view.ViewGroup.MarginLayoutParams params = 
                (android.view.ViewGroup.MarginLayoutParams) headerLayout.getLayoutParams();
            params.topMargin = insets.top;
            headerLayout.setLayoutParams(params);
        }
        
        // Apply bottom insets to the confirm button
        if (btnConfirmLocation != null) {
            android.view.ViewGroup.MarginLayoutParams params = 
                (android.view.ViewGroup.MarginLayoutParams) btnConfirmLocation.getLayoutParams();
            params.bottomMargin = insets.bottom + 16; // 16dp base margin + system insets
            btnConfirmLocation.setLayoutParams(params);
        }
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        etSearchLocation = findViewById(R.id.etSearchLocation);
        btnSearch = findViewById(R.id.btnSearch);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        
        geocoder = new Geocoder(this, Locale.getDefault());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> handleBackPress());
        
        btnSearch.setOnClickListener(v -> searchLocation());
        
        btnConfirmLocation.setOnClickListener(v -> confirmLocation());
    }

    private void loadCurrentLocation() {
        Intent intent = getIntent();
        double lat = intent.getDoubleExtra("current_latitude", 13.7565);
        double lng = intent.getDoubleExtra("current_longitude", 121.0583);
        selectedLocation = new LatLng(lat, lng);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        // Set initial location
        if (selectedLocation != null) {
            updateMapLocation(selectedLocation);
            // Load the address for the initial location
            updateAddressFromLocation(selectedLocation);
        }
        
        // Enable map controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        
        // Set map click listener
        mMap.setOnMapClickListener(latLng -> {
            selectedLocation = latLng;
            updateMapLocation(latLng);
            updateAddressFromLocation(latLng);
        });
    }

    private void updateMapLocation(LatLng location) {
        if (mMap != null) {
            // Clear previous marker
            if (selectedMarker != null) {
                selectedMarker.remove();
            }
            
            // Add new marker
            selectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Selected Location"));
            
            // Move camera
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16));
            
            // Enable confirm button
            btnConfirmLocation.setEnabled(true);
            btnConfirmLocation.setText("Confirm This Location");
        }
    }

    private void searchLocation() {
        String searchText = etSearchLocation.getText().toString().trim();
        if (searchText.isEmpty()) {
            etSearchLocation.setError("Please enter a location to search");
            return;
        }

        try {
            List<Address> addresses = geocoder.getFromLocationName(searchText + ", Philippines", 5);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng location = new LatLng(address.getLatitude(), address.getLongitude());
                selectedLocation = location;
                updateMapLocation(location);
                
                // Update search field with formatted address
                etSearchLocation.setText(address.getAddressLine(0));
            } else {
                Toast.makeText(this, "Location not found. Please try a different search term.", 
                        Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Search failed. Please check your internet connection.", 
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAddressFromLocation(LatLng location) {
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(
                        location.latitude, location.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String formattedAddress = address.getAddressLine(0);
                    runOnUiThread(() -> {
                        etSearchLocation.setText(formattedAddress);
                        Toast.makeText(LocationPickerActivity.this, 
                            "Address updated", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        etSearchLocation.setText(String.format(Locale.US, "%.6f, %.6f", 
                            location.latitude, location.longitude));
                    });
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    etSearchLocation.setText(String.format(Locale.US, "%.6f, %.6f", 
                        location.latitude, location.longitude));
                    android.util.Log.e("LocationPicker", "Geocoding failed", e);
                });
            }
        }).start();
    }

    private void confirmLocation() {
        if (selectedLocation != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitude", selectedLocation.latitude);
            resultIntent.putExtra("longitude", selectedLocation.longitude);
            resultIntent.putExtra("address", etSearchLocation.getText().toString());
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle back press with confirmation if location changed
     */
    private void handleBackPress() {
        // Check if location has been changed from original
        Intent intent = getIntent();
        double originalLat = intent.getDoubleExtra("current_latitude", 13.7565);
        double originalLng = intent.getDoubleExtra("current_longitude", 121.0583);

        boolean locationChanged = selectedLocation != null &&
                                 (Math.abs(selectedLocation.latitude - originalLat) > 0.0001 ||
                                  Math.abs(selectedLocation.longitude - originalLng) > 0.0001);

        if (locationChanged) {
            ConfirmationDialogHelper.showCustomConfirmation(this,
                    "Discard Location Changes?",
                    "You have selected a new location. Are you sure you want to go back without confirming it?",
                    new ConfirmationDialogHelper.OnConfirmListener() {
                        @Override
                        public void onConfirm() {
                            finish();
                        }

                        @Override
                        public void onCancel() {
                            // Stay on location picker
                        }
                    });
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }
}
