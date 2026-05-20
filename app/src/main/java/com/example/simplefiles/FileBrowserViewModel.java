package com.example.simplefiles;

import android.app.Application;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileBrowserViewModel extends AndroidViewModel {

    // ── LiveData exposed to the UI ─────────────────────────────────────────────

    private final MutableLiveData<List<FileItem>> displayedFiles  = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean>        isLoading       = new MutableLiveData<>(false);
    private final MutableLiveData<String>         errorMessage    = new MutableLiveData<>(null);
    private final MutableLiveData<String>         currentPath     = new MutableLiveData<>(null);

    public LiveData<List<FileItem>> getDisplayedFiles() { return displayedFiles; }
    public LiveData<Boolean>        getIsLoading()      { return isLoading; }
    public LiveData<String>         getErrorMessage()   { return errorMessage; }
    public LiveData<String>         getCurrentPath()    { return currentPath; }

    // ── Internal state ─────────────────────────────────────────────────────────

    private List<FileItem>  allFiles    = new ArrayList<>();
    private String          searchQuery = "";
    private FileSortOption  sortOption  = FileSortOption.NAME_ASC;

    private final FileRepository  repository;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    // ── Constructor ────────────────────────────────────────────────────────────

    public FileBrowserViewModel(@NonNull Application application) {
        super(application);
        repository = new FileRepository(application);
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    public void navigateInto(FileItem folder) {
        currentPath.setValue(folder.getPath());
        isLoading.setValue(false); // bypass debounce for explicit navigation
        loadFiles();
    }

    public void navigateUp() {
        String path = currentPath.getValue();
        if (path == null) return;

        File parent = new File(path).getParentFile();
        String externalRoot = Environment.getExternalStorageDirectory().getAbsolutePath();

        // Go to root view if we'd leave external storage
        if (parent == null || !path.startsWith(externalRoot)
                || parent.getAbsolutePath().equals(externalRoot)
                || path.equals(externalRoot)) {
            currentPath.setValue(null);
        } else {
            currentPath.setValue(parent.getAbsolutePath());
        }

        isLoading.setValue(false);
        loadFiles();
    }

    public boolean isInSubDirectory() {
        return currentPath.getValue() != null;
    }

    // ── File loading ───────────────────────────────────────────────────────────

    public void loadFiles() {
        if (Boolean.TRUE.equals(isLoading.getValue())) return;
        isLoading.setValue(true);
        errorMessage.setValue(null);

        final String path = currentPath.getValue();

        ioExecutor.execute(() -> {
            try {
                List<FileItem> loaded;
                if (path == null) {
                    // Root view: standard public directories + MediaStore files
                    loaded = new ArrayList<>();
                    loaded.addAll(repository.getPublicRootDirectories());
                    loaded.addAll(repository.getAllFiles());
                } else {
                    // Inside a directory: direct filesystem listing
                    loaded = repository.getFilesInDirectory(path);
                }
                allFiles = loaded;
                List<FileItem> filtered = FileFilterSorter.apply(allFiles, searchQuery, sortOption);
                if (path != null) {
                    filtered.add(0, new FileItem("..", null, 0, 0, true));
                }
                displayedFiles.postValue(filtered);
            } catch (Exception e) {
                errorMessage.postValue("Could not load files: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    // ── Search & sort ──────────────────────────────────────────────────────────

    public void onSearchQueryChanged(String query) {
        this.searchQuery = query == null ? "" : query;
        refreshDisplayedList();
    }

    public void onSortOptionChanged(FileSortOption option) {
        this.sortOption = option;
        refreshDisplayedList();
    }

    public FileSortOption getCurrentSortOption() { return sortOption; }

    private void refreshDisplayedList() {
        final String         q      = searchQuery;
        final FileSortOption s      = sortOption;
        final List<FileItem> source = allFiles;
        final String         path   = currentPath.getValue();
        ioExecutor.execute(() -> {
            List<FileItem> result = FileFilterSorter.apply(source, q, s);
            if (path != null) {
                result.add(0, new FileItem("..", null, 0, 0, true));
            }
            displayedFiles.postValue(result);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ioExecutor.shutdown();
    }
}
