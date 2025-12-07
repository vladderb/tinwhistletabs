package fr.charleslabs.tinwhistletabs.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.charleslabs.tinwhistletabs.R;
import fr.charleslabs.tinwhistletabs.music.ABCParser;
import fr.charleslabs.tinwhistletabs.music.MusicNote;

public class AddCustomSongDialog extends DialogFragment {
    
    private EditText titleInput;
    private EditText authorInput;
    private Spinner typeSpinner;
    private EditText abcInput;
    private AddSongCallback callback;
    private boolean titleManuallyEdited = false;
    
    public interface AddSongCallback {
        void onSongAdded(String title, String author, String type, String abc, List<MusicNote> notes, String key);
    }
    
    public AddCustomSongDialog(AddSongCallback callback) {
        this.callback = callback;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_custom_song, null);
        
        titleInput = view.findViewById(R.id.dialog_title_input);
        authorInput = view.findViewById(R.id.dialog_author_input);
        typeSpinner = view.findViewById(R.id.dialog_type_spinner);
        abcInput = view.findViewById(R.id.dialog_abc_input);
        
        // Setup spinner with tune types
        String[] types = {"Reel", "Jig", "Hornpipe", "Polka", "Slide", "Waltz", 
                         "March", "Song", "Slip Jig", "Misc."};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        
        // Track if title was manually edited
        titleInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // If user types something, mark as manually edited
                if (count > 0) {
                    titleManuallyEdited = true;
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Auto-fill title from ABC notation
        abcInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                // Only auto-fill if title hasn't been manually edited
                if (!titleManuallyEdited) {
                    String abc = s.toString();
                    String extractedTitle = extractTitleFromABC(abc);
                    if (extractedTitle != null && !extractedTitle.isEmpty()) {
                        titleInput.setText(extractedTitle);
                        titleManuallyEdited = false; // Reset flag after auto-fill
                    }
                }
            }
        });
        
        builder.setView(view)
                .setTitle("Add Custom Song")
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (dialog, id) -> dismiss());
        
        AlertDialog dialog = builder.create();
        
        // Handle back button to close keyboard first
        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                // Check if keyboard is open
                View currentFocus = dialog.getCurrentFocus();
                if (currentFocus != null) {
                    InputMethodManager imm = (InputMethodManager) requireContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && imm.isAcceptingText()) {
                        // Keyboard is open, close it
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                        currentFocus.clearFocus();
                        return true; // Consume the event
                    }
                }
            }
            return false; // Let default behavior handle it
        });
        
        // Override Save button to prevent dialog from closing on error
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String title = titleInput.getText().toString().trim();
                String author = authorInput.getText().toString().trim();
                String type = typeSpinner.getSelectedItem().toString();
                String abc = abcInput.getText().toString().trim();
                
                if (validateAndParse(title, author, type, abc)) {
                    dismiss();
                }
            });
        });
        
        return dialog;
    }
    
    /**
     * Extract title from ABC notation (T: header)
     */
    private String extractTitleFromABC(String abc) {
        if (abc == null || abc.isEmpty()) {
            return null;
        }
        
        // Look for T: header (title)
        Pattern pattern = Pattern.compile("^T:\\s*(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(abc);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return null;
    }
    
    private boolean validateAndParse(String title, String author, String type, String abc) {
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (abc.isEmpty()) {
            Toast.makeText(getContext(), "Please enter ABC notation", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Parse ABC notation
        try {
            ABCParser.ABCParseResult result = ABCParser.parse(abc);
            
            if (result.notes.isEmpty()) {
                Toast.makeText(getContext(), "No notes found in ABC notation", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            // Use title from ABC if not specified manually
            if (title.isEmpty() && !result.title.isEmpty()) {
                title = result.title;
            }
            
            if (callback != null) {
                callback.onSongAdded(title, author, type, abc, result.notes, result.key);
            }
            
            Toast.makeText(getContext(), "Song added successfully!", Toast.LENGTH_SHORT).show();
            return true;
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error parsing ABC: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
