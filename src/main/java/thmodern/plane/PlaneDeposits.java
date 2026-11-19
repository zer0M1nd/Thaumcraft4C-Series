package thmodern.plane;
import java.util.*;
import java.util.random.RandomGenerator;

/** Ordered weights leave unsupported aspects as an unselected interval, without renormalizing. */
public final class PlaneDeposits {
 private PlaneDeposits(){}
 public static final List<String> POCKETS=List.of("terra","ignis","ordo","aer","aqua","perditio","metallum","vitreus","spiritus","praecantatio","auram","potentia","arbor","corpus","sensus","perfodio","instrumentum","lucrum","vitium","venenum","permutatio","cognitio","humanus","fames","fabrico");
 public static final List<String> CAVES=List.of("terra","ignis","ordo","aer","aqua","perditio","metallum","mortuus","exanimis","sensus","lucrum");
 private static final List<String> PRIMALS=List.of("air","fire","water","earth","order","entropy");
 public static Optional<String> select(PlaneParameters p,List<String> eligible,double roll){for(String aspect:eligible){double weight=p.fraction(aspect);if(weight>0&&roll<weight)return Optional.of(aspect);roll-=weight;}return Optional.empty();}
 public static String primal(String aspect){return switch(aspect){case "aer"->"air";case "ignis"->"fire";case "aqua"->"water";case "terra"->"earth";case "ordo"->"order";case "perditio"->"entropy";default->null;};}
 private static String ore(String name){return "thaumcraft:"+name+"_infused_stone";}
 public static String pocket(String aspect,RandomGenerator random){
  String primal=primal(aspect);if(primal!=null)return aspect.equals("ignis")&&!random.nextBoolean()?"minecraft:lava":ore(primal);
  return switch(aspect){
   case "metallum"->random.nextFloat()<=.5?"minecraft:iron_ore":random.nextFloat()<=.8?"minecraft:gold_ore":"thaumcraft:cinnabar_ore";
   case "praecantatio","auram"->ore(PRIMALS.get(random.nextInt(6)));
   case "vitreus"->"minecraft:quartz_block";case "spiritus"->"minecraft:soul_sand";case "potentia"->"minecraft:redstone_block";
   case "arbor"->random.nextBoolean()?"minecraft:oak_log":"thaumcraft:amber_ore";
   case "corpus","fames"->"thaumcraft:flesh_block";case "sensus"->"minecraft:lapis_ore";
   case "perfodio","instrumentum"->{float f=random.nextFloat();yield f<.4?"minecraft:iron_ore":f<.5?"minecraft:gold_ore":f<.6?"minecraft:diamond_ore":f<.7?"minecraft:emerald_ore":f<.8?"minecraft:lapis_ore":List.of("thaumcraft:cinnabar_ore",ore("air"),ore("fire"),ore("water"),ore("earth"),ore("order"),ore("entropy"),"thaumcraft:amber_ore").get(random.nextInt(8));}
   case "lucrum"->{float f=random.nextFloat();yield f<.4?"minecraft:gold_ore":f<.6?"minecraft:diamond_ore":f<.8?"minecraft:emerald_ore":"minecraft:lapis_ore";}
   case "vitium"->"thaumcraft:taint_crust";case "venenum"->"thaumcraft:liquid_death";case "permutatio"->"thaumcraft:cinnabar_ore";
   case "cognitio"->random.nextBoolean()?"minecraft:bookshelf":"thaumichorizons:brain";
   case "humanus"->List.of("minecraft:bookshelf","thaumichorizons:brain","thaumcraft:flesh_block").get(random.nextInt(3));
   case "fabrico"->"minecraft:crafting_table";default->throw new IllegalArgumentException("Unknown deposit aspect "+aspect);
  };
 }
 public static String cave(String aspect,RandomGenerator random){String primal=primal(aspect);if(primal!=null)return "thaumcraft:"+primal+"_crystal_cluster";return switch(aspect){case "mortuus","exanimis"->"minecraft:skeleton_skull";case "sensus"->"minecraft:lapis_block";case "lucrum"->random.nextBoolean()?"minecraft:gold_block":"minecraft:emerald_block";case "metallum"->random.nextBoolean()?"minecraft:gold_block":"minecraft:iron_block";default->throw new IllegalArgumentException("Unknown cave aspect "+aspect);};}
}
