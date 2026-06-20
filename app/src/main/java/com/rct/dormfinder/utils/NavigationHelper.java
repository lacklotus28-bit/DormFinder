package com.rct.dormfinder.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.badge.BadgeDrawable;
import com.rct.dormfinder.R;
import com.rct.dormfinder.activities.BookingManagementActivity;
import com.rct.dormfinder.activities.ChatListActivity;
import com.rct.dormfinder.activities.FavoritesActivity;
import com.rct.dormfinder.activities.LandlordHomeActivity;
import com.rct.dormfinder.activities.SearchFilterActivity;
import com.rct.dormfinder.activities.MyDormitoriesActivity;
import com.rct.dormfinder.activities.ProfileActivity;
import com.rct.dormfinder.activities.StudentBookingsActivity;
import com.rct.dormfinder.activities.StudentHomeActivity;
import com.rct.dormfinder.activities.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Helper class for managing bottom navigation across the app
 * Provides unified navigation setup for both Student and Landlord user types
 */
public class NavigationHelper {
    private static final String TAG = "NavigationHelper";

    /**
     * Setup bottom navigation for Student users
     * Navigation items: Home | Messages | Favorites | Bookings | Profile
     * 
     * @param activity The current activity
     * @param bottomNav The BottomNavigationView to setup
     * @param activeItemId The menu item ID that should be highlighted as active
     */
    public static void setupStudentBottomNavigation(Activity activity, BottomNavigationView bottomNav, int activeItemId) {
        Log.d(TAG, "Setting up student bottom navigation for: " + activity.getClass().getSimpleName());
        
        // Inflate student menu
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.student_bottom_nav_menu);
        
        // Set the active item
        if (activeItemId != 0) {
            bottomNav.setSelectedItemId(activeItemId);
            Log.d(TAG, "Active item set to: " + activeItemId);
        }
        
        // Disable reselection animation (prevents unnecessary refreshes)
        bottomNav.setOnItemReselectedListener(item -> {
            Log.d(TAG, "Item reselected (ignored): " + item.getTitle());
        });
        
        // Handle navigation item clicks
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            boolean isGuest = false;
            try {
                com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                isGuest = (user != null && user.isAnonymous());
            } catch (Exception e) {
                Log.w(TAG, "Failed to check auth state for guest gating: " + e.getMessage());
            }
            
            // Don't navigate if already on this page
            if (itemId == activeItemId) {
                Log.d(TAG, "Already on this page, ignoring navigation");
                return true;
            }
            
            Intent intent = null;
            
            if (itemId == R.id.nav_home) {
                Log.d(TAG, "Navigating to: Home");
                intent = new Intent(activity, StudentHomeActivity.class);
            } else if (itemId == R.id.nav_messages) {
                if (isGuest) {
                    Toast.makeText(activity, "Sign in to view messages", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(activity, LoginActivity.class);
                    loginIntent.putExtra("redirect_to", "messages");
                    activity.startActivity(loginIntent);
                    bottomNav.setSelectedItemId(activeItemId);
                    return true;
                }
                Log.d(TAG, "Navigating to: Messages");
                intent = new Intent(activity, ChatListActivity.class);
            } else if (itemId == R.id.nav_favorites) {
                if (isGuest) {
                    Toast.makeText(activity, "Sign in to view favorites", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(activity, LoginActivity.class);
                    loginIntent.putExtra("redirect_to", "favorites");
                    activity.startActivity(loginIntent);
                    bottomNav.setSelectedItemId(activeItemId);
                    return true;
                }
                Log.d(TAG, "Navigating to: Favorites");
                intent = new Intent(activity, FavoritesActivity.class);
            } else if (itemId == R.id.nav_bookings) {
                if (isGuest) {
                    Toast.makeText(activity, "Sign in to view bookings", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(activity, LoginActivity.class);
                    loginIntent.putExtra("redirect_to", "bookings");
                    activity.startActivity(loginIntent);
                    bottomNav.setSelectedItemId(activeItemId);
                    return true;
                }
                Log.d(TAG, "Navigating to: Bookings");
                intent = new Intent(activity, StudentBookingsActivity.class);
            } else if (itemId == R.id.nav_profile) {
                if (isGuest) {
                    Toast.makeText(activity, "Sign in to view profile", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(activity, LoginActivity.class);
                    loginIntent.putExtra("redirect_to", "profile");
                    activity.startActivity(loginIntent);
                    bottomNav.setSelectedItemId(activeItemId);
                    return true;
                }
                Log.d(TAG, "Navigating to: Profile");
                intent = new Intent(activity, ProfileActivity.class);
            }
            
            if (intent != null) {
                // Clear back stack and set as new root
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0); // No animation for smooth transition
                return true;
            }
            
            return false;
        });
    }

