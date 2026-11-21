package tc4revival.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import tc4revival.aspect.AspectCatalog;
import tc4revival.research.ResearchHex;
import tc4revival.research.ResearchNote;

/** Geometry and the original depth-first connection highlight; no client/world dependency. */
public  class ResearchTableView {
    public record Point(double x, double y) {}
    public record Edge(ResearchHex from, ResearchHex to) {}
    public record Connections(List<Edge> edges, Set<ResearchHex> lit) {}
    private ResearchTableView() {}
    /** Flat-topped outline from hex1.png, normalized from its 32px artwork to a 16px cell. */
    public static List<Point> cellOutline() {
        return List.of(new Point(.5,8),new Point(4,14.5),new Point(12,14.5),
                new Point(15.5,8),new Point(12,1.5),new Point(4,1.5));
    }
    public static Point pixel(ResearchHex hex) { return new Point(13.5 * hex.q(), 9 * Math.sqrt(3) * (hex.r() + hex.q() / 2.0)); }
    public static ResearchHex hex(double x, double y) {
        double a = x / 13.5, b = (-y / Math.sqrt(3) - x / 3) / 9, c = -a - b;
        int ia = (int) Math.round(a), ib = (int) Math.round(b), ic = (int) Math.round(c);
        double da = Math.abs(ia - a), db = Math.abs(ib - b), dc = Math.abs(ic - c);
        if (da > db && da > dc) ia = -ib - ic;
        else if (db > dc) ib = -ia - ic;
        else ic = -ia - ib;
        return new ResearchHex(ia, ic);
    }
    public static int lastPage(int count) { return Math.max(0, (count - 20) / 5); }
    public static int aspectIndex(int page, double x, double y) {
        if (x < 0 || y < 0 || x >= 80 || y >= 80) return -1;
        return page * 5 + (int) (x / 16) * 5 + (int) (y / 16);
    }
    public static Connections connections(ResearchNote note, AspectCatalog aspects, Predicate<String> known) {
        var visited = new HashSet<ResearchHex>(); var edges = new ArrayList<Edge>(); var lit = new HashSet<ResearchHex>();
        for (var entry : note.grid().entrySet()) if (entry.getValue().type() == 1 && known.test(entry.getValue().aspect()))
            visit(entry.getKey(), note, aspects, known, visited, edges, lit);
        return new Connections(List.copyOf(edges), Set.copyOf(lit));
    }
    private static void visit(ResearchHex hex, ResearchNote note, AspectCatalog aspects, Predicate<String> known,
            Set<ResearchHex> visited, List<Edge> edges, Set<ResearchHex> lit) {
        visited.add(hex);
        for (int direction = 0; direction < 6; direction++) {
            var next = hex.neighbor(direction); var cell = note.grid().get(next);
            if (visited.contains(next) || cell == null || cell.type() == 0 || !known.test(cell.aspect())) continue;
            var from = aspects.require(note.grid().get(hex).aspect()); var to = aspects.require(cell.aspect());
            if (!aspects.areRelated(from, to)) continue;
            edges.add(new Edge(hex, next)); lit.add(next);
            if (cell.negative()) continue; // Show the red terminal edge, never route through a negative.
            visit(next, note, aspects, known, visited, edges, lit);
        }
    }
}
