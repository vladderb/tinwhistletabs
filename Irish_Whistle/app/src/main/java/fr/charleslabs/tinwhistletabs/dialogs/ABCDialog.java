package fr.charleslabs.tinwhistletabs.dialogs;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.List;

import fr.charleslabs.tinwhistletabs.R;
import fr.charleslabs.tinwhistletabs.music.ABCParser;
import fr.charleslabs.tinwhistletabs.music.CustomSongsManager;
import fr.charleslabs.tinwhistletabs.music.MusicNote;

public class ABCDialog extends DialogFragment {
    
    private final String abc;
    private final String title;
    private final String filename;
    private final boolean isCustomSong;
    private final SaveCallback callback;
    
    public interface SaveCallback {
        void onABCSaved(List<MusicNote> newNotes);
    }
    
    public ABCDialog(String title, String abc, String filename, boolean isCustomSong, SaveCallback callback) {
        this.title = title;
        this.abc = abc;
        this.filename = filename;
        this.isCustomSong = isCustomSong;
        this.callback = callback;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_abc, null);
        
        TextView titleView = view.findViewById(R.id.dialog_abc_title);
        EditText abcView = view.findViewById(R.id.dialog_abc_content);
        Button copyButton = view.findViewById(R.id.dialog_abc_copy);
        Button saveButton = view.findViewById(R.id.dialog_abc_save);
        
        titleView.setText(title);
        
        if (abc != null && !abc.isEmpty()) {
            abcView.setText(abc);
        } else {
            abcView.setText("ABC notation not available for this song.");
            abcView.setEnabled(false);
        }
        
        // Show Save button only for custom songs
        if (isCustomSong && abc != null && !abc.isEmpty()) {
            saveButton.setVisibility(View.VISIBLE);
        }
        
        copyButton.setOnClickListener(v -> {
            String currentAbc = abcView.getText().toString();
            if (currentAbc != null && !currentAbc.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("ABC Notation", currentAbc);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "ABC notation copied to clipboard", 
                        Toast.LENGTH_SHORT).show();
            }
        });
        
        saveButton.setOnClickListener(v -> {
            String newAbc = abcView.getText().toString().trim();
            if (newAbc.isEmpty()) {
                Toast.makeText(getContext(), "ABC notation cannot be empty", 
                        Toast.LENGTH_SHORT).show();
                return;
            }
            
            saveABC(newAbc);
        });
        
        builder.setView(view)
                .setPositiveButton("Close", (dialog, id) -> dismiss());
        
        return builder.create();
    }
    
    private void saveABC(String newAbc) {
        try {
            // Parse new ABC notation
            ABCParser.ABCParseResult result = ABCParser.parse(newAbc);
            
            if (result.notes.isEmpty()) {
                Toast.makeText(getContext(), "No notes found in ABC notation", 
                        Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Обновляем файл с нотами
            CustomSongsManager manager = new CustomSongsManager(requireContext());
            manager.updateSongABC(filename, newAbc, result.notes);
            
            // Вызываем callback с новыми нотами
            if (callback != null) {
                callback.onABCSaved(result.notes);
            }
            
            dismiss();
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error parsing ABC: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }
}
