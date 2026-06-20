package com.rct.dormfinder.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesManager {
    private static final String PREF_NAME = "DormFinderFavorites";
    private static final String KEY_FAVORITES = "favorite_dorm_ids";
    
    private SharedPreferences prefs;
    private Gson gson;
    private FirebaseFirestore db;
    private String currentUserId;
    private boolean isGuest;
    private Context context;

    public FavoritesManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            isGuest = user.isAnonymous();
        } else {
            currentUserId = null;
            isGuest = false;
        }
    }

    public void addFavorite(String dormId) {
        if (isGuest) {
            Toast.makeText(context, "Sign in to add favorites", Toast.LENGTH_SHORT).show();
            return;
        }
        Set<String> favorites = getFavorites();
        favorites.add(dormId);
        saveFavorites(favorites);
        
        // Sync to Firestore
        syncToFirestore();
    }

    public void removeFavorite(String dormId) {
        if (isGuest) {
            Toast.makeText(context, "Sign in to manage favorites", Toast.LENGTH_SHORT).show();
            return;
        }
        Set<String> favorites = getFavorites();
        favorites.remove(dormId);
        saveFavorites(favorites);
        
        // Sync to Firestore
        syncToFirestore();
    }

    public boolean isFavorite(String dormId) {
        return getFavorites().contains(dormId);
    }

    public Set<String> getFavorites() {
        String json = prefs.getString(KEY_FAVORITES, "[]");
        List<String> favoritesList = gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
        return new HashSet<>(favoritesList);
    }

    private void saveFavorites(Set<String> favorites) {
        String json = gson.toJson(new ArrayList<>(favorites));
        prefs.edit().putString(KEY_FAVORITES, json).apply();
    }

    private void syncToFirestore() {
        if (currentUserId == null) return;
        
        Set<String> favorites = getFavorites();
        db.collection("users").document(currentUserId)
                .update("favoriteDormitories", new ArrayList<>(favorites))
                .addOnFailureListener(e -> {
                    android.util.Log.w("FavoritesManager", "Failed to sync favorites: " + e.getMessage());
                });
    }

    public void loadFromFirestore(OnFavoritesLoadedListener listener) {
        if (currentUserId == null) {
            listener.onLoaded(new ArrayList<>());
            return;
        }

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        List<String> firestoreFavorites = (List<String>) document.get("favoriteDormitories");
                        if (firestoreFavorites != null) {
                            saveFavorites(new HashSet<>(firestoreFavorites));
                            listener.onLoaded(firestoreFavorites);
                        } else {
                            listener.onLoaded(new ArrayList<>());
                        }
                    } else {
                        listener.onLoaded(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onLoaded(new ArrayList<>());
                });
    }

    public interface OnFavoritesLoadedListener {
        void onLoaded(List<String> favorites);
    }

    public void clearAllFavorites() {
        saveFavorites(new HashSet<>());
        syncToFirestore();
    }
}
