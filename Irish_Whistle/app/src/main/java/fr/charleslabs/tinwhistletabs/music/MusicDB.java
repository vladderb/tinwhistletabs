package fr.charleslabs.tinwhistletabs.music;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.charleslabs.tinwhistletabs.R;
import fr.charleslabs.tinwhistletabs.utils.Utils;

public class MusicDB {
    public List<MusicSheet> musicDB = new ArrayList<>();

    //Singleton
    private static MusicDB instance;
    public static MusicDB getInstance(Context c){
        if(instance == null){
            instance = new MusicDB(c.getApplicationContext());
        }
        return instance;
    }
    private MusicDB(Context c){
        try {
            String fileContent =  Utils.fileToString(c.getResources().openRawResource(R.raw.db));

            JSONArray jsonSheets = new JSONArray(fileContent);

            for (int i=0; i < jsonSheets.length(); i++) {
                musicDB.add(new MusicSheet(jsonSheets.getJSONObject(i)));
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static List<MusicNote> open(Context c, String filename) throws IOException {
        android.util.Log.d("MusicDB", "Opening file: " + filename);
        
        if (filename.startsWith("custom_")) {
            // Custom track - read from file
            CustomSongsManager manager = new CustomSongsManager(c);
            java.io.File customFile = manager.getCustomSongFile(filename);
            
            if (!customFile.exists()) {
                throw new IOException("Custom song file not found: " + filename);
            }
            
            String fileContent = Utils.fileToString(new java.io.FileInputStream(customFile));
            String[] notesArray = fileContent.split(",");
            List<MusicNote> notes = new ArrayList<>();
            
            for (String note : notesArray){
                final String[] split = note.split("/");
                notes.add(new MusicNote(Integer.parseInt(split[0]),Integer.parseInt(split[1])));
            }
            
            return notes;
        } else if (filename.startsWith("builtin_")) {
            // Built-in track - search for ABC in database
            android.util.Log.d("MusicDB", "Looking for builtin track in database, total tracks: " + getInstance(c).musicDB.size());
            
            for (MusicSheet sheet : getInstance(c).musicDB) {
                android.util.Log.d("MusicDB", "Checking sheet: " + sheet.getFile() + " (title: " + sheet.getTitle() + ")");
                if (filename.equals(sheet.getFile())) {
                    android.util.Log.d("MusicDB", "Found matching sheet, ABC length: " + (sheet.getABC() != null ? sheet.getABC().length() : "null"));
                    try {
                        ABCParser.ABCParseResult result = ABCParser.parse(sheet.getABC());
                        android.util.Log.d("MusicDB", "Successfully parsed ABC, notes count: " + result.notes.size());
                        return result.notes;
                    } catch (Exception e) {
                        android.util.Log.e("MusicDB", "Failed to parse ABC", e);
                        throw new IOException("Failed to parse ABC notation for " + filename + ": " + e.getMessage());
                    }
                }
            }
            throw new IOException("Built-in track not found: " + filename);
        } else {
            throw new IOException("Unknown track type: " + filename);
        }
    }
}
