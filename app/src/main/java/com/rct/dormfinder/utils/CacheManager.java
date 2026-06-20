package com.rct.dormfinder.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rct.dormfinder.models.Dormitory;
import java.util.ArrayList;
import java.util.List;

public class CacheManager {
    private static final String PREF_NAME = "DormFinderCache";
    private static final String KEY_DORMS = "cached_dorms";
    private SharedPreferences prefs;
    private Gson gson;

    public CacheManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveDormitories(List<Dormitory> dorms) {
        String json = gson.toJson(dorms);
        prefs.edit().putString(KEY_DORMS, json).apply();
    }

    public List<Dormitory> getCachedDormitories() {
        String json = prefs.getString(KEY_DORMS, "[]");
        return gson.fromJson(json, new TypeToken<List<Dormitory>>(){}.getType());
    }

    public void clearCache() {
        prefs.edit().clear().apply();
    }
}