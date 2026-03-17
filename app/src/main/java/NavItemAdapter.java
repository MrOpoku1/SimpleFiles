// NavItemAdapter.java
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplefiles.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.Collections;
import java.util.List;

public class NavItemAdapter extends RecyclerView.Adapter<NavItemAdapter.NavViewHolder> {

    public interface OnRenameClickListener {
        void onRename(NavItem item, int position);
    }

    private final List<NavItem> items;
    private final OnRenameClickListener renameListener;

    public NavItemAdapter(List<NavItem> items, OnRenameClickListener renameListener) {
        this.items = items;
        this.renameListener = renameListener;
    }

    @NonNull
    @Override
    public NavViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nav_card, parent, false);
        return new NavViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NavViewHolder holder, int position) {
        NavItem item = items.get(position);

        holder.label.setText(item.getLabel());

        // Prevent the listener firing during bind
        holder.toggle.setOnCheckedChangeListener(null);
        holder.toggle.setChecked(item.isVisible());
        holder.toggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                item.setVisible(isChecked));

        holder.btnRename.setOnClickListener(v ->
                renameListener.onRename(item, holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Called by ItemTouchHelper when a drag completes
    public void moveItem(int from, int to) {
        Collections.swap(items, from, to);
        notifyItemMoved(from, to);
    }

    public List<NavItem> getItems() {
        return items;
    }

    // --- ViewHolder ---
    static class NavViewHolder extends RecyclerView.ViewHolder {
        TextView label;
        SwitchMaterial toggle;
        Button btnRename;

        NavViewHolder(@NonNull View itemView) {
            super(itemView);
            label     = itemView.findViewById(R.id.itemLabel);
            toggle    = itemView.findViewById(R.id.toggleVisible);
            btnRename = itemView.findViewById(R.id.btnRename);
        }
    }
}