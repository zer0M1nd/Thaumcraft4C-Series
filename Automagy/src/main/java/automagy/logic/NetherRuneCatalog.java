package automagy.logic;
import java.util.*;
/** Built-in rune semantics, independent of Minecraft classes. A controller selects a variant once per cycle. */
public final class NetherRuneCatalog {
 public record Definition(String key,boolean creature,boolean speed,String output,int duration,Map<String,Integer> cost,Map<String,Integer> baitCost,int baitUses,int instability,int lureDistance){
  public Definition{cost=Map.copyOf(cost);baitCost=Map.copyOf(baitCost);}
  public NethermindStructure.Rune structure(){return new NethermindStructure.Rune(key,false,speed,creature,lureDistance);}
 }
 public static final Map<String,Definition> BUILTINS=definitions();
 private static Map<String,Definition> definitions(){var map=new LinkedHashMap<String,Definition>();
  add(map,"rock","minecraft:netherrack",60,Map.of("ignis",1,"terra",2));
  add(map,"lava","minecraft:lava",90,Map.of("ignis",6,"terra",2));
  add(map,"crystal","minecraft:nether_quartz_ore",100,Map.of("vitreus",3,"terra",1));
  add(map,"light","minecraft:glowstone",100,Map.of("lux",10,"sensus",3));
  add(map,"soul","minecraft:soul_sand",100,Map.of("terra",1,"vinculum",1,"spiritus",1));
  map.put("celerity",new Definition("celerity",false,true,"",0,Map.of(),Map.of(),0,0,6));
  mob(map,"zombie","minecraft:zombified_piglin",Map.of("corpus",2,"ignis",2),Map.of("fames",15,"cognitio",9,"lucrum",3),15,0,6);
  mob(map,"blaze","minecraft:blaze",Map.of("ignis",4,"praecantatio",2),Map.of("fames",10,"victus",5),10,0,6);
  mob(map,"ghast","minecraft:ghast",Map.of("exanimis",1,"spiritus",2),Map.of("fames",6,"permutatio",1,"spiritus",4,"aqua",4),6,200,8);
  mob(map,"skeleton","minecraft:skeleton",Map.of("mortuus",1,"corpus",1),Map.of("fames",20,"telum",5,"sensus",5),20,50,6);
  mob(map,"slime","minecraft:magma_cube",Map.of("limus",1,"ignis",2),Map.of("fames",16,"limus",1),8,0,6);
  mob(map,"bat","thaumcraft:fire_bat",Map.of("bestia",1,"ignis",1,"perditio",1),Map.of("fames",10,"humanus",1),10,0,6);
  mob(map,"wisp","automagy:nether_wisp",Map.of("auram",1,"ignis",2,"aer",1),Map.of("fames",8,"auram",3),8,375,6);
  return Collections.unmodifiableMap(map);
 }
 private static void add(Map<String,Definition> map,String key,String output,int ticks,Map<String,Integer> cost){map.put(key,new Definition(key,false,false,output,ticks,cost,Map.of(),0,0,6));}
 private static void mob(Map<String,Definition> map,String key,String output,Map<String,Integer> cost,Map<String,Integer> bait,int uses,int instability,int distance){map.put(key,new Definition(key,true,false,output,360,cost,bait,uses,instability,distance));}
 public static Definition basic(boolean lava){return lava?BUILTINS.get("lava"):new Definition("basic",false,false,"minecraft:netherrack",60,Map.of("ignis",2,"terra",4),Map.of(),0,0,6);}
 private NetherRuneCatalog(){}
}
