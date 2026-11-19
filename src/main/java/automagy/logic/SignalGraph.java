package automagy.logic;

import java.util.*;
import java.util.function.IntUnaryOperator;

/** Rebuild from independent sources; never seed a recalculation with yesterday's wire output. */
public final class SignalGraph<K> {
    private record Node(int source, IntUnaryOperator transform) {}
    private record Edge<K>(K target, int loss, int cap) {}
    private final Map<K, Node> nodes = new LinkedHashMap<>();
    private final Map<K, List<Edge<K>>> edges = new HashMap<>();
    public void node(K key, int source, IntUnaryOperator transform) { nodes.put(key, new Node(clamp(source), transform)); }
    public void edge(K from, K to, int loss, int cap) {
        if (loss < 0 || cap < 0 || cap > 32767) throw new IllegalArgumentException("Invalid signal edge");
        edges.computeIfAbsent(from, ignored -> new ArrayList<>()).add(new Edge<>(to, loss, cap));
    }
    public Map<K, Integer> solve() {
        Map<K, Integer> output = new LinkedHashMap<>();
        ArrayDeque<K> pending = new ArrayDeque<>();
        Set<K> queued = new HashSet<>();
        nodes.forEach((key, node) -> {
            int power = clamp(node.transform.applyAsInt(node.source)); output.put(key, power);
            if (power > 0) { pending.addLast(key); queued.add(key); }
        });
        while (!pending.isEmpty()) {
            K from = pending.removeFirst(); queued.remove(from);
            for (Edge<K> edge : edges.getOrDefault(from, List.of())) {
                Node target = nodes.get(edge.target); if (target == null) continue;
                int candidate = clamp(target.transform.applyAsInt(Math.max(0, Math.min(edge.cap, output.get(from)) - edge.loss)));
                if (candidate > output.get(edge.target)) {
                    output.put(edge.target, candidate);
                    if (queued.add(edge.target)) pending.addLast(edge.target);
                }
            }
        }
        return Collections.unmodifiableMap(output);
    }
    private static int clamp(int value) { return Math.max(0, Math.min(32767, value)); }
    public static int transform(String kind, int setting, int input) {
        input = clamp(input);
        return switch (kind) {
            case "amp" -> input > 0 ? Math.max(50, input) : 0;
            case "dim" -> Math.min(input, Math.max(1, Math.min(14, setting)));
            case "dense" -> input >= Math.max(2, Math.min(15, setting)) ? input : 0;
            default -> input;
        };
    }
}
