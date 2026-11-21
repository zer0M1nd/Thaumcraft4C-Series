package arcanearteries;

/** Published 0.8.5 mob yields; deliberately independent of modern BM's health-based yields. */
public final class SacrificeValues {
    public enum School { THAUMIC, BOTANICAL, EVIL, CUSTOM }
    public enum Victim { DEFAULT, VILLAGER, SLIME, ENDERMAN, ANIMAL }
    public static int self(School school, boolean hard, boolean soulFray, int custom, int customFray) {
        if (school == School.CUSTOM) return soulFray ? customFray : custom;
        int base = school == School.BOTANICAL ? 500 : 250;
        if (hard) base /= 2;
        return soulFray ? base / 10 : base;
    }
    public static int mob(School school, Victim kind) {
        if (school == School.CUSTOM) throw new IllegalArgumentException("Custom yields come from configuration");
        return switch (kind) {
            case VILLAGER -> school == School.BOTANICAL ? 4000 : 2250;
            case SLIME -> school == School.BOTANICAL ? 300 : 225;
            case ENDERMAN -> school == School.BOTANICAL ? 400 : 300;
            case ANIMAL -> school == School.BOTANICAL ? 500 : 375;
            default -> school == School.BOTANICAL ? 1000 : 625;
        };
    }
    private SacrificeValues() {}
}
