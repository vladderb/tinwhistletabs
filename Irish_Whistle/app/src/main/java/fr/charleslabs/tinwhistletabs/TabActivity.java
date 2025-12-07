package fr.charleslabs.tinwhistletabs;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import java.util.List;

import fr.charleslabs.tinwhistletabs.android.SingleTapTouchListener;
import fr.charleslabs.tinwhistletabs.android.TextViewScaleGestureDetector;
import fr.charleslabs.tinwhistletabs.dialogs.ABCDialog;
import fr.charleslabs.tinwhistletabs.dialogs.KeyDialog;
import fr.charleslabs.tinwhistletabs.dialogs.SheetInfoDialog;
import fr.charleslabs.tinwhistletabs.dialogs.TempoDialog;
import fr.charleslabs.tinwhistletabs.music.CustomSongsManager;
import fr.charleslabs.tinwhistletabs.music.MusicDB;
import fr.charleslabs.tinwhistletabs.music.TrashManager;
import fr.charleslabs.tinwhistletabs.music.MusicNote;
import fr.charleslabs.tinwhistletabs.music.MusicPlayer;
import fr.charleslabs.tinwhistletabs.music.MusicSettings;
import fr.charleslabs.tinwhistletabs.music.MusicSheet;
import fr.charleslabs.tinwhistletabs.utils.AndroidUtils;

