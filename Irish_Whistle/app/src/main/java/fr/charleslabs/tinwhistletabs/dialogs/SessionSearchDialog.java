package fr.charleslabs.tinwhistletabs.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

import fr.charleslabs.tinwhistletabs.R;
import fr.charleslabs.tinwhistletabs.api.TheSessionApi;

public class SessionSearchDialog extends DialogFragment {
    
    public interface OnTuneSelectedListener {
        void onTuneSelected(String tuneName, String tuneType, String abc);
    }
    
    private EditText queryInput;
    private Button searchButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private ListView resultsList;
    private SessionTuneAdapter adapter;
    private OnTuneSelectedListener listener;
    
    public SessionSearchDialog(OnTuneSelectedListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_session_search, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        queryInput = view.findViewById(R.id.sessionSearch_query);
        searchButton = view.findViewById(R.id.sessionSearch_btnSearch);
        progressBar = view.findViewById(R.id.sessionSearch_progress);
        statusText = view.findViewById(R.id.sessionSearch_status);
        resultsList = view.findViewById(R.id.sessionSearch_results);
        
        adapter = new SessionTuneAdapter();
        resultsList.setAdapter(adapter);
        
        searchButton.setOnClickListener(v -> performSearch());
        
        queryInput.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });
        
        resultsList.setOnItemClickListener((parent, itemView, position, id) -> {
            TheSessionApi.TuneResult tune = adapter.getItem(position);
            if (tune != null) {
                loadTuneABC(tune);
            }
        });
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }
    
    private void performSearch() {
        String query = queryInput.getText().toString().trim();
        
        if (query.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a tune name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Hide keyboard
        android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) requireContext()
                        .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(queryInput.getWindowToken(), 0);
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.GONE);
        searchButton.setEnabled(false);
        adapter.clear();
        
        TheSessionApi.searchTunes(query, new TheSessionApi.SearchCallback() {
            @Override
            public void onSuccess(List<TheSessionApi.TuneResult> results) {
                progressBar.setVisibility(View.GONE);
                searchButton.setEnabled(true);
                
                if (results.isEmpty()) {
                    statusText.setText("No tunes found");
                    statusText.setVisibility(View.VISIBLE);
                } else {
                    adapter.setData(results);
                }
            }
            
            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                searchButton.setEnabled(true);
                statusText.setText("Error: " + error);
                statusText.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void loadTuneABC(TheSessionApi.TuneResult tune) {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.GONE);
        
        android.util.Log.d("SessionSearch", "Loading ABC for tune ID: " + tune.id);
        
        TheSessionApi.getTuneABC(tune.id, new TheSessionApi.SearchCallback() {
            @Override
            public void onSuccess(List<TheSessionApi.TuneResult> results) {
                progressBar.setVisibility(View.GONE);
                
                android.util.Log.d("SessionSearch", "Received " + results.size() + " settings");
                
                if (results.isEmpty()) {
                    Toast.makeText(getContext(), "No ABC notation found", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // If multiple settings, show selection dialog
                if (results.size() > 1) {
                    showSettingSelection(results);
                } else {
                    TheSessionApi.TuneResult result = results.get(0);
                    android.util.Log.d("SessionSearch", "ABC length: " + 
                            (result.abc != null ? result.abc.length() : "null"));
                    
                    if (listener != null) {
                        listener.onTuneSelected(result.name, result.type, result.abc);
                    }
                    dismiss();
                }
            }
            
            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                android.util.Log.e("SessionSearch", "Error loading ABC: " + error);
                Toast.makeText(getContext(), "Error loading ABC: " + error, 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showSettingSelection(List<TheSessionApi.TuneResult> settings) {
        String[] settingNames = new String[settings.size()];
        for (int i = 0; i < settings.size(); i++) {
            settingNames[i] = settings.get(i).name;
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Version")
                .setItems(settingNames, (dialog, which) -> {
                    TheSessionApi.TuneResult result = settings.get(which);
                    if (listener != null) {
                        listener.onTuneSelected(result.name, result.type, result.abc);
                    }
                    dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private class SessionTuneAdapter extends ArrayAdapter<TheSessionApi.TuneResult> {
        private List<TheSessionApi.TuneResult> data = new ArrayList<>();
        
        public SessionTuneAdapter() {
            super(requireContext(), R.layout.session_tune_item);
        }
        
        public void setData(List<TheSessionApi.TuneResult> newData) {
            data.clear();
            data.addAll(newData);
            clear();
            addAll(newData);
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.session_tune_item, parent, false);
            }
            
            TheSessionApi.TuneResult tune = getItem(position);
            if (tune != null) {
                TextView nameView = convertView.findViewById(R.id.sessionTune_name);
                TextView typeView = convertView.findViewById(R.id.sessionTune_type);
                
                nameView.setText(tune.name);
                typeView.setText(tune.type);
            }
            
            return convertView;
        }
    }
}
