package com.example.simplefiles;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FileBrowserActivity extends AppCompatActivity
        implements FileAdapter.OnFileClickListener {

    private FileBrowserViewModel viewModel;
    private FileAdapter          adapter;
    private ProgressBar          progressBar;
    private TextView             tvEmpty;

    // ── Permission launcher ────────────────────────────────────────────────────

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean granted = result.values().stream().anyMatch(v -> v);
                        if (granted) viewModel.loadFiles();
                        else showPermissionDeniedMessage();
                    });

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);

        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupViewModel();
        checkPermissionsAndLoad();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file_browser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sort) {
            showSortDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Setup ──────────────────────────────────────────────────────────────────

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("");  // logo takes the left side
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rv_files);
        progressBar     = findViewById(R.id.progress_bar);
        tvEmpty         = findViewById(R.id.tv_empty);

        adapter = new FileAdapter(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    private void setupSearch() {
        SearchView searchView = findViewById(R.id.et_search);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.onSearchQueryChanged(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.onSearchQueryChanged(newText);
                return true;
            }
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(FileBrowserViewModel.class);

        viewModel.getDisplayedFiles().observe(this, files -> {
            adapter.submitList(files);
            tvEmpty.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getIsLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty())
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });
    }

    // ── Permissions ────────────────────────────────────────────────────────────

    private void checkPermissionsAndLoad() {
        String[] permissions = getRequiredPermissions();
        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) viewModel.loadFiles();
        else permissionLauncher.launch(permissions);
    }

    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
            };
        }
        return new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE };
    }

    private void showPermissionDeniedMessage() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("SimpleFiles needs storage access to browse your files.")
                .setPositiveButton("OK", null)
                .show();
    }

    // ── Sort dialog ────────────────────────────────────────────────────────────

    private void showSortDialog() {
        FileSortOption[] options = FileSortOption.values();
        String[]         labels  = new String[options.length];
        int current = 0;
        FileSortOption active = viewModel.getCurrentSortOption();
        for (int i = 0; i < options.length; i++) {
            labels[i] = options[i].getLabel();
            if (options[i] == active) current = i;
        }
        new AlertDialog.Builder(this)
                .setTitle("Sort by")
                .setSingleChoiceItems(labels, current, (dialog, which) -> {
                    viewModel.onSortOptionChanged(options[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── FileAdapter.OnFileClickListener ────────────────────────────────────────

    @Override
    public void onFileClick(FileItem item) {
        // TODO: open PDF viewer / image viewer
        Toast.makeText(this, "Opened: " + item.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFileLongClick(FileItem item) {
        Toast.makeText(this, item.getPath(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onShareClick(FileItem item) {
        Toast.makeText(this, "Share: " + item.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(FileItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete \"" + item.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) ->
                        Toast.makeText(this, "Delete coming soon", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onStarClick(FileItem item) {
        Toast.makeText(this, "Starred: " + item.getName(), Toast.LENGTH_SHORT).show();
    }
}