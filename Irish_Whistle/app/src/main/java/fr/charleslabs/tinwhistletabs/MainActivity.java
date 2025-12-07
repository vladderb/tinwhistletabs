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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.charleslabs.tinwhistletabs.android.SheetsAdapter;
import fr.charleslabs.tinwhistletabs.dialogs.AddCustomSongDialog;
import fr.charleslabs.tinwhistletabs.dialogs.AppCreditsDialog;
import fr.charleslabs.tinwhistletabs.music.CustomSongsManager;
import fr.charleslabs.tinwhistletabs.music.FavoritesManager;
import fr.charleslabs.tinwhistletabs.music.MusicDB;
import fr.charleslabs.tinwhistletabs.music.MusicNote;
import fr.charleslabs.tinwhistletabs.music.MusicSheet;
import fr.charleslabs.tinwhistletabs.music.TrashManager;

public class MainActivity extends AppCompatActivity  {
    public static final String EXTRA_SHEET= "fr.charleslabs.tinwhistletabs.SHEET";
    private SheetsAdapter adapter;
    private ListView listView;
    private FavoritesManager favoritesManager;
    private boolean showOnlyFavorites = false;
    private MenuItem favoritesMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize managers
        favoritesManager = new FavoritesManager(this);

        // Set up ListView
        listView = findViewById(R.id.sheetsList);

        // Get result list (built-in + custom songs)
        List<MusicSheet> listData = getAllSongs();
        adapter = new SheetsAdapter(this, listData, findViewById(R.id.noResultsFoundView), favoritesManager, 
            () -> refreshSongList());
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

        // Store favorites menu item reference
        favoritesMenuItem = menu.findItem(R.id.mainAction_favorites);
        updateFavoritesIcon();

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
        int itemId = item.getItemId();
        
        if (itemId == R.id.mainAction_about) {
            DialogFragment dialogFragment = new AppCreditsDialog();
            dialogFragment.show(getSupportFragmentManager(),"dialog");
        } else if (itemId == R.id.mainAction_addSong) {
            showAddSongDialog();
        } else if (itemId == R.id.mainAction_trash) {
            Intent intent = new Intent(this, TrashActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.mainAction_favorites) {
            toggleFavoritesFilter();
        } else if (itemId == R.id.sort_name_asc) {
            sortSongs(SortType.NAME_ASC);
        } else if (itemId == R.id.sort_name_desc) {
            sortSongs(SortType.NAME_DESC);
        } else if (itemId == R.id.sort_favorites) {
            sortSongs(SortType.FAVORITES_FIRST);
        } else if (itemId == R.id.sort_type) {
            sortSongs(SortType.BY_TYPE);
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
        
        // Filter by favorites if enabled
        if (showOnlyFavorites) {
            List<MusicSheet> favoriteSongs = new ArrayList<>();
            for (MusicSheet song : allSongs) {
                if (favoritesManager.isFavorite(song.getFile())) {
                    favoriteSongs.add(song);
                }
            }
            return favoriteSongs;
        }
        
        return allSongs;
    }
    
    private void refreshSongList() {
        List<MusicSheet> listData = getAllSongs();
        adapter = new SheetsAdapter(this, listData, findViewById(R.id.noResultsFoundView), favoritesManager,
            () -> refreshSongList());
        listView.setAdapter(adapter);
    }
    
    private void toggleFavoritesFilter() {
        showOnlyFavorites = !showOnlyFavorites;
        updateFavoritesIcon();
        refreshSongList();
        
        String message = showOnlyFavorites ? "Showing favorites only" : "Showing all songs";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void updateFavoritesIcon() {
        if (favoritesMenuItem != null) {
            int iconRes = showOnlyFavorites ? R.drawable.ic_star : R.drawable.ic_star_border;
            favoritesMenuItem.setIcon(iconRes);
            
            int greenColor = ContextCompat.getColor(this, R.color.md_theme_primary);
            favoritesMenuItem.getIcon().setTint(greenColor);
        }
    }
    
    private enum SortType {
        NAME_ASC, NAME_DESC, FAVORITES_FIRST, BY_TYPE
    }
    
    private void sortSongs(SortType sortType) {
        List<MusicSheet> listData = getAllSongs();
        
        switch (sortType) {
            case NAME_ASC:
                Collections.sort(listData, new Comparator<MusicSheet>() {
                    @Override
                    public int compare(MusicSheet s1, MusicSheet s2) {
                        return s1.getTitle().compareToIgnoreCase(s2.getTitle());
                    }
                });
                Toast.makeText(this, "Sorted by name (A-Z)", Toast.LENGTH_SHORT).show();
                break;
                
            case NAME_DESC:
                Collections.sort(listData, new Comparator<MusicSheet>() {
                    @Override
                    public int compare(MusicSheet s1, MusicSheet s2) {
                        return s2.getTitle().compareToIgnoreCase(s1.getTitle());
                    }
                });
                Toast.makeText(this, "Sorted by name (Z-A)", Toast.LENGTH_SHORT).show();
                break;
                
            case FAVORITES_FIRST:
                Collections.sort(listData, new Comparator<MusicSheet>() {
                    @Override
                    public int compare(MusicSheet s1, MusicSheet s2) {
                        boolean isFav1 = favoritesManager.isFavorite(s1.getFile());
                        boolean isFav2 = favoritesManager.isFavorite(s2.getFile());
                        
                        if (isFav1 && !isFav2) return -1;
                        if (!isFav1 && isFav2) return 1;
                        
                        return s1.getTitle().compareToIgnoreCase(s2.getTitle());
                    }
                });
                Toast.makeText(this, "Sorted by favorites first", Toast.LENGTH_SHORT).show();
                break;
                
            case BY_TYPE:
                Collections.sort(listData, new Comparator<MusicSheet>() {
                    @Override
                    public int compare(MusicSheet s1, MusicSheet s2) {
                        int typeCompare = s1.getType().compareToIgnoreCase(s2.getType());
                        if (typeCompare != 0) return typeCompare;
                        
                        return s1.getTitle().compareToIgnoreCase(s2.getTitle());
                    }
                });
                Toast.makeText(this, "Sorted by type", Toast.LENGTH_SHORT).show();
                break;
        }
        
        adapter = new SheetsAdapter(this, listData, findViewById(R.id.noResultsFoundView), favoritesManager,
            () -> refreshSongList());
        listView.setAdapter(adapter);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh list when returning to activity
        refreshSongList();
    }
}
