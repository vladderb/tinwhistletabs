package fr.charleslabs.tinwhistletabs.music;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ABCParser {
    
    private static final Map<String, Integer> NOTE_TO_PITCH = new HashMap<>();
    private static final int DEFAULT_NOTE_LENGTH = 8; // 1/8 note
    private static final int DEFAULT_TEMPO = 120;
    private static final int WHISTLE_MIN_PITCH = 54; // d
    private static final int WHISTLE_MAX_PITCH = 78; // d''
    private static final int MIDI_TO_PROJECT_OFFSET = 20;
    private static final int OCTAVE_SHIFT = 12;
    
    static {
        // Mapping ABC notes to MIDI pitch, then subtract 20 to get project pitch
        // Based on original Python parser: pitch = MIDI_note - 20
        // IMPORTANT: In ABC uppercase = octave 4, lowercase = octave 5, with apostrophe = octave 6
        
        // Octave 3 (very low, with comma) = MIDI octave 3
        NOTE_TO_PITCH.put("C,", 48 - 20);   // 28
        NOTE_TO_PITCH.put("D,", 50 - 20);   // 30
        NOTE_TO_PITCH.put("E,", 52 - 20);   // 32
        NOTE_TO_PITCH.put("F,", 53 - 20);   // 33
        NOTE_TO_PITCH.put("G,", 55 - 20);   // 35
        NOTE_TO_PITCH.put("A,", 57 - 20);   // 37
        NOTE_TO_PITCH.put("B,", 59 - 20);   // 39
        
        // Octave 4 (uppercase letters in ABC) = MIDI octave 4
        NOTE_TO_PITCH.put("C", 60 - 20);   // 40
        NOTE_TO_PITCH.put("^C", 61 - 20);  // 41 (C#)
        NOTE_TO_PITCH.put("_D", 61 - 20);  // 41 (Db)
        NOTE_TO_PITCH.put("D", 62 - 20);   // 42
        NOTE_TO_PITCH.put("^D", 63 - 20);  // 43 (D#)
        NOTE_TO_PITCH.put("_E", 63 - 20);  // 43 (Eb)
        NOTE_TO_PITCH.put("E", 64 - 20);   // 44
        NOTE_TO_PITCH.put("F", 65 - 20);   // 45
        NOTE_TO_PITCH.put("^F", 66 - 20);  // 46 (F#)
        NOTE_TO_PITCH.put("_G", 66 - 20);  // 46 (Gb)
        NOTE_TO_PITCH.put("G", 67 - 20);   // 47
        NOTE_TO_PITCH.put("^G", 68 - 20);  // 48 (G#)
        NOTE_TO_PITCH.put("_A", 68 - 20);  // 48 (Ab)
        NOTE_TO_PITCH.put("A", 69 - 20);   // 49
        NOTE_TO_PITCH.put("^A", 70 - 20);  // 50 (A#)
        NOTE_TO_PITCH.put("_B", 70 - 20);  // 50 (Bb)
        NOTE_TO_PITCH.put("B", 71 - 20);   // 51
        
        // Octave 5 (lowercase letters in ABC) = MIDI octave 5
        NOTE_TO_PITCH.put("c", 72 - 20);   // 52
        NOTE_TO_PITCH.put("^c", 73 - 20);  // 53 (c#)
        NOTE_TO_PITCH.put("_d", 73 - 20);  // 53 (db)
        NOTE_TO_PITCH.put("d", 74 - 20);   // 54 - start of D whistle range!
        NOTE_TO_PITCH.put("^d", 75 - 20);  // 55 (d#)
        NOTE_TO_PITCH.put("_e", 75 - 20);  // 55 (eb)
        NOTE_TO_PITCH.put("e", 76 - 20);   // 56
        NOTE_TO_PITCH.put("f", 77 - 20);   // 57
        NOTE_TO_PITCH.put("^f", 78 - 20);  // 58 (f#)
        NOTE_TO_PITCH.put("_g", 78 - 20);  // 58 (gb)
        NOTE_TO_PITCH.put("g", 79 - 20);   // 59
        NOTE_TO_PITCH.put("^g", 80 - 20);  // 60 (g#)
        NOTE_TO_PITCH.put("_a", 80 - 20);  // 60 (ab)
        NOTE_TO_PITCH.put("a", 81 - 20);   // 61
        NOTE_TO_PITCH.put("^a", 82 - 20);  // 62 (a#)
        NOTE_TO_PITCH.put("_b", 82 - 20);  // 62 (bb)
        NOTE_TO_PITCH.put("b", 83 - 20);   // 63
        
        // Octave 6 (with apostrophe) = MIDI octave 6
        NOTE_TO_PITCH.put("c'", 84 - 20);  // 64
        NOTE_TO_PITCH.put("^c'", 85 - 20); // 65 (c#')
        NOTE_TO_PITCH.put("_d'", 85 - 20); // 65 (db')
        NOTE_TO_PITCH.put("d'", 86 - 20);  // 66 - upper D (with +)
        NOTE_TO_PITCH.put("^d'", 87 - 20); // 67 (d#')
        NOTE_TO_PITCH.put("_e'", 87 - 20); // 67 (eb')
        NOTE_TO_PITCH.put("e'", 88 - 20);  // 68
        NOTE_TO_PITCH.put("f'", 89 - 20);  // 69
        NOTE_TO_PITCH.put("^f'", 90 - 20); // 70 (f#')
        NOTE_TO_PITCH.put("_g'", 90 - 20); // 70 (gb')
        NOTE_TO_PITCH.put("g'", 91 - 20);  // 71
        NOTE_TO_PITCH.put("^g'", 92 - 20); // 72 (g#')
        NOTE_TO_PITCH.put("_a'", 92 - 20); // 72 (ab')
        NOTE_TO_PITCH.put("a'", 93 - 20);  // 73
        NOTE_TO_PITCH.put("^a'", 94 - 20); // 74 (a#')
        NOTE_TO_PITCH.put("_b'", 94 - 20); // 74 (bb')
        NOTE_TO_PITCH.put("b'", 95 - 20);  // 75
        
        // Octave 7 (with two apostrophes) = MIDI octave 7
        NOTE_TO_PITCH.put("c''", 96 - 20); // 76
        NOTE_TO_PITCH.put("d''", 98 - 20); // 78 - end of D whistle range!
        NOTE_TO_PITCH.put("e''", 100 - 20); // 80
    }
    
    public static class ABCParseResult {
        public List<MusicNote> notes;
        public String key;
        public String meter;
        public int tempo;
        public String title;
        
        public ABCParseResult() {
            notes = new ArrayList<>();
            key = "D";
            meter = "4/4";
            tempo = DEFAULT_TEMPO;
            title = "";
        }
    }
    
    public static ABCParseResult parse(String abc) throws Exception {
        ABCParseResult result = new ABCParseResult();
        
        if (abc == null || abc.trim().isEmpty()) {
            throw new Exception("ABC notation is empty");
        }
        
        // Remove ornaments (~)
        abc = abc.replace("~", "");
        
        String[] lines = abc.split("\n");
        int defaultLength = DEFAULT_NOTE_LENGTH;
        TempoInfo tempoInfo = new TempoInfo(DEFAULT_TEMPO, 1, 4);
        Map<String, Integer> keySignature = new HashMap<>();
        
        for (String line : lines) {
            line = line.trim();
            
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("%")) {
                continue;
            }
            
            // Parse headers
            if (line.startsWith("T:")) {
                result.title = line.substring(2).trim();
                continue;
            }
            
            if (line.startsWith("R:")) {
                // Rhythm type is saved only for display, doesn't affect tempo
                continue;
            }
            
            if (line.startsWith("K:")) {
                result.key = line.substring(2).trim().split("\\s+")[0];
                keySignature = getKeySignature(result.key);
                continue;
            }
            
            if (line.startsWith("M:")) {
                result.meter = line.substring(2).trim();
                continue;
            }
            
            if (line.startsWith("L:")) {
                String lengthStr = line.substring(2).trim();
                defaultLength = parseFraction(lengthStr);
                continue;
            }
            
            if (line.startsWith("Q:")) {
                tempoInfo = parseTempo(line.substring(2).trim(), defaultLength);
                // For backward compatibility save BPM in result.tempo
                result.tempo = tempoInfo.bpm;
                continue;
            }
            
            // Skip other headers
            if (line.matches("^[A-Z]:.*")) {
                continue;
            }
            
            // Parse notes
            parseNoteLine(line, result.notes, defaultLength, tempoInfo, keySignature);
        }
        
        if (result.notes.isEmpty()) {
            throw new Exception("No notes found in ABC notation");
        }
        
        // Automatic transposition to D whistle range (54-78)
        // Log pitch before transposition
        int minBefore = Integer.MAX_VALUE, maxBefore = Integer.MIN_VALUE;
        StringBuilder pitchDebug = new StringBuilder("First 10 pitches: ");
        int debugCount = 0;
        for (MusicNote note : result.notes) {
            if (!note.isRest()) {
                int p = note.getPitch();
                if (p < minBefore) minBefore = p;
                if (p > maxBefore) maxBefore = p;
                if (debugCount < 10) {
                    pitchDebug.append(p).append(" ");
                    debugCount++;
                }
            }
        }
        System.out.println("ABC Parser: Key=" + result.key + ", Before transpose - min=" + minBefore + ", max=" + maxBefore);
        System.out.println(pitchDebug.toString());
        
        autoTranspose(result.notes);
        
        // Log pitch after transposition
        int minAfter = Integer.MAX_VALUE, maxAfter = Integer.MIN_VALUE;
        for (MusicNote note : result.notes) {
            if (!note.isRest()) {
                int p = note.getPitch();
                if (p < minAfter) minAfter = p;
                if (p > maxAfter) maxAfter = p;
            }
        }
        System.out.println("ABC Parser: After transpose - min=" + minAfter + ", max=" + maxAfter + ", shift=" + (minAfter - minBefore));
        
        android.util.Log.d("ABCParser", "Parse complete: " + result.notes.size() + " notes, tempo=" + result.tempo + " BPM, key=" + result.key);
        
        // Log last 5 notes for debugging
        int startIdx = Math.max(0, result.notes.size() - 5);
        for (int i = startIdx; i < result.notes.size(); i++) {
            MusicNote note = result.notes.get(i);
            android.util.Log.d("ABCParser", "Note[" + i + "]: pitch=" + note.getPitch() + 
                    ", duration=" + note.getLengthInMS(1.0f) + "ms, isRest=" + note.isRest());
        }
        
        return result;
    }
    
    private static void autoTranspose(List<MusicNote> notes) {
        PitchRange range = findPitchRange(notes);
        if (!range.isValid()) return;
        
        // Half-holed notes (chromatic notes)
        int[] halfHoled = {55, 57, 60, 62, 64, 67, 69, 72, 74, 76};
        
        // First try without half-holed notes
        int shift = findTransposition(notes, range.min, range.max, WHISTLE_MIN_PITCH, WHISTLE_MAX_PITCH, halfHoled, false);
        
        // If failed, allow half-holed notes
        if (shift == Integer.MIN_VALUE) {
            System.out.println("Warning: couldn't find transposition without half-holed notes, trying with...");
            shift = findTransposition(notes, range.min, range.max, WHISTLE_MIN_PITCH, WHISTLE_MAX_PITCH, halfHoled, true);
        }
        
        if (shift != Integer.MIN_VALUE && shift != 0) {
            transposeNotes(notes, shift);
        }
    }
    
    private static class PitchRange {
        final int min;
        final int max;
        
        PitchRange(int min, int max) {
            this.min = min;
            this.max = max;
        }
        
        boolean isValid() {
            return min != Integer.MAX_VALUE;
        }
    }
    
    private static PitchRange findPitchRange(List<MusicNote> notes) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        
        for (MusicNote note : notes) {
            if (!note.isRest()) {
                int pitch = note.getPitch();
                if (pitch < min) min = pitch;
                if (pitch > max) max = pitch;
            }
        }
        
        return new PitchRange(min, max);
    }
    
    private static int findTransposition(List<MusicNote> notes, int min, int max, 
                                        int whistleMin, int whistleMax, 
                                        int[] halfHoled, boolean allowHalfHoled) {
        // If already in range and no half-holed notes (or they are allowed)
        if (min >= whistleMin && max <= whistleMax) {
            if (allowHalfHoled || !hasHalfHoled(notes, 0, halfHoled)) {
                return 0;
            }
        }
        
        // Try octave up (+12) - works in most cases
        if (min + 12 >= whistleMin && max + 12 <= whistleMax) {
            if (allowHalfHoled || !hasHalfHoled(notes, 12, halfHoled)) {
                return 12;
            }
        }
        
        // Try octave down (-12)
        if (min - 12 >= whistleMin && max - 12 <= whistleMax) {
            if (allowHalfHoled || !hasHalfHoled(notes, -12, halfHoled)) {
                return -12;
            }
        }
        
        // Automatic semitone search - try all shifts in range
        // Start from -24 to +24 to cover all possibilities
        for (int shift = -24; shift <= 24; shift++) {
            if (min + shift >= whistleMin && max + shift <= whistleMax) {
                if (allowHalfHoled || !hasHalfHoled(notes, shift, halfHoled)) {
                    if (shift != 0 && shift != 12 && shift != -12) {
                        System.out.println("Warning: shift is not perfect octave (" + shift + ")");
                    }
                    return shift;
                }
            }
        }
        
        // If no suitable transposition found
        return Integer.MIN_VALUE;
    }
    
    private static boolean hasHalfHoled(List<MusicNote> notes, int shift, int[] halfHoled) {
        for (MusicNote note : notes) {
            if (!note.isRest()) {
                int transposedPitch = note.getPitch() + shift;
                for (int hh : halfHoled) {
                    if (transposedPitch == hh) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private static void transposeNotes(List<MusicNote> notes, int shift) {
        for (MusicNote note : notes) {
            note.transpose(shift);
        }
    }
    
    private static Map<String, Integer> getKeySignature(String key) {
        Map<String, Integer> signature = new HashMap<>();
        
        // Normalize key to uppercase for comparison
        String normalizedKey = key.toUpperCase().trim();
        
        // Determine number of sharps/flats for main keys
        // For major and minor keys
        switch (normalizedKey) {
            // Minor keys (check FIRST to avoid confusion with major)
            case "EMINOR":
            case "EMIN":
            case "EM":
                // E minor = G major (1 sharp: F#)
                signature.put("F", 1);
                break;
            case "BMINOR":
            case "BMIN":
            case "BM":
                // B minor = D major (2 sharps: F#, C#)
                signature.put("F", 1);
                signature.put("C", 1);
                break;
            case "AMINOR":
            case "AMIN":
            case "AM":
                // A minor = C major (no sharps/flats)
                break;
            case "DMINOR":
            case "DMIN":
            case "DM":
                // D minor = F major (1 flat: Bb)
                signature.put("B", -1);
                break;
            case "GMINOR":
            case "GMIN":
            case "GM":
                // G minor = Bb major (2 flats: Bb, Eb)
                signature.put("B", -1);
                signature.put("E", -1);
                break;
            case "FMINOR":
            case "FMIN":
            case "FM":
                // F minor = Ab major (4 flats: Bb, Eb, Ab, Db)
                signature.put("B", -1);
                signature.put("E", -1);
                signature.put("A", -1);
                signature.put("D", -1);
                break;
            case "CMINOR":
            case "CMIN":
            case "CM":
                // C minor = Eb major (3 flats: Bb, Eb, Ab)
                signature.put("B", -1);
                signature.put("E", -1);
                signature.put("A", -1);
                break;
                
            // Major keys with sharps
            case "GMAJOR":
            case "GMAJ":
            case "G": // 1 sharp: F#
                signature.put("F", 1);
                break;
            case "DMAJOR":
            case "DMAJ":
            case "D": // 2 sharps: F#, C#
                signature.put("F", 1);
                signature.put("C", 1);
                break;
            case "AMAJOR":
            case "AMAJ":
            case "A": // 3 sharps: F#, C#, G#
                signature.put("F", 1);
                signature.put("C", 1);
                signature.put("G", 1);
                break;
            case "EMAJOR":
            case "EMAJ":
            case "E": // 4 диеза: F#, C#, G#, D#
                signature.put("F", 1);
                signature.put("C", 1);
                signature.put("G", 1);
                signature.put("D", 1);
                break;
            case "BMAJOR":
            case "BMAJ":
            case "B": // 5 sharps: F#, C#, G#, D#, A#
                signature.put("F", 1);
                signature.put("C", 1);
                signature.put("G", 1);
                signature.put("D", 1);
                signature.put("A", 1);
                break;
                
            // Major keys with flats
            case "FMAJOR":
            case "FMAJ":
            case "F": // 1 flat: Bb
                signature.put("B", -1);
                break;
            case "BBMAJOR":
            case "BBMAJ":
            case "BB": // 2 flats: Bb, Eb
                signature.put("B", -1);
                signature.put("E", -1);
                break;
            case "EBMAJOR":
            case "EBMAJ":
            case "EB": // 3 flats: Bb, Eb, Ab
                signature.put("B", -1);
                signature.put("E", -1);
                signature.put("A", -1);
                break;
                
            // C major - no sharps/flats
            case "CMAJOR":
            case "CMAJ":
            case "C":
            default:
                break;
        }
        
        return signature;
    }
    
    private static void parseNoteLine(String line, List<MusicNote> notes, int defaultLength, TempoInfo tempoInfo, Map<String, Integer> keySignature) {
        String originalLine = line;
        // Remove bars and repeats
        line = line.replaceAll("[|:\\[\\]\\{\\}]", " ");
        android.util.Log.v("ABCParser", "Parsing line: '" + originalLine + "' -> '" + line + "'");
        
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            
            // Skip spaces
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            
            // Rest
            if (c == 'z' || c == 'x') {
                int length = parseNoteLength(line, i + 1, defaultLength);
                int duration = tempoInfo.calculateDuration(length);
                notes.add(new MusicNote(0, duration)); // 0 = rest
                i = skipLength(line, i + 1);
                continue;
            }
            
            // Note
            if ((c >= 'A' && c <= 'G') || (c >= 'a' && c <= 'g')) {
                StringBuilder noteStr = new StringBuilder();
                boolean hasExplicitAccidental = false;
                
                // Check modifier (sharp/flat/natural)
                if (i > 0 && (line.charAt(i - 1) == '^' || line.charAt(i - 1) == '_' || line.charAt(i - 1) == '=')) {
                    noteStr.append(line.charAt(i - 1));
                    hasExplicitAccidental = true;
                }
                
                // Save base note (without octave)
                char baseNote = c;
                String baseNoteName = String.valueOf(Character.toUpperCase(baseNote));
                
                noteStr.append(c);
                i++;
                
                // Check octave (apostrophe or comma)
                while (i < line.length() && (line.charAt(i) == '\'' || line.charAt(i) == ',')) {
                    noteStr.append(line.charAt(i));
                    i++;
                }
                
                // Parse duration
                int length = parseNoteLength(line, i, defaultLength);
                i = skipLength(line, i);
                
                // Get pitch
                Integer pitch = NOTE_TO_PITCH.get(noteStr.toString());
                
                // If no explicit accidental, apply key signature
                if (!hasExplicitAccidental && keySignature.containsKey(baseNoteName)) {
                    int accidental = keySignature.get(baseNoteName);
                    if (accidental == 1) {
                        // Sharp - look for version with ^
                        String sharpNote = "^" + noteStr.toString();
                        Integer sharpPitch = NOTE_TO_PITCH.get(sharpNote);
                        if (sharpPitch != null) {
                            pitch = sharpPitch;
                        }
                    } else if (accidental == -1) {
                        // Flat - look for version with _
                        String flatNote = "_" + noteStr.toString();
                        Integer flatPitch = NOTE_TO_PITCH.get(flatNote);
                        if (flatPitch != null) {
                            pitch = flatPitch;
                        }
                    }
                }
                
                if (pitch == null) {
                    // Try without modifier
                    pitch = NOTE_TO_PITCH.get(noteStr.toString().replaceAll("[\\^_=]", ""));
                }
                
                if (pitch != null) {
                    int duration = tempoInfo.calculateDuration(length);
                    notes.add(new MusicNote(pitch, duration));
                    android.util.Log.v("ABCParser", "Added note: " + noteStr + " -> pitch=" + pitch + ", duration=" + duration + "ms, total notes=" + notes.size());
                } else {
                    android.util.Log.w("ABCParser", "Failed to parse note: " + noteStr);
                }
                continue;
            }
            
            // Skip other characters
            i++;
        }
    }
    
    private static int parseNoteLength(String line, int startPos, int defaultLength) {
        if (startPos >= line.length()) {
            return defaultLength;
        }
        
        char c = line.charAt(startPos);
        
        // Check fraction (e.g., /2, /4)
        if (c == '/') {
            int endPos = startPos + 1;
            while (endPos < line.length() && Character.isDigit(line.charAt(endPos))) {
                endPos++;
            }
            if (endPos > startPos + 1) {
                int divisor = Integer.parseInt(line.substring(startPos + 1, endPos));
                return defaultLength / divisor;
            }
            return defaultLength / 2; // Just / means half
        }
        
        // Check number (e.g., 2, 3, 4)
        if (Character.isDigit(c)) {
            int endPos = startPos;
            while (endPos < line.length() && Character.isDigit(line.charAt(endPos))) {
                endPos++;
            }
            int multiplier = Integer.parseInt(line.substring(startPos, endPos));
            return defaultLength * multiplier;
        }
        
        return defaultLength;
    }
    
    private static int skipLength(String line, int startPos) {
        if (startPos >= line.length()) {
            return startPos;
        }
        
        char c = line.charAt(startPos);
        
        if (c == '/') {
            startPos++;
            while (startPos < line.length() && Character.isDigit(line.charAt(startPos))) {
                startPos++;
            }
            return startPos;
        }
        
        if (Character.isDigit(c)) {
            while (startPos < line.length() && Character.isDigit(line.charAt(startPos))) {
                startPos++;
            }
            return startPos;
        }
        
        return startPos;
    }
    

    
    private static int parseFraction(String fraction) {
        if (fraction.contains("/")) {
            String[] parts = fraction.split("/");
            int numerator = Integer.parseInt(parts[0].trim());
            int denominator = Integer.parseInt(parts[1].trim());
            return (8 * numerator) / denominator;
        }
        return DEFAULT_NOTE_LENGTH;
    }
    
    private static class TempoInfo {
        int bpm;              // Beats per minute
        int beatNumerator;    // Numerator of beat duration (e.g., 3 for 3/8)
        int beatDenominator;  // Denominator of beat duration (e.g., 8 for 3/8)
        
        TempoInfo(int bpm, int beatNumerator, int beatDenominator) {
            this.bpm = bpm;
            this.beatNumerator = beatNumerator;
            this.beatDenominator = beatDenominator;
        }
        
        // Calculate duration in milliseconds for note with given length
        int calculateDuration(int noteLength) {
            // noteLength in our system: 8 = 1/8 note, 16 = 1/4 note, etc.
            // beatNumerator/beatDenominator - duration of one beat
            
            // Duration of one beat in milliseconds
            double beatDurationMs = 60000.0 / bpm;
            
            // Beat length in our system (where 8 = 1/8)
            // For example: 3/8 = 3 * (8/8) = 3 in our system
            double beatLengthInOurSystem = beatNumerator * (8.0 / beatDenominator);
            
            // Duration of one unit of our system in milliseconds
            double unitDurationMs = beatDurationMs / beatLengthInOurSystem;
            
            // Note duration
            return (int) (unitDurationMs * noteLength);
        }
    }
    
    private static TempoInfo parseTempo(String tempoStr, int defaultLength) {
        // Format: "1/4=120" or "3/8=110" or just "120"
        Pattern pattern = Pattern.compile("(\\d+)/(\\d+)\\s*=\\s*(\\d+)");
        Matcher matcher = pattern.matcher(tempoStr);
        
        if (matcher.find()) {
            int numerator = Integer.parseInt(matcher.group(1));
            int denominator = Integer.parseInt(matcher.group(2));
            int bpm = Integer.parseInt(matcher.group(3));
            
            android.util.Log.d("ABCParser", "Parsed tempo: " + numerator + "/" + denominator + "=" + bpm);
            return new TempoInfo(bpm, numerator, denominator);
        }
        
        // Simple format "120" - assume quarter notes
        Pattern simplePattern = Pattern.compile("(\\d+)");
        Matcher simpleMatcher = simplePattern.matcher(tempoStr);
        if (simpleMatcher.find()) {
            int bpm = Integer.parseInt(simpleMatcher.group(1));
            android.util.Log.d("ABCParser", "Parsed simple tempo: " + bpm + " (assuming 1/4 notes)");
            return new TempoInfo(bpm, 1, 4);
        }
        
        // Default - quarter notes
        return new TempoInfo(DEFAULT_TEMPO, 1, 4);
    }
    

}
