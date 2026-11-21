package tc4revival.research;

/** Absolute column selection, independent of the five-column viewport and filtered search position. */
public class ResearchColumnSelection {
    private int remembered, selected;
    private boolean searching;
    private String picked;

    public ResearchColumnSelection(int remembered) { this.remembered = this.selected = Math.max(0, remembered); }
    public int selected() { return selected; }
    public int remembered() { return remembered; }
    public String picked() { return picked; }
    public void cancelPick() { picked = null; }
    public void pick(String tag) { picked = tag == null || tag.equals(picked) ? null : tag; }
    public static int lastColumn(int count) { return Math.max(0, (count + 4) / 5 - 1); }
    public int windowStart(int count) { return Math.max(0, Math.min(selected - 2, lastColumn(count) - 4)); }
    public void clamp(int count) {
        if (count == 0) return;
        selected = Math.max(0, Math.min(selected, lastColumn(count)));
        // A temporary empty knowledge snapshot must not erase the remembered position.
        if (!searching) remembered = selected;
    }
    public void move(int delta, int count) { if (count > 0) { selected += delta; clamp(count); } }
    public void queryChanged(boolean nonempty, int count) {
        searching = nonempty;
        selected = searching ? 0 : remembered;
        clamp(count);
    }
    public int index(int row, int count) {
        int index = selected * 5 + row;
        return row >= 0 && row < 5 && index < count ? index : -1;
    }
}
