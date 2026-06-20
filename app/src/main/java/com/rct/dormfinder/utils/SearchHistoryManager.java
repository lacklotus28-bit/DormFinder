package com.rct.dormfinder.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class SearchHistoryManager {
    private static final String PREF_NAME = "DormFinderSearchHistory";
    private static final String KEY_HISTORY = "search_history";
    private static final int MAX_HISTORY_SIZE = 10;
    
    private SharedPreferences prefs;
    private Gson gson;

    public SearchHistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void addSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) return;
        
        LinkedHashSet<String> history = getSearchHistory();
        
        // Remove if already exists (to move to top)
        history.remove(query);
        
        // Add to beginning
        List<String> historyList = new ArrayList<>();
        historyList.add(query);
        historyList.addAll(history);
        
        // Limit size
        if (historyList.size() > MAX_HISTORY_SIZE) {
            historyList = historyList.subList(0, MAX_HISTORY_SIZE);
        }
        
        saveSearchHistory(new LinkedHashSet<>(historyList));
    }

    public LinkedHashSet<String> getSearchHistory() {
        String json = prefs.getString(KEY_HISTORY, "[]");
        List<String> historyList = gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
        return new LinkedHashSet<>(historyList);
    }

    private void saveSearchHistory(LinkedHashSet<String> history) {
        String json = gson.toJson(new ArrayList<>(history));
        prefs.edit().putString(KEY_HISTORY, json).apply();
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }

    public void removeSearchQuery(String query) {
        LinkedHashSet<String> history = getSearchHistory();
        history.remove(query);
        saveSearchHistory(history);
    }
}
