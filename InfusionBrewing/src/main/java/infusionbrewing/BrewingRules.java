package infusionbrewing;

/** Legacy 1.3a gameplay values, independent of registry initialization. */
public final class BrewingRules {
    private BrewingRules() {}
    public static boolean valid(int id) { return id>=1 && id<=23; }
    public static boolean instant(int id) { return id==6 || id==7; }
    public static boolean noAmplifier(int id) { return id==9 || id>=12 && id<=16; }
    public static boolean support(int id) { return switch(id) {case 1,3,5,6,8,10,11,12,13,14,16,21,22,23 -> true; default -> false;}; }
    public static int duration(int step) { if(step<0 || step>3)throw new IllegalArgumentException("Duration step");return 2400<<step; }
    public static int nextDuration(int ticks) { if(ticks<1 || ticks>=19200)return -1; int i=0;while(i<3 && ticks>duration(i))i++;return i==3?-1:duration(i+1); }
    public static int upkeepInterval(int amplifier) { return 1200/(Math.max(0,Math.min(3,amplifier))+1); }
    public static int nearby(int tier) { return new int[]{6,12,16,20,24}[tier]; }
    public static int spawnCount(int tier) { return 4+2*tier; }
    public static int minDelay(int tier) { return new int[]{200,160,80,40,20}[tier]; }
    public static int maxDelay(int tier) { return new int[]{800,320,160,80,40}[tier]; }
    public static int powerTier(int nearby,int count) {
        int a=4,b=4;for(int i=0;i<5;i++){if(nearby<=nearby(i)){a=i;break;}}
        for(int i=0;i<5;i++){if(count<=spawnCount(i)){b=i;break;}}return Math.min(a,b);
    }
    public static int speedTier(int min,int max) {
        int a=4,b=4;for(int i=0;i<5;i++){if(min>=minDelay(i)){a=i;break;}}
        for(int i=0;i<5;i++){if(max>=maxDelay(i)){b=i;break;}}return Math.min(a,b);
    }
}
