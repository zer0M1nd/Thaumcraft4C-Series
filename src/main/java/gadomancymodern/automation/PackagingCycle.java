package gadomancymodern.automation;

/** Stateful 47-tick cycle shared by the block entity and regression tests. */
public class PackagingCycle {
    private int progress=-1;
    private boolean lastPowered;
    private boolean observedPower;
    public int progress(){return progress;}
    public void restore(int value){progress=Math.max(-1,Math.min(47,value));}
    public void powerChanged(boolean powered,boolean canPack){
        if(observedPower&&powered&&!lastPowered&&canPack&&progress<0)progress=0;
        lastPowered=powered;observedPower=true;
    }
    public boolean tick(boolean autoStart,boolean canPack,long gameTime){
        if(progress>=47){progress=-1;return canPack;}
        if(progress>=0)progress++;
        else if(autoStart&&canPack&&gameTime%5==0)progress=0;
        return false;
    }
}
