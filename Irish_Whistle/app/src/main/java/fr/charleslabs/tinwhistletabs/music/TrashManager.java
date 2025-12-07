package fr.charleslabs.tinwhistletabs.music;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TrashManager {
    private static final String PREFS_NAME = "TrashPrefs";
    private static final String KEY_TRASH = "trash_items";
    private static final long TRASH_RETENTION_DAYS = 30;
    private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000;

    public static class TrashItem {
        public String sheetId;
        public String title;
        public long deletedTime;

        public TrashItem(String sheetId, String title, long deletedTime) {
            this.sheetId = sheetId;
            this.title = title;
            this.deletedTime = deletedTime;
        }

        public boolean isExpired() {
            long now = System.currentTimeMillis();
            return (now - deletedTime) > (TRASH_RETENTION_DAYS * MILLIS_PER_DAY);
        }

        public int getDaysRemaining() {
            long now = System.currentTimeMillis();
            long elapsed = now - deletedTime;
            long remaining = (TRASH_RETENTION_DAYS * MILLIS_PER_DAY) - elapsed;
            return (int) (remaining / MILLIS_PER_DAY);
        }
    }

    public static void moveToTrash(Context context, String sheetId, String title) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        List<TrashItem> items = getTrashItems(context);
        
        // Add new item
        items.add(new TrashItem(sheetId, title, System.currentTimeMillis()));
        
        // Save
        saveTrashItems(prefs, items);
    }

    public static void restoreFromTrash(Context context, String sheetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        List<TrashItem> items = getTrashItems(context);
        
        // Remove item
        items.removeIf(item -> item.sheetId.equals(sheetId));
        
        // Save
        saveTrashItems(prefs, items);
    }

    public static void permanentlyDelete(Context context, String sheetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        List<TrashItem> items = getTrashItems(context);
        
        // Remove item
        items.removeIf(item -> item.sheetId.equals(sheetId));
        
        // Save
        saveTrashItems(prefs, items);
    }

    public static List<TrashItem> getTrashItems(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_TRASH, "[]");
        List<TrashItem> items = new ArrayList<>();
        
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                TrashItem item = new TrashItem(
                    obj.getString("sheetId"),
                    obj.getString("title"),
                    obj.getLong("deletedTime")
                );
                
                // Auto-delete expired items
                if (!item.isExpired()) {
                    items.add(item);
                }
            }
            
            // Save cleaned list if any items were expired
            if (items.size() != array.length()) {
                saveTrashItems(prefs, items);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return items;
    }

    public static boolean isInTrash(Context context, String sheetId) {
        List<TrashItem> items = getTrashItems(context);
        for (TrashItem item : items) {
            if (item.sheetId.equals(sheetId)) {
                return true;
            }
        }
        return false;
    }

    private static void saveTrashItems(SharedPreferences prefs, List<TrashItem> items) {
        JSONArray array = new JSONArray();
        for (TrashItem item : items) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("sheetId", item.sheetId);
                obj.put("title", item.title);
                obj.put("deletedTime", item.deletedTime);
                array.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        prefs.edit().putString(KEY_TRASH, array.toString()).apply();
    }

    public static void cleanExpiredItems(Context context) {
        getTrashItems(context);
    }
}
