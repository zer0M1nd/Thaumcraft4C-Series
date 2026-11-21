package tc4revival.research;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable note state. Completed notes retain only anchors and the connected component. */
public record ResearchNote(String key, int color, boolean complete, int copies, Map<ResearchHex, Cell> grid, int layoutSize) {
    /** Zero is an automatically generated/legacy board; retain the original constructor descriptor. */
    public ResearchNote(String key, int color, boolean complete, int copies, Map<ResearchHex, Cell> grid) {
        this(key, color, complete, copies, grid, 0);
    }
    public record Cell(int type, String aspect) {
        public Cell {
            if (type < 0 || type > 3 || (type > 0 && (aspect == null || aspect.isEmpty()))) {
                throw new IllegalArgumentException("Invalid research cell");
            }
        }
        public boolean anchor() { return type == 1; }
        public boolean negative() { return type == 3; }
        public boolean fixed() { return anchor() || negative(); }
        public boolean occupied() { return type > 0; }
        public static Cell blank() { return new Cell(0, null); }
    }
    public ResearchNote {
        Objects.requireNonNull(key);
        if (copies < 0 || grid.size() > 61 || layoutSize < 0 || layoutSize > 5)
            throw new IllegalArgumentException("Invalid research note size or copy count");
        var captured = new LinkedHashMap<ResearchHex, Cell>();
        grid.forEach((hex, cell) -> {
            if (hex.radius() > 4 || hex.q() < -4 || hex.q() > 4 || hex.r() < -4 || hex.r() > 4) {
                throw new IllegalArgumentException("Research cell outside board");
            }
            if (layoutSize > 0 && hex.radius() >= layoutSize)
                throw new IllegalArgumentException("Research cell outside authored layout");
            captured.put(hex, Objects.requireNonNull(cell));
        });
        grid = Collections.unmodifiableMap(captured);
    }
}
