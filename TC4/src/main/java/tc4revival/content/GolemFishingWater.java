package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

/** Golem upgrades and surrounding water determine the category of native fishing loot. */
public  class GolemFishingWater {
    public record Quality(float bite,float junkAdjustment,float treasureAdjustment) {}
    private GolemFishingWater() {}
    public static boolean water(ServerLevel level,BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos) && level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER);
    }
    public static BlockPos find(GolemWorkHost host,int radius) {
        if (!(host.mob().level() instanceof ServerLevel level)) return null;
        var home=host.home(); var random=host.mob().getRandom();
        for (int attempt=0;attempt<radius*2;attempt++) {
            int x=home.getX()+random.nextInt(1+radius*2)-radius;
            int y=Mth.floor(home.getY()+random.nextInt(1+radius)-radius/2f);
            int z=home.getZ()+random.nextInt(1+radius*2)-radius;
            var pos=new BlockPos(x,y,z);
            if (water(level,pos) && level.isEmptyBlock(pos.above())) return pos;
        }
        return null;
    }
    public static Quality quality(ServerLevel level,BlockPos target) {
        float bite=0,junk=0,treasure=0;
        for (var direction:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
            var pos=target.relative(direction);
            if (!water(level,pos) || !level.isEmptyBlock(pos.above())) continue;
            bite+=.00003f; junk-=.005f; treasure+=.00125f;
            if (level.canSeeSky(pos.above())) { bite+=.00003f; junk-=.005f; treasure+=.00125f; }
            for (int depth=1;depth<=3;depth++) if (water(level,pos.below(depth))) { bite+=.000015f; treasure+=.001f; }
        }
        return new Quality(bite,junk,treasure);
    }
    public static String pool(float roll,GolemConfiguration config,Quality quality) {
        float junk=Mth.clamp(.1f-config.upgrades(5)*.025f+quality.junkAdjustment(),0,1);
        float treasure=Mth.clamp(.05f+config.upgrades(4)*.0125f+quality.treasureAdjustment(),0,1);
        return roll<junk?"junk":roll-junk<treasure?"treasure":"fish";
    }
}
