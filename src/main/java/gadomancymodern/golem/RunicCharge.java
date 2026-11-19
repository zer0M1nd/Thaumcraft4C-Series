package gadomancymodern.golem;
public class RunicCharge {
    public record Hit(float remainingDamage,float nextCharge){}
    public static float charged(int elapsed,int limit){return Math.min(limit,elapsed*.02f);}
    public static Hit hit(float charge,float amount){float full=(float)Math.floor(charge);if(full<=0)return new Hit(amount,charge);float rest=full-amount,fraction=charge-full;return rest<=0?new Hit(-rest,fraction):new Hit(0,(float)Math.floor(rest)+fraction);}
    public static int ticks(float charge){return Math.round(charge/.02f);}
}
