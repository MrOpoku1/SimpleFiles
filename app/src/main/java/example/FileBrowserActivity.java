package com.example.simplefiles;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
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

/**
 * File Browser Activity
 *
 * Layout IDs expected (define in activity_file_browser.xml):
 *   R.id.toolbar            – AppCompat Toolbar (shows sort menu icon)
 *   R.id.et_search          – EditText for live search
 *   R.id.rv_files           – RecyclerView showing file list
 *   R.id.progress_bar       – ProgressBar (shown while loading)
 *   R.id.tv_empty           – TextView shown when list is empty
 *
 * Permissions:
 *   API 29–32 → READ_EXTERNAL_STORAGE
 *   API 33+   → READ_MEDIA_IMAGES + READ_MEDIA_VIDEO + READ_MEDIA_AUDIO (or READ_MEDIA_VISUAL_USER_SELECTED)
 *   (declared in AndroidManifest.xml — see notes at bottom of this file)
 */
public class FileBrowserActivity extends AppCompatActivity
        implements FileAdapter.OnFileClickListener {

    // ── Views & adapter ────────────────────────────────────────────────────────

    private FileBrowserViewModel viewModel;
    private FileAdapter          adapter;

    private ProgressBar progressBar;
    private TextView    tvEmpty;
    private EditText    etSearch;

    // ── Permission launcher ────────────────────────────────────────────────────

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean granted = result.values().stream().anyMatch(v -> v);
                        if (granted) {
                            viewModel.loadFiles();
                        } else {
                            showPermissionDeniedMessage();
                        }
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
        int id = item.getItemId();

        if (id == R.id.action_sort) {
            showSortDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Setup helpers ──────────────────────────────────────────────────────────

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("SimpleFiles");
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.rv_files);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty     = findViewById(R.id.tv_empty);

        adapter = new FileAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                viewModel.onSearchQueryChanged(s.toString());
            }
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(FileBrowserViewModel.class);

        // Observe file list
        viewModel.getDisplayedFiles().observe(this, files -> {
            adapter.submitList(files);
            tvEmpty.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
        });

        // Observe loading state
        viewModel.getIsLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        // Observe errors
        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── Permissions ────────────────────────────────────────────────────────────

    private void checkPermissionsAndLoad() {
        String[] permissions = getRequiredPermissions();
        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            viewModel.loadFiles();
        } else {
            permissionLauncher.launch(permissions);
        }
    }

    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
            };
        } else {
            return new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE };
        }
    }

    private void showPermissionDeniedMessage() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("SimpleFiles needs storage access to browse your files. " +
                        "Please grant access in Settings.")
                .setPositiveButton("OK", null)
                .show();
    }

    // ── Sort dialog ────────────────────────────────────────────────────────────

    private void showSortDialog() {
        FileSortOption[] options   = FileSortOption.values();
        String[]         labels    = new String[options.length];
        int              current   = 0;
        FileSortOption   activSort = viewModel.getCurrentSortOption();

        for (int i = 0; i < options.length; i++) {
            labels[i] = options[i].getLabel();
            if (options[i] == activSort) current = i;
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
        // TODO: open file viewer / PDF annotator
        Toast.makeText(this, "Opened: " + item.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFileLongClick(FileItem item) {
        // TODO: show context menu (share, delete, rename, annotate)
        Toast.makeText(this, item.getName() + "\n" + item.getPath(), Toast.LENGTH_LONG).show();
    }
}

/*
 * ════════════════════════════════════════════════════════════
 * AndroidManifest.xml — add these inside <manifest> block:
 * ════════════════════════════════════════════════════════════
 *
 * <!-- API 29–32 -->
 * <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
 *     android:maxSdkVersion="32" />
 *
 * <!-- API 33+ -->
 * <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
 * <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
 * <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
 *
 * Then declare the activity:
 * <activity android:name=".FileBrowserActivity"
 *     android:windowSoftInputMode="adjustResize" />
 * ════════════════════════════════════════════════════════════
 */
