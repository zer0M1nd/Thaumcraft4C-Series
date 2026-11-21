package fmmodern;

/** Pure chance/quantity rules shared by gameplay and regression tests. Rolls are zero-based. */
public final class SinRules {
    private SinRules() {}
    public static final int ENVY_ROLL_BOUND = 175;
    public static final int LUST_ROLL_BOUND = 30;
    public static boolean envyCarrier(int roll) { return roll == 1; }
    /** Original 0.575 riding-crop bonus: four winning rolls out of thirty. */
    public static boolean cropLust(int roll) { return roll >= 0 && roll < 4; }
    public static boolean silverfish(int roll, int looting, boolean mountain) { return roll <= (mountain ? 2 + looting*2 : 1 + looting*3); }
    public static boolean skull(int roll, int looting, String type) { return roll <= switch(type) { case "player"->1+looting; case "skeleton","wither_skeleton"->3+looting; default->2+2*looting; }; }
    public static int educatedExperience(int vanilla,int level) { return Math.max(0,vanilla)*(1+3*Math.max(0,level)); }
    /** Equivalent to legacy retry-on-lust, without recursion. */
    public static int corruptionSin(int roll,boolean noLust){
        int bound=noLust?6:7;
        if(roll<0||roll>=bound)throw new IllegalArgumentException("Sin roll out of range");
        return noLust&&roll>=4?roll+1:roll;
    }
    public static float clusterChance(int level) { return .2f+level*.075f; }
}
