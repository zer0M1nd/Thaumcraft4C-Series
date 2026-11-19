package gadomancymodern.automation;

import java.util.function.BooleanSupplier;

/** Original 15-tick growth and saturation schedule, independent of world rendering. */
public class ProtectionCycle {
    protected int range,saturation;
    public int range(){return range;}
    public int saturation(){return saturation;}
    public void restore(int range,int saturation){this.range=Math.max(0,Math.min(15,range));this.saturation=Math.max(0,Math.min(15,saturation));}
    /** False matches the legacy early return while saturated (including its entity-update skip). */
    public boolean advance(int power,BooleanSupplier consume){
        if(range==0)saturation=0;
        if(saturation>0){saturation--;return false;}
        power=Math.max(0,Math.min(15,power));boolean decrease=range>power;
        if(range<=power&&power>0){decrease=true;if(consume.getAsBoolean()){if(range<power){range++;saturation=16-range;}decrease=false;}}
        if(decrease&&range>0)range--;
        return true;
    }
}
