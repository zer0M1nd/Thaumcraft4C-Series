package tc4revival.content;

import tc4revival.research.PlayerKnowledge;

/** Cleansing never removes permanent warp; successful reductions reset the event counter. */
public  class WarpCleansing {
    private WarpCleansing() {}
    public static int wardDuration(int permanent) {
        return Math.min(32000,200000/Math.max(1,(int)Math.sqrt(Math.max(0,permanent))));
    }
    public static boolean wash(PlayerKnowledge knowledge,boolean warded,boolean inPureWater,float roll) {
        float chance=.33f+(warded ? .25f : 0)+(inPureWater ? .25f : 0);
        boolean sticky=roll<chance && knowledge.warp(PlayerKnowledge.WarpKind.STICKY)>0;
        boolean temporary=knowledge.warp(PlayerKnowledge.WarpKind.TEMPORARY)>0;
        if (sticky) knowledge.changeWarp(PlayerKnowledge.WarpKind.STICKY,-1);
        if (temporary) knowledge.setWarp(PlayerKnowledge.WarpKind.TEMPORARY,0);
        if (sticky || temporary) knowledge.setWarpCounter(knowledge.totalWarp());
        return sticky;
    }
}
