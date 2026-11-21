package thaumicomniverse.compat.create;

/** The same relative-speed ceiling used by Create's train/entity collision. */
public final class RelocationImpact {
    private RelocationImpact() {}
    public static double contribution(double hardness) {
        return Math.pow(Math.max(0, hardness) + .25, .6);
    }
    public static float damage(double mass, double relativeSpeed, boolean monster) {
        if (!Double.isFinite(mass) || !Double.isFinite(relativeSpeed) || mass <= 0 || relativeSpeed <= .35)
            return 0;
        double ceiling = Math.floor(relativeSpeed * (monster ? 32 : 16));
        return (float)Math.min(ceiling, 4 * Math.pow(mass * relativeSpeed * relativeSpeed, .65));
    }
}
