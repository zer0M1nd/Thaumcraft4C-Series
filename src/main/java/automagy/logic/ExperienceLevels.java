package automagy.logic;
/** Modern level curve; lifetime experienceTotal is not the player's spendable balance. */
public final class ExperienceLevels {
 public static int base(int level){double n=Math.max(0,level);double value=n<=16?n*n+6*n:n<=31?2.5*n*n-40.5*n+360:4.5*n*n-162.5*n+2220;return (int)Math.min(Integer.MAX_VALUE,value);}
 public static int total(int level,float progress,int nextLevelCost){long total=(long)base(level)+(long)(Math.max(0,Math.min(1,progress))*Math.max(0,nextLevelCost));return (int)Math.min(Integer.MAX_VALUE,total);}
 public static double level(int xp){int amount=Math.max(0,xp),lo=0,hi=21864;while(lo+1<hi){int mid=(lo+hi)/2;if(base(mid)<=amount)lo=mid;else hi=mid;}long delta=(long)base(lo+1)-base(lo);return lo+(delta<=0?0:(amount-base(lo))/(double)delta);}
 private ExperienceLevels(){}
}
