package tc4revival.research;

import java.util.ArrayList;
import java.util.List;

/** Axial coordinates; direction and ring order are part of TC4's note generation. */
public record ResearchHex(int q, int r) {
    public ResearchHex neighbor(int direction) {
        return switch (direction) {
            case 0 -> new ResearchHex(q + 1, r);
            case 1 -> new ResearchHex(q + 1, r - 1);
            case 2 -> new ResearchHex(q, r - 1);
            case 3 -> new ResearchHex(q - 1, r);
            case 4 -> new ResearchHex(q - 1, r + 1);
            case 5 -> new ResearchHex(q, r + 1);
            default -> throw new IllegalArgumentException("Invalid hex direction");
        };
    }
    public int radius() { return Math.max(Math.abs(q), Math.max(Math.abs(r), Math.abs(q + r))); }
    public int distance(ResearchHex other) { return new ResearchHex(q - other.q, r - other.r).radius(); }
    public String legacyKey() { return q + ":" + r; }
    public static List<ResearchHex> ring(int radius) {
        if (radius < 1 || radius > 4) throw new IllegalArgumentException("Note radius must be 1..4");
        var cells = new ArrayList<ResearchHex>(radius * 6);
        var current = new ResearchHex(-radius, radius);
        for (int side = 0; side < 6; side++) {
            for (int step = 0; step < radius; step++) {
                cells.add(current);
                current = current.neighbor(side);
            }
        }
        return List.copyOf(cells);
    }
}
