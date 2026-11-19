package tc4revival.client;

/** FXBeamPower uses separate bare-eye modifiers for the ribbon and receiving flare. */
public class VisBeamVisibility {
    private VisBeamVisibility() {}
    public static float ribbon(float opacity,boolean revealed){return opacity*(revealed?1f:.1f);}
    public static float flare(float opacity,boolean revealed){return opacity*(revealed?1f:.2f);}
}
