package thaumicomniverse.compat.gregtech;

import com.gregtechceu.gtceu.api.data.worldgen.*;
import com.gregtechceu.gtceu.common.data.GTOres;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

public final class ArcaneOreVein {
    public static void register(){
        GTOres.create(ArcaneGT.id("thaumic_vein"),vein->vein.clusterSize(36).density(.45f).weight(20)
                .layer(WorldGenLayers.STONE).heightRangeUniform(-32,48)
                .dimensions(ResourceLocation.parse(ModList.get().isLoaded("allthemodium")?"allthemodium:mining":"minecraft:overworld"))
                .layeredVeinGenerator(generator->generator.withLayerPattern(()->{
                    var pattern=GTLayerPattern.builder(new TagMatchTest(BlockTags.BASE_STONE_OVERWORLD));
                    for(var material:ArcaneMaterials.INFUSED.values())pattern.layer(layer->layer.weight(2).mat(material).size(1,2));
                    pattern.layer(layer->layer.weight(2).mat(com.gregtechceu.gtceu.common.data.GTMaterials.Cinnabar).size(1,2));
                    pattern.layer(layer->layer.weight(2).mat(ArcaneMaterials.AMBER).size(1,2));
                    return pattern.build();
                })));
    }
    private ArcaneOreVein(){}
}
