package fr.charleslabs.tinwhistletabs.music;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages favorite songs using SharedPreferences
 */
public class FavoritesManager {
    private static final String PREFS_NAME = "FavoritesPrefs";
    private static final String KEY_FAVORITES = "favorites";
    
    private final SharedPreferences prefs;
    
    public FavoritesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Add song to favorites
     */
    public void addFavorite(String sheetId) {
        Set<String> favorites = getFavorites();
        favorites.add(sheetId);
        saveFavorites(favorites);
    }
    
    /**
     * Remove song from favorites
     */
    public void removeFavorite(String sheetId) {
        Set<String> favorites = getFavorites();
        favorites.remove(sheetId);
        saveFavorites(favorites);
    }
    
    /**
     * Toggle favorite status
     */
    public boolean toggleFavorite(String sheetId) {
        Set<String> favorites = getFavorites();
        boolean isFavorite;
        
        if (favorites.contains(sheetId)) {
            favorites.remove(sheetId);
            isFavorite = false;
        } else {
            favorites.add(sheetId);
            isFavorite = true;
        }
        
        saveFavorites(favorites);
        return isFavorite;
    }
    
    /**
     * Check if song is favorite
     */
    public boolean isFavorite(String sheetId) {
        return getFavorites().contains(sheetId);
    }
    
    /**
     * Get all favorite song IDs
     */
    public Set<String> getFavorites() {
        Set<String> favorites = prefs.getStringSet(KEY_FAVORITES, null);
        if (favorites == null) {
            return new HashSet<>();
        }
        // Return a mutable copy
        return new HashSet<>(favorites);
    }
    
    /**
     * Save favorites to SharedPreferences
     */
    private void saveFavorites(Set<String> favorites) {
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
    }
    
    /**
     * Clear all favorites
     */
    public void clearAll() {
        prefs.edit().remove(KEY_FAVORITES).apply();
    }
}
