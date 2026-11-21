package thaumicomniverse.alignment;

import java.math.BigInteger;
import java.util.*;

/** Server-side signed aspect arithmetic. Zero components are removed before comparison. */
public class AlignmentMath {
    public record Offering(String identity, int count, int sign, Map<String, Long> aspects) {
        public Offering {
            Objects.requireNonNull(identity);
            if (identity.isBlank() || count < 1 || (sign != 1 && sign != -1))
                throw new IllegalArgumentException("Invalid offering");
            aspects = Collections.unmodifiableSortedMap(new TreeMap<>(aspects));
            aspects.forEach((key, value) -> {
                if (key.isBlank() || value < 0) throw new IllegalArgumentException("Invalid aspect value");
            });
        }
    }
    public record Summary(Map<String, Long> vector, long count, int kinds) {
        public Summary { vector = Collections.unmodifiableSortedMap(new TreeMap<>(vector)); }
    }
    public static Summary summarize(Collection<Offering> offerings) {
        var vector = new TreeMap<String, Long>();
        var kinds = new HashSet<String>();
        long count = 0;
        for (var offering : offerings) {
            count = Math.addExact(count, offering.count());
            kinds.add(offering.identity());
            offering.aspects().forEach((key, amount) -> {
                long signed = Math.multiplyExact(Math.multiplyExact(amount, offering.count()), offering.sign());
                vector.merge(key, signed, Math::addExact);
            });
        }
        vector.values().removeIf(value -> value == 0);
        return new Summary(vector, count, kinds.size());
    }
    public static boolean matches(Summary provided, Map<String, Long> target) {
        var normalized = new TreeMap<>(target);
        normalized.values().removeIf(value -> value == 0);
        return provided.vector().equals(normalized);
    }
    public static int penaltySteps(long count, int kinds, long referenceCount, int referenceKinds) {
        if (count < 1 || kinds < 1 || referenceCount < 1 || referenceKinds < 1)
            throw new IllegalArgumentException("Reference and offering counts must be positive");
        return thresholdSteps(count, referenceCount, 2, 1)
                + thresholdSteps(kinds, referenceKinds, 3, 2);
    }
    /** floor(log_(numerator/denominator)(actual/reference)), with exact integer thresholds. */
    private static int thresholdSteps(long actual, long reference, int numerator, int denominator) {
        BigInteger left = BigInteger.valueOf(actual), right = BigInteger.valueOf(reference);
        int result = 0;
        while (true) {
            left = left.multiply(BigInteger.valueOf(denominator));
            right = right.multiply(BigInteger.valueOf(numerator));
            if (left.compareTo(right) < 0) return result;
            result++;
        }
    }
    public static int yield(int minimum, int maximum, int steps) {
        if (minimum < 1 || maximum < minimum || maximum > 64 || steps < 0)
            throw new IllegalArgumentException("Invalid yield bounds");
        int value = maximum;
        for (int step = 0; step < steps && value > minimum; step++)
            value = Math.max(minimum, value * 3 / 4);
        return value;
    }
    public static double risk(double base, double stability, Summary actual, Summary reference) {
        if (!Double.isFinite(base) || !Double.isFinite(stability) || base < 0 || stability < 0
                || reference.count() < 1 || reference.kinds() < 1)
            throw new IllegalArgumentException("Invalid risk parameters");
        double excess = Math.max(0, (double) actual.count() / reference.count() - 1)
                + Math.max(0, (double) actual.kinds() / reference.kinds() - 1);
        return Math.max(0, base + excess - stability);
    }
}
