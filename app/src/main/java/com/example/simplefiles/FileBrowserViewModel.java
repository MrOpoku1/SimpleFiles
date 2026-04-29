package com.example.simplefiles;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for the file browser screen.
 *
 * Responsibilities:
 *  • Load files from MediaStore on a background thread
 *  • Hold search query and sort option as observable state
 *  • Expose a single `displayedFiles` LiveData the Fragment/Activity observes
 *
 * The ViewModel survives configuration changes (rotation), so the file list
 * is NOT reloaded every time the screen rotates.
 */
public class FileBrowserViewModel extends AndroidViewModel {

    // ── LiveData ───────────────────────────────────────────────────────────────

    /** The filtered + sorted list the UI should display. */
    private final MutableLiveData<List<FileItem>> displayedFiles = new MutableLiveData<>(new ArrayList<>());
    /** True while the initial MediaStore query is running. */
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    /** Non-null when a non-fatal error should be shown to the user. */
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    public LiveData<List<FileItem>> getDisplayedFiles() { return displayedFiles; }
    public LiveData<Boolean>        getIsLoading()      { return isLoading; }
    public LiveData<String>         getErrorMessage()   { return errorMessage; }

    // ── Internal state ─────────────────────────────────────────────────────────

    /** Full unfiltered list returned by MediaStore (never shown directly). */
    private List<FileItem> allFiles = new ArrayList<>();

    private String         searchQuery  = "";
    private FileSortOption sortOption   = FileSortOption.NAME_ASC;

    private final FileRepository   repository;
    private final ExecutorService  ioExecutor = Executors.newSingleThreadExecutor();

    // ── Constructor ────────────────────────────────────────────────────────────

    public FileBrowserViewModel(@NonNull Application application) {
        super(application);
        repository = new FileRepository(application);
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Load all files from MediaStore. Safe to call multiple times (debounced by isLoading). */
    public void loadFiles() {
        if (Boolean.TRUE.equals(isLoading.getValue())) return;
        isLoading.setValue(true);
        errorMessage.setValue(null);

        ioExecutor.execute(() -> {
            try {
                List<FileItem> loaded = repository.getAllFiles();
                allFiles = loaded;
                List<FileItem> filtered = FileFilterSorter.apply(allFiles, searchQuery, sortOption);
                displayedFiles.postValue(filtered);
            } catch (Exception e) {
                errorMessage.postValue("Could not load files: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    /** Called every time the user types in the search box. */
    public void onSearchQueryChanged(String query) {
        this.searchQuery = query == null ? "" : query;
        refreshDisplayedList();
    }

    /** Called when the user picks a new sort option from the menu. */
    public void onSortOptionChanged(FileSortOption option) {
        this.sortOption = option;
        refreshDisplayedList();
    }

    /** Returns the current sort option (used to highlight the active menu item). */
    public FileSortOption getCurrentSortOption() { return sortOption; }

    // ── Private ────────────────────────────────────────────────────────────────

    /**
     * Re-applies filter + sort on the background thread so the UI thread
     * never does list processing — important for large file lists.
     */
    private void refreshDisplayedList() {
        final String   q = searchQuery;
        final FileSortOption s = sortOption;
        final List<FileItem> source = allFiles;

        ioExecutor.execute(() -> {
            List<FileItem> result = FileFilterSorter.apply(source, q, s);
            displayedFiles.postValue(result);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ioExecutor.shutdown();
    }
}