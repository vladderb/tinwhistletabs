package fr.charleslabs.tinwhistletabs.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

import fr.charleslabs.tinwhistletabs.R;
import fr.charleslabs.tinwhistletabs.music.CustomSongsManager;
import fr.charleslabs.tinwhistletabs.music.MusicSheet;
import fr.charleslabs.tinwhistletabs.music.TrashManager;

public class SheetsAdapter extends BaseAdapter implements Filterable{
    private final List<MusicSheet> sheets;
    private List<MusicSheet> sheetsFiltered;
    private final Context context;
    private final View noResult;

    public SheetsAdapter(Context context,List<MusicSheet> sheets, View noResult) {
        this.sheets = sheets;
        this.sheetsFiltered = sheets;
        this.context = context;
        this.noResult = noResult;
    }

    @Override
    public int getCount() {
        return sheetsFiltered.size();
    }

    @Override
    public Object getItem(int position) {
        return sheetsFiltered.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent){
        //Set up the view
        if(convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_layout,parent,false);

        ComponentViewHolder viewHolder = (ComponentViewHolder)convertView.getTag();
        if(viewHolder==null){
            viewHolder = new ComponentViewHolder();
            viewHolder.sheetName = convertView.findViewById(R.id.mainActivity_SheetName);
            viewHolder.sheetDetails = convertView.findViewById(R.id.mainActivity_SheetDetails);
            viewHolder.sheetImage = convertView.findViewById(R.id.mainActivity_sheetPicture);
            viewHolder.btnDelete = convertView.findViewById(R.id.mainActivity_btnDelete);
            convertView.setTag(viewHolder);
        }

        //Fetch the item in the component list
        final MusicSheet sheet = sheetsFiltered.get(position);

        //Fill up the view
        viewHolder.sheetName.setText(sheet.getTitle());
        viewHolder.sheetDetails.setText(context.getResources().getString(R.string.
                mainActivity_sheetDetails_string, sheet.getType(), sheet.getWhistle()));
        switch (sheet.getType()) {
            case "Reel":
                viewHolder.sheetImage.setImageResource(R.drawable.reel);
                break;
            case "Jig":
                viewHolder.sheetImage.setImageResource(R.drawable.jig);
                break;
            case "Slip Jig":
                viewHolder.sheetImage.setImageResource(R.drawable.slipjig);
                break;
            case "Slide":
                viewHolder.sheetImage.setImageResource(R.drawable.slide);
                break;
            case "Polka":
                viewHolder.sheetImage.setImageResource(R.drawable.polka);
                break;
            case "March":
                viewHolder.sheetImage.setImageResource(R.drawable.march);
                break;
            case "Hornpipe":
                viewHolder.sheetImage.setImageResource(R.drawable.hornpipe);
                break;
            case "Song":
                viewHolder.sheetImage.setImageResource(R.drawable.song);
                break;
            case "Waltz":
                viewHolder.sheetImage.setImageResource(R.drawable.waltz);
                break;
            case "Misc.":
            default:
                viewHolder.sheetImage.setImageResource(R.drawable.misc);
                break;
        }

        // Handle delete button - only show for custom songs
        if (sheet.getFile().startsWith("custom_")) {
            viewHolder.btnDelete.setVisibility(View.VISIBLE);
            viewHolder.btnDelete.setFocusable(true);
            viewHolder.btnDelete.setClickable(true);
            viewHolder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Переместить в корзину?")
                        .setMessage("Трек \"" + sheet.getTitle() + "\" будет перемещен в корзину. Вы сможете восстановить его в течение 30 дней.")
                        .setPositiveButton("В корзину", (dialog, which) -> {
                            TrashManager.moveToTrash(context, sheet.getFile(), sheet.getTitle());
                            sheetsFiltered.remove(position);
                            sheets.remove(sheet);
                            notifyDataSetChanged();
                            Toast.makeText(context, "Перемещено в корзину", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            });
        } else {
            viewHolder.btnDelete.setVisibility(View.GONE);
            viewHolder.btnDelete.setOnClickListener(null);
        }

        //Return the view
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if(constraint == null || constraint.length() == 0){
                    filterResults.count = sheets.size();
                    filterResults.values = sheets;
                }else{
                    List<MusicSheet> resultsModel = new ArrayList<>();
                    String searchStr = constraint.toString().toLowerCase();

                    for(MusicSheet sheet : sheets)
                        if(sheet.filter(searchStr)) resultsModel.add(sheet);

                    filterResults.count = resultsModel.size();
                    filterResults.values = resultsModel;
                }
                return filterResults;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                sheetsFiltered = (List<MusicSheet>) results.values;
                notifyDataSetChanged();
                if(noResult != null)
                    if(sheetsFiltered.isEmpty())
                        noResult.setVisibility(View.VISIBLE);
                    else
                        noResult.setVisibility(View.GONE);
            }
        };
    }

    // View Holder
    private static class ComponentViewHolder{
        TextView sheetName;
        TextView sheetDetails;
        ImageView sheetImage;
        ImageButton btnDelete;
    }
}