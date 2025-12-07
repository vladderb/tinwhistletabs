package fr.charleslabs.tinwhistletabs.music;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

public class MusicSheet implements Serializable {
    // Allocated at construction
    private final String title;
    private final String author;
    private final String file;
    private final String type;
    private final String sheet_author;
    private final String license ;
    private final String key;

    private final String whistle;

    private String abc;

    public MusicSheet(JSONObject jsonObject) throws JSONException {
        // Mandatory
        this.title = jsonObject.getString("title");
        this.file = jsonObject.getString("file");
        this.type = jsonObject.getString("type");
        if (jsonObject.has("abc")) this.abc = jsonObject.getString("abc");
        else this.abc = null;

        // Optional
        if (jsonObject.has("author")) this.author = jsonObject.getString("author");
        else this.author = null;
        if (jsonObject.has("sheet_author")) this.sheet_author = jsonObject.getString("sheet_author");
        else this.sheet_author = null;
        if (jsonObject.has("license")) this.license = jsonObject.getString("license");
        else this.license = null;
        if (jsonObject.has("key")) this.key = jsonObject.getString("key");
        else this.key = MusicSettings.DEFAULT_KEY;
        if (jsonObject.has("whistle")) this.whistle = jsonObject.getString("whistle");
        else this.whistle = "D";
    }

    public void transposeKey(final List<MusicNote> notes, final String oldKey, final String newKey){
        if (!oldKey.equals(newKey)) {
            final int shift = MusicSettings.getShift(newKey) - MusicSettings.getShift(oldKey);
            transpose(notes, shift);
        }
    }

    private static void transpose(final List<MusicNote> notes, final int shift){
        if(shift != 0)
            for(MusicNote note : notes)
                note.transpose(shift);
    }

    public static String notesToTabs(final List<MusicNote> notes) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < notes.size(); i++) {
            MusicNote note = notes.get(i);
            if (!note.isRest()) {
                buffer.append(note.toTab());
                
                // Добавляем пробелы в зависимости от длительности ноты
                float lengthMs = note.getLengthInMS(1.0f);
                if (lengthMs >= 800) {
                    buffer.append("  ");
                } else if (lengthMs >= 400) {
                    buffer.append(" ");
                }
            }
        }
        return buffer.toString();
    }
    
    public String notesToTabsWithLineBreaks(final List<MusicNote> notes) {
        if (abc == null || abc.isEmpty()) {
            return notesToTabs(notes);
        }
        
        return parseABCStructure(notes);
    }
    
    private String parseABCStructure(final List<MusicNote> notes) {
        StringBuilder result = new StringBuilder();
        int noteIndex = 0;
        int totalNotesInABC = 0;
        
        // Разбиваем ABC на строки
        String[] abcLines = abc.split("\\n");
        
        for (String line : abcLines) {
            // Пропускаем заголовки и метаданные
            if (line.trim().isEmpty() || 
                line.matches("^[A-Z]:.*") || 
                line.startsWith("%")) {
                continue;
            }
            
            // Подсчитываем количество нот в строке ABC (включая паузы)
            int notesInLine = countNotesInABCLine(line);
            totalNotesInABC += notesInLine;
            
            // Добавляем соответствующее количество нот из списка
            for (int i = 0; i < notesInLine && noteIndex < notes.size(); i++) {
                MusicNote note = notes.get(noteIndex);
                if (!note.isRest()) {
                    result.append(note.toTab());
                    
                    // Добавляем пробелы в зависимости от длительности ноты
                    float lengthMs = note.getLengthInMS(1.0f);
                    if (lengthMs >= 800) {
                        result.append("  ");
                    } else if (lengthMs >= 400) {
                        result.append(" ");
                    }
                }
                noteIndex++;
            }
            
            // Добавляем перенос строки после каждой строки ABC
            result.append("\n");
        }
        
        // Проверяем что мы обработали все ноты
        if (totalNotesInABC != notes.size()) {
            android.util.Log.w("MusicSheet", "Mismatch: counted " + totalNotesInABC + " notes in ABC, but have " + notes.size() + " notes in list");
        }
        
        return result.toString();
    }
    
    private int countNotesInABCLine(String line) {
        int count = 0;
        
        // Удаляем такты, повторы и другие символы разметки
        String cleaned = line.replaceAll("[|:\\[\\]\\(\\)\\{\\}]", " ");
        android.util.Log.v("MusicSheet", "countNotesInABCLine: '" + line + "' -> '" + cleaned + "'");
        
        int i = 0;
        while (i < cleaned.length()) {
            char c = cleaned.charAt(i);
            
            // Пропускаем пробелы
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            
            // Проверяем модификаторы перед нотой (^, _, =)
            if (c == '^' || c == '_' || c == '=') {
                i++;
                if (i >= cleaned.length()) break;
                c = cleaned.charAt(i);
            }
            
            // Проверяем ноту или паузу
            if ((c >= 'A' && c <= 'G') || (c >= 'a' && c <= 'g') || c == 'z' || c == 'x') {
                int startPos = i;
                count++;
                i++;
                
                // Пропускаем октавные модификаторы (апострофы и запятые)
                while (i < cleaned.length() && (cleaned.charAt(i) == '\'' || cleaned.charAt(i) == ',')) {
                    i++;
                }
                
                // Пропускаем модификаторы длительности
                while (i < cleaned.length()) {
                    char next = cleaned.charAt(i);
                    if (Character.isDigit(next) || next == '/' || next == '-') {
                        i++;
                    } else {
                        break;
                    }
                }
                
                android.util.Log.v("MusicSheet", "  Found note #" + count + ": '" + cleaned.substring(startPos, i) + "'");
            } else {
                i++;
            }
        }
        
        android.util.Log.v("MusicSheet", "  Total counted: " + count + " notes");
        return count;
    }

    public static float noteIndexToTime(final List<MusicNote> notes, final int noteIndex,
                                        final float tempoModifier){
        float time = 0;
        int i = 0, trueNotes = 0;

        while (trueNotes < noteIndex) {
            if (i >= notes.size())
                return 0;
            if(!notes.get(i).isRest())
                trueNotes++;
            time += notes.get(i).getLengthInS(tempoModifier);
            i ++;
        }
        return time;
    }

    // Filter
    public boolean filter(final String search){
        return this.getTitle().toLowerCase().contains(search);
    }

    // Getter and setters
    public String getAuthor() {return author;}
    public String getTitle() {return title;}
    public String getFile() {return file;}
    public String getKey() {return key;}
    public String getType() {return type;}
    public String getSheetAuthor() {return sheet_author;}
    public String getLicense() {return license;}
    public String getABC() {return abc;}
    public String getWhistle() {
        return whistle;
    }
}
