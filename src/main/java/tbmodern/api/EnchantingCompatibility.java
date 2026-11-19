package tbmodern.api;

import java.util.List;
import java.util.Map;
import tbmodern.Configuration;

/** Pull contract for an optional Thaumic Tinkerer enchanting-table adapter. No external class loading. */
public final class EnchantingCompatibility {
    public record Entry(String enchantment,String texture,boolean treasure,Map<String,Integer> baseVis) {
        public Entry {baseVis=Map.copyOf(baseVis);}
    }
    private static Entry entry(String id,String texture,Map<String,Integer> vis) {
        return new Entry("thaumicbases:"+id,"thaumicbases:textures/enchantments/"+texture+".png",false,vis);
    }
    private static final List<Entry> ENTRIES=List.of(
        entry("elder_knowledge","elder_knowledge",Map.of("aer",8,"ordo",12,"terra",16)),
        entry("eldritch_bane","eldritch_bane",Map.of("ignis",12,"perditio",12,"ordo",8)),
        entry("magic_touch","magic_touched",Map.of("ignis",16,"ordo",16,"aqua",8,"perditio",12)),
        entry("tainted","tainted",Map.of("ignis",12,"perditio",16)),
        entry("vaporising","vaporising",Map.of("perditio",8,"ordo",12,"ignis",16)));
    /** Call when displaying/using the machine; server config can change between loaded worlds. */
    public static List<Entry> entries() {
        boolean enabled=Configuration.SPEC.isLoaded()?Configuration.TT_COMPAT.get():Configuration.TT_COMPAT.getDefault();
        return enabled?ENTRIES:List.of();
    }
    private EnchantingCompatibility() {}
}
