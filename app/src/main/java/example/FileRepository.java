package com.example.simplefiles;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Queries the device's MediaStore for files, respecting Scoped Storage (API 29+).
 *
 * On API 29+ apps can no longer walk arbitrary File paths.
 * Instead we query MediaStore collections:
 *   - MediaStore.Files.getContentUri("external")  → all file types
 *   - MediaStore.Images.Media.EXTERNAL_CONTENT_URI → images only
 *
 * No MANAGE_EXTERNAL_STORAGE permission needed for this approach.
 * Users grant access via READ_EXTERNAL_STORAGE (API 29) or
 * READ_MEDIA_* permissions (API 33+).
 */
public class FileRepository {

    private final Context context;

    public FileRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Returns ALL files visible to the app (PDFs, images, docs, etc.)
     * using MediaStore.Files – the broadest collection available.
     */
    public List<FileItem> getAllFiles() {
        List<FileItem> results = new ArrayList<>();

        Uri collection = MediaStore.Files.getContentUri("external");

        String[] projection = {
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE
        };

        // Exclude hidden files and Android system folders
        String selection =
                MediaStore.Files.FileColumns.DISPLAY_NAME + " NOT LIKE '.%'" +
                " AND " + MediaStore.Files.FileColumns.DATA + " NOT LIKE '%/Android/data/%'" +
                " AND " + MediaStore.Files.FileColumns.DATA + " NOT LIKE '%/Android/obb/%'";

        try (Cursor cursor = context.getContentResolver().query(
                collection, projection, selection, null,
                MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC")) {

            if (cursor == null) return results;

            int nameCol     = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
            int pathCol     = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
            int sizeCol     = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            int dateCol     = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);

            while (cursor.moveToNext()) {
                String name  = cursor.getString(nameCol);
                String path  = cursor.getString(pathCol);
                long   size  = cursor.getLong(sizeCol);
                long   date  = cursor.getLong(dateCol) * 1000L; // seconds → millis

                if (name == null || name.isEmpty()) continue;

                results.add(new FileItem(name, path, size, date, false));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Returns only PDF files, using a MIME type filter.
     */
    public List<FileItem> getPdfFiles() {
        return getFilesByMime("application/pdf");
    }

    /**
     * Returns only image files.
     */
    public List<FileItem> getImageFiles() {
        List<FileItem> results = new ArrayList<>();
        results.addAll(getFilesByMime("image/jpeg"));
        results.addAll(getFilesByMime("image/png"));
        results.addAll(getFilesByMime("image/gif"));
        results.addAll(getFilesByMime("image/webp"));
        return results;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private List<FileItem> getFilesByMime(String mimeType) {
        List<FileItem> results = new ArrayList<>();
        Uri collection = MediaStore.Files.getContentUri("external");

        String[] projection = {
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
        };

        String selection = MediaStore.Files.FileColumns.MIME_TYPE + " = ?";
        String[] selectionArgs = { mimeType };

        try (Cursor cursor = context.getContentResolver().query(
                collection, projection, selection, selectionArgs, null)) {

            if (cursor == null) return results;

            int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
            int pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
            int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);

            while (cursor.moveToNext()) {
                String name = cursor.getString(nameCol);
                String path = cursor.getString(pathCol);
                long   size = cursor.getLong(sizeCol);
                long   date = cursor.getLong(dateCol) * 1000L;
                if (name != null && !name.isEmpty())
                    results.add(new FileItem(name, path, size, date, false));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
}
