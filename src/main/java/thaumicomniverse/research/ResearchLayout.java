package thaumicomniverse.research;

import com.google.gson.*;
import java.util.*;

/** Packs the resolved page, including optional and registry-generated research, on the server. */
public final class ResearchLayout {
    // TC4's six original pages, measured between the extreme research coordinates.
    public static final double MIN_DENSITY = 29d / (18 * 11);
    public static final double MAX_DENSITY = 49d / (16 * 15);
    private static final double TARGET_DENSITY = .175;
    private ResearchLayout() {}

    private record Point(int x, int y) {
        double distance(Point other) { return Math.hypot(x - other.x, y - other.y); }
    }
    private record Group(String name, List<JsonObject> nodes) {}
    private record Placement(Map<String, Point> points, double score) {}

    public static void apply(JsonArray categories) {
        var entries = new TreeMap<String, JsonObject>();
        for (var element : categories) {
            var category = element.getAsJsonObject();
            if (!CollaborationResearch.CATEGORY.equals(category.get("key").getAsString())) continue;
            for (var entry : category.getAsJsonArray("research")) {
                var row = entry.getAsJsonObject();
                entries.put(row.get("key").getAsString(), row);
            }
        }
        if (entries.isEmpty()) return;
        var grouped = new TreeMap<String, List<JsonObject>>();
        entries.values().forEach(row -> grouped.computeIfAbsent(group(row), k -> new ArrayList<>()).add(row));
        var groups = grouped.entrySet().stream().map(e -> new Group(e.getKey(), e.getValue()))
                .sorted(Comparator.<Group>comparingInt(g -> -g.nodes.size()).thenComparing(Group::name)).toList();
        var links = links(entries);
        Placement best = null;
        int count = entries.size(), root = (int) Math.ceil(Math.sqrt(count));
        for (int width = Math.max(2, root / 2); width <= Math.max(3, root * 2); width++) {
            for (int shape = 0; shape < 3; shape++) {
                var packed = pack(groups, links, width, shape);
                var candidate = space(packed, links);
                if (candidate != null && (best == null || candidate.score < best.score)) best = candidate;
            }
        }
        if (best == null) throw new IllegalStateException("Cannot lay out Omniverse research: " + count);
        int centerX = best.points.values().stream().mapToInt(Point::x).max().orElse(0) / 2;
        int centerY = best.points.values().stream().mapToInt(Point::y).max().orElse(0) / 2;
        for (var entry : entries.entrySet()) {
            var point = best.points.get(entry.getKey());
            entry.getValue().addProperty("displayColumn", point.x - centerX);
            entry.getValue().addProperty("displayRow", point.y - centerY);
        }
    }

    /** Explicit groups also let data packs place a shared prerequisite with its dependent mod. */
    public static String group(JsonObject row) {
        if (row.has("omniverse_layout_group")) return row.get("omniverse_layout_group").getAsString();
        String key = row.get("key").getAsString();
        String prefix = "thaumicomniverse:ENCHANT/";
        if (key.startsWith(prefix)) return key.substring(prefix.length()).split("/", 2)[0];
        for (String field : List.of("omniverse_requires", "omniverse_requires_any")) {
            if (row.has(field) && !row.getAsJsonArray(field).isEmpty())
                return row.getAsJsonArray(field).get(0).getAsString();
        }
        return "thaumicomniverse";
    }

    private static Map<String, Set<String>> links(Map<String, JsonObject> entries) {
        var links = new TreeMap<String, Set<String>>();
        entries.keySet().forEach(key -> links.put(key, new TreeSet<>()));
        entries.forEach((key, row) -> {
            for (String field : List.of("parents", "parentsAny", "siblings")) {
                if (!row.has(field)) continue;
                for (var element : row.getAsJsonArray(field)) {
                    String target = element.getAsString();
                    if (row.has("parentMirrors") && row.getAsJsonObject("parentMirrors").has(target))
                        target = row.getAsJsonObject("parentMirrors").get(target).getAsString();
                    if (!target.equals(key) && entries.containsKey(target)) {
                        links.get(key).add(target);
                        links.get(target).add(key);
                    }
                }
            }
        });
        return links;
    }

