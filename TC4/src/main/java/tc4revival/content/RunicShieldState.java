package tc4revival.content;

import java.util.function.BooleanSupplier;

/** Transient per-entity shielding. The event adapter supplies a game-tick-derived clock. */
public  class RunicShieldState {
    public record Equipment(int maximum,int charged,int kinetic,int healing,int emergency) {}
    public record Hit(float remainder,boolean shielded,int kinetic,int healing,int emergency,boolean broken) {}
    private Equipment equipment=new Equipment(0,0,0,0,0);
    private float charge;
    private long nextCycle,kineticCooldown,healingCooldown,emergencyCooldown;
    /** Whole icons for the existing HUD/API; absorption itself retains fractional points. */
    public int charge() { return (int)Math.ceil(charge); }
    public float exactCharge() { return charge; }
    public int maximum() { return equipment.maximum(); }
    public Equipment equipment() { return equipment; }
    /** Explicit addon restoration; separate from vis recharge cadence and its payment. */
    public float restore(float amount) {
        if(!Float.isFinite(amount) || amount<=0)return 0;
        float before=charge;charge=Math.min(equipment.maximum(),charge+amount);return charge-before;
    }
    public void equipment(Equipment value) { equipment=value; charge=Math.max(0,Math.min(charge,value.maximum())); }
    public boolean recharge(long now,int interval,BooleanSupplier consume) {
        if (equipment.maximum()<=0 || charge>=equipment.maximum() || nextCycle>=now || !consume.getAsBoolean()) return false;
        nextCycle=now+Math.max(0L,(long)interval-equipment.charged()*500L);
        charge=Math.min(equipment.maximum(),charge+1); return true;
    }
    public Hit hurt(float damage,long now) {
        if (!Float.isFinite(damage) || damage<=0 || equipment.maximum()<=0 || charge<=0) return new Hit(damage,false,0,0,0,false);
        float remainder;
        if (charge>damage) { charge-=damage; remainder=0; }
        else { remainder=damage-charge; charge=0; }
        int kinetic=0,healing=0,emergency=0;
        if (charge<=0) {
            if (equipment.kinetic()>0 && kineticCooldown<now) { kinetic=equipment.kinetic(); kineticCooldown=now+20000; }
            if (equipment.healing()>0 && healingCooldown<now) { healing=equipment.healing(); healingCooldown=now+20000; }
            if (equipment.emergency()>0 && emergencyCooldown<now) { emergency=equipment.emergency(); emergencyCooldown=now+60000; charge=Math.min(equipment.maximum(),8*emergency); }
        }
        return new Hit(remainder,true,kinetic,healing,emergency,charge<=0);
    }
}
