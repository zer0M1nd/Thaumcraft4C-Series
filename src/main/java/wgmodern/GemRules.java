package wgmodern;
import java.util.*;
/** Original gem affinities are data, and can be extended without enum changes. */
public class GemRules {
 public static final List<String> PRIMALS=List.of("aer","ignis","aqua","terra","ordo","perditio");
 public static int potency(int filled,boolean oval,boolean affinity){return affinity?Math.max(0,Math.min(3,filled)-(oval?1:0)):0;}
 public static int brittle(int filled,boolean affinity,boolean aversion){return affinity?0:Math.min(3,filled)+(aversion?1:0);}
 public static int charge(String aspect,boolean mirrors){return switch(aspect){case "aer","ignis","terra"->1;case "aqua","ordo"->2;case "perditio"->mirrors?16:2;default->32;};}
 public static float backstab(float base,int level,boolean unaware){return base*(1+.2f*level+(unaware?.4f:0));}
}
