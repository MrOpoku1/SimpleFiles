package com.example.simplefiles;

import java.util.Date;

/**
 * Model representing a single file or folder in the browser.
 */
public class FileItem {

    public enum Type { FOLDER, PDF, IMAGE, TEXT, OTHER }

    private final String name;
    private final String path;       // Absolute path (used for display / legacy storage)
    private final long sizeBytes;
    private final long lastModified; // epoch millis
    private final Type type;
    private final boolean isDirectory;

    public FileItem(String name, String path, long sizeBytes, long lastModified, boolean isDirectory) {
        this.name         = name;
        this.path         = path;
        this.sizeBytes    = sizeBytes;
        this.lastModified = lastModified;
        this.isDirectory  = isDirectory;
        this.type         = isDirectory ? Type.FOLDER : resolveType(name);
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public String getName()         { return name; }
    public String getPath()         { return path; }
    public long   getSizeBytes()    { return sizeBytes; }
    public long   getLastModified() { return lastModified; }
    public Type   getType()         { return type; }
    public boolean isDirectory()    { return isDirectory; }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Human-readable file size (e.g. "2.4 MB"). Returns "" for folders. */
    public String getFormattedSize() {
        if (isDirectory) return "";
        if (sizeBytes < 1024)              return sizeBytes + " B";
        if (sizeBytes < 1024 * 1024)       return String.format("%.1f KB", sizeBytes / 1024.0);
        if (sizeBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", sizeBytes / (1024.0 * 1024));
        return String.format("%.1f GB", sizeBytes / (1024.0 * 1024 * 1024));
    }

    /** Human-readable last-modified date. */
    public String getFormattedDate() {
        return new java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                .format(new Date(lastModified));
    }

    /** Emoji icon for quick visual recognition. */
    public String getIcon() {
        switch (type) {
            case FOLDER: return "📁";
            case PDF:    return "📄";
            case IMAGE:  return "🖼";
            case TEXT:   return "📝";
            default:     return "📎";
        }
    }

    // ── Private ────────────────────────────────────────────────────────────────

    private static Type resolveType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".pdf"))
            return Type.PDF;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".gif")  ||
                lower.endsWith(".webp") || lower.endsWith(".bmp"))
            return Type.IMAGE;
        if (lower.endsWith(".txt") || lower.endsWith(".md") ||
                lower.endsWith(".csv") || lower.endsWith(".log"))
            return Type.TEXT;
        return Type.OTHER;
    }
}