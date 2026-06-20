package com.rct.dormfinder.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.rct.dormfinder.R;

/**
 * Base Activity that handles edge-to-edge display for all activities
 * All activities should extend this class to get proper window insets handling
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setupEdgeToEdge();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        setupEdgeToEdge();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        setupEdgeToEdge();
    }

    /**
     * Setup edge-to-edge display handling
     * This method is called automatically after setContentView
     */
    private void setupEdgeToEdge() {
        View rootView = findViewById(android.R.id.content);
        
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            Log.d(TAG, getClass().getSimpleName() + " - Window Insets - Top: " + insets.top + 
                       ", Bottom: " + insets.bottom + ", Left: " + insets.left + ", Right: " + insets.right);
            
            // Apply insets to common UI elements
            applyInsetsToViews(insets);
            
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * Apply window insets to common UI elements
     * Override this method in child activities for custom inset handling
     */
    protected void applyInsetsToViews(Insets insets) {
        // Child activities should override this method to apply insets to their specific views
        // Example:
        // applyTopInsets(insets, R.id.headerLayout);
        // applyBottomInsets(insets, R.id.bottomNavigation);
        // applySideInsets(insets, R.id.mainContent);
    }

    /**
     * Apply top insets to a view (for headers/toolbars)
     * This method preserves the existing padding and adds the system inset on top
     */
    protected void applyTopInsets(Insets insets, int viewId) {
        View view = findViewById(viewId);
        if (view != null) {
            // Get the current padding
            int currentPaddingLeft = view.getPaddingLeft();
            int currentPaddingTop = view.getPaddingTop();
            int currentPaddingRight = view.getPaddingRight();
            int currentPaddingBottom = view.getPaddingBottom();
            
            // Reduce the top inset by 50% (change this value to adjust spacing)
            // 0.0 = no spacing, 0.5 = half spacing, 1.0 = full spacing
            float topInsetMultiplier = 0.5f;
            int reducedTopInset = (int) (insets.top * topInsetMultiplier);
            
            // Apply new padding with reduced system inset added to top
            view.setPadding(
                currentPaddingLeft,
                currentPaddingTop + reducedTopInset,
                currentPaddingRight,
                currentPaddingBottom
            );
            Log.d(TAG, "Applied top insets (" + reducedTopInset + "dp from " + insets.top + "dp) to view ID: " + viewId);
        } else {
            Log.w(TAG, "View with ID " + viewId + " not found for top insets");
        }
    }

    /**
     * Apply top insets to a view with custom multiplier (for headers/toolbars)
     * This method allows per-activity control of top inset spacing
     * @param insets The system window insets
     * @param viewId The view to apply insets to
     * @param multiplier How much of the inset to apply (0.0 = none, 1.0 = full)
     */
    protected void applyTopInsets(Insets insets, int viewId, float multiplier) {
        View view = findViewById(viewId);
        if (view != null) {
            // Get the current padding
            int currentPaddingLeft = view.getPaddingLeft();
            int currentPaddingTop = view.getPaddingTop();
            int currentPaddingRight = view.getPaddingRight();
            int currentPaddingBottom = view.getPaddingBottom();
            
            // Apply custom multiplier to top inset
            int reducedTopInset = (int) (insets.top * multiplier);
            
            // Apply new padding with reduced system inset added to top
            view.setPadding(
                currentPaddingLeft,
                currentPaddingTop + reducedTopInset,
                currentPaddingRight,
                currentPaddingBottom
            );
            Log.d(TAG, "Applied top insets (" + reducedTopInset + "dp from " + insets.top + "dp with multiplier " + multiplier + ") to view ID: " + viewId);
        } else {
            Log.w(TAG, "View with ID " + viewId + " not found for top insets");
        }
    }

    /**
     * Apply bottom insets to a view (for bottom navigation)
     * This method preserves the existing padding and adds the system inset at bottom
     */
    protected void applyBottomInsets(Insets insets, int viewId) {
        View view = findViewById(viewId);
        if (view != null) {
            // Get the current padding
            int currentPaddingLeft = view.getPaddingLeft();
            int currentPaddingTop = view.getPaddingTop();
            int currentPaddingRight = view.getPaddingRight();
            int currentPaddingBottom = view.getPaddingBottom();
            
            // Reduce the bottom inset by 70% (good for navigation bars)
            // Change this value to adjust global spacing for all bottom elements
            // 0.0 = no spacing, 0.5 = half spacing, 0.7 = comfortable, 1.0 = full spacing
            float bottomInsetMultiplier = 0.7f;
            int reducedBottomInset = (int) (insets.bottom * bottomInsetMultiplier);
            
            // Apply new padding with reduced system inset added to bottom
            view.setPadding(
                currentPaddingLeft,
                currentPaddingTop,
                currentPaddingRight,
                currentPaddingBottom + reducedBottomInset
            );
            Log.d(TAG, "Applied bottom insets (" + reducedBottomInset + "dp from " + insets.bottom + "dp) to view ID: " + viewId);
        } else {
            Log.w(TAG, "View with ID " + viewId + " not found for bottom insets");
        }
    }

    /**
     * Apply bottom insets to a view with custom multiplier (for bottom navigation)
     * This method allows per-activity control of bottom inset spacing
     * @param insets The system window insets
     * @param viewId The view to apply insets to
     * @param multiplier How much of the inset to apply (0.0 = none, 1.0 = full)
     */
    protected void applyBottomInsets(Insets insets, int viewId, float multiplier) {
        View view = findViewById(viewId);
        if (view != null) {
            // Get the current padding
            int currentPaddingLeft = view.getPaddingLeft();
            int currentPaddingTop = view.getPaddingTop();
            int currentPaddingRight = view.getPaddingRight();
            int currentPaddingBottom = view.getPaddingBottom();
            
            // Apply custom multiplier to bottom inset
            int reducedBottomInset = (int) (insets.bottom * multiplier);
            
            // Apply new padding with reduced system inset added to bottom
            view.setPadding(
                currentPaddingLeft,
                currentPaddingTop,
                currentPaddingRight,
                currentPaddingBottom + reducedBottomInset
            );
            Log.d(TAG, "Applied bottom insets (" + reducedBottomInset + "dp from " + insets.bottom + "dp with multiplier " + multiplier + ") to view ID: " + viewId);
        } else {
            Log.w(TAG, "View with ID " + viewId + " not found for bottom insets");
        }
    }

    /**
     * Apply bottom insets specifically for navigation bars (uses different default)
     * Navigation bars typically need more spacing for comfortable thumb reach
     */
    protected void applyBottomNavInsets(Insets insets, int viewId) {
        // Navigation bars use 70% spacing by default for better ergonomics
        applyBottomInsets(insets, viewId, 1.0f);
    }

    /**
     * Apply bottom insets specifically for button containers (uses different default)
     * Button containers can use less spacing as they're typically for one-time actions
     */
    protected void applyBottomButtonInsets(Insets insets, int viewId) {
        // Button containers use 50% spacing by default
        applyBottomInsets(insets, viewId, 0.5f);
    }

    /**
     * Apply side insets to a view (for notches/cutouts on sides)
     */
    protected void applySideInsets(Insets insets, int viewId) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setPadding(
                insets.left + view.getPaddingLeft(),
                view.getPaddingTop(),
                insets.right + view.getPaddingRight(),
                view.getPaddingBottom()
            );
            Log.d(TAG, "Applied side insets to view ID: " + viewId);
        }
    }

    /**
     * Apply custom insets to a specific view
     * Useful for child activities with special layouts
     */
    protected void applyCustomInsets(Insets insets, int viewId, boolean top, boolean bottom, boolean left, boolean right) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setPadding(
                (left ? insets.left : 0) + view.getPaddingLeft(),
                (top ? insets.top : 0) + view.getPaddingTop(),
                (right ? insets.right : 0) + view.getPaddingRight(),
                (bottom ? insets.bottom : 0) + view.getPaddingBottom()
            );
            Log.d(TAG, "Applied custom insets to view ID: " + viewId);
        }
    }
}
