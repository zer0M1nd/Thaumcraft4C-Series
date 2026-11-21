package automagy.logic;

import java.util.function.Supplier;

/** One tick of the legacy boiler. Caller commits the ready batch from a captured fluid/aspect snapshot. */
public final class BoilerCycle {
    public static final int FLUID_CAPACITY=16000,BATCH=1000,ESSENTIA_CAPACITY=50;
    public record Fuel(int ticks,boolean boosted) {public Fuel{ticks=Math.max(0,ticks);}}
    public record State(int burn,int currentBurn,int cook,boolean boosted) {}
    private int burn,currentBurn,cook;
    private boolean boosted;
    public State state(){return new State(burn,currentBurn,cook,boosted);}
    public void restore(State state){burn=Math.max(0,state.burn());currentBurn=Math.max(0,state.currentBurn());cook=Math.max(0,state.cook());boosted=state.boosted();}
    public int outputInterval(){return boosted?20:40;}
    public static boolean canProcess(int fluid,long stored,long produced){return fluid>=BATCH&&stored>=0&&produced>0&&produced<=ESSENTIA_CAPACITY&&stored<=ESSENTIA_CAPACITY-produced;}
    public static int duration(int produced,int bellows){return Math.max(1,(int)(produced*10L*(1-.125*Math.max(0,Math.min(6,bellows)))));}
    /** Fuel supplier is invoked only when eligible to start burning; it must consume at most one fuel. */
    public boolean tick(int fluid,long stored,int produced,int bellows,Supplier<Fuel> fuel){
        if(burn>0)burn--;
        boolean ready=canProcess(fluid,stored,produced);
        if(burn==0&&ready){var next=fuel.get();burn=next.ticks();currentBurn=burn;if(burn>0)boosted=next.boosted();}
        if(burn<=0||!ready){cook=0;return false;}
        if(cook<Integer.MAX_VALUE)cook++;
        if(cook>=duration(produced,bellows)){cook=0;return true;}return false;
    }
}
