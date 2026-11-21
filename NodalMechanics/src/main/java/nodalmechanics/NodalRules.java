package nodalmechanics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Original Nodal Mechanics 1.0-7 rules, independent of rendering and game registries. */
public class NodalRules {
    public static final int CAPACITY_PER_PHIAL = 15;
    public static final int ESSENTIA_PER_PHIAL = 150;
    public static Map<String, Integer> counts(String encoded, Predicate<String> known) {
        if (encoded == null || encoded.isEmpty() || encoded.length() > 519) return Map.of();
        var tags = encoded.split(",", -1);
        if (tags.length > 8) return Map.of();
        var result = new LinkedHashMap<String, Integer>();
        for (String tag : tags) {
            if (tag.isEmpty() || !known.test(tag)) return Map.of();
            result.merge(tag, 1, Integer::sum);
        }
        return Collections.unmodifiableMap(result);
    }
    /** Ordinals are TC4 NodeType: normal, unstable, dark, tainted, hungry, pure. */
    public static int type(int roll) {
        if (roll < 0 || roll >= 100) throw new IllegalArgumentException("Type roll outside 0..99");
        if (roll < 80) return 0;
        if (roll < 85) return 1;
        if (roll < 90) return 4;
        if (roll < 93) return 2;
        if (roll < 96) return 3;
        return 5;
    }
    /** -1 is no modifier; 0 bright, 1 pale, 2 fading. */
    public static int modifier(int presence, int value) {
        if (presence < 0 || presence >= 100 || value < 0 || value >= 50) throw new IllegalArgumentException("Invalid modifier roll");
        return presence >= 60 ? -1 : value < 20 ? 0 : value < 40 ? 1 : 2;
    }
}
