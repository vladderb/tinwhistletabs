package fr.charleslabs.tinwhistletabs.music;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CustomSongsManager {
    private static final String PREFS_NAME = "custom_songs";
    private static final String KEY_SONGS = "songs_list";
    private static final String CUSTOM_FILES_DIR = "custom_songs";
    
    private final Context context;
    private final SharedPreferences prefs;
    
    public CustomSongsManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void addSong(String title, String author, String type, String abc, 
                       List<MusicNote> notes, String key) throws IOException, JSONException {
        
        // Generate unique filename
        String filename = "custom_" + System.currentTimeMillis();
        
        // Save notes to file
        saveNotesToFile(filename, notes);
        
        // Get current song list
        JSONArray songsArray = getSongsArray();
        
        // Convert key from ABC format to project format
        String convertedKey = convertABCKeyToProjectKey(key);
        
        // Create new entry
        JSONObject songObject = new JSONObject();
        songObject.put("title", title);
        songObject.put("type", type);
        songObject.put("file", filename);
        songObject.put("abc", abc);
        songObject.put("key", convertedKey);
        songObject.put("whistle", "D");
        songObject.put("custom", true);
        
        if (author != null && !author.isEmpty()) {
            songObject.put("author", author);
        }
        
        // Add to list
        songsArray.put(songObject);
        
        // Save updated list
        prefs.edit().putString(KEY_SONGS, songsArray.toString()).apply();
    }
    
    private String convertABCKeyToProjectKey(String abcKey) {
        // Convert ABC keys (D, G, A, etc.) to project format (High D, High G, etc.)
        if (abcKey == null || abcKey.isEmpty()) {
            return MusicSettings.DEFAULT_KEY;
        }
        
        // Remove modifiers, modes (major, minor, mixolydian, etc.) and take only letter
        String baseKey = abcKey.replaceAll("(?i)(major|minor|mixolydian|dorian|phrygian|lydian|aeolian|locrian)", "")
                               .replaceAll("[#b]", "")
                               .trim()
                               .toUpperCase();
        
        // Take only first letter if there's something else
        if (baseKey.length() > 2) {
            baseKey = baseKey.substring(0, 1);
        }
        
        // Mapping ABC keys to project keys
        switch (baseKey) {
            case "G":
                return "High G";
            case "F":
                return "High F";
            case "E":
                return "High E";
            case "EB":
            case "E♭":
                return "High Eb";
            case "D":
                return "High D";
            case "DB":
            case "D♭":
                return "High Db";
            case "C":
                return "High C";
            case "B":
                return "B";
            case "BB":
            case "B♭":
                return "Bb";
            case "A":
                return "Low A";
            default:
                // If not recognized, return High D
                return MusicSettings.DEFAULT_KEY;
        }
    }
    
    public List<MusicSheet> getCustomSongs() {
        List<MusicSheet> songs = new ArrayList<>();
        
        try {
            JSONArray songsArray = getSongsArray();
            
            for (int i = 0; i < songsArray.length(); i++) {
                JSONObject songObject = songsArray.getJSONObject(i);
                songs.add(new MusicSheet(songObject));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return songs;
    }
    
    public void deleteSong(String filename) {
        try {
            // Delete notes file
            File file = new File(context.getFilesDir(), CUSTOM_FILES_DIR + "/" + filename + ".txt");
            if (file.exists()) {
                file.delete();
            }
            
            // Remove from list
            JSONArray songsArray = getSongsArray();
            JSONArray newArray = new JSONArray();
            
            for (int i = 0; i < songsArray.length(); i++) {
                JSONObject song = songsArray.getJSONObject(i);
                if (!song.getString("file").equals(filename)) {
                    newArray.put(song);
                }
            }
            
            prefs.edit().putString(KEY_SONGS, newArray.toString()).apply();
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    private JSONArray getSongsArray() {
        String songsJson = prefs.getString(KEY_SONGS, "[]");
        try {
            return new JSONArray(songsJson);
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }
    
    private void saveNotesToFile(String filename, List<MusicNote> notes) throws IOException {
        // Create directory if it doesn't exist
        File dir = new File(context.getFilesDir(), CUSTOM_FILES_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Format notes string as pitch/duration,pitch/duration,...
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < notes.size(); i++) {
            MusicNote note = notes.get(i);
            content.append(note.getPitch())
                   .append("/")
                   .append((int)note.getLengthInMS(1.0f));
            
            if (i < notes.size() - 1) {
                content.append(",");
            }
        }
        
        // Save to file
        File file = new File(dir, filename + ".txt");
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(content.toString().getBytes());
        fos.close();
    }
    
    public File getCustomSongFile(String filename) {
        return new File(context.getFilesDir(), CUSTOM_FILES_DIR + "/" + filename + ".txt");
    }
    
    public void updateSongABC(String filename, String newAbc, List<MusicNote> notes) throws IOException, JSONException {
        // Update notes file
        saveNotesToFile(filename, notes);
        
        // Update ABC in JSON
        JSONArray songsArray = getSongsArray();
        JSONArray updatedArray = new JSONArray();
        boolean found = false;
        
        for (int i = 0; i < songsArray.length(); i++) {
            JSONObject song = songsArray.getJSONObject(i);
            if (song.getString("file").equals(filename)) {
                song.put("abc", newAbc);
                found = true;
            }
            updatedArray.put(song);
        }
        
        if (!found) {
            throw new IOException("Song with filename " + filename + " not found");
        }
        
        // Save and apply changes immediately
        prefs.edit().putString(KEY_SONGS, updatedArray.toString()).commit();
    }
    
    public int deleteAllCustomSongs() {
        int deletedCount = 0;
        
        try {
            JSONArray songsArray = getSongsArray();
            
            // Delete all files
            for (int i = 0; i < songsArray.length(); i++) {
                JSONObject song = songsArray.getJSONObject(i);
                String filename = song.getString("file");
                
                File file = new File(context.getFilesDir(), CUSTOM_FILES_DIR + "/" + filename + ".txt");
                if (file.exists() && file.delete()) {
                    deletedCount++;
                }
            }
            
            // Clear list
            prefs.edit().putString(KEY_SONGS, "[]").apply();
            
            // Delete directory if empty
            File dir = new File(context.getFilesDir(), CUSTOM_FILES_DIR);
            if (dir.exists() && dir.isDirectory()) {
                String[] files = dir.list();
                if (files == null || files.length == 0) {
                    dir.delete();
                }
            }
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return deletedCount;
    }
}