    /**
     * Setup bottom navigation for Landlord users
     * Navigation items: Home | My Dorms | Requests | Messages | Profile
     * 
     * @param activity The current activity
     * @param bottomNav The BottomNavigationView to setup
     * @param activeItemId The menu item ID that should be highlighted as active
     */
    public static void setupLandlordBottomNavigation(Activity activity, BottomNavigationView bottomNav, int activeItemId) {
        Log.d(TAG, "Setting up landlord bottom navigation for: " + activity.getClass().getSimpleName());
        
        // Inflate landlord menu
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.landlord_bottom_nav_menu);
        
        // Set the active item
        if (activeItemId != 0) {
            bottomNav.setSelectedItemId(activeItemId);
            Log.d(TAG, "Active item set to: " + activeItemId);
        }
        
        // Disable reselection animation (prevents unnecessary refreshes)
        bottomNav.setOnItemReselectedListener(item -> {
            Log.d(TAG, "Item reselected (ignored): " + item.getTitle());
        });
        
        // Handle navigation item clicks
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            // Don't navigate if already on this page
            if (itemId == activeItemId) {
                Log.d(TAG, "Already on this page, ignoring navigation");
                return true;
            }
            
            Intent intent = null;
            
            if (itemId == R.id.nav_home) {
                Log.d(TAG, "Navigating to: Home");
                intent = new Intent(activity, LandlordHomeActivity.class);
            } else if (itemId == R.id.nav_my_dorms) {
                Log.d(TAG, "Navigating to: My Dorms");
                intent = new Intent(activity, MyDormitoriesActivity.class);
            } else if (itemId == R.id.nav_requests) {
                Log.d(TAG, "Navigating to: Requests");
                intent = new Intent(activity, BookingManagementActivity.class);
            } else if (itemId == R.id.nav_messages) {
                Log.d(TAG, "Navigating to: Messages");
                intent = new Intent(activity, ChatListActivity.class);
            } else if (itemId == R.id.nav_profile) {
                Log.d(TAG, "Navigating to: Profile");
                intent = new Intent(activity, ProfileActivity.class);
            }
            
            if (intent != null) {
                // Clear back stack and set as new root
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0); // No animation for smooth transition
                return true;
            }
            
            return false;
        });
    }
    
    /**
     * Update badge count on a navigation item
     * 
     * @param bottomNav The BottomNavigationView
     * @param itemId The menu item ID to update badge for
     * @param count The badge count (0 to hide badge)
     */
    public static void updateBadge(BottomNavigationView bottomNav, int itemId, int count) {
        if (bottomNav == null) {
            Log.w(TAG, "Cannot update badge - bottomNav is null");
            return;
        }
        
        BadgeDrawable badge = bottomNav.getOrCreateBadge(itemId);
        
        if (count > 0) {
            badge.setVisible(true);
            badge.setNumber(count);
            badge.setMaxCharacterCount(2); // Show "99+" for counts > 99
            Log.d(TAG, "Badge updated for item " + itemId + ": " + count);
        } else {
            badge.setVisible(false);
            Log.d(TAG, "Badge hidden for item " + itemId);
        }
    }
    
    /**
     * Clear badge on a navigation item
     * 
     * @param bottomNav The BottomNavigationView
     * @param itemId The menu item ID to clear badge from
     */
    public static void clearBadge(BottomNavigationView bottomNav, int itemId) {
        if (bottomNav == null) return;
        
        BadgeDrawable badge = bottomNav.getBadge(itemId);
        if (badge != null) {
            badge.setVisible(false);
            Log.d(TAG, "Badge cleared for item " + itemId);
        }
    }
}
