package tc4revival.content;

/** Position rules shared with the legacy renderer, including its signed int multiplication. */
public  class TaintFibreAppearance {
    private TaintFibreAppearance() {}
    public static int overlay(int x,int y,int z,int side) {
        var random=new java.util.Random(side+y+x*z);
        return random.nextInt(100)<95?0:1+random.nextInt(3);
    }
    public static double[] plantOffset(int x,int y,int z) {
        long seed=(long)(x*3129871)^(long)z*116129781L^(long)y;
        seed=seed*seed*42317861L+seed*11L;
        return new double[]{(((float)(seed>>16&15)/15f)-.5)*.5,(((float)(seed>>24&15)/15f)-.5)*.5};
    }
    public static boolean hasCross(int variant,boolean solidBelow) {
        return variant>=3&&variant<=4 || (variant==1||variant==2)&&solidBelow;
    }
}
