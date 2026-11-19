package thmodern.plane;
import java.util.*;
/** Immutable legacy environmental formulas; no game registries are needed to inspect a plan. */
public final class PlaneParameters {
 private final Map<String,Integer> aspects;private final long total;
 public PlaneParameters(Map<String,Integer> input){var copy=new LinkedHashMap<String,Integer>();input.forEach((key,value)->{if(key!=null&&value!=null&&value>0)copy.put(key,value);});aspects=Collections.unmodifiableMap(copy);total=copy.values().stream().mapToLong(Integer::longValue).sum();}
 public Map<String,Integer> aspects(){return aspects;}public int amount(String aspect){return aspects.getOrDefault(aspect,0);}public double fraction(String aspect){return total==0?0:amount(aspect)/(double)total;}
 public int radius(){return (int)Math.min(127,Math.max(32,total*.75));}
 private double weighted(double base,Map<String,Double> weights){for(var entry:weights.entrySet())base+=fraction(entry.getKey())*entry.getValue();return base;}
 private static double clamp(double value,double max){return Math.max(0,Math.min(max,value));}
 public double noise(){return clamp(weighted(50,Map.of("ordo",-100d,"perditio",100d,"motus",50d,"permutatio",100d,"volatus",50d,"iter",-100d)),100);}
 public double life(){return clamp(weighted(0,Map.ofEntries(Map.entry("lux",25d),Map.entry("tempestas",25d),Map.entry("victus",150d),Map.entry("venenum",-75d),Map.entry("mortuus",-100d),Map.entry("tenebrae",-25d),Map.entry("spiritus",50d),Map.entry("sano",150d),Map.entry("auram",50d),Map.entry("limus",50d),Map.entry("herba",100d),Map.entry("arbor",100d),Map.entry("bestia",100d),Map.entry("corpus",50d),Map.entry("exanimis",-50d),Map.entry("cognitio",25d),Map.entry("sensus",25d),Map.entry("humanus",25d),Map.entry("messis",50d),Map.entry("meto",25d),Map.entry("telum",-25d),Map.entry("fames",-25d),Map.entry("pannus",25d))),100);}
 public double temperature(){return clamp(weighted(.5,Map.of("ignis",1d,"aqua",-.5,"ordo",-.5,"vacuos",-.5,"lux",.5,"tempestas",-.25,"motus",.5,"gelum",-1d,"potentia",.5,"tenebrae",-.25)),1);}
 public double moisture(){return clamp(weighted(.5,Map.of("aqua",1d,"ignis",-.5,"perditio",-.25,"vacuos",-.25,"tempestas",.5,"limus",.5)),1);}
 public String biome(){if(amount("vitium")>0)return "thaumcraft:tainted_land";if(amount("alienis")>0||amount("exanimis")>0)return "thaumcraft:eerie";if(amount("praecantatio")>0||amount("auram")>0)return "thaumcraft:magical_forest";double t=temperature(),m=moisture();return t>.8?"minecraft:desert":t>.5?(m<.4?"minecraft:savanna":"minecraft:jungle"):t>.2?(m<.4?"minecraft:plains":"minecraft:forest"):"minecraft:snowy_plains";}
 public int color(java.util.function.ToIntFunction<String> colors){int red=0,green=0,blue=0;for(var key:aspects.keySet()){int rgb=colors.applyAsInt(key);double weight=fraction(key);red+=(int)((rgb>>16&255)*weight);green+=(int)((rgb>>8&255)*weight);blue+=(int)((rgb&255)*weight);}return red<<16|green<<8|blue;}
 public List<String> effects(){var result=new ArrayList<String>();for(var pair:List.of("motus:speed","volatus:jump_boost","sano:regeneration","iter:speed","instrumentum:haste","telum:strength","tutamen:resistance")){var parts=pair.split(":");if(amount(parts[0])>0)result.add("minecraft:"+parts[1]);}return List.copyOf(result);}
 public List<String> shells(){var result=new ArrayList<String>();if(amount("alienis")>0)result.add("eldritch");if(amount("tenebrae")>0)result.add("obsidian");if(amount("alienis")>0)result.add("eldritch");if(amount("vitreus")>0)result.add("glass");if(amount("gelum")>0)result.add("packed_ice");return List.copyOf(result);}
}
