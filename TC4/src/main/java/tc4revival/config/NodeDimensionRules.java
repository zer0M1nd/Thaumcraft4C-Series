package tc4revival.config;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** Per-dimension wild-node probability; unspecified dimensions use the ordinary rate. */
public class NodeDimensionRules {
    public static final List<String> DEFAULTS = List.of("twilightforest:twilight_forest=2",
            "ad_astra:moon=0", "ad_astra:mars=0", "ad_astra:venus=0", "ad_astra:mercury=0", "ad_astra:glacio=0",
            "ad_astra:earth_orbit=0", "ad_astra:moon_orbit=0", "ad_astra:mars_orbit=0", "ad_astra:venus_orbit=0", "ad_astra:mercury_orbit=0", "ad_astra:glacio_orbit=0");
    private final Map<String,Double> multipliers;
    public NodeDimensionRules(List<? extends String> entries) {
        var parsed=new LinkedHashMap<String,Double>();
        for(String entry:entries) {
            if(!valid(entry))throw new IllegalArgumentException("Invalid node dimension rule: "+entry);
            int split=entry.lastIndexOf('=');
            parsed.put(entry.substring(0,split).strip(),Double.parseDouble(entry.substring(split+1).strip()));
        }
        multipliers=Map.copyOf(parsed);
    }
    public static boolean valid(Object value) {
        if(!(value instanceof String entry))return false;
        int split=entry.lastIndexOf('=');if(split<1)return false;
        String id=entry.substring(0,split).strip();
        if(!id.contains(":")||ResourceLocation.tryParse(id)==null)return false;
        try {double n=Double.parseDouble(entry.substring(split+1).strip());return Double.isFinite(n)&&n>=0;}
        catch(NumberFormatException failure){return false;}
    }
    public double multiplier(String dimension){return multipliers.getOrDefault(dimension,1d);}
    public static boolean roll(RandomSource random,int rarity,double multiplier) {
        if(multiplier<=0)return false;
        // Keep the old random stream exactly when the multiplier is unchanged.
        if(multiplier==1)return random.nextInt(rarity)==0;
        return random.nextDouble()<Math.min(1,multiplier/rarity);
    }
}
