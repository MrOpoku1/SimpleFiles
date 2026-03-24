package com.example.simplefiles;

public class NavItem {
    private String id;
    private String label;
    private boolean isVisible;
    private int textSize;       // e.g. 13 (sp)
    private boolean showIcon;

    public NavItem(String id, String label, boolean isVisible) {
        this.id = id;
        this.label = label;
        this.isVisible = isVisible;
        this.textSize = 13;      // default
        this.showIcon = true;
    }

    public String getId()           { return id; }
    public String getLabel()        { return label; }
    public boolean isVisible()      { return isVisible; }
    public int getTextSize()        { return textSize; }
    public boolean isShowIcon()     { return showIcon; }

    public void setLabel(String label)      { this.label = label; }
    public void setVisible(boolean v)       { isVisible = v; }
    public void setTextSize(int size)       { textSize = size; }
    public void setShowIcon(boolean show)   { showIcon = show; }
}