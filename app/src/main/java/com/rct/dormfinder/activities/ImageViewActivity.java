package com.rct.dormfinder.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.chrisbanes.photoview.PhotoView;
import com.rct.dormfinder.R;

public class ImageViewActivity extends AppCompatActivity {
    private PhotoView photoView;
    private ImageButton btnBack;
    private TextView tvTitle;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        initializeViews();
        setupListeners();
        loadImage();
    }

    private void initializeViews() {
        photoView = findViewById(R.id.photoView);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadImage() {
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String title = getIntent().getStringExtra("title");

        if (title != null) {
            tvTitle.setText(title);
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(photoView);

            progressBar.setVisibility(View.GONE);
        } else {
            Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
