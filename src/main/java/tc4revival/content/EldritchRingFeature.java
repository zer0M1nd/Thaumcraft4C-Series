package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import tc4revival.config.TCConfig;

/** Selects surface candidates during decoration; the server commits structures and maze data together. */
public  class EldritchRingFeature extends Feature<NoneFeatureConfiguration> {
    public EldritchRingFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!context.level().getLevel().dimension().equals(Level.OVERWORLD) || context.chunkGenerator() instanceof FlatLevelSource
                || !TCConfig.SERVER.generates(TCConfig.WorldFeature.STRUCTURES)) return false;
        var random=context.random(); int cx=context.origin().getX()>>4,cz=context.origin().getZ()>>4;
        int x=cx*16+random.nextInt(16),z=cz*16+random.nextInt(16);
        // A mound attempt occupied the first branch of the old shared structure roll.
        if (random.nextInt(150)==0 || random.nextInt(66)!=0) return false;
        int width=11+random.nextInt(6)*2,height=11+random.nextInt(6)*2;
        EldritchSurfaceGeneration.enqueue(context.level().getLevel(),new BlockPos(x,0,z),width,height,random.nextLong());
        return true;
    }
}