public class TabActivity extends AppCompatActivity implements TempoDialog.TempoChangeCallback,
        KeyDialog.KeyChangeCallback, SingleTapTouchListener.SingleTapCallback {
    public static final String EXTRA_ABC= "fr.charleslabs.tinwhistletabs.ABC";
    public static final String EXTRA_SHEET_TITLE= "fr.charleslabs.tinwhistletabs.SHEET_TITLE";
    public static final float START_DELAY_AMOUNT = 1.5f; // s
    private static final int COUNTDOWN_STEPS = 3;
    private static final int SCROLL_DURATION = 750; // ms

    // States
    private  boolean isPlaying = false;
    private MusicSheet sheet = null;
    private int tempo = MusicSettings.DEFAULT_TEMPO; // Current tempo in BPM
    private Handler musicHandler = new Handler();
    private List<MusicNote> notes;
    private int scroll_value = -1;

    // UI elements
    private ScrollView scrollView;
    private Spannable span = null;
    private  TextView tab = null;
    private  TextView countdownOverlay = null;
    private WebView sheetMusicView = null;
    private boolean isSheetMusicVisible = false;

    // Zoom
    private ScaleGestureDetector mScaleDetector;

    // Cursor
    private int cursorPos = 0;  // Position in tablature text
    private int currentNoteIndex = 0;  // Index of current note in notes array
    private int startCursorPos = 0;  // Start position for highlighting
    private int startNoteIndex = 0;  // Start note index

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab);

        // Get data
        final Intent intent = getIntent();
        if (!intent.hasExtra(MainActivity.EXTRA_SHEET)) finish();
        sheet = (MusicSheet)intent.getSerializableExtra(MainActivity.EXTRA_SHEET);

        // Set action bar title
        try{
            ActionBar actionBar = this.getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(sheet.getTitle());
        }catch (Exception ignored){}

        // Sheet UI
        tab = findViewById(R.id.TabActivity_tab);
        countdownOverlay = findViewById(R.id.TabActivity_countdown);
        sheetMusicView = findViewById(R.id.TabActivity_sheetMusic);
        
        // Setup WebView for sheet music
        WebSettings webSettings = sheetMusicView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        
        // Use hardware rendering to match TextView rendering
        sheetMusicView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // Add JavaScript interface for note clicks
        sheetMusicView.addJavascriptInterface(new WebAppInterface(), "Android");
        
        try {
            android.util.Log.d("TabActivity", "Opening track: " + sheet.getFile());
            android.util.Log.d("TabActivity", "Track title: " + sheet.getTitle());
            android.util.Log.d("TabActivity", "Has ABC: " + (sheet.getABC() != null));
            notes = MusicDB.open(this, sheet.getFile());
            android.util.Log.d("TabActivity", "Successfully loaded " + notes.size() + " notes");
        } catch (Exception e) {
            android.util.Log.e("TabActivity", "Error opening track: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading track: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
        scrollView = findViewById(R.id.TabActivity_tabScrollPane);
        
        String tabText = sheet.notesToTabsWithLineBreaks(notes);
        
        // LOG: Show tabs on main screen
        android.util.Log.d("TAB_MAIN_SCREEN", "=== MAIN SCREEN TABS ===");
        android.util.Log.d("TAB_MAIN_SCREEN", "Tab text length: " + tabText.length());
        android.util.Log.d("TAB_MAIN_SCREEN", "First 50 chars: " + tabText.substring(0, Math.min(50, tabText.length())));
        
        // LOG: Show first 20 characters with their codes
        int charCount = 0;
        for (int i = 0; i < tabText.length() && charCount < 20; i++) {
            char c = tabText.charAt(i);
            if (c != '\n' && c != ' ') {
                charCount++;
                android.util.Log.d("TAB_MAIN_SCREEN", "Char #" + charCount + ": '" + c + "' (code: " + (int)c + ")");
            }
        }
        
        tab.setText(tabText, TextView.BufferType.SPANNABLE);
        span = (Spannable)tab.getText();
        
        // Load sheet music if ABC is available
        if (sheet.getABC() != null && !sheet.getABC().isEmpty()) {
            loadSheetMusic();
        }

        // Reset tempo to default for each new track
        tempo = MusicSettings.DEFAULT_TEMPO;

        // Media buttons
        findViewById(R.id.TabActivity_btnPlayPause).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                playPause();
            }
        });
        findViewById(R.id.TabActivity_btnStop).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stop();
            }
        });
        
        // Tempo button
        findViewById(R.id.TabActivity_btnTempo).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final DialogFragment tempoDialog = new TempoDialog(tempo, MusicSettings.isStartDelayed, TabActivity.this);
                tempoDialog.show(getSupportFragmentManager(),"dialog");
            }
        });
        
        // ABC button with text
        com.google.android.material.floatingactionbutton.FloatingActionButton abcBtn = 
                findViewById(R.id.TabActivity_btnABC);
        abcBtn.setImageDrawable(AndroidUtils.createTextDrawable(this, "ABC", 
                ContextCompat.getColor(this, R.color.md_theme_primary)));
        abcBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stop();
                showABCDialog();
            }
        });
        
        findViewById(R.id.TabActivity_btnInfo).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleSheetMusic();
            }
        });
        this.setTune();

        // Scale gesture
        mScaleDetector = new ScaleGestureDetector(this, new TextViewScaleGestureDetector(tab));
        // Tap on TextView
        tab.setOnTouchListener(new SingleTapTouchListener(this));
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.tabAction_key:
                final DialogFragment keyDialog = new KeyDialog(MusicSettings.currentKey, this);
                keyDialog.show(getSupportFragmentManager(),"dialog");
                break;
            case R.id.tabAction_delete:
                showDeleteConfirmation();
                break;
            case R.id.tabAction_more:
                final SheetInfoDialog sheetInfoDialog = new SheetInfoDialog(getApplicationContext(), sheet);
                sheetInfoDialog.show(getSupportFragmentManager(),"dialog");
                break;
            case android.R.id.home:
                this.stop();
                finish();
                break;
        }
        return true;
    }

    private void setTune(){
        try {
            // For all tracks use multiplier 1.0 - notes already have correct duration
            MusicPlayer.getInstance().setAudioTrack(MusicPlayer.genMusic(notes, 1.0f));
            findViewById(R.id.TabActivity_btnPlayPause).setEnabled(true);
            findViewById(R.id.TabActivity_btnStop).setEnabled(true);
        }catch (Exception e){
            Toast.makeText(this,getString(R.string.error_tune_generation,e.getMessage()),Toast.LENGTH_SHORT).show();
            findViewById(R.id.TabActivity_btnPlayPause).setEnabled(false);
            findViewById(R.id.TabActivity_btnStop).setEnabled(false);
        }
    }
    private void playPause(){
        com.google.android.material.floatingactionbutton.FloatingActionButton playPauseBtn = 
                findViewById(R.id.TabActivity_btnPlayPause);
        
        if(!isPlaying){
            isPlaying = true;
            playPauseBtn.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_media_pause));
            if (!MusicSettings.isStartDelayed)
                play();
            else{
                drawCursor(true);
                countdown(COUNTDOWN_STEPS-1);
            }
        } else {
            musicHandler.removeCallbacksAndMessages(null);
            MusicPlayer.getInstance().pause();
            countdownOverlay.setVisibility(View.GONE);
            isPlaying = false;
            playPauseBtn.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_media_play));
            
            // Disable auto-scroll in WebView
            if (sheetMusicView != null && isSheetMusicVisible) {
                sheetMusicView.evaluateJavascript("if(typeof setPlayingMode === 'function') setPlayingMode(false);", null);
            }
        }
    }

    private void countdown(final int stepsLeft){
        countdownOverlay.setVisibility(View.VISIBLE);
        countdownOverlay.setText(Integer.toString(stepsLeft+1));
        musicHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(stepsLeft <= 0){
                    countdownOverlay.setVisibility(View.GONE);
                    play();
                }
                else
                    countdown(stepsLeft - 1);
            }
        }, (long)((START_DELAY_AMOUNT * 1000)/COUNTDOWN_STEPS));
    }

    private void play(){
        scroll_value = -1; // invalidate scroll value
        // Remember start positions
        startCursorPos = cursorPos;
        startNoteIndex = currentNoteIndex;
        MusicPlayer.getInstance().move(MusicSheet.noteIndexToTime(notes, currentNoteIndex, 1.0f));
        moveCursor(musicHandler, currentNoteIndex);
        MusicPlayer.getInstance().play();
        
        // Enable auto-scroll in WebView
        if (sheetMusicView != null && isSheetMusicVisible) {
            sheetMusicView.evaluateJavascript("if(typeof setPlayingMode === 'function') setPlayingMode(true);", null);
        }
    }

    private void stop(){
        countdownOverlay.setVisibility(View.GONE);
        cursorPos = 0;
        currentNoteIndex = 0;
        startCursorPos = 0;
        startNoteIndex = 0;
        scroll_value = -1; // invalidate scroll value
        musicHandler.removeCallbacksAndMessages(null);
        AndroidUtils.clearSpans(span);
        MusicPlayer.getInstance().stop();
        isPlaying = false;
        
        // Reset highlighting in WebView and disable auto-scroll
        if (sheetMusicView != null && isSheetMusicVisible) {
            sheetMusicView.evaluateJavascript("if(typeof clearHighlight === 'function') clearHighlight();", null);
            sheetMusicView.evaluateJavascript("if(typeof setPlayingMode === 'function') setPlayingMode(false);", null);
        }
        
        // Restore play icon
        com.google.android.material.floatingactionbutton.FloatingActionButton playPauseBtn = 
                findViewById(R.id.TabActivity_btnPlayPause);
        playPauseBtn.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_media_play));
    }

    private void moveCursor(final Handler handler, final int index){
        if (index >= notes.size()){
            android.util.Log.d("TabActivity", "moveCursor: reached end, index=" + index + ", notes.size=" + notes.size());
            stop();
            return;
        }

        MusicNote note = notes.get(index);
        android.util.Log.v("TabActivity", "moveCursor: index=" + index + ", cursorPos=" + cursorPos + 
                ", isRest=" + note.isRest() + ", pitch=" + note.getPitch() + 
                ", duration=" + note.getLengthInMS(1.0f) + "ms");

        // Highlight note in sheet music view
        highlightNoteInSheet(index);

        // Move cursor in text tab
        if(!note.isRest()) {
            // Find position in text for current note
            String text = tab.getText().toString();
            
            // Skip line breaks and spaces from current position
            while (cursorPos < text.length() && (text.charAt(cursorPos) == '\n' || text.charAt(cursorPos) == ' ')) {
                cursorPos++;
            }
            
            drawCursor(true);
            
            // Move to next note
            if (cursorPos < text.length()) {
                cursorPos++;
            }
        }
        
        // Wait for next note
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index < notes.size() - 1)
                    moveCursor(handler,index+1);
                else {
                    android.util.Log.d("TabActivity", "moveCursor: finished playback");
                    stop();
                }
            }
        }, (long)(note.getLengthInMS(1.0f)));
    }

    private void drawCursor(final boolean scroll){
        try {
                // Clear previous spans
                AndroidUtils.clearSpans(span);
                // Highlight all played notes from start position to current
                span.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorAccent)),
                        startCursorPos, cursorPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                final Layout layout = tab.getLayout();
                final int current_scroll = layout.getLineTop(layout.getLineForOffset(cursorPos));
                if (scroll && current_scroll != scroll_value) {
                    scroll_value = current_scroll;
                    try {
                        ObjectAnimator.ofInt(scrollView, "scrollY",current_scroll)
                                .setDuration(SCROLL_DURATION).start();
                    } catch (Exception e) {
                        scrollView.scrollTo(0, current_scroll);
                    }
                }
        } catch(Exception ignored){}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tab_menu, menu);
        
        // Show delete button for all tracks
        MenuItem deleteItem = menu.findItem(R.id.tabAction_delete);
        if (deleteItem != null) {
            deleteItem.setVisible(true);
        }
        
        return true;
    }

    // Settings callbacks
    @Override
    public void tempoChangeCallback(int newTempo, boolean isDelayApplied) {
        if(newTempo != tempo) {
            this.stop();
            tempo = newTempo;
            this.setTune();
        }
        MusicSettings.isStartDelayed = isDelayApplied;
    }
    @Override
    public void keyChangeCallback(String newKey) {
        if(!newKey.equals(MusicSettings.currentKey)) {
            this.stop();
            this.sheet.transposeKey(notes, MusicSettings.currentKey, newKey);
            MusicSettings.currentKey = newKey;
            this.setTune();
        }
    }



    // Scale tab on pinch
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        super.dispatchTouchEvent(event);
        mScaleDetector.onTouchEvent(event);
        return true;
    }

    // Single tap in tab
    @Override
    public void singleTapCallback(SingleTapTouchListener origin, View v, MotionEvent event) {
        final int charIndex = AndroidUtils.getCharacterOffset((TextView) v,(int) event.getX(),(int) event.getY());
        if (charIndex < 0) return;
        
        // Convert character position to note index
        // Count only note characters (skipping line breaks and spaces)
        String text = ((TextView) v).getText().toString();
        int noteIndex = 0;
        
        // Count notes up to and including charIndex position
        for (int i = 0; i <= charIndex && i < text.length(); i++) {
            char c = text.charAt(i);
            // Skip line breaks and spaces
            if (c != '\n' && c != ' ') {
                if (i < charIndex) {
                    noteIndex++;
                }
            }
        }
        
        // Check that index is within notes array bounds
        if (noteIndex >= notes.size()) {
            android.util.Log.w("TabActivity", "Note index " + noteIndex + " out of bounds, clamping to " + (notes.size() - 1));
            noteIndex = notes.size() - 1;
        }
        
        android.util.Log.d("TabActivity", "Clicked at charIndex=" + charIndex + ", converted to noteIndex=" + noteIndex);
        
        this.stop();
        // Save both positions
        cursorPos = charIndex;  // Position in text for display
        currentNoteIndex = noteIndex;  // Note index for playback
        AndroidUtils.clearSpans(span);
        drawCursor(false);
        MusicPlayer.getInstance().move(MusicSheet.noteIndexToTime(notes, noteIndex, 1.0f));
    }

    // Back capture
    @Override
    public void onBackPressed(){
        this.stop();
        super.onBackPressed();
    }
    
    private void showABCDialog() {
        String abc = sheet.getABC();
        if (abc == null || abc.isEmpty()) {
            Toast.makeText(this, "ABC notation not available for this song", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if this is a custom song
        boolean isCustomSong = sheet.getFile().startsWith("custom_");
        
        ABCDialog dialog = new ABCDialog(
            sheet.getTitle(), 
            abc, 
            sheet.getFile(),
            isCustomSong,
            (newNotes) -> {
                // Callback on save - update notes right here
                reloadNotes(newNotes);
            }
        );
        dialog.show(getSupportFragmentManager(), "abc_dialog");
    }
    
    private void reloadNotes(List<MusicNote> newNotes) {
        // Stop playback
        stop();
        
        // Update notes
        notes = newNotes;
        
        // Update tablature display
        tab.setText(sheet.notesToTabsWithLineBreaks(notes), TextView.BufferType.SPANNABLE);
        span = (Spannable)tab.getText();
        
        // Apply current key
        sheet.transposeKey(notes, sheet.getKey(), MusicSettings.currentKey);
        
        // Update audio track
        setTune();
        
        // Reset cursor
        cursorPos = 0;
        
        Toast.makeText(this, "Song updated successfully!", Toast.LENGTH_SHORT).show();
    }
    
    private void showDeleteConfirmation() {
        String message;
        if (sheet.getFile().startsWith("builtin_")) {
            message = "Are you sure you want to hide \"" + sheet.getTitle() + "\"? You can restore it from the trash.";
        } else {
            message = "Are you sure you want to delete \"" + sheet.getTitle() + "\"?";
        }
        
        new AlertDialog.Builder(this)
                .setTitle("Delete Song")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteSong();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteSong() {
        try {
            if (sheet.getFile().startsWith("custom_")) {
                // Custom track - delete physically
                CustomSongsManager manager = new CustomSongsManager(this);
                manager.deleteSong(sheet.getFile());
                Toast.makeText(this, "Song deleted successfully", Toast.LENGTH_SHORT).show();
            } else if (sheet.getFile().startsWith("builtin_")) {
                // Built-in track - move to trash
                TrashManager.moveToTrash(this, sheet.getFile(), sheet.getTitle());
                Toast.makeText(this, "Song moved to trash", Toast.LENGTH_SHORT).show();
            }
            
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error deleting song: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }
    
    private void loadSheetMusic() {
        if (sheet.getABC() == null || sheet.getABC().isEmpty()) {
            return;
        }
        
        final String abc = escape(sheet.getABC());
        // Generate tablature from already transposed notes (same as main screen)
        // This ensures tabs match between screens
        final String tablature = MusicSheet.notesToTabs(notes);
        final String escapedTab = escape(tablature);
        
        // LOG: Show generated tabs
        android.util.Log.d("TAB_DEBUG", "=== JAVA TAB GENERATION ===");
        android.util.Log.d("TAB_DEBUG", "Total notes: " + notes.size());
        android.util.Log.d("TAB_DEBUG", "Generated tablature length: " + tablature.length());
        android.util.Log.d("TAB_DEBUG", "First 50 chars: " + tablature.substring(0, Math.min(50, tablature.length())));
        
        // LOG: Show first 20 notes with their pitches and tabs
        for (int i = 0; i < Math.min(20, notes.size()); i++) {
            MusicNote note = notes.get(i);
            String tab = note.toTab();
            android.util.Log.d("TAB_DEBUG", "Note #" + (i+1) + ": pitch=" + note.getPitch() + 
                    ", tab='" + tab + "' (char code: " + (tab.isEmpty() ? "rest" : (int)tab.charAt(0)) + ")");
        }
        
        // Clear cache and history
        sheetMusicView.clearCache(true);
        sheetMusicView.clearHistory();
        
        // Add timestamp to avoid caching
        long timestamp = System.currentTimeMillis();
        sheetMusicView.loadUrl("file:///android_asset/sheet_simple.html?v=" + timestamp);
        sheetMusicView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView web, String url) {
                // Pass tablature from Java (already transposed, no half-holes)
                web.loadUrl("javascript:renderMusicWithTab(\"" + abc + "\", \"" + escapedTab + "\");");
                

            }
        });
    }
    
    private void highlightNoteInSheet(int noteIndex) {
        if (isSheetMusicVisible && sheetMusicView != null) {
            sheetMusicView.loadUrl("javascript:highlightNote(" + noteIndex + ");");
        }
    }
    
    private void toggleSheetMusic() {
        if (sheet.getABC() == null || sheet.getABC().isEmpty()) {
            Toast.makeText(this, "Sheet music not available for this song", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        isSheetMusicVisible = !isSheetMusicVisible;
        
        if (isSheetMusicVisible) {
            // Show sheet music with tablature, hide text tablature
            sheetMusicView.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.GONE);
        } else {
            // Show text tablature, hide sheet music
            sheetMusicView.setVisibility(View.GONE);
            scrollView.setVisibility(View.VISIBLE);
        }
        
        // Update button icon
        com.google.android.material.floatingactionbutton.FloatingActionButton infoBtn = 
                findViewById(R.id.TabActivity_btnInfo);
        if (isSheetMusicVisible) {
            infoBtn.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_close_clear_cancel));
        } else {
            infoBtn.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_music_note));
        }
    }
    
    // JavaScript interface for WebView
    public class WebAppInterface {
        @JavascriptInterface
        public void onNoteClicked(int noteIndex) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    android.util.Log.d("TabActivity", "Note clicked in sheet: " + noteIndex);
                    stop();
                    currentNoteIndex = noteIndex;
                    
                    // Convert note index to text position
                    String text = tab.getText().toString();
                    int textPos = 0;
                    int noteCount = 0;
                    
                    for (int i = 0; i < text.length() && noteCount < noteIndex; i++) {
                        char c = text.charAt(i);
                        if (c != '\n' && c != ' ') {
                            noteCount++;
                            textPos = i + 1;
                        }
                    }
                    
                    cursorPos = textPos;
                    highlightNoteInSheet(noteIndex);
                    MusicPlayer.getInstance().move(MusicSheet.noteIndexToTime(notes, noteIndex, 1.0f));
                }
            });
        }
    }
    
    /**
     * Escape a String to make it safe for JavaScript.
     * Escapes backslashes, quotes, and special characters.
     */
    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\f", "\\f")
                .replace("'", "\\'")
                .replace("\"", "\\\"");
    }

}
