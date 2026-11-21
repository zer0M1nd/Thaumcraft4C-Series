package automagy.logic;
public final class MobLureRules {
 public static boolean inRange(double dx,double dz,double y,int bottom,int top,double radius){return radius>=0&&y>=bottom&&y<=top&&dx*dx+dz*dz<=radius*radius;}
 public static boolean protectsTarget(boolean hasTarget,int age,int lastHurt,double distanceSquared){return hasTarget&&lastHurt>0&&age-lastHurt<100&&distanceSquared<100;}
 private MobLureRules(){}
}
