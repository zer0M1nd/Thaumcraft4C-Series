package tc4revival.research;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/** Ordered, single-success environmental scan, independent of world access and rendering. */
public  class ResearchTableBonuses {
    public record Source(String aspect, int chance, int randomAspectChance) {
        public static final Source NONE = new Source(null, 0, 0);
        public Source {
            if ((aspect == null) != (chance == 0) || chance < 0 || randomAspectChance < 0)
                throw new IllegalArgumentException("Invalid environmental source");
        }
        public static Source primal(String aspect, int chance) { return new Source(aspect, chance, 0); }
        public static Source bookshelf() { return new Source(null, 0, 300); }
        public static Source brainJar() { return new Source(null, 0, 200); }
    }
    public interface Environment {
        int tableY();
        int height();
        default int minimumHeight() { return 0; }
        default int maximumHeight() { return minimumHeight()+height(); }
        boolean darkCoveredNight();
        Source at(int relativeX, int relativeY, int relativeZ);
    }
    private ResearchTableBonuses() {}

    /** Recalculate every thirty seconds of game time. */
    public static int nextCounter(int counter) { return counter >= 599 ? 0 : counter + 1; }

    public static Set<String> recalculate(Set<String> existing, Environment world, List<String> declarationOrder,
            IntUnaryOperator random) {
        if (declarationOrder.isEmpty()) throw new IllegalArgumentException("Empty aspect catalog");
        var result = new LinkedHashSet<>(existing);
        if (world.darkCoveredNight() && draw(random, 20) == 0) result.add("perditio");
        for (float fraction : new float[] {0.5f, 0.66f, 0.75f}) {
            if (!result.contains("aer") && world.tableY() > world.minimumHeight()+world.height() * fraction && draw(random, 20) == 0) result.add("aer");
        }
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) for (int y = -8; y <= 8; y++) {
            int absoluteY = world.tableY() + y;
            if (absoluteY < world.minimumHeight() || absoluteY >= world.maximumHeight()) continue;
            var source = world.at(x, y, z);
            if (source.aspect() != null && !result.contains(source.aspect()) && draw(random, source.chance()) == 0) {
                result.add(source.aspect());
                return java.util.Collections.unmodifiableSet(result);
            }
            if (source.randomAspectChance() > 0 && draw(random, source.randomAspectChance()) == 0) {
                var missing=declarationOrder.stream().filter(aspect->!result.contains(aspect)).toList();
                if(!missing.isEmpty()) {
                    result.add(missing.get(draw(random,missing.size())));
                    return java.util.Collections.unmodifiableSet(result);
                }
            }
        }
        return java.util.Collections.unmodifiableSet(result);
    }
    private static int draw(IntUnaryOperator random, int bound) {
        int value = random.applyAsInt(bound);
        if (value < 0 || value >= bound) throw new IllegalArgumentException("Invalid bounded random result");
        return value;
    }
}
