package fr.charleslabs.tinwhistletabs.api;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * API client for The Session (thesession.org)
 * Documentation: https://thesession.org/api
 */
public class TheSessionApi {
    private static final String BASE_URL = "https://thesession.org/tunes/search";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public interface SearchCallback {
        void onSuccess(List<TuneResult> results);
        void onError(String error);
    }
    
    public static class TuneResult {
        public int id;
        public String name;
        public String type;
        public String abc;
        public String url;
        
        public TuneResult(int id, String name, String type, String abc, String url) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.abc = abc;
            this.url = url;
        }
    }
    
    /**
     * Search for tunes by name
     */
    public static void searchTunes(String query, SearchCallback callback) {
        executor.execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String urlString = BASE_URL + "?q=" + encodedQuery + "&format=json&perpage=20";
                
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "TinWhistleTabs-Android");
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    List<TuneResult> results = parseSearchResults(response.toString());
                    
                    mainHandler.post(() -> callback.onSuccess(results));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP error: " + responseCode));
                }
                
                conn.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Get tune details including ABC notation
     */
    public static void getTuneABC(int tuneId, SearchCallback callback) {
        executor.execute(() -> {
            try {
                String urlString = "https://thesession.org/tunes/" + tuneId + "?format=json";
                android.util.Log.d("TheSessionApi", "Fetching: " + urlString);
                
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "TinWhistleTabs-Android");
                
                int responseCode = conn.getResponseCode();
                android.util.Log.d("TheSessionApi", "Response code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String jsonResponse = response.toString();
                    android.util.Log.d("TheSessionApi", "Response length: " + jsonResponse.length());
                    android.util.Log.d("TheSessionApi", "Response preview: " + 
                            jsonResponse.substring(0, Math.min(200, jsonResponse.length())));
                    
                    List<TuneResult> results = parseTuneDetails(jsonResponse);
                    android.util.Log.d("TheSessionApi", "Parsed " + results.size() + " results");
                    
                    mainHandler.post(() -> callback.onSuccess(results));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP error: " + responseCode));
                }
                
                conn.disconnect();
            } catch (Exception e) {
                android.util.Log.e("TheSessionApi", "Error fetching tune", e);
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }
    
    private static List<TuneResult> parseSearchResults(String json) {
        List<TuneResult> results = new ArrayList<>();
        
        try {
            JSONObject root = new JSONObject(json);
            JSONArray tunes = root.getJSONArray("tunes");
            
            for (int i = 0; i < tunes.length(); i++) {
                JSONObject tune = tunes.getJSONObject(i);
                
                int id = tune.getInt("id");
                String name = tune.getString("name");
                String type = tune.getString("type");
                String url = tune.getString("url");
                
                results.add(new TuneResult(id, name, type, null, url));
            }
        } catch (Exception e) {
            android.util.Log.e("TheSessionApi", "Error parsing search results", e);
        }
        
        return results;
    }
    
    private static List<TuneResult> parseTuneDetails(String json) {
        List<TuneResult> results = new ArrayList<>();
        
        try {
            JSONObject root = new JSONObject(json);
            
            int id = root.getInt("id");
            String name = root.getString("name");
            String type = root.getString("type");
            String url = root.getString("url");
            
            android.util.Log.d("TheSessionApi", "Parsing tune: " + name + " (type: " + type + ")");
            
            // Get settings (different versions of the tune)
            JSONArray settings = root.getJSONArray("settings");
            android.util.Log.d("TheSessionApi", "Found " + settings.length() + " settings");
            
            for (int i = 0; i < settings.length(); i++) {
                JSONObject setting = settings.getJSONObject(i);
                String abcBody = setting.getString("abc");
                
                // Get additional info
                String key = setting.optString("key", "");
                String meter = setting.optString("meter", "");
                String mode = setting.optString("mode", "");
                
                // Build full ABC notation with headers
                StringBuilder fullAbc = new StringBuilder();
                fullAbc.append("X: 1\n");
                fullAbc.append("T: ").append(name).append("\n");
                
                // Add type/rhythm
                String rhythm = mapTypeToRhythm(type);
                if (!rhythm.isEmpty()) {
                    fullAbc.append("R: ").append(rhythm).append("\n");
                }
                
                // Add meter if available
                if (!meter.isEmpty()) {
                    fullAbc.append("M: ").append(meter).append("\n");
                } else {
                    // Default meter based on type
                    String defaultMeter = getDefaultMeter(type);
                    if (!defaultMeter.isEmpty()) {
                        fullAbc.append("M: ").append(defaultMeter).append("\n");
                    }
                }
                
                // Add default note length
                fullAbc.append("L: 1/8\n");
                
                // Add key
                if (!key.isEmpty()) {
                    if (!mode.isEmpty() && !mode.equalsIgnoreCase("major")) {
                        fullAbc.append("K: ").append(key).append(" ").append(mode).append("\n");
                    } else {
                        fullAbc.append("K: ").append(key).append("\n");
                    }
                } else {
                    fullAbc.append("K: D\n"); // Default key
                }
                
                // Add the tune body
                fullAbc.append(abcBody);
                
                android.util.Log.d("TheSessionApi", "Setting " + (i+1) + " full ABC length: " + fullAbc.length());
                
                String tuneName = name;
                if (settings.length() > 1) {
                    tuneName = name + " (Setting " + (i + 1) + ")";
                }
                
                results.add(new TuneResult(id, tuneName, type, fullAbc.toString(), url));
            }
        } catch (Exception e) {
            android.util.Log.e("TheSessionApi", "Error parsing tune details", e);
            android.util.Log.e("TheSessionApi", "JSON was: " + json.substring(0, Math.min(500, json.length())));
        }
        
        return results;
    }
    
    private static String mapTypeToRhythm(String type) {
        switch (type.toLowerCase()) {
            case "reel": return "reel";
            case "jig": return "jig";
            case "slip jig": return "slip jig";
            case "hornpipe": return "hornpipe";
            case "polka": return "polka";
            case "slide": return "slide";
            case "waltz": return "waltz";
            case "march": return "march";
            case "barndance": return "barndance";
            case "strathspey": return "strathspey";
            default: return type;
        }
    }
    
    private static String getDefaultMeter(String type) {
        switch (type.toLowerCase()) {
            case "reel":
            case "hornpipe":
            case "barndance":
            case "strathspey":
                return "4/4";
            case "jig":
                return "6/8";
            case "slip jig":
                return "9/8";
            case "slide":
                return "12/8";
            case "polka":
                return "2/4";
            case "waltz":
                return "3/4";
            case "march":
                return "2/4";
            default:
                return "";
        }
    }
}
