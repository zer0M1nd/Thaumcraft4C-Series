package tc4revival.client;

/** TC4's three-second top-left popup movement. */
public  class ResearchPopupMotion {
    private ResearchPopupMotion() {}
    public static int offset(long elapsed) {
        double fraction = elapsed / 3000.0;
        double edge = Math.max(0, 1 - Math.min(fraction * 2, 2 - fraction * 2) * 4);
        return -(int) (edge * edge * edge * edge * 36);
    }
}
