package tc4revival.content;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** One RNG stream for the forest override and its inherited decorator. */
public  class ForestDecorationFeature extends Feature<NoneFeatureConfiguration> {
    public ForestDecorationFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        boolean placed=TCWorldFeatures.FOREST_MOSS.get().place(context);
        placed|=TCWorldFeatures.FOREST_MUSHROOM.get().place(context);
        placed|=TCWorldFeatures.FOREST_ORES.get().place(context);
        placed|=TCWorldFeatures.FOREST_VEGETATION.get().place(context);
        return placed;
    }
}
