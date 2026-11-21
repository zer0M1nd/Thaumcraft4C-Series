package tc4revival.content;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/** TC4 biome parameters for the old biomes that still have a modern counterpart. */
public  class LegacyBiomeProfiles {
    private LegacyBiomeProfiles() {}

    // Forge 1.7.10 returned an EnumSet, so declaration order affects random aspect selection
    // and the first applicable greatwood probability. Composite types were expanded at registration.
    private enum Kind {
        HOT(100,"ignis"), COLD(80,"ordo"), SPARSE(80,"perditio"), DENSE(100,"ordo"),
        WET(80,"aqua"), DRY(80,"perditio"), SAVANNA(80,"aer",.2f),
        CONIFEROUS(100,"terra",.2f), JUNGLE(100,"herba"), SPOOKY(80,"spiritus"),
        DEAD(50,"mortuus"), LUSH(100,"herba",.5f), NETHER(120,"ignis"), END(80,"vacuos"),
        MUSHROOM(140,"ordo"), MAGICAL(100,null,1), OCEAN(120,"aqua"), RIVER(100,"aqua"),
        MESA(80,"ignis"), FOREST(120,"terra",1), PLAINS(80,"aer",.2f),
        MOUNTAIN(100,"aer"), HILLS(120,"aer"), SWAMP(120,"perditio",.2f),
        SANDY(80,"terra"), SNOWY(80,"ordo"), WASTELAND(80,"perditio"), BEACH(80,"terra");
        final int aura;
        final String aspect;
        final float greatwood;
        Kind(int aura,String aspect) { this(aura,aspect,0); }
        Kind(int aura,String aspect,float greatwood) { this.aura=aura; this.aspect=aspect; this.greatwood=greatwood; }
    }

    public record Profile(AuraNodeGeneration.BiomeProfile node,float greatwoodChance,boolean magical,boolean sandySurface) {}
    private static final Profile UNKNOWN=new Profile(new AuraNodeGeneration.BiomeProfile(100,List.of(),false),0,false,false);
    private static final Map<ResourceLocation,Profile> PROFILES=create();

    public static Profile get(Holder<Biome> biome) {
        if(biome.is(TCBiomes.MAGICAL_FORESTS))return get(TCBiomes.MAGICAL_FOREST.location());
        return biome.unwrapKey().map(key->get(key.location())).orElse(UNKNOWN);
    }
    public static Profile get(ResourceLocation id) { return PROFILES.getOrDefault(id,UNKNOWN); }

    private static Map<ResourceLocation,Profile> create() {
        var profiles=new HashMap<ResourceLocation,Profile>();
        vanilla(profiles,"ocean",Kind.OCEAN);
        vanilla(profiles,"deep_ocean",Kind.OCEAN);
        vanilla(profiles,"plains",Kind.PLAINS);
        vanilla(profiles,"desert",Kind.HOT,Kind.DRY,Kind.SANDY);
        vanilla(profiles,"windswept_hills",Kind.MOUNTAIN,Kind.HILLS);
        vanilla(profiles,"forest",Kind.FOREST);
        vanilla(profiles,"taiga",Kind.COLD,Kind.CONIFEROUS,Kind.FOREST);
        vanilla(profiles,"swamp",Kind.WET,Kind.SWAMP);
        vanilla(profiles,"river",Kind.RIVER);
        vanilla(profiles,"nether_wastes",Kind.HOT,Kind.DRY,Kind.NETHER);
        vanilla(profiles,"the_end",Kind.COLD,Kind.DRY,Kind.END);
        vanilla(profiles,"frozen_ocean",Kind.COLD,Kind.OCEAN,Kind.SNOWY);
        vanilla(profiles,"frozen_river",Kind.COLD,Kind.RIVER,Kind.SNOWY);
        vanilla(profiles,"snowy_plains",Kind.COLD,Kind.SNOWY,Kind.WASTELAND);
        vanilla(profiles,"mushroom_fields",Kind.MUSHROOM);
        vanilla(profiles,"beach",Kind.BEACH);
        vanilla(profiles,"jungle",Kind.HOT,Kind.WET,Kind.DENSE,Kind.JUNGLE);
        vanilla(profiles,"sparse_jungle",Kind.HOT,Kind.WET,Kind.JUNGLE,Kind.FOREST);
        vanilla(profiles,"stony_shore",Kind.BEACH);
        vanilla(profiles,"snowy_beach",Kind.COLD,Kind.BEACH,Kind.SNOWY);
        vanilla(profiles,"birch_forest",Kind.FOREST);
        vanilla(profiles,"dark_forest",Kind.SPOOKY,Kind.DENSE,Kind.FOREST);
        vanilla(profiles,"snowy_taiga",Kind.COLD,Kind.CONIFEROUS,Kind.FOREST,Kind.SNOWY);
        vanilla(profiles,"old_growth_pine_taiga",Kind.COLD,Kind.CONIFEROUS,Kind.FOREST);
        vanilla(profiles,"windswept_forest",Kind.MOUNTAIN,Kind.FOREST,Kind.SPARSE);
        vanilla(profiles,"savanna",Kind.HOT,Kind.SAVANNA,Kind.PLAINS,Kind.SPARSE);
        vanilla(profiles,"savanna_plateau",Kind.HOT,Kind.SAVANNA,Kind.PLAINS,Kind.SPARSE);
        vanilla(profiles,"badlands",Kind.MESA,Kind.SANDY);
        vanilla(profiles,"wooded_badlands",Kind.MESA,Kind.SPARSE,Kind.SANDY);
        put(profiles,"thaumcraft:tainted_land",true,Kind.MAGICAL,Kind.WASTELAND);
        put(profiles,"thaumcraft:eerie",false,Kind.MAGICAL,Kind.SPOOKY);
        put(profiles,"thaumcraft:eldritch_lands",false,Kind.MAGICAL,Kind.SPOOKY,Kind.END);
        put(profiles,"thaumcraft:magical_forest",false,Kind.MAGICAL,Kind.FOREST);
        return Map.copyOf(profiles);
    }
    private static void vanilla(Map<ResourceLocation,Profile> profiles,String path,Kind... kinds) {
        put(profiles,"minecraft:"+path,false,kinds);
    }
    private static void put(Map<ResourceLocation,Profile> profiles,String id,boolean tainted,Kind... kinds) {
        var ordered=EnumSet.copyOf(Arrays.asList(kinds));
        int aura=ordered.stream().mapToInt(kind->kind.aura).sum()/ordered.size();
        // Stream.toList retains MAGICAL's null aspect, unlike List.of/copyOf.
        var aspects=ordered.stream().map(kind->kind.aspect).toList();
        float trees=ordered.stream().filter(kind->kind.greatwood>0).findFirst().map(kind->kind.greatwood).orElse(0f);
        profiles.put(ResourceLocation.parse(id),new Profile(new AuraNodeGeneration.BiomeProfile(aura,aspects,tainted),trees,ordered.contains(Kind.MAGICAL),ordered.contains(Kind.SANDY)));
    }
}
