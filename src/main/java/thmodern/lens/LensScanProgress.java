package thmodern.lens;
/** Tick continuity is independent of frame rate and client packet frequency. */
public final class LensScanProgress {
    public static final int DURATION=80;
    private Object target;private long started,observed;private int progress;
    public int progress(){return progress;}
    public void clear(){target=null;progress=0;}
    public boolean observe(Object next,long tick){
        if(next==null){clear();return false;}
        if(target==null||!target.equals(next)||tick<observed||tick-observed>1){target=next;started=observed=tick;progress=0;return false;}
        observed=tick;progress=(int)Math.min(DURATION,tick-started);if(progress<DURATION)return false;clear();return true;
    }
}
