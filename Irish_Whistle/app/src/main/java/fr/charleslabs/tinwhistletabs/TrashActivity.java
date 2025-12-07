package fr.charleslabs.tinwhistletabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import fr.charleslabs.tinwhistletabs.music.CustomSongsManager;
import fr.charleslabs.tinwhistletabs.music.TrashManager;

public class TrashActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private TrashAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        // Set action bar
        try {
            ActionBar actionBar = this.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle("Trash");
            }
        } catch (Exception ignored) {}

        recyclerView = findViewById(R.id.trashActivity_recyclerView);
        emptyMessage = findViewById(R.id.trashActivity_emptyMessage);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadTrashItems();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadTrashItems() {
        List<TrashManager.TrashItem> items = TrashManager.getTrashItems(this);
        
        if (items.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyMessage.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyMessage.setVisibility(View.GONE);
            adapter = new TrashAdapter(items);
            recyclerView.setAdapter(adapter);
        }
    }

    private class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.ViewHolder> {
        private List<TrashManager.TrashItem> items;

        TrashAdapter(List<TrashManager.TrashItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.trash_item_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TrashManager.TrashItem item = items.get(position);
            holder.title.setText(item.title);
            
            int daysRemaining = item.getDaysRemaining();
            if (daysRemaining > 1) {
                holder.daysRemaining.setText("Will be deleted in " + daysRemaining + " days");
            } else if (daysRemaining == 1) {
                holder.daysRemaining.setText("Will be deleted tomorrow");
            } else {
                holder.daysRemaining.setText("Will be deleted today");
            }

            holder.btnRestore.setOnClickListener(v -> {
                TrashManager.restoreFromTrash(TrashActivity.this, item.sheetId);
                items.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, items.size());
                
                if (items.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyMessage.setVisibility(View.VISIBLE);
                }
            });

            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(TrashActivity.this)
                        .setTitle("Delete Permanently?")
                        .setMessage("Track \"" + item.title + "\" will be permanently deleted. This action cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            CustomSongsManager manager = new CustomSongsManager(TrashActivity.this);
                            manager.deleteSong(item.sheetId);
                            TrashManager.permanentlyDelete(TrashActivity.this, item.sheetId);
                            items.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, items.size());
                            
                            if (items.isEmpty()) {
                                recyclerView.setVisibility(View.GONE);
                                emptyMessage.setVisibility(View.VISIBLE);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView daysRemaining;
            MaterialButton btnRestore;
            ImageButton btnDelete;

            ViewHolder(View view) {
                super(view);
                title = view.findViewById(R.id.trashItem_title);
                daysRemaining = view.findViewById(R.id.trashItem_daysRemaining);
                btnRestore = view.findViewById(R.id.trashItem_btnRestore);
                btnDelete = view.findViewById(R.id.trashItem_btnDelete);
            }
        }
    }
}
