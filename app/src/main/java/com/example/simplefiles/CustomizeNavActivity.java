package com.example.simplefiles;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class CustomizeNavActivity extends AppCompatActivity {

    private NavItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customization_menu);

        // Build your item list
        List<NavItem> items = new ArrayList<>();
        items.add(new NavItem("font_up",   "A+ (Font Up)",   true));
        items.add(new NavItem("font_down", "A- (Font Down)", true));
        items.add(new NavItem("home",      "Home",           true));
        items.add(new NavItem("bookmarks", "Bookmarks",      true));
        items.add(new NavItem("settings",  "Settings",       false));

        // Set up adapter with settings listener instead of rename listener
        adapter = new NavItemAdapter(items, this::showItemSettings);

        // Set up RecyclerView
        RecyclerView rv = findViewById(R.id.rvNavItems);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // Drag-to-reorder
        ItemTouchHelper touchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder from,
                                          @NonNull RecyclerView.ViewHolder to) {
                        adapter.moveItem(from.getAdapterPosition(), to.getAdapterPosition());
                        return true;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        // No swipe actions needed
                    }
                });
        touchHelper.attachToRecyclerView(rv);

        // Buttons
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnReset).setOnClickListener(v -> {
            // TODO: restore original visibility
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            List<NavItem> saved = adapter.getItems();
            // TODO: persist to SharedPreferences
        });
    }

    // Replaces showRenameDialog — opens the per-item settings bottom sheet
    private void showItemSettings(NavItem item, int position) {
        NavItemSettingsDialog.newInstance(item, position, changedPosition -> {
            adapter.notifyItemChanged(changedPosition);
        }).show(getSupportFragmentManager(), "item_settings");
    }
}