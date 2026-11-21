package ttmodern;

/** Independent movement policy for legacy tunable awakened boots. */
public final class BootMotion {
    public record Input(float forward,float strafe){}
    public static Input boosted(float forward,float strafe,boolean omni){return new Input(omni?finite(forward):Math.max(0,finite(forward)),omni?finite(strafe):0);}
    private static float finite(float value){return Float.isFinite(value)?Math.max(-1,Math.min(1,value)):0;}
    public static double multiplier(double value){return Double.isFinite(value)?Math.max(0,Math.min(10,value)):1;}
    private BootMotion(){}
}
