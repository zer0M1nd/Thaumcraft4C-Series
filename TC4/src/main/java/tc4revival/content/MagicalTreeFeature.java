package tc4revival.content;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Tree geometry entry points shared by configured features and the biome decoration pass. */
public  class MagicalTreeFeature extends Feature<NoneFeatureConfiguration> {
    public enum Kind { GREATWOOD, NATURAL_GREATWOOD, SPIDER_GREATWOOD, SILVERWOOD, FOREST_SILVERWOOD, FOREST_OAK, FOREST_TREE }
    private final Kind kind;
    public MagicalTreeFeature(Kind kind) { super(NoneFeatureConfiguration.CODEC); this.kind=kind; }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return switch (kind) {
            case FOREST_TREE -> {
                var random=context.random();
                if (random.nextInt(14)==0) yield SilverwoodGrowth.natural(context.level(),context.origin(),random,8,5);
                if (random.nextInt(10)==0) yield GreatwoodGrowth.natural(context.level(),context.origin(),random);
                // Fresh legacy large-oak generator: an obstructed tree must not permanently
                // shrink the singleton feature in every later chunk/world. No small-oak fallback.
                yield new ForestOakGrowth().grow(context.level(),context.origin(),random);
            }
            case FOREST_OAK -> new ForestOakGrowth().grow(context.level(),context.origin(),context.random());
            case GREATWOOD -> GreatwoodGrowth.grow(context.level(),context.origin(),context.random());
            case NATURAL_GREATWOOD -> GreatwoodGrowth.natural(context.level(),context.origin(),context.random());
            case SPIDER_GREATWOOD -> GreatwoodGrowth.grow(context.level(),context.origin(),context.random(),true);
            case SILVERWOOD -> SilverwoodGrowth.natural(context.level(),context.origin(),context.random(),7,4);
            case FOREST_SILVERWOOD -> SilverwoodGrowth.natural(context.level(),context.origin(),context.random(),8,5);
        };
    }
}
