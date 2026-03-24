package com.example.simplefiles;// com.example.simplefiles.NavItemAdapter.java
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.Collections;
import java.util.List;

public class NavItemAdapter extends RecyclerView.Adapter<NavItemAdapter.NavViewHolder> {

    private final List<NavItem> items;
    public interface OnSettingsClickListener {
        void onSettings(NavItem item, int position);
    }

    // Update constructor
    private final OnSettingsClickListener settingsListener;

    public NavItemAdapter(List<NavItem> items, OnSettingsClickListener settingsListener) {
        this.items = items;
        this.settingsListener = settingsListener;
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

        holder.btnSettings.setOnClickListener(v ->
                settingsListener.onSettings(item, holder.getAdapterPosition()));
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
        Button btnSettings;

        NavViewHolder(@NonNull View itemView) {
            super(itemView);
            label     = itemView.findViewById(R.id.itemLabel);
            toggle    = itemView.findViewById(R.id.toggleVisible);
            btnSettings = itemView.findViewById(R.id.btnSettings);
        }
    }
}