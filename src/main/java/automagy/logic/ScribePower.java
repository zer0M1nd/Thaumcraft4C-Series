package automagy.logic;

import java.util.function.IntUnaryOperator;

/** The legacy scribe consumes up to 10 centivis, granting two powered ticks per centivis. */
public final class ScribePower {
    private int ticks;
    public int remaining(){return ticks;}
    public boolean powered(){return ticks>0;}
    public void restore(int saved){ticks=Math.max(0,Math.min(20,saved));}
    /** A disconnected device cannot drain; already-paid charge still counts down. */
    public void tick(boolean linked,IntUnaryOperator drain){
        if(ticks>0)ticks--;
        if(ticks==0&&linked)ticks=2*Math.max(0,Math.min(10,drain.applyAsInt(10)));
    }
}
