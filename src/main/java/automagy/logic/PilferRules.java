package automagy.logic;

/** Greedy chest arithmetic. Mode 0 is coordinated separately for all collectors at one source. */
public final class PilferRules {
    public record Limit(long maximum, int overridesMode) {
        public Limit { maximum = Math.max(0, maximum); }
        public static Limit unlimited() { return new Limit(Long.MAX_VALUE, -1); }
    }
    public static int amount(int mode, double fraction, int desired, long source, long destination, int room, Limit filter) {
        if (mode < 1 || mode > 3 || source <= 0 || room <= 0) return 0;
        long wanted;
        if (filter.overridesMode == mode) wanted = filter.maximum;
        else wanted = switch (mode) {
            case 1 -> (long)Math.ceil((source + (double)destination) * boundedFraction(fraction)) - destination;
            case 2 -> source - Math.max(0, desired);
            default -> Math.max(0, desired) - destination;
        };
        return (int)Math.max(0, Math.min(Math.min(source, room), Math.min(wanted, filter.maximum)));
    }
    public static double boundedFraction(double value) { return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : .5; }
    public record Share(int[] amounts, double[] remainders) {
        public Share { amounts = amounts.clone(); remainders = remainders.clone(); }
        @Override public int[] amounts() { return amounts.clone(); }
        @Override public double[] remainders() { return remainders.clone(); }
    }
    /** Carries fractional entitlements across arrivals. Full destinations don't accrue additional debt. */
    public static Share share(int added, int available, double[] fractions, int[] room, double[] previous) {
        if (fractions.length != room.length || room.length != previous.length) throw new IllegalArgumentException("Mismatched collectors");
        int count = room.length; int[] result = new int[count]; double[] carry = previous.clone(), priority = new double[count];
        double totalFraction = 0;
        for (int i = 0; i < count; i++) if (room[i] > 0) totalFraction += boundedFraction(fractions[i]);
        int remaining = Math.max(0, available);
        for (int i = 0; i < count; i++) {
            carry[i] = Double.isFinite(carry[i]) ? Math.max(0, Math.min(Math.nextDown(1.0), carry[i])) : 0;
            if (room[i] <= 0) continue;
            double fraction = boundedFraction(fractions[i]) / Math.max(1, totalFraction);
            double entitlement = Math.max(0, added) * fraction;
            if (entitlement > room[i]) result[i] = room[i];
            else if (fraction >= 1) result[i] = Math.max(0, added);
            else {
                int whole = (int)Math.floor(entitlement); double part = entitlement - whole;
                if (part > 0) {
                    part += carry[i];
                    if (part >= 1) { whole++; part--; priority[i] = 1; }
                    carry[i] = part; priority[i] += part;
                }
                result[i] = Math.min(room[i], whole);
            }
        }
        // Prevent rounding carried by several collectors from taking more than currently exists.
        boolean[] assigned = new boolean[count];
        for (int pass = 0; pass < count; pass++) {
            int best = -1;
            for (int i = 0; i < count; i++) if (!assigned[i] && (best < 0 || priority[i] > priority[best])) best = i;
            assigned[best] = true; result[best] = Math.min(result[best], remaining); remaining -= result[best];
        }
        return new Share(result, carry);
    }
    private PilferRules() {}
}
