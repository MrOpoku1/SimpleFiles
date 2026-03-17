// NavItem.java
public class NavItem {
    private String id;
    private String label;
    private boolean isVisible;

    public NavItem(String id, String label, boolean isVisible) {
        this.id = id;
        this.label = label;
        this.isVisible = isVisible;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public boolean isVisible() { return isVisible; }

    public void setLabel(String label) { this.label = label; }
    public void setVisible(boolean visible) { isVisible = visible; }
}