    private static Map<String, Point> pack(List<Group> groups, Map<String, Set<String>> links,
                                          int width, int shape) {
        var points = new TreeMap<String, Point>();
        int[] skyline = new int[width];
        for (var group : groups) {
            int n = group.nodes.size();
            int w = Math.min(width, Math.max(1, (int) Math.ceil(Math.sqrt(n)) + shape - 1));
            int h = (n + w - 1) / w, bx = 0, by = Integer.MAX_VALUE;
            // Skyline packing reserves each mod's entire rectangle: other mods cannot interleave.
            for (int x = 0; x + w <= width; x++) {
                int y = 0;
                for (int i = x; i < x + w; i++) y = Math.max(y, skyline[i]);
                if (y < by) { bx = x; by = y; }
            }
            for (int x = bx; x < bx + w; x++) skyline[x] = by + h;
            var cells = new ArrayList<Point>();
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) cells.add(new Point(bx + x, by + y));
            var remaining = new TreeSet<String>();
            group.nodes.forEach(row -> remaining.add(row.get("key").getAsString()));
            while (!remaining.isEmpty()) {
                // Hubs first, then their neighbours; disconnected enchantments fill the same block.
                String key = remaining.stream().min(Comparator.<String>comparingInt(k ->
                        -(int) links.get(k).stream().filter(points::containsKey).count())
                        .thenComparingInt(k -> -links.get(k).size()).thenComparing(k -> k)).orElseThrow();
                var neighbours = links.get(key).stream().filter(points::containsKey).map(points::get).toList();
                var center = new Point(bx + (w - 1) / 2, by + (h - 1) / 2);
                var chosen = cells.stream().min(Comparator.<Point>comparingDouble(p -> neighbours.isEmpty()
                        ? p.distance(center) : neighbours.stream().mapToDouble(p::distance).sum())
                        .thenComparingInt(Point::y).thenComparingInt(Point::x)).orElseThrow();
                points.put(key, chosen); cells.remove(chosen); remaining.remove(key);
            }
            // Improve real edges without ever moving a node out of its mod's rectangle.
            var keys = group.nodes.stream().map(r -> r.get("key").getAsString()).toList();
            for (int pass = 0; pass < 4; pass++) {
                boolean changed = false;
                for (int a = 0; a < keys.size(); a++) for (int b = a + 1; b < keys.size(); b++) {
                    String ka = keys.get(a), kb = keys.get(b);
                    if (links.get(ka).isEmpty() && links.get(kb).isEmpty()) continue;
                    double before = cost(ka, points, links) + cost(kb, points, links);
                    Point pa = points.get(ka), pb = points.get(kb);
                    points.put(ka, pb); points.put(kb, pa);
                    if (cost(ka, points, links) + cost(kb, points, links) + .001 < before) changed = true;
                    else { points.put(ka, pa); points.put(kb, pb); }
                }
                if (!changed) break;
            }
        }
        return points;
    }

    private static double cost(String key, Map<String, Point> points, Map<String, Set<String>> links) {
        return links.get(key).stream().filter(points::containsKey).mapToDouble(k -> {
            double d = points.get(key).distance(points.get(k)); return d * d;
        }).sum();
    }

    private static Placement space(Map<String, Point> points, Map<String, Set<String>> links) {
        int minX = points.values().stream().mapToInt(Point::x).min().orElse(0);
        int minY = points.values().stream().mapToInt(Point::y).min().orElse(0);
        int dx = points.values().stream().mapToInt(Point::x).max().orElse(0) - minX;
        int dy = points.values().stream().mapToInt(Point::y).max().orElse(0) - minY;
        if (points.size() == 1) return new Placement(points, 0);
        Placement best = null;
        // Integer monotone expansion keeps at least one empty grid cell between research icons.
        for (int w = Math.max(2, dx * 2); w <= Math.max(8, dx * 3 + 2); w++) {
            for (int h = Math.max(2, dy * 2); h <= Math.max(8, dy * 3 + 2); h++) {
                double density = (double) points.size() / (w * h);
                if (density < MIN_DENSITY || density > MAX_DENSITY) continue;
                double edges = 0;
                for (var edge : links.entrySet()) for (String target : edge.getValue()) {
                    var a = scale(points.get(edge.getKey()), minX, minY, dx, dy, w, h);
                    var b = scale(points.get(target), minX, minY, dx, dy, w, h);
                    double d = a.distance(b); edges += d * d / points.size();
                }
                double score = edges + Math.abs(Math.log((double) w / h)) * 3
                        + Math.abs(density - TARGET_DENSITY) * 80;
                if (best == null || score < best.score) {
                    var scaled = new TreeMap<String, Point>();
                    for (var e : points.entrySet()) scaled.put(e.getKey(), scale(e.getValue(), minX, minY, dx, dy, w, h));
                    best = new Placement(scaled, score);
                }
            }
        }
        return best;
    }

    private static Point scale(Point p, int minX, int minY, int dx, int dy, int w, int h) {
        return new Point(dx == 0 ? (p.y - minY) * w / Math.max(1, dy) : (p.x - minX) * w / dx,
                dy == 0 ? (p.x - minX) * h / Math.max(1, dx) : (p.y - minY) * h / dy);
    }
}
