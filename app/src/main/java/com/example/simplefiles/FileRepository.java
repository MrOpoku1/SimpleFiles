package com.example.simplefiles;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FileRepository {

    private final Context context;

    public FileRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    // ── Root view: well-known public directories ───────────────────────────────

    public List<FileItem> getPublicRootDirectories() {
        List<FileItem> dirs = new ArrayList<>();

        String[] types = {
                Environment.DIRECTORY_DOWNLOADS,
                Environment.DIRECTORY_DOCUMENTS,
                Environment.DIRECTORY_DCIM,
                Environment.DIRECTORY_PICTURES,
        };

        for (String type : types) {
            File dir = Environment.getExternalStoragePublicDirectory(type);
            if (dir != null && dir.exists()) {
                String[] children = dir.list();
                int count = children != null ? children.length : 0;
                dirs.add(new FileItem(dir.getName(), dir.getAbsolutePath(),
                        count, dir.lastModified(), true));
            }
        }

        // API 29+ extra dirs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String[] extraTypes = {
                    Environment.DIRECTORY_AUDIOBOOKS,
                    Environment.DIRECTORY_SCREENSHOTS,
            };
            for (String type : extraTypes) {
                File dir = Environment.getExternalStoragePublicDirectory(type);
                if (dir != null && dir.exists()) {
                    String[] children = dir.list();
                    int count = children != null ? children.length : 0;
                    dirs.add(new FileItem(dir.getName(), dir.getAbsolutePath(),
                            count, dir.lastModified(), true));
                }
            }
        }

        return dirs;
    }

    // ── Directory navigation: list a specific folder directly ─────────────────

    public List<FileItem> getFilesInDirectory(String path) {
        List<FileItem> results = new ArrayList<>();
        File dir = new File(path);
        File[] children = dir.listFiles();

        if (children == null) return results; // no permission or empty

        // Sort: folders first, then by name
        Arrays.sort(children, Comparator
                .comparing((File f) -> !f.isDirectory()) // false < true → dirs first
                .thenComparing(f -> f.getName().toLowerCase()));

        for (File child : children) {
            if (child.getName().startsWith(".")) continue; // skip hidden
            results.add(new FileItem(
                    child.getName(),
                    child.getAbsolutePath(),
                    child.isDirectory() ? 0 : child.length(),
                    child.lastModified(),
                    child.isDirectory()
            ));
        }
        return results;
    }

    // ── MediaStore query: all indexed files ───────────────────────────────────

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
                MediaStore.Files.FileColumns.DISPLAY_NAME + " NOT LIKE '.%'"
                + " AND " + MediaStore.Files.FileColumns.DATA + " NOT LIKE '%/Android/data/%'"
                + " AND " + MediaStore.Files.FileColumns.DATA + " NOT LIKE '%/Android/obb/%'"
                + " AND (" + MediaStore.Files.FileColumns.MIME_TYPE + " IS NOT NULL"
                + " OR "   + MediaStore.Files.FileColumns.SIZE      + " > 0)";

        try (Cursor cursor = context.getContentResolver().query(
                collection, projection, selection, null,
                MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC")) {

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

                if (name == null || name.isEmpty() || path == null) continue;

                results.add(new FileItem(name, path, size, date, false));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
}
