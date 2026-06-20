package com.rct.dormfinder.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.ImageUploadAdapter;
import com.rct.dormfinder.models.Dormitory;
import com.rct.dormfinder.utils.CloudinaryManager;
import com.rct.dormfinder.utils.ConfirmationDialogHelper;
import com.rct.dormfinder.utils.ImageCompressor;
import com.rct.dormfinder.utils.SimpleItemTouchHelperCallback;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddDormitoryActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int PICK_IMAGES_REQUEST = 1;
    private static final int LOCATION_PICKER_REQUEST = 2;
    private static final int LOCATION_PERMISSION_REQUEST = 101;

    private ImageView ivBack;
    private EditText etDormName, etAddress, etDescription, etMonthlyPrice, etTotalRooms;
    private Spinner spinnerCity;
    private CheckBox cbWifi, cbAircon, cbParking, cbLaundry, cbSecurity, cbKitchen, cbLivingRoom, cbBalcony;
    private RecyclerView recyclerViewImages;
    private Button btnAddImages, btnSelectLocation, btnUseCurrentLocation, btnSaveDormitory;
    private TextView tvSelectedLocation, tvUploadProgress;
    private LinearLayout layoutMapContainer;
    private android.widget.ProgressBar progressBarUpload;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    private GoogleMap mMap;
    private double selectedLatitude = 13.7565;
    private double selectedLongitude = 121.0583;
    private boolean locationSelected = false;

    private List<Object> allImages;
    private ImageUploadAdapter imageAdapter;
    private String currentUserId;
    private boolean isEditMode = false;
    private String editDormitoryId;
    private List<String> existingImageUrls;
    private Geocoder geocoder;
    private Runnable addressGeocodeRunnable;
    private android.os.Handler addressHandler = new android.os.Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_dormitory);
        
        initializeViews();
        setupFirebase();
        setupSpinners();
        setupRecyclerView();
        setupMap();
        setupListeners();
        checkEditMode();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        etDormName = findViewById(R.id.etDormName);
        etAddress = findViewById(R.id.etAddress);
        etDescription = findViewById(R.id.etDescription);
        etMonthlyPrice = findViewById(R.id.etMonthlyPrice);
        etTotalRooms = findViewById(R.id.etTotalRooms);
        spinnerCity = findViewById(R.id.spinnerCity);
        
        cbWifi = findViewById(R.id.cbWifi);
        cbAircon = findViewById(R.id.cbAircon);
        cbParking = findViewById(R.id.cbParking);
        cbLaundry = findViewById(R.id.cbLaundry);
        cbSecurity = findViewById(R.id.cbSecurity);
        cbKitchen = findViewById(R.id.cbKitchen);
        cbLivingRoom = findViewById(R.id.cbLivingRoom);
        cbBalcony = findViewById(R.id.cbBalcony);
        
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        btnAddImages = findViewById(R.id.btnAddImages);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        btnUseCurrentLocation = findViewById(R.id.btnUseCurrentLocation);
        btnSaveDormitory = findViewById(R.id.btnSaveDormitory);
        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);
        tvUploadProgress = findViewById(R.id.tvUploadProgress);
        progressBarUpload = findViewById(R.id.progressBarUpload);
        layoutMapContainer = findViewById(R.id.layoutMapContainer);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        geocoder = new Geocoder(this, Locale.getDefault());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        android.util.Log.d("Firebase", "Current user ID: " + currentUserId);
    }

    private void setupSpinners() {
        String[] cities = {"Batangas", "Lipa"};
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_black_text, cities);
        cityAdapter.setDropDownViewResource(R.layout.spinner_item_black_text);
        spinnerCity.setAdapter(cityAdapter);
    }

    private void setupRecyclerView() {
        allImages = new ArrayList<>();
        existingImageUrls = new ArrayList<>();
        imageAdapter = new ImageUploadAdapter(allImages, this);
        recyclerViewImages.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerViewImages.setAdapter(imageAdapter);
        
        imageAdapter.setOnImageRemoveListener(position -> {
            updateImageCount();
        });
        
        imageAdapter.setOnImageReorderListener(() -> {
            updateImageCount();
            Toast.makeText(this, "First image will be the cover photo", Toast.LENGTH_SHORT).show();
        });
        
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(imageAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerViewImages);
    }
    
    private void updateImageCount() {
        int totalImages = imageAdapter.getItemCount();
        if (totalImages > 0) {
            btnAddImages.setText("Images Selected (" + totalImages + ")");
        } else {
            btnAddImages.setText("Select Images");
        }
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
        btnAddImages.setOnClickListener(v -> selectImages());
        btnSelectLocation.setOnClickListener(v -> openLocationPicker());
        btnUseCurrentLocation.setOnClickListener(v -> useCurrentLocation());
        btnSaveDormitory.setOnClickListener(v -> saveDormitory());
        
        // Add TextWatcher for address field to update map location
        etAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous geocoding request
                if (addressGeocodeRunnable != null) {
                    addressHandler.removeCallbacks(addressGeocodeRunnable);
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                String address = s.toString().trim();
                if (address.length() > 10) { // Only geocode if address is substantial
                    // Delay geocoding by 1.5 seconds to avoid too many API calls
                    addressGeocodeRunnable = () -> geocodeAddressAndUpdateMap(address);
                    addressHandler.postDelayed(addressGeocodeRunnable, 1500);
                }
            }
        });
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        editDormitoryId = intent.getStringExtra("dormitory_id");
        if (editDormitoryId != null) {
            isEditMode = true;
            setTitle("Edit Dormitory");
            btnSaveDormitory.setText("Update Dormitory");
            loadDormitoryData();
        }
    }

    private void selectImages() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 100);
                return;
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                return;
            }
        }
        
        openImagePicker();
    }
    
    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_REQUEST);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied. Cannot access images.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndUpdate();
            } else {
                Toast.makeText(this, "Location permission denied. Please enable it in settings.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openLocationPicker() {
        Intent intent = new Intent(this, LocationPickerActivity.class);
        intent.putExtra("current_latitude", selectedLatitude);
        intent.putExtra("current_longitude", selectedLongitude);
        startActivityForResult(intent, LOCATION_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            List<Uri> newImages = new ArrayList<>();
            
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                int currentCount = imageAdapter.getItemCount();
                int availableSlots = 10 - currentCount;
                
                for (int i = 0; i < count && i < availableSlots; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    newImages.add(imageUri);
                }
                
                if (count > availableSlots) {
                    Toast.makeText(this, "Maximum 10 images allowed. Some images were not added.", 
                        Toast.LENGTH_SHORT).show();
                }
            } else if (data.getData() != null) {
                if (imageAdapter.getItemCount() < 10) {
                    newImages.add(data.getData());
                } else {
                    Toast.makeText(this, "Maximum 10 images reached", Toast.LENGTH_SHORT).show();
                }
            }
            
            if (!newImages.isEmpty()) {
                compressAndAddImages(newImages);
            }
        }
        
        if (requestCode == LOCATION_PICKER_REQUEST && resultCode == RESULT_OK) {
            selectedLatitude = data.getDoubleExtra("latitude", selectedLatitude);
            selectedLongitude = data.getDoubleExtra("longitude", selectedLongitude);
            String selectedAddress = data.getStringExtra("address");
            locationSelected = true;
            
            // Update the address field with the selected location's address
            if (selectedAddress != null && !selectedAddress.isEmpty()) {
                etAddress.setText(selectedAddress);
                tvSelectedLocation.setText("Location Selected: " + selectedAddress);
            } else {
                tvSelectedLocation.setText("Location Selected");
            }
            tvSelectedLocation.setTextColor(getColor(R.color.orange_primary));
            
            if (mMap != null) {
                updateMapLocation();
            }
        }
    }
    
    private void compressAndAddImages(List<Uri> imagesToCompress) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Compressing images...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        new Thread(() -> {
            List<Uri> compressedUris = new ArrayList<>();
            for (Uri uri : imagesToCompress) {
                Uri compressedUri = ImageCompressor.compressImage(this, uri);
                compressedUris.add(compressedUri);
            }
            
            runOnUiThread(() -> {
                progressDialog.dismiss();
                imageAdapter.addNewImages(compressedUris);
                updateImageCount();
                Toast.makeText(this, "Images compressed and added", Toast.LENGTH_SHORT).show();
            });
        }).start();
        
        ImageCompressor.cleanupOldFiles(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        updateMapLocation();
    }

    private void updateMapLocation() {
        if (mMap != null) {
            LatLng location = new LatLng(selectedLatitude, selectedLongitude);
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(location).title("Dormitory Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        }
    }

    private void saveDormitory() {
        if (!validateInput()) {
            return;
        }

        btnSaveDormitory.setEnabled(false);
        btnSaveDormitory.setText(isEditMode ? "Updating..." : "Saving...");

        List<Uri> newImagesToUpload = imageAdapter.getNewImages();
        
        if (!newImagesToUpload.isEmpty()) {
            uploadImagesToCloudinary(newImagesToUpload);
        } else if (isEditMode && !imageAdapter.getExistingImageUrls().isEmpty()) {
            saveDormitoryData(new ArrayList<>());
        } else {
            Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show();
            resetSaveButton();
        }
    }

    private void uploadImagesToCloudinary(List<Uri> imagesToUpload) {
        CloudinaryManager cloudinaryManager = CloudinaryManager.getInstance(this);
        
        cloudinaryManager.testConnection();
        
        if (!cloudinaryManager.isInitialized()) {
            Toast.makeText(this, "Cloudinary not properly configured. Please check your credentials.", 
                Toast.LENGTH_LONG).show();
            resetSaveButton();
            return;
        }
        
        if (progressBarUpload != null) {
            progressBarUpload.setVisibility(View.VISIBLE);
            progressBarUpload.setMax(imagesToUpload.size());
            progressBarUpload.setProgress(0);
        }
        if (tvUploadProgress != null) {
            tvUploadProgress.setVisibility(View.VISIBLE);
            tvUploadProgress.setText("Preparing to upload " + imagesToUpload.size() + " images...");
        }
        
        android.util.Log.d("AddDormitory", "Starting upload of " + imagesToUpload.size() + " images");
        
        cloudinaryManager.setProgressListener(new CloudinaryManager.OnUploadProgressListener() {
            @Override
            public void onProgress(int current, int total, String message) {
                runOnUiThread(() -> {
                    if (progressBarUpload != null) {
                        progressBarUpload.setProgress(current);
                    }
                    if (tvUploadProgress != null) {
                        tvUploadProgress.setText(message);
                    }
                    android.util.Log.d("AddDormitory", "Upload progress: " + current + "/" + total);
                });
            }
        });
        
        cloudinaryManager.uploadImages(imagesToUpload, 
            editDormitoryId != null ? editDormitoryId : "new_" + System.currentTimeMillis(),
            new CloudinaryManager.OnUploadCompleteListener() {
                @Override
                public void onUploadComplete(List<String> imageUrls) {
                    runOnUiThread(() -> {
                        android.util.Log.d("AddDormitory", "✅ Upload complete! URLs received: " + imageUrls.size());
                        
                        for (int i = 0; i < imageUrls.size(); i++) {
                            android.util.Log.d("AddDormitory", "   Image " + (i+1) + ": " + imageUrls.get(i));
                        }
                        
                        if (progressBarUpload != null) progressBarUpload.setVisibility(View.GONE);
                        if (tvUploadProgress != null) tvUploadProgress.setVisibility(View.GONE);
                        
                        if (imageUrls.isEmpty()) {
                            Toast.makeText(AddDormitoryActivity.this,
                                "No images were uploaded. Please try again.",
                                Toast.LENGTH_LONG).show();
                            resetSaveButton();
                        } else {
                            Toast.makeText(AddDormitoryActivity.this,
                                "Images uploaded successfully!",
                                Toast.LENGTH_SHORT).show();
                            saveDormitoryData(imageUrls);
                        }
                    });
                }

                @Override
                public void onUploadError(String error) {
                    runOnUiThread(() -> {
                        android.util.Log.e("AddDormitory", "❌ Upload error: " + error);
                        
                        if (progressBarUpload != null) progressBarUpload.setVisibility(View.GONE);
                        if (tvUploadProgress != null) tvUploadProgress.setVisibility(View.GONE);
                        
                        Toast.makeText(AddDormitoryActivity.this,
                            "Failed to upload images: " + error,
                            Toast.LENGTH_LONG).show();
                        resetSaveButton();
                    });
                }
            });
    }

    private boolean validateInput() {
        String name = etDormName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etMonthlyPrice.getText().toString().trim();
        String roomsStr = etTotalRooms.getText().toString().trim();

        if (name.isEmpty()) {
            etDormName.setError("Dormitory name is required");
            etDormName.requestFocus();
            return false;
        }

        if (address.isEmpty()) {
            etAddress.setError("Address is required");
            etAddress.requestFocus();
            return false;
        }

        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return false;
        }

        if (priceStr.isEmpty()) {
            etMonthlyPrice.setError("Monthly price is required");
            etMonthlyPrice.requestFocus();
            return false;
        }

        if (roomsStr.isEmpty()) {
            etTotalRooms.setError("Total rooms is required");
            etTotalRooms.requestFocus();
            return false;
        }

        try {
            double price = Double.parseDouble(priceStr);
            int rooms = Integer.parseInt(roomsStr);
            
            if (price <= 0) {
                etMonthlyPrice.setError("Price must be greater than 0");
                etMonthlyPrice.requestFocus();
                return false;
            }
            
            if (rooms <= 0) {
                etTotalRooms.setError("Rooms must be greater than 0");
                etTotalRooms.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for price and rooms", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isEditMode && !locationSelected) {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveDormitoryData(List<String> imagePaths) {
        String name = etDormName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String city = spinnerCity.getSelectedItem().toString();
        String description = etDescription.getText().toString().trim();
        double price = Double.parseDouble(etMonthlyPrice.getText().toString().trim());
        int totalRooms = Integer.parseInt(etTotalRooms.getText().toString().trim());

        List<String> amenities = getSelectedAmenities();

        Dormitory dormitory;
        if (isEditMode) {
            updateExistingDormitory(name, address, city, description, price, totalRooms, amenities, imagePaths);
        } else {
            dormitory = new Dormitory(currentUserId, name, address, city, 
                                    selectedLatitude, selectedLongitude, description, price, totalRooms);
            dormitory.setAmenities(amenities);
            dormitory.setImages(imagePaths);

            db.collection("dormitories")
                    .add(dormitory)
                    .addOnSuccessListener(documentReference -> {
                        String dormId = documentReference.getId();
                        android.util.Log.d("AddDormitory", "Dormitory saved with ID: " + dormId);
                        
                        documentReference.update("dormId", dormId)
                                .addOnSuccessListener(aVoid -> {
                                    android.util.Log.d("AddDormitory", "Dormitory ID updated successfully");
                                    Toast.makeText(this, "Dormitory added successfully!", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to add dormitory: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        resetSaveButton();
                    });
        }
    }

    private void updateExistingDormitory(String name, String address, String city, String description,
                                       double price, int totalRooms, List<String> amenities, List<String> newImagePaths) {
        List<String> remainingExistingImages = imageAdapter.getExistingImageUrls();
        
        List<String> finalImageList = new ArrayList<>();
        finalImageList.addAll(remainingExistingImages);
        if (newImagePaths != null && !newImagePaths.isEmpty()) {
            finalImageList.addAll(newImagePaths);
        }
        
        db.collection("dormitories").document(editDormitoryId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Dormitory existingDorm = document.toObject(Dormitory.class);
                        if (existingDorm != null) {
                            existingDorm.setName(name);
                            existingDorm.setAddress(address);
                            existingDorm.setCity(city);
                            existingDorm.setDescription(description);
                            existingDorm.setMonthlyPrice(price);
                            existingDorm.setTotalRooms(totalRooms);
                            existingDorm.setAmenities(amenities);
                            existingDorm.setUpdatedAt(Timestamp.now());
                            existingDorm.setImages(finalImageList);
                            
                            if (locationSelected) {
                                existingDorm.setLatitude(selectedLatitude);
                                existingDorm.setLongitude(selectedLongitude);
                            }

                            db.collection("dormitories").document(editDormitoryId)
                                    .set(existingDorm)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Dormitory updated successfully!", Toast.LENGTH_SHORT).show();
                                        setResult(RESULT_OK);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to update dormitory: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                        resetSaveButton();
                                    });
                        }
                    }
                });
    }

    private List<String> getSelectedAmenities() {
        List<String> amenities = new ArrayList<>();
        
        if (cbWifi.isChecked()) amenities.add("WiFi");
        if (cbAircon.isChecked()) amenities.add("Air Conditioning");
        if (cbParking.isChecked()) amenities.add("Parking");
        if (cbLaundry.isChecked()) amenities.add("Laundry");
        if (cbSecurity.isChecked()) amenities.add("24/7 Security");
        if (cbKitchen.isChecked()) amenities.add("Kitchen");
        if (cbLivingRoom.isChecked()) amenities.add("Living Room");
        if (cbBalcony.isChecked()) amenities.add("Balcony");
        
        return amenities;
    }

    private void loadDormitoryData() {
        db.collection("dormitories").document(editDormitoryId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Dormitory dormitory = document.toObject(Dormitory.class);
                        if (dormitory != null) {
                            populateFormWithDormitoryData(dormitory);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load dormitory data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateFormWithDormitoryData(Dormitory dormitory) {
        etDormName.setText(dormitory.getName());
        etAddress.setText(dormitory.getAddress());
        etDescription.setText(dormitory.getDescription());
        etMonthlyPrice.setText(String.valueOf(dormitory.getMonthlyPrice()));
        etTotalRooms.setText(String.valueOf(dormitory.getTotalRooms()));
        
        String[] cities = {"Batangas", "Lipa"};
        for (int i = 0; i < cities.length; i++) {
            if (cities[i].equals(dormitory.getCity())) {
                spinnerCity.setSelection(i);
                break;
            }
        }
        
        selectedLatitude = dormitory.getLatitude();
        selectedLongitude = dormitory.getLongitude();
        locationSelected = true;
        tvSelectedLocation.setText("Current Location");
        tvSelectedLocation.setTextColor(getColor(R.color.orange_primary));
        
        List<String> amenities = dormitory.getAmenities();
        if (amenities != null) {
            cbWifi.setChecked(amenities.contains("WiFi"));
            cbAircon.setChecked(amenities.contains("Air Conditioning"));
            cbParking.setChecked(amenities.contains("Parking"));
            cbLaundry.setChecked(amenities.contains("Laundry"));
            cbSecurity.setChecked(amenities.contains("24/7 Security"));
            cbKitchen.setChecked(amenities.contains("Kitchen"));
            cbLivingRoom.setChecked(amenities.contains("Living Room"));
            cbBalcony.setChecked(amenities.contains("Balcony"));
        }
        
        List<String> existingImages = dormitory.getImages();
        if (existingImages != null && !existingImages.isEmpty()) {
            existingImageUrls = new ArrayList<>(existingImages);
            imageAdapter.setExistingImages(existingImages);
            updateImageCount();
        }
        
        if (mMap != null) {
            updateMapLocation();
        }
    }

    private void resetSaveButton() {
        btnSaveDormitory.setEnabled(true);
        btnSaveDormitory.setText(isEditMode ? "Update Dormitory" : "Save Dormitory");
    }

    /**
     * Geocodes the address and updates the map location
     * Runs on background thread to prevent UI blocking
     */
    private void geocodeAddressAndUpdateMap(String address) {
        new Thread(() -> {
            try {
                // Add Philippines to address for better geocoding results
                String searchAddress = address;
                if (!address.toLowerCase().contains("philippines")) {
                    searchAddress = address + ", Philippines";
                }
                
                List<Address> addresses = geocoder.getFromLocationName(searchAddress, 1);
                
                if (addresses != null && !addresses.isEmpty()) {
                    Address location = addresses.get(0);
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    
                    runOnUiThread(() -> {
                        selectedLatitude = latitude;
                        selectedLongitude = longitude;
                        locationSelected = true;
                        
                        // Update map
                        if (mMap != null) {
                            updateMapLocation();
                        }
                        
                        // Update status text
                        tvSelectedLocation.setText("Location auto-updated from address");
                        tvSelectedLocation.setTextColor(getColor(R.color.orange_primary));
                        
                        android.util.Log.d("AddDormitory", "Address geocoded: " + latitude + ", " + longitude);
                    });
                } else {
                    runOnUiThread(() -> {
                        android.util.Log.d("AddDormitory", "No location found for address: " + address);
                    });
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    android.util.Log.e("AddDormitory", "Geocoding failed: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Request to use the device's current location
     * Checks and requests permissions if needed
     */
    private void useCurrentLocation() {
        // Check for location permissions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                
                // Request permissions
                requestPermissions(new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                }, LOCATION_PERMISSION_REQUEST);
                return;
            }
        }
        
        // Permission granted, get current location
        getCurrentLocationAndUpdate();
    }

    /**
     * Gets the current location and updates the form
     * Must be called only after permissions are granted
     */
    private void getCurrentLocationAndUpdate() {
        try {
            // Show loading state
            btnUseCurrentLocation.setEnabled(false);
            btnUseCurrentLocation.setText("Getting location...");
            
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Got location
                        selectedLatitude = location.getLatitude();
                        selectedLongitude = location.getLongitude();
                        locationSelected = true;
                        
                        // Update map
                        if (mMap != null) {
                            updateMapLocation();
                        }
                        
                        // Get address from coordinates
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                        
                        // Update status
                        tvSelectedLocation.setText("Current location set");
                        tvSelectedLocation.setTextColor(getColor(R.color.orange_primary));
                        
                        Toast.makeText(this, "Current location selected!", Toast.LENGTH_SHORT).show();
                        
                        android.util.Log.d("AddDormitory", "Current location: " + selectedLatitude + ", " + selectedLongitude);
                    } else {
                        Toast.makeText(this, "Unable to get current location. Please try again or select on map.", 
                            Toast.LENGTH_LONG).show();
                    }
                    
                    // Reset button
                    btnUseCurrentLocation.setEnabled(true);
                    btnUseCurrentLocation.setText("📍 Use My Location");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                    android.util.Log.e("AddDormitory", "Location error: " + e.getMessage());
                    
                    // Reset button
                    btnUseCurrentLocation.setEnabled(true);
                    btnUseCurrentLocation.setText("📍 Use My Location");
                });
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission error", Toast.LENGTH_SHORT).show();
            btnUseCurrentLocation.setEnabled(true);
            btnUseCurrentLocation.setText("📍 Use My Location");
        }
    }

    /**
     * Reverse geocodes coordinates to get address
     * Runs on background thread
     */
    private void getAddressFromLocation(double latitude, double longitude) {
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String fullAddress = address.getAddressLine(0);
                    
                    runOnUiThread(() -> {
                        etAddress.setText(fullAddress);
                        android.util.Log.d("AddDormitory", "Address from location: " + fullAddress);
                    });
                } else {
                    runOnUiThread(() -> {
                        android.util.Log.d("AddDormitory", "No address found for coordinates");
                    });
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    android.util.Log.e("AddDormitory", "Reverse geocoding failed: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Handle back button press with confirmation if data entered
     */
    private void handleBackPress() {
        // Check if any data has been entered
        boolean hasData = !etDormName.getText().toString().trim().isEmpty() ||
                         !etAddress.getText().toString().trim().isEmpty() ||
                         !etDescription.getText().toString().trim().isEmpty() ||
                         !etMonthlyPrice.getText().toString().trim().isEmpty() ||
                         !etTotalRooms.getText().toString().trim().isEmpty() ||
                         imageAdapter.getItemCount() > 0;

        if (hasData) {
            ConfirmationDialogHelper.showLeaveFormDialog(this, 
                isEditMode ? "Edit Dormitory" : "Add Dormitory",
                new ConfirmationDialogHelper.OnConfirmListener() {
                    @Override
                    public void onConfirm() {
                        finish();
                    }

                    @Override
                    public void onCancel() {
                        // User wants to stay, do nothing
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
