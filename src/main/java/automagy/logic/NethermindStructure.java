package automagy.logic;
import java.util.*;
/** Geometry is relative to the Nethermind; callers never need to force-load chunks. */
public final class NethermindStructure {
 public record Offset(int x,int y,int z){}
 public record Rune(String key,boolean basic,boolean speed,boolean creature,int lureDistance){
  public Rune{Objects.requireNonNull(key);if((basic?1:0)+(speed?1:0)+(creature?1:0)>1)throw new IllegalArgumentException("Conflicting rune kinds");lureDistance=Math.max(6,lureDistance);}
 }
 public interface World {
  boolean loaded(int x,int y,int z);
  Rune rune(int x,int y,int z);
  /** 0: absent, 1: inactive lure, 2: active lure. */
  int lure(int x,int y,int z);
  /** Solidity excluding air and the base mod's flux gas/goo. */
  boolean obstructs(int x,int y,int z);
 }
 public record Result(String problem,List<Offset> selected,int speedRunes,int creatureRunes,int lureDepth){
  public Result{selected=List.copyOf(selected);}
  public boolean valid(){return problem.isEmpty();}public boolean unloaded(){return problem.equals("unloaded");}
 }
 public static final List<Offset> RUNES=offsets();
 public static final List<Offset> LURE_RING=List.of(new Offset(-1,0,-3),new Offset(0,0,-3),new Offset(1,0,-3),new Offset(-1,0,3),new Offset(0,0,3),new Offset(1,0,3),new Offset(-3,0,-1),new Offset(-3,0,0),new Offset(-3,0,1),new Offset(3,0,-1),new Offset(3,0,0),new Offset(3,0,1),new Offset(-2,0,-2),new Offset(-2,0,2),new Offset(2,0,-2),new Offset(2,0,2));
 private static List<Offset> offsets(){var list=new ArrayList<Offset>();for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++)if(x!=0||z!=0)list.add(new Offset(x,0,z));for(int[] dir:new int[][]{{-1,0},{1,0},{0,-1},{0,1}}){list.add(new Offset(dir[0]*2,0,dir[1]*2));list.add(new Offset(dir[0]*3,0,dir[1]*3));list.add(new Offset(dir[0]*3,-1,dir[1]*3));}return List.copyOf(list);}
 public static Result scan(World world){var selected=new ArrayList<Offset>();int speed=0,creatures=0,minDepth=6;
  // An unavailable required block suspends the machine rather than destroying an unloaded structure.
  for(var p:RUNES)if(!world.loaded(p.x,p.y,p.z))return fail("unloaded");
  for(var p:RUNES){var rune=world.rune(p.x,p.y,p.z);if(rune==null)return fail("missing_rune");if(rune.speed){speed=Math.min(12,speed+1);continue;}if(rune.basic)continue;selected.add(p);if(rune.creature){creatures++;minDepth=Math.max(minDepth,rune.lureDistance);}}
  if(creatures>0&&creatures!=selected.size())return fail("mixed_runes");
  if(creatures==0)return new Result("",selected,speed,0,0);
  if(creatures<4)return fail("insufficient_creature_runes");
  for(int depth=1;depth<=10;depth++){
   if(!world.loaded(0,-depth,0))return fail("unloaded");int lure=depth>=6?world.lure(0,-depth,0):0;
   if(lure>0){if(depth<minDepth)return fail("lure_too_close");if(lure!=2)return fail("inactive_lure");return new Result("",selected,speed,creatures,depth);}
   for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){if(!world.loaded(x,-depth,z))return fail("unloaded");if(world.obstructs(x,-depth,z))return fail("obstructed_shaft");}
  }return fail("missing_lure");
 }
 private static Result fail(String reason){return new Result(reason,List.of(),0,0,0);}
 public static int duration(int ticks,int speed){if(ticks<=0)return ticks;return Math.max(1,Math.round(ticks*(1-.075f*Math.max(0,Math.min(12,speed)))));}
 /** The first roll has four equal slots; fewer than four specifying runes leave basic outcomes. */
 public static boolean chooseBasic(int specifying,int fourWayRoll){if(fourWayRoll<0||fourWayRoll>=4)throw new IllegalArgumentException("Four-way roll");return specifying<4&&fourWayRoll>=Math.max(0,specifying);}
 public static int instability(int current,int speed,int stability,int extra,int randomExtra){long increase=80L+5L*Math.max(0,Math.min(12,speed))-10L*Math.max(0,stability)+Math.max(0,extra)+Math.max(0,randomExtra);return (int)Math.min(1000,Math.max(0,current)+Math.max(0,increase));}
 public static int stability(int[] pillars,int nitor){if(pillars.length!=4)throw new IllegalArgumentException("Four pillars");int sum=0;boolean equal=pillars[0]>0;for(int p:pillars){sum+=Math.max(0,Math.min(4,p));equal&=p==pillars[0];}return sum+(equal?3:0)+2*Math.max(0,Math.min(8,nitor));}
 private NethermindStructure(){}
}
