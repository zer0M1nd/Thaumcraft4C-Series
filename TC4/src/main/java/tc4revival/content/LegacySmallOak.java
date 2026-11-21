package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
/** The ordinary 4-6 block oak selected by the old base biome. No vines or modern beehives. */
 class LegacySmallOak {
    private LegacySmallOak(){}
    static boolean grow(WorldGenLevel level,BlockPos base,RandomSource random){
        int height=4+random.nextInt(3);
        if(base.getY()<=level.getMinBuildHeight()||base.getY()+height+1>=level.getMaxBuildHeight()||!level.hasChunksAt(base.offset(-2,-1,-2),base.offset(2,height+1,2)))return false;
        for(int y=0;y<=height+1;y++){
            int radius=y==0?0:y>=height-1?2:1;
            for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
                var s=level.getBlockState(base.offset(x,y,z));
                if(!s.isAir()&&!s.is(BlockTags.LEAVES)&&!s.is(Blocks.GRASS_BLOCK)&&!s.is(Blocks.DIRT)&&!s.is(BlockTags.LOGS)&&!s.is(Blocks.OAK_SAPLING)&&!s.is(Blocks.VINE))return false;
            }
        }
        var soil=level.getBlockState(base.below());
        if(!soil.canSustainPlant(level,base.below(),Direction.UP,(net.minecraftforge.common.IPlantable)Blocks.OAK_SAPLING))return false;
        if(soil.is(Blocks.GRASS_BLOCK)||soil.is(Blocks.FARMLAND))level.setBlock(base.below(),Blocks.DIRT.defaultBlockState(),2);
        for(int y=height-3;y<=height;y++){
            int relative=y-height,radius=1-relative/2;
            for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
                if(Math.abs(x)==radius&&Math.abs(z)==radius&&(random.nextInt(2)==0||relative==0))continue;
                var p=base.offset(x,y,z);if(!level.getBlockState(p).isSolidRender(level,p))level.setBlock(p,Blocks.OAK_LEAVES.defaultBlockState(),2);
            }
        }
        for(int y=0;y<height;y++){
            var p=base.above(y);var s=level.getBlockState(p);if(s.isAir()||s.is(BlockTags.LEAVES)||s.is(Blocks.VINE))level.setBlock(p,Blocks.OAK_LOG.defaultBlockState(),2);
        }
        return true;
    }
}
