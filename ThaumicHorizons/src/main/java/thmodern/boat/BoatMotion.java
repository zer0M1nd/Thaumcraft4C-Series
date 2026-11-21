package thmodern.boat;
/** Small material-specific addition to modern acceleration, retaining the old .45 speed cap. */
public final class BoatMotion {
    public record Horizontal(double x,double z){}
    public static Horizontal row(boolean metal,double x,double z,float yaw,boolean forward){
        if(forward){double extra=metal?.01:.005,angle=Math.toRadians(yaw);x-=Math.sin(angle)*extra;z+=Math.cos(angle)*extra;}
        double speed=Math.hypot(x,z);if(speed>.45){x*=.45/speed;z*=.45/speed;}
        return new Horizontal(x,z);
    }
    private BoatMotion(){}
}
