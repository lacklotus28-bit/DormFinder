package com.rct.dormfinder.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.rct.dormfinder.R;
import com.rct.dormfinder.adapters.FullscreenImageAdapter;
import java.util.ArrayList;
import java.util.List;

public class FullscreenImageActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private TextView tvImageCounter;
    private ImageView ivClose;
    
    private List<String> imageUrls;
    private int currentPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        // Get images and position from intent
        imageUrls = getIntent().getStringArrayListExtra("image_urls");
        currentPosition = getIntent().getIntExtra("position", 0);

        if (imageUrls == null || imageUrls.isEmpty()) {
            finish();
            return;
        }

        initializeViews();
        setupViewPager();
        setupListeners();
    }

    private void initializeViews() {
        viewPager = findViewById(R.id.viewPager);
        tvImageCounter = findViewById(R.id.tvImageCounter);
        ivClose = findViewById(R.id.ivClose);
    }

    private void setupViewPager() {
        FullscreenImageAdapter adapter = new FullscreenImageAdapter(imageUrls, this);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);
        
        updateImageCounter(currentPosition);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateImageCounter(position);
            }
        });
    }

    private void setupListeners() {
        ivClose.setOnClickListener(v -> finish());
    }

    private void updateImageCounter(int position) {
        String counter = (position + 1) + " / " + imageUrls.size();
        tvImageCounter.setText(counter);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
