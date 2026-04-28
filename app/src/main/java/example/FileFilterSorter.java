package com.example.simplefiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Stateless utility that filters and sorts a list of FileItems.
 *
 * Search: case-insensitive substring match on file name.
 * Sort:   driven by FileSortOption enum.
 * Folders always float to the top regardless of sort mode.
 */
public class FileFilterSorter {

    private FileFilterSorter() { /* utility class */ }

    /**
     * Apply search query and sort option to a source list.
     * The source list is never mutated; a new list is returned.
     *
     * @param source  full unfiltered file list
     * @param query   search string (empty or null = no filter)
     * @param sort    how to order the results
     * @return        filtered + sorted copy
     */
    public static List<FileItem> apply(List<FileItem> source, String query, FileSortOption sort) {
        List<FileItem> filtered = filter(source, query);
        sort(filtered, sort);
        return filtered;
    }

    // ── Filter ─────────────────────────────────────────────────────────────────

    private static List<FileItem> filter(List<FileItem> source, String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(source);
        }
        String lower = query.trim().toLowerCase(Locale.getDefault());
        List<FileItem> out = new ArrayList<>();
        for (FileItem item : source) {
            if (item.getName().toLowerCase(Locale.getDefault()).contains(lower)) {
                out.add(item);
            }
        }
        return out;
    }

    // ── Sort ───────────────────────────────────────────────────────────────────

    private static void sort(List<FileItem> list, FileSortOption option) {
        Collections.sort(list, (a, b) -> {
            // Folders always come first
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return  1;

            switch (option) {
                case NAME_ASC:
                    return a.getName().compareToIgnoreCase(b.getName());
                case NAME_DESC:
                    return b.getName().compareToIgnoreCase(a.getName());
                case DATE_NEW:
                    return Long.compare(b.getLastModified(), a.getLastModified());
                case DATE_OLD:
                    return Long.compare(a.getLastModified(), b.getLastModified());
                case SIZE_LARGE:
                    return Long.compare(b.getSizeBytes(), a.getSizeBytes());
                case SIZE_SMALL:
                    return Long.compare(a.getSizeBytes(), b.getSizeBytes());
                case TYPE:
                    int typeCompare = a.getType().compareTo(b.getType());
                    if (typeCompare != 0) return typeCompare;
                    return a.getName().compareToIgnoreCase(b.getName());
                default:
                    return 0;
            }
        });
    }
}
