package com.rct.dormfinder.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.rct.dormfinder.R;

/**
 * Tutorial Helper - Creates game-like interactive tutorials with overlay tooltips
 * Similar to mobile game tutorials that highlight specific UI elements
 */
public class TutorialHelper {
    private static final String PREF_NAME = "TutorialPrefs";
    private Activity activity;
    private SharedPreferences prefs;
    private FrameLayout overlayLayout;
    private TutorialStep[] steps;
    private int currentStepIndex = 0;
    private OnTutorialCompleteListener completionListener;

    public interface OnTutorialCompleteListener {
        void onTutorialComplete();
    }

    public TutorialHelper(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Check if user has completed a specific tutorial
     */
    public boolean hasSeen(String tutorialKey) {
        return prefs.getBoolean(tutorialKey, false);
    }

    /**
     * Mark a tutorial as seen
     */
    public void markAsSeen(String tutorialKey) {
        prefs.edit().putBoolean(tutorialKey, true).apply();
    }

    /**
     * Reset all tutorials (useful for testing)
     */
    public void resetAll() {
        prefs.edit().clear().apply();
    }

    /**
     * Start a tutorial with multiple steps
     */
    public void startTutorial(String tutorialKey, TutorialStep[] steps, OnTutorialCompleteListener listener) {
        if (hasSeen(tutorialKey)) {
            if (listener != null) listener.onTutorialComplete();
            return;
        }

        this.steps = steps;
        this.currentStepIndex = 0;
        this.completionListener = listener;

        showStep(0);
    }

    private void showStep(int stepIndex) {
        if (stepIndex >= steps.length) {
            // Tutorial complete
            dismissOverlay();
            if (completionListener != null) {
                completionListener.onTutorialComplete();
            }
            return;
        }

        TutorialStep step = steps[stepIndex];
        
        // Wait for view to be laid out
        if (step.targetView != null) {
            step.targetView.post(() -> {
                createOverlay(step);
            });
        } else {
            createOverlay(step);
        }
    }

    private void createOverlay(TutorialStep step) {
        // Remove existing overlay if any
        dismissOverlay();

        // Create overlay with hole effect using custom view
        overlayLayout = new FrameLayout(activity) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                
                // Only draw hole if there's a target view
                if (step.targetView != null && step.targetView.getVisibility() == View.VISIBLE) {
                    // Draw semi-transparent dark overlay
                    Paint overlayPaint = new Paint();
                    overlayPaint.setColor(Color.parseColor("#DD000000")); // Darker overlay
                    overlayPaint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);
                    
                    // Cut out hole for target view using overlay-relative coordinates
                    Rect targetRect = new Rect();
                    step.targetView.getGlobalVisibleRect(targetRect);
                    int[] overlayLoc = new int[2];
                    getLocationOnScreen(overlayLoc);

                    int padding = 20;
                    float left = targetRect.left - overlayLoc[0] - padding;
                    float top = targetRect.top - overlayLoc[1] - padding;
                    float right = targetRect.right - overlayLoc[0] + padding;
                    float bottom = targetRect.bottom - overlayLoc[1] + padding;
                    
                    // Create hole with rounded corners
                    Paint holePaint = new Paint();
                    holePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                    holePaint.setAntiAlias(true);
                    
                    RectF holeRect = new RectF(left, top, right, bottom);
                    canvas.drawRoundRect(holeRect, 30, 30, holePaint);
                    
                    // Draw bright border around hole
                    Paint borderPaint = new Paint();
                    borderPaint.setColor(Color.parseColor("#FFFFFF")); // Bright white
                    borderPaint.setStyle(Paint.Style.STROKE);
                    borderPaint.setStrokeWidth(8);
                    borderPaint.setAntiAlias(true);
                    canvas.drawRoundRect(holeRect, 30, 30, borderPaint);
                    
                    // Draw pulsing effect
                    Paint pulsePaint = new Paint();
                    pulsePaint.setColor(Color.parseColor("#40FFFFFF")); // Semi-transparent white
                    pulsePaint.setStyle(Paint.Style.STROKE);
                    pulsePaint.setStrokeWidth(4);
                    pulsePaint.setAntiAlias(true);
                    
                    RectF pulseRect = new RectF(
                        left - 10, top - 10, 
                        right + 10, bottom + 10
                    );
                    canvas.drawRoundRect(pulseRect, 35, 35, pulsePaint);
                }
            }
        };
        
        overlayLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        
        // Enable drawing
        overlayLayout.setWillNotDraw(false);
        overlayLayout.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        overlayLayout.setClickable(true);

        // If there's a target view, create pulsing animation
        if (step.targetView != null && step.targetView.getVisibility() == View.VISIBLE) {
            createPulsingAnimation();
            
            // Make target view clickable through overlay if needed
            if (step.shouldClickTarget) {
                overlayLayout.setOnTouchListener((v, event) -> {
                    // Use overlay-relative coordinates for hit detection
                    Rect targetRect = new Rect();
                    step.targetView.getGlobalVisibleRect(targetRect);
                    int[] overlayLoc = new int[2];
                    v.getLocationOnScreen(overlayLoc);

                    float x = event.getX();
                    float y = event.getY();

                    int padding = 20;
                    float left = targetRect.left - overlayLoc[0] - padding;
                    float top = targetRect.top - overlayLoc[1] - padding;
                    float right = targetRect.right - overlayLoc[0] + padding;
                    float bottom = targetRect.bottom - overlayLoc[1] + padding;

                    if (x >= left && x <= right && y >= top && y <= bottom) {
                        step.targetView.performClick();
                        return true;
                    }
                    return false;
                });
            }
        } else {
            // No target view - just dark overlay
            overlayLayout.setBackgroundColor(Color.parseColor("#DD000000"));
        }

        // Create tooltip
        createTooltip(step);

        // Add overlay to activity root view
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(overlayLayout);
    }

    private void createPulsingAnimation() {
        // Animate overlay to pulse
        overlayLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (overlayLayout != null && overlayLayout.getParent() != null) {
                    overlayLayout.invalidate();
                    overlayLayout.postDelayed(this, 800);
                }
            }
        }, 800);
    }
    
    private int getStatusBarHeight() {
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return activity.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private void createTooltip(TutorialStep step) {
        // Create tooltip container
        LinearLayout tooltipContainer = new LinearLayout(activity);
        tooltipContainer.setOrientation(LinearLayout.VERTICAL);
        tooltipContainer.setBackgroundColor(Color.WHITE);
        tooltipContainer.setPadding(40, 30, 40, 30);
        tooltipContainer.setElevation(16);
        
        // Set rounded corners
        Drawable background = ContextCompat.getDrawable(activity, R.drawable.rounded_white_bg);
        if (background != null) {
            tooltipContainer.setBackground(background);
        }

        // Title
        if (step.title != null && !step.title.isEmpty()) {
            TextView tvTitle = new TextView(activity);
            tvTitle.setText(step.title);
            tvTitle.setTextSize(20);
            tvTitle.setTextColor(Color.BLACK);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            titleParams.bottomMargin = 20;
            tooltipContainer.addView(tvTitle, titleParams);
        }

        // Message
        TextView tvMessage = new TextView(activity);
        tvMessage.setText(step.message);
        tvMessage.setTextSize(16);
        tvMessage.setTextColor(Color.parseColor("#666666"));
        tvMessage.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        messageParams.bottomMargin = 30;
        tooltipContainer.addView(tvMessage, messageParams);

        // Buttons container
        LinearLayout buttonsLayout = new LinearLayout(activity);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(Gravity.CENTER);

        // Skip button (if not last step)
        if (currentStepIndex < steps.length - 1) {
            TextView btnSkip = new TextView(activity);
            btnSkip.setText("Skip Tutorial");
            btnSkip.setTextSize(14);
            btnSkip.setTextColor(Color.parseColor("#999999"));
            btnSkip.setPadding(30, 20, 30, 20);
            btnSkip.setOnClickListener(v -> {
                dismissOverlay();
                if (completionListener != null) {
                    completionListener.onTutorialComplete();
                }
            });
            
            LinearLayout.LayoutParams skipParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            skipParams.rightMargin = 20;
            buttonsLayout.addView(btnSkip, skipParams);
        }

        // Next/Got It button
        Button btnNext = new Button(activity);
        btnNext.setText(currentStepIndex == steps.length - 1 ? "Got it!" : "Next");
        btnNext.setTextColor(Color.WHITE);
        btnNext.setTextSize(16);
        btnNext.setTypeface(null, android.graphics.Typeface.BOLD);
        btnNext.setPadding(60, 20, 60, 20);
        
        Drawable btnBackground = ContextCompat.getDrawable(activity, R.drawable.button_primary);
        if (btnBackground != null) {
            btnNext.setBackground(btnBackground);
        }
        
        btnNext.setOnClickListener(v -> {
            currentStepIndex++;
            showStep(currentStepIndex);
        });
        
        buttonsLayout.addView(btnNext);

        tooltipContainer.addView(buttonsLayout);

        // Position tooltip
        FrameLayout.LayoutParams tooltipParams = new FrameLayout.LayoutParams(
                (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.85),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        
        // Position based on target view location (overlay-relative)
        if (step.targetView != null && step.targetView.getVisibility() == View.VISIBLE) {
            Rect targetRect = new Rect();
            step.targetView.getGlobalVisibleRect(targetRect);
            int[] overlayLoc = new int[2];
            overlayLayout.getLocationOnScreen(overlayLoc);

            int targetY = targetRect.top - overlayLoc[1];
            int targetHeight = targetRect.height();
            int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
            
            // Calculate available space above and below target
            int spaceAbove = targetY;
            int spaceBelow = screenHeight - (targetY + targetHeight);
            
            // Position tooltip where there's more space
            if (spaceBelow > spaceAbove && spaceBelow > 300) {
                // Show below target
                tooltipParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                tooltipParams.topMargin = targetY + targetHeight + 60;
            } else if (spaceAbove > 300) {
                // Show above target
                tooltipParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                tooltipParams.topMargin = Math.max(50, targetY - 400);
            } else {
                // Center if not enough space either side
                tooltipParams.gravity = Gravity.CENTER;
            }
        } else {
            // Center if no target
            tooltipParams.gravity = Gravity.CENTER;
        }

        overlayLayout.addView(tooltipContainer, tooltipParams);
        
        // Add arrow pointing to target (if target exists)
        if (step.targetView != null && step.targetView.getVisibility() == View.VISIBLE) {
            addArrowPointer(step.targetView, tooltipParams);
        }

        // Add step indicator (e.g., "1/5")
        TextView tvStepIndicator = new TextView(activity);
        tvStepIndicator.setText((currentStepIndex + 1) + "/" + steps.length);
        tvStepIndicator.setTextColor(Color.WHITE);
        tvStepIndicator.setTextSize(14);
        tvStepIndicator.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStepIndicator.setPadding(20, 20, 20, 20);
        
        FrameLayout.LayoutParams indicatorParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        indicatorParams.gravity = Gravity.TOP | Gravity.END;
        indicatorParams.topMargin = 40;
        indicatorParams.rightMargin = 40;
        
        overlayLayout.addView(tvStepIndicator, indicatorParams);
    }

    private void addArrowPointer(View targetView, FrameLayout.LayoutParams tooltipParams) {
        // REMOVED: Visual arrow/hand pointer completely
        // The tutorial now relies on the glowing border effect around the highlighted element
        return;
        
        /* OLD CODE - COMMENTED OUT FOR REFERENCE
        // Create arrow/hand icon pointing to target
        TextView arrow = new TextView(activity);
        arrow.setText("👆"); // Pointing hand emoji
        arrow.setTextSize(40);
        arrow.setShadowLayer(10, 0, 0, Color.BLACK);
        
        int[] location = new int[2];
        targetView.getLocationOnScreen(location);
        int statusBarHeight = getStatusBarHeight();
        
        FrameLayout.LayoutParams arrowParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        
        // Position arrow based on tooltip position
        int targetCenterX = location[0] + (targetView.getWidth() / 2);
        int targetY = location[1] - statusBarHeight;
        
        // Check if tooltip is above or below target
        boolean tooltipBelow = (tooltipParams.gravity & Gravity.TOP) != 0 && 
                              tooltipParams.topMargin > targetY;
        
        if (tooltipBelow) {
            // Tooltip is below, arrow points up to target
            arrow.setRotation(180); // Point up
            arrowParams.gravity = Gravity.TOP | Gravity.START;
            arrowParams.leftMargin = targetCenterX - 40;
            arrowParams.topMargin = targetY + targetView.getHeight() - 20;
        } else {
            // Tooltip is above or centered, arrow points down to target
            arrowParams.gravity = Gravity.TOP | Gravity.START;
            arrowParams.leftMargin = targetCenterX - 40;
            arrowParams.topMargin = targetY - 80;
        }
        
        // Animate arrow
        arrow.setAlpha(1.0f);
        arrow.animate()
            .translationY(tooltipBelow ? -20 : 20)
            .alpha(0.7f)
            .setDuration(600)
            .withEndAction(() -> {
                if (arrow.getParent() != null) {
                    arrow.animate()
                        .translationY(0)
                        .alpha(1.0f)
                        .setDuration(600)
                        .withEndAction(() -> addArrowPointer(targetView, tooltipParams))
                        .start();
                }
            })
            .start();
        
        overlayLayout.addView(arrow, arrowParams);
        */

    }

    private void dismissOverlay() {
        if (overlayLayout != null && overlayLayout.getParent() != null) {
            ((ViewGroup) overlayLayout.getParent()).removeView(overlayLayout);
            overlayLayout = null;
        }
    }

    /**
     * Represents a single tutorial step
     */
    public static class TutorialStep {
        public String title;
        public String message;
        public View targetView;
        public boolean shouldClickTarget;

        public TutorialStep(String title, String message, View targetView) {
            this(title, message, targetView, false);
        }

        public TutorialStep(String title, String message, View targetView, boolean shouldClickTarget) {
            this.title = title;
            this.message = message;
            this.targetView = targetView;
            this.shouldClickTarget = shouldClickTarget;
        }

        // Constructor for steps without target (general info)
        public TutorialStep(String title, String message) {
            this(title, message, null, false);
        }
    }
}
