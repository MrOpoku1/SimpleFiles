// CustomizeNavActivity.java
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplefiles.R;

import java.util.ArrayList;
import java.util.List;
import androidx.annotation.NonNull;

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

        // Set up adapter
        adapter = new NavItemAdapter(items, this::showRenameDialog);

        // Set up RecyclerView
        RecyclerView rv = findViewById(R.id.rvNavItems);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter); // or rv.setAdapter(adapter);

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
            // TODO:  original visibility
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            List<NavItem> saved = adapter.getItems();
            // TODO: persist to SharedPreferences
        });
    }

    private void showRenameDialog(NavItem item, int position) {
        EditText input = new EditText(this);
        input.setText(item.getLabel());

        new AlertDialog.Builder(this)
                .setTitle("Rename")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    item.setLabel(input.getText().toString().trim());
                    adapter.notifyItemChanged(position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}