package automagy.logic;
public final class DisjunctionRules {
 private DisjunctionRules(){}
 public static float damage(java.util.List<Integer> amounts){int highest=0;long sum=0;for(int amount:amounts)if(amount>0){highest=Math.max(highest,amount);sum+=amount;}return highest+(sum-highest)*.5f;}
 public static boolean inRange(double x,double y,double z){return x*x+y*y+z*z<8.5*8.5;}
}
