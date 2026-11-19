package tc4revival.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class ForestBiomeServerTests {
    private ForestBiomeServerTests() {}
    /** Exercises the transformed native methods too, rather than just the palette helper. */
    @GameTest(template="tree_test_empty",timeoutTicks=100)
    public static void paletteHooksAreScopedAndDoNotMutateRegistryData(GameTestHelper test) {
        var registry=test.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var forest=registry.getHolderOrThrow(TCBiomes.MAGICAL_FOREST).value();
        var plains=registry.getHolderOrThrow(Biomes.PLAINS).value();
        int plainsGrass=plains.getGrassColor(12,34),plainsFoliage=plains.getFoliageColor();
        int forestGrass=forest.getGrassColor(12,34),forestFoliage=forest.getFoliageColor();
        var effects=forest.getSpecialEffects();
        try {
            ForestBiomeColors.update(forest,true);
            test.assertTrue(forest.getGrassColor(12,34)==0x66AACC && forest.getFoliageColor()==0x77CCEE,
                    "Native grass and foliage methods both apply the blue forest preference");
            test.assertTrue(plains.getGrassColor(12,34)==plainsGrass && plains.getFoliageColor()==plainsFoliage,
                    "The preference leaves other biomes untouched");
            test.assertTrue(forest.getSpecialEffects()==effects
                            && effects.getGrassColorOverride().orElseThrow()==forestGrass
                            && effects.getFoliageColorOverride().orElseThrow()==forestFoliage,
                    "The serializable biome effects remain unchanged");
            ForestBiomeColors.update(forest,false);
            test.assertTrue(forest.getGrassColor(12,34)==forestGrass && forest.getFoliageColor()==forestFoliage,
                    "Turning the preference off restores the received palette");
            ForestBiomeColors.update(null,false);
            test.assertTrue(forest.getGrassColor(12,34)==forestGrass,"Disconnect cleanup removes the local override");
            test.succeed();
        } finally { ForestBiomeColors.update(null,false); }
    }
}
