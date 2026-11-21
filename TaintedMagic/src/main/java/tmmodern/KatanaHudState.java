package tmmodern;

/** Original HUDHandler fade and ItemKatana rune opacity, independent of GL state. */
public class KatanaHudState {
    private float equipped;

    public float advance(boolean held, float partialTicks) {
        equipped = Math.max(0, Math.min(30, equipped + (held ? partialTicks : -partialTicks)));
        return equipped / 30;
    }

    public void reset() { equipped = 0; }

    public static float pulse(int ticks, int rune) {
        return (float) Math.sin((ticks + rune * 5) / 10f) * .3f;
    }

    public static float backgroundAlpha(int ticks, int rune, float fade) {
        return (pulse(ticks, rune) + .7f) * fade;
    }

    public static float progressAlpha(int ticks, int rune) {
        return Math.min(1, .9f + pulse(ticks, rune));
    }
}
