package fr.charleslabs.tinwhistletabs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

import fr.charleslabs.tinwhistletabs.android.SheetsAdapter;
import fr.charleslabs.tinwhistletabs.dialogs.AddCustomSongDialog;
import fr.charleslabs.tinwhistletabs.dialogs.AppCreditsDialog;
import fr.charleslabs.tinwhistletabs.music.CustomSongsManager;
import fr.charleslabs.tinwhistletabs.music.MusicDB;
import fr.charleslabs.tinwhistletabs.music.MusicNote;
import fr.charleslabs.tinwhistletabs.music.MusicSheet;
import fr.charleslabs.tinwhistletabs.music.TrashManager;

public class MainActivity extends AppCompatActivity  {
    public static final String EXTRA_SHEET= "fr.charleslabs.tinwhistletabs.SHEET";
    private SheetsAdapter adapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up ListView
        listView = findViewById(R.id.sheetsList);

        // Get result list (built-in + custom songs)
        List<MusicSheet> listData = getAllSongs();
        adapter = new SheetsAdapter(this, listData, findViewById(R.id.noResultsFoundView));
        listView.setAdapter(adapter);

        // Handle click on item
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id){
                MusicSheet a = (MusicSheet) parent.getItemAtPosition(position);
                Intent intent = new Intent(getApplicationContext(), TabActivity.class);
                intent.putExtra(EXTRA_SHEET, a);
                startActivity(intent);
            }
        });

        // Handle contact button
        findViewById(R.id.mainActivity_contact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                            "mailto",getString(R.string.dev_email), null));
                    intent.putExtra(Intent.EXTRA_SUBJECT, "TinWhistle App Contact");
                    startActivity(intent);
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), getString(R.string.error_contact,e.getMessage()), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        // Set green color for icons
        int greenColor = ContextCompat.getColor(this, R.color.md_theme_primary);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() != null) {
                item.getIcon().setTint(greenColor);
            }
        }

        final MenuItem searchItem = menu.findItem(R.id.mainAction_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mainAction_about) {
            DialogFragment dialogFragment = new AppCreditsDialog();
            dialogFragment.show(getSupportFragmentManager(),"dialog");
        } else if (item.getItemId() == R.id.mainAction_addSong) {
            showAddSongDialog();
        } else if (item.getItemId() == R.id.mainAction_trash) {
            Intent intent = new Intent(this, TrashActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.mainAction_deleteAll) {
            showDeleteAllConfirmation();
        }

        return true;
    }
    
    private void showAddSongDialog() {
        AddCustomSongDialog dialog = new AddCustomSongDialog(
            (title, author, type, abc, notes, key) -> {
                try {
                    CustomSongsManager manager = new CustomSongsManager(this);
                    manager.addSong(title, author, type, abc, notes, key);
                    
                    // Refresh song list
                    refreshSongList();
                    
                    Toast.makeText(this, "Song added successfully!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Error saving song: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                }
            }
        );
        dialog.show(getSupportFragmentManager(), "add_song_dialog");
    }
    
    private List<MusicSheet> getAllSongs() {
        List<MusicSheet> allSongs = new ArrayList<>();
        
        // Add built-in tunes
        allSongs.addAll(MusicDB.getInstance(this).musicDB);
        
        // Add custom songs (except those in trash)
        CustomSongsManager manager = new CustomSongsManager(this);
        List<MusicSheet> customSongs = manager.getCustomSongs();
        for (MusicSheet song : customSongs) {
            if (!TrashManager.isInTrash(this, song.getFile())) {
                allSongs.add(song);
            }
        }
        
        return allSongs;
    }
    
    private void refreshSongList() {
        List<MusicSheet> listData = getAllSongs();
        adapter = new SheetsAdapter(this, listData, findViewById(R.id.noResultsFoundView));
        listView.setAdapter(adapter);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh list when returning to activity
        refreshSongList();
    }
    
    private void showDeleteAllConfirmation() {
        CustomSongsManager manager = new CustomSongsManager(this);
        List<MusicSheet> customSongs = manager.getCustomSongs();
        
        if (customSongs.isEmpty()) {
            Toast.makeText(this, "No custom songs to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete All Custom Songs")
                .setMessage("Are you sure you want to delete all " + customSongs.size() + " custom songs? This action cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    deleteAllCustomSongs();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteAllCustomSongs() {
        CustomSongsManager manager = new CustomSongsManager(this);
        int deletedCount = manager.deleteAllCustomSongs();
        
        // Refresh list
        refreshSongList();
        
        Toast.makeText(this, "Deleted " + deletedCount + " custom songs", Toast.LENGTH_SHORT).show();
    }
}
