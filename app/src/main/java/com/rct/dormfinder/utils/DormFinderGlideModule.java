package com.rct.dormfinder.utils;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

/**
 * Glide configuration module for DormFinder app
 * Configures image loading and caching behavior
 */
@GlideModule
public class DormFinderGlideModule extends AppGlideModule {
    
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        // Set log level
        builder.setLogLevel(Log.DEBUG);
        
        // Set default request options
        RequestOptions defaultOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(30000); // 30 second timeout
        
        builder.setDefaultRequestOptions(defaultOptions);
    }
    
    @Override
    public boolean isManifestParsingEnabled() {
        // We're using annotations, so disable manifest parsing
        return false;
    }
}
