package tc4revival.content;

/** Shared 1.7.10 sword rules, before platform block classification and combat hooks. */
public  class LegacySwordRules {
    public enum Surface { COBWEB, PLANT, REPLACEABLE_PLANT, VINE, LEAVES, GOURD, OTHER }
    public static final int USE_DURATION = 72000;
    private LegacySwordRules() {}
    public static float attackModifier(float materialDamage) { return 4 + materialDamage; }
    public static int weaponAspect(float materialDamage) { return (int) (materialDamage + 1); }
    public static float destroySpeed(Surface surface) {
        return switch (surface) {
            case COBWEB -> 15;
            case PLANT, REPLACEABLE_PLANT, VINE, LEAVES, GOURD -> 1.5f;
            case OTHER -> 1;
        };
    }
    public static boolean harvests(Surface surface) { return surface == Surface.COBWEB; }
    public static int hitWear() { return 1; }
    public static int miningWear(float hardness) { return hardness == 0 ? 0 : 2; }
    public static float blockedDamage(float damage, boolean bypassesArmor) {
        return !bypassesArmor && damage > 0 ? (damage + 1) * .5f : damage;
    }
}
