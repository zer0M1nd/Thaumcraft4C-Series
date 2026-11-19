package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Ascends below the canopy, wandering horizontally only when the air pair is obstructed. */
public  class ManaPodFeature extends Feature<NoneFeatureConfiguration> {
    public ManaPodFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world=context.level(); var random=context.random(); var origin=context.origin();
        int x=origin.getX(),z=origin.getZ();
        var block=TCArcane.MANA_POD.get();
        for (int y=Math.max(world.getMinBuildHeight()+1,origin.getY());y<world.getMaxBuildHeight()-1;y++) {
            var pos=new BlockPos(x,y,z);
            if (!world.hasChunkAt(pos) || y>=LegacySurfaceHeight.at(world,x,z)) return false;
            if (world.isEmptyBlock(pos) && world.isEmptyBlock(pos.below())) {
                if (block.defaultBlockState().canSurvive(world,pos)) {
                    if (!DecorationWrites.set(world,pos,block.defaultBlockState().setValue(ManaPodBlock.AGE,2+random.nextInt(5)),2)) return false;
                    if (world.getBlockEntity(pos) instanceof ManaPodBlockEntity pod) pod.grow(world);
                    return true;
                }
            } else {
                x=origin.getX()+random.nextInt(4)-random.nextInt(4);
                z=origin.getZ()+random.nextInt(4)-random.nextInt(4);
            }
        }
        return false;
    }
}
