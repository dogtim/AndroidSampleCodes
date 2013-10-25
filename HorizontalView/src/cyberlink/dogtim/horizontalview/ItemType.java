package cyberlink.dogtim.horizontalview;

public enum ItemType {
    OriginalItem("original item"),

    EditingItem("editing item"),
    
    /**
     * Workaround: when onDrag event occur, other event such as onTouch, onClick would be malfunction <p>
     * as a result, we need window drag event to know what location of pointer<p>
     */
    WindowItme("window item"),
    
    FakeItem("fake item");
    
    private String name;
    private ItemType(String source) {
        this.name = source;
    }
    public String getName() {
        return name;
    }
}
