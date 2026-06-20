package com.rct.dormfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.LandlordDormitoryAdapter;
import com.rct.dormfinder.models.Dormitory;
import java.util.ArrayList;
import java.util.List;
import com.rct.dormfinder.utils.NavigationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MyDormitoriesActivity extends BaseActivity {
    private ImageView ivBack;
    private RecyclerView recyclerViewDormitories;
    private FloatingActionButton fabAddDormitory;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private LandlordDormitoryAdapter adapter;
    private List<Dormitory> dormitories;
    private ListenerRegistration dormitoriesListener;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_dormitories);

        initializeViews();
        setupFirebase();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();
        setupRealtimeUpdates();
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        NavigationHelper.setupLandlordBottomNavigation(this, bottomNavigation, R.id.nav_my_dorms);
    }

    @Override
    protected void applyInsetsToViews(androidx.core.graphics.Insets insets) {
        // Apply top insets to header
        applyTopInsets(insets, R.id.headerLayout);
        // Apply bottom insets to bottom navigation (uses global 70% spacing)
        applyBottomInsets(insets, R.id.bottomNavigation);
        
        // Calculate header height and apply margin to content
        findViewById(R.id.headerLayout).post(() -> {
            int measuredHeaderHeight = findViewById(R.id.headerLayout).getHeight();
            int bottomNavHeight = findViewById(R.id.bottomNavigation).getHeight();
            
            // Apply top margin to SwipeRefreshLayout to account for header
            ViewGroup.MarginLayoutParams swipeParams = 
                (ViewGroup.MarginLayoutParams) swipeRefreshLayout.getLayoutParams();
            swipeParams.topMargin = measuredHeaderHeight;
            swipeRefreshLayout.setLayoutParams(swipeParams);
            
            // Apply padding to RecyclerView to avoid content being covered
            int topPadding = getResources().getDimensionPixelSize(R.dimen.spacing_large);
            int bottomPadding = bottomNavHeight + getResources().getDimensionPixelSize(R.dimen.spacing_extra_large) + 
                                getResources().getDimensionPixelSize(R.dimen.spacing_xxl); // Extra space for FAB
            
            recyclerViewDormitories.setPadding(
                recyclerViewDormitories.getPaddingLeft(),
                topPadding,
                recyclerViewDormitories.getPaddingRight(),
                bottomPadding
            );
        });
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        recyclerViewDormitories = findViewById(R.id.recyclerViewDormitories);
        fabAddDormitory = findViewById(R.id.fabAddDormitory);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
    }

    private void setupRecyclerView() {
        dormitories = new ArrayList<>();
        adapter = new LandlordDormitoryAdapter(dormitories, this);
        recyclerViewDormitories.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDormitories.setAdapter(adapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());
        
        fabAddDormitory.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddDormitoryActivity.class);
            startActivityForResult(intent, 100);
        });
        
        // Pull to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadDormitories();
        });
        
        swipeRefreshLayout.setColorSchemeResources(
            R.color.orange_primary
        );
    }

    private void setupRealtimeUpdates() {
        android.util.Log.d("MyDormitories", "Setting up real-time updates for user: " + currentUserId);
        
        // Set up Firestore real-time listener
        dormitoriesListener = db.collection("dormitories")
                .whereEqualTo("landlordId", currentUserId)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        android.util.Log.e("MyDormitories", "Listen failed: " + error.getMessage());
                        Toast.makeText(this, "Failed to listen for updates", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                        return;
                    }
                    
                    if (queryDocumentSnapshots != null) {
                        android.util.Log.d("MyDormitories", "Real-time update received. Documents: " + 
                            queryDocumentSnapshots.size());
                        
                        dormitories.clear();
                        for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                            try {
                                Dormitory dormitory = queryDocumentSnapshots.getDocuments().get(i)
                                        .toObject(Dormitory.class);
                                if (dormitory != null) {
                                    dormitory.setDormId(queryDocumentSnapshots.getDocuments().get(i).getId());
                                    dormitories.add(dormitory);
                                    android.util.Log.d("MyDormitories", "Added dormitory: " + dormitory.getName() + 
                                            " - isAvailable: " + dormitory.isAvailable() + 
                                            " - availableRooms: " + dormitory.getAvailableRooms());
                                }
                            } catch (Exception e) {
                                android.util.Log.e("MyDormitories", "Error parsing dormitory: " + e.getMessage());
                            }
                        }
                        
                        adapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                        android.util.Log.d("MyDormitories", "Adapter updated with " + dormitories.size() + " dormitories");
                        
                        // Show empty state if no dormitories
                        if (dormitories.isEmpty()) {
                            Toast.makeText(this, "No dormitories found. Add your first dormitory!", 
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void loadDormitories() {
        android.util.Log.d("MyDormitories", "Manual refresh triggered");
        swipeRefreshLayout.setRefreshing(true);
        
        // The real-time listener will automatically update the data
        // This method is called by pull-to-refresh, showing the refresh indicator
        // The listener callback will hide it when data arrives
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Data will be automatically updated via real-time listener
            Toast.makeText(this, "Dormitory saved successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the real-time listener when activity is destroyed
        if (dormitoriesListener != null) {
            dormitoriesListener.remove();
            android.util.Log.d("MyDormitories", "Real-time listener removed");
        }
    }
}
