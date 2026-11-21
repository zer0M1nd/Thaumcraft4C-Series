package timodern;

import java.util.*;

/** Original 4.32 effects, including the optional Forbidden Magic infernus effect. */
public class InfusionEffects {
    public record Definition(String aspect, int cost, Set<String> hooks, int interval) {}
    public static final Map<String,Definition> ALL = new LinkedHashMap<>();
    static {
        add("aer",2,40,"tick");add("alienis",1,1,"contact");add("bestia",4,0,"spawn");
        add("cognitio",1,0,"enchant");add("exanimis",4,1,"tick");add("fabrico",4,0,"tint","punch");
        add("gelum",1,200,"tick");add("humanus",8,0,"spawn","hardness","resistance","destroy");
        add("ignis",1,0,"fire");add("infernus",4,1,"contact");add("iter",8,1,"tick","link");
        add("lucrum",4,4,"tick");add("lux",1,0,"light");add("machina",2,1,"signal","link");
        add("messis",1,0,"plant");add("mortuus",8,1,"tick");add("ordo",2,0,"stabilise");
        add("pannus",1,1,"fall","contact");add("perditio",4,1,"tick");add("permutatio",8,1,"tick","link");
        add("potentia",1,0,"signal");add("praecantatio",1,0,"visual");add("sano",1,50,"tick");
        add("sensus",4,0,"model","punch");add("spiritus",4,0,"collision");add("tempestas",1,50,"tick");
        add("terra",2,1,"tick");add("venenum",4,80,"tick");add("victus",8,1,"tick");
        add("vinculum",4,1,"tick");add("vitreus",2,0,"opacity");add("volatus",8,1,"tick");
    }
    public static void add(String id,int cost,int interval,String... hooks) {
        if(ALL.putIfAbsent(id,new Definition(id,cost,Set.of(hooks),interval))!=null)throw new IllegalArgumentException(id);
    }
    public static String local(String aspect){int c=aspect.indexOf(':');return c<0?aspect:aspect.substring(c+1);}
    public static Definition get(String aspect){return ALL.get(local(aspect));}
}
