package com.example.simplefiles;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class FileBrowserActivity extends AppCompatActivity
        implements FileAdapter.OnFileClickListener {

    private FileBrowserViewModel viewModel;
    private FileAdapter          adapter;
    private ProgressBar          progressBar;
    private TextView             tvEmpty;
    private SharedPreferences    prefs;

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

        prefs = getSharedPreferences("simplefiles_prefs", MODE_PRIVATE);
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
        Intent intent = new Intent(this, ViewTextFile.class);
        intent.putExtra("FILE_PATH", item.getPath());
        startActivity(intent);
    }

    @Override
    public void onFileLongClick(FileItem item) {
        Toast.makeText(this, item.getPath(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onShareClick(FileItem item) {
        if (item.getPath() == null) return;
        try {
            File file = new File(item.getPath());
            Uri uri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(getMimeType(item));
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share " + item.getName()));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot share this file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(FileItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete \"" + item.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> performDelete(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onStarClick(FileItem item) {
        String path = item.getPath();
        if (path == null) return;
        Set<String> starred = new HashSet<>(
                prefs.getStringSet("starred_files", new HashSet<>()));
        if (starred.contains(path)) {
            starred.remove(path);
            Toast.makeText(this, "Removed from favourites", Toast.LENGTH_SHORT).show();
        } else {
            starred.add(path);
            Toast.makeText(this, "Added to favourites", Toast.LENGTH_SHORT).show();
        }
        prefs.edit().putStringSet("starred_files", starred).apply();
    }

    private void performDelete(FileItem item) {
        if (item.getPath() == null) return;
        boolean deleted = false;
        try {
            File file = new File(item.getPath());
            deleted = file.delete();
        } catch (Exception ignored) {}

        if (!deleted) {
            // Fallback: try via ContentResolver (works for app-owned files on API 30+)
            try {
                int rows = getContentResolver().delete(
                        MediaStore.Files.getContentUri("external"),
                        MediaStore.Files.FileColumns.DATA + " = ?",
                        new String[]{ item.getPath() });
                deleted = rows > 0;
            } catch (Exception ignored) {}
        } else {
            // Sync MediaStore so the file disappears from other apps
            getContentResolver().delete(
                    MediaStore.Files.getContentUri("external"),
                    MediaStore.Files.FileColumns.DATA + " = ?",
                    new String[]{ item.getPath() });
        }

        if (deleted) {
            viewModel.loadFiles();
            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Could not delete — permission denied", Toast.LENGTH_LONG).show();
        }
    }

    private String getMimeType(FileItem item) {
        switch (item.getType()) {
            case PDF:   return "application/pdf";
            case IMAGE: return "image/*";
            case TEXT:  return "text/plain";
            default:    return "*/*";
        }
    }
}