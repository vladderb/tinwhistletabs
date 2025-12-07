package fr.charleslabs.tinwhistletabs.music;

/**
 * Contains various music parameters defaults.
 * Non-instantiatable class.
 */
public final class MusicSettings {
    private MusicSettings(){}

    // TEMPO (в BPM - ударов в минуту)
    final public static int DEFAULT_TEMPO = 100;
    final public static int MIN_TEMPO = 40, MAX_TEMPO = 240;

    // KEYS
    final public static String DEFAULT_KEY = "High D";
    final public static String[] WHISTLE_KEYS = {"High G","High F","High E","High Eb","High D","High Db","High C","B","Bb","Low A","Low G#","Low G","Low F#","Low F","Low E","Low Eb","Low D","Low C"};
    private final static int[] WHISTLE_OFFSET_D = {5,3,2,1,0,-1,-2,-3,-4,-5,-6,-7,-8,-9,-10,-11,-12,-14}; // Offset from High D
    public static String currentKey = DEFAULT_KEY;
    public static boolean isStartDelayed = false;
    // Functions
    public static int getShift(String key){
        return MusicSettings.WHISTLE_OFFSET_D[java.util.Arrays.asList(MusicSettings.WHISTLE_KEYS).indexOf(key)];
    }
}
