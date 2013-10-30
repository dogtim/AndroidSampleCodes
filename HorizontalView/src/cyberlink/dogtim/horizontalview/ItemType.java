package cyberlink.dogtim.horizontalview;

public enum ItemType {
    photoItem("photo item"),
    TransitionItem("transition item"),

    EditingItem("editing item"),
    FakeItem("fake item"),

    /**
     * Workaround: when onDrag event occur, other event such as onTouch, onClick would be malfunction <p>
     * as a result, we need window drag event to know what location of pointer<p>
     */
    WindowItem("window item");

    private String name;
    private ItemType(String source) {
        this.name = source;
    }
    public String getName() {
        return name;
    }
}
