package tc4revival.research;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/** Protect a small connecting subtree before removing any blank cells. */
final class ResearchNoteHoles {
    private static final int TREE_CANDIDATES = 8;

    private ResearchNoteHoles() {}

    static void carve(Map<ResearchHex, ResearchNote.Cell> grid, int requested, IntUnaryOperator random) {
        if (requested <= 0) return;
        var protectedCells = connectingSubtree(grid, random);
        // Authored negatives may already separate the anchors. Never worsen such a board.
        if (protectedCells.isEmpty()) return;
        var candidates = new ArrayList<>(grid.keySet().stream()
                .filter(hex -> !grid.get(hex).occupied() && !protectedCells.get().contains(hex)).toList());
        int count = Math.min(requested, candidates.size());
        for (int i = 0; i < count; i++) {
            int selected = i + random.applyAsInt(candidates.size() - i);
            java.util.Collections.swap(candidates, i, selected);
            grid.remove(candidates.get(i));
        }
    }

    /**
     * Negatives are forbidden vertices. Each randomized breadth-first traversal spans the
     * anchor's accessible component; the union of its root-to-anchor paths is the unique
     * minimal terminal subtree of that tree (equivalent to pruning nonterminal leaves).
     * Keep the smallest of eight candidates. This approximates the graph's Steiner tree,
     * with bounded O(8 * (V + E)) work and O(V + E) storage on at most 61 cells.
     */
    static Optional<Set<ResearchHex>> connectingSubtree(Map<ResearchHex, ResearchNote.Cell> grid,
            IntUnaryOperator random) {
        var vertices = grid.keySet().stream().filter(hex -> !grid.get(hex).negative())
                .sorted(Comparator.comparingInt(ResearchHex::q).thenComparingInt(ResearchHex::r)).toList();
        var indices = new HashMap<ResearchHex, Integer>();
        var terminals = new ArrayList<Integer>();
        for (int i = 0; i < vertices.size(); i++) {
            indices.put(vertices.get(i), i);
            if (grid.get(vertices.get(i)).anchor()) terminals.add(i);
        }
        if (terminals.isEmpty()) return Optional.empty();
        int[][] neighbors = new int[vertices.size()][];
        for (int i = 0; i < vertices.size(); i++) {
            int[] adjacent = new int[6]; int count = 0;
            for (int side = 0; side < 6; side++) {
                var index = indices.get(vertices.get(i).neighbor(side));
                if (index != null) adjacent[count++] = index;
            }
            neighbors[i] = Arrays.copyOf(adjacent, count);
        }
        Set<ResearchHex> best = null;
        int[] parent = new int[vertices.size()], queue = new int[vertices.size()];
        for (int attempt = 0; attempt < TREE_CANDIDATES; attempt++) {
            Arrays.fill(parent, -2);
            int root = terminals.get(random.applyAsInt(terminals.size()));
            parent[root] = -1; queue[0] = root;
            for (int head = 0, tail = 1; head < tail; head++) {
                int current = queue[head];
                // Shuffling in place retains a deterministic result for the same RNG seed.
                int[] adjacent = neighbors[current];
                for (int i = adjacent.length - 1; i > 0; i--) {
                    int selected = random.applyAsInt(i + 1);
                    int saved = adjacent[i]; adjacent[i] = adjacent[selected]; adjacent[selected] = saved;
                }
                for (int next : adjacent) if (parent[next] == -2) {
                    parent[next] = current; queue[tail++] = next;
                }
            }
            var subtree = new HashSet<ResearchHex>();
            for (int terminal : terminals) {
                if (parent[terminal] == -2) return Optional.empty();
                for (int current = terminal; current != -1; current = parent[current])
                    if (!subtree.add(vertices.get(current))) break;
            }
            if (best == null || subtree.size() < best.size()) best = subtree;
            if (best.size() == terminals.size()) break;
        }
        return Optional.of(Set.copyOf(best));
    }
}
