package com.example.simplefiles;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class CustomizeNavActivity extends AppCompatActivity {

    private static final String PREFS_NAME  = "nav_prefs";
    private static final String KEY_COUNT   = "nav_count";

    private NavItemAdapter adapter;

    /**
     * Default tool set — IDs must match the switch in ViewTextFile.executeNavAction().
     * Enabled items appear in the viewer drawer; disabled ones are hidden but kept here
     * so the user can turn them on in the customise screen.
     */
    private static List<NavItem> buildDefaults() {
        List<NavItem> items = new ArrayList<>();
        items.add(new NavItem("font_up",       "Font +",        true));
        items.add(new NavItem("font_down",     "Font -",        true));
        items.add(new NavItem("scroll_top",    "Scroll to Top", true));
        items.add(new NavItem("scroll_bottom", "Scroll to End", true));
        items.add(new NavItem("page_up",       "Page Up",       true));
        items.add(new NavItem("page_down",     "Page Down",     true));
        items.add(new NavItem("share",         "Share File",    true));
        items.add(new NavItem("go_back",       "Close Viewer",  false));
        items.add(new NavItem("close_menu",    "Close Menu",    false));
        return items;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customization_menu);

        List<NavItem> items = loadFromPrefs();

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
            List<NavItem> defaults = buildDefaults();
            adapter.setItems(defaults);
            Toast.makeText(this, "Reset to defaults", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            saveToPrefs(adapter.getItems());
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // Replaces showRenameDialog — opens the per-item settings bottom sheet
    private void showItemSettings(NavItem item, int position) {
        NavItemSettingsDialog.newInstance(item, position, changedPosition -> {
            adapter.notifyItemChanged(changedPosition);
        }).show(getSupportFragmentManager(), "item_settings");
    }

    private void saveToPrefs(List<NavItem> items) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(KEY_COUNT, items.size());
        for (int i = 0; i < items.size(); i++) {
            NavItem item = items.get(i);
            editor.putString("nav_id_"      + i, item.getId());
            editor.putString("nav_label_"   + i, item.getLabel());
            editor.putBoolean("nav_visible_" + i, item.isVisible());
            editor.putInt("nav_textsize_"   + i, item.getTextSize());
            editor.putBoolean("nav_icon_"   + i, item.isShowIcon());
        }
        editor.apply();
    }

    private List<NavItem> loadFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int count = prefs.getInt(KEY_COUNT, -1);
        if (count <= 0) return buildDefaults();

        List<NavItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String  id      = prefs.getString("nav_id_"      + i, "item_" + i);
            String  label   = prefs.getString("nav_label_"   + i, "Item " + i);
            boolean visible = prefs.getBoolean("nav_visible_" + i, true);
            int     size    = prefs.getInt("nav_textsize_"   + i, 13);
            boolean icon    = prefs.getBoolean("nav_icon_"   + i, true);

            NavItem item = new NavItem(id, label, visible);
            item.setTextSize(size);
            item.setShowIcon(icon);
            items.add(item);
        }
        return items;
    }
}