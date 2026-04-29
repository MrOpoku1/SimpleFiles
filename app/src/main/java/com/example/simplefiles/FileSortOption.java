package com.example.simplefiles;

/**
 * All available sort modes for the file browser.
 * Each option has a human-readable label used in the sort menu.
 */
public enum FileSortOption {
    NAME_ASC   ("Name (A → Z)"),
    NAME_DESC  ("Name (Z → A)"),
    DATE_NEW   ("Date (Newest)"),
    DATE_OLD   ("Date (Oldest)"),
    SIZE_LARGE ("Size (Largest)"),
    SIZE_SMALL ("Size (Smallest)"),
    TYPE       ("Type");

    private final String label;

    FileSortOption(String label) { this.label = label; }

    public String getLabel() { return label; }
}