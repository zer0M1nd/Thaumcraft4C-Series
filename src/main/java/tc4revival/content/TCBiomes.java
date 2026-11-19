package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;

/** Dynamic biome entries are supplied by generated data packs. */
public  class TCBiomes {
    public static final net.minecraft.tags.TagKey<Biome> MAGICAL_FORESTS=net.minecraft.tags.TagKey.create(Registries.BIOME,ResourceLocation.parse("thaumcraft:magical_forests"));
    public static final ResourceKey<Biome> MAGICAL_FOREST=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("thaumcraft:magical_forest"));
    public static final ResourceKey<Biome> TAINTED_LAND=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("thaumcraft:tainted_land"));
    public static final ResourceKey<Biome> OUTER_LANDS=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("thaumcraft:outer_lands"));
    public static final ResourceKey<Biome> EERIE=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("thaumcraft:eerie"));
    private TCBiomes() {}
    public static boolean isTainted(LevelReader level,BlockPos pos) { return level.getBiome(pos).is(TAINTED_LAND); }
}
