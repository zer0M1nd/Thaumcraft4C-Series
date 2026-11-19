package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Surface obelisk ring terrain preparation and its randomly selected initial encounter. */
public  class EldritchRing {
    private EldritchRing() {}
    static boolean soil(Block block) {
        return thaumcraft.api.worldgen.NaturalGenerationRules.soil("thaumcraft:eldritch_ring",block,block==Blocks.STONE || block==Blocks.SAND || block==Blocks.PACKED_ICE || block==Blocks.GRASS_BLOCK || block==Blocks.GRAVEL || block==Blocks.DIRT);
    }
    private static boolean valid(net.minecraft.world.level.WorldGenLevel level,BlockPos ground) {
        int distance=0;
        // The old unbounded search only accepted results within two blocks.
        while (!level.isEmptyBlock(ground.above(distance))) { if (++distance>2) return false; }
        BlockPos floor=ground.above(distance-1); var state=level.getBlockState(floor);
        return soil(state.getBlock()) || (state.is(Blocks.SNOW) || state.is(Blocks.GRASS) || state.is(Blocks.FERN)) && soil(level.getBlockState(floor.below()).getBlock());
    }
    public static boolean generate(ServerLevel level,BlockPos ground,int width,int height,RandomSource random) {
        if (!EldritchMazeRuntime.canSchedule(level,ground.getX()>>4,ground.getZ()>>4,width,height)) return false;
        for (int[] offset:new int[][]{{-3,-3},{0,0},{3,0},{3,3},{0,3}}) if (!valid(level,ground.offset(offset[0],0,offset[1]))) return false;
        return generateStructure(level,ground,width,height,random);
    }
    // A registered start already validated its noise terrain. Do not reject it after
    // surface rules run (e.g. stone becomes snow/red sand), leaving /locate an empty start.
    public static boolean generateStructure(net.minecraft.world.level.WorldGenLevel level,BlockPos ground,int width,int height,RandomSource random) {
        return generateStructure(level,ground,width,height,random,random.nextInt(4));
    }
    public static boolean generateStructure(net.minecraft.world.level.WorldGenLevel level,BlockPos ground,int width,int height,RandomSource random,int boss) {
        if(boss<0||boss>3)throw new IllegalArgumentException("Invalid maze boss");
        if (ground.getY()-4<level.getMinBuildHeight() || ground.getY()+7>=level.getMaxBuildHeight()
                || !level.getLevel().getWorldBorder().isWithinBounds(ground.offset(-3,0,-3)) || !level.getLevel().getWorldBorder().isWithinBounds(ground.offset(3,0,3))) return false;
        for (int dx=-3;dx<=3;dx++) for (int dz=-3;dz<=3;dz++) {
            if (Math.abs(dx)==3 && Math.abs(dz)==3) continue;
            BlockPos column=ground.offset(dx,0,dz);
            for (int dy=-4;dy<5;dy++) {
                BlockPos pos=column.above(dy); var state=level.getBlockState(pos);
                if (dy<=0 || state.canBeReplaced() || !state.blocksMotion() || state.is(BlockTags.LEAVES))
                    level.setBlock(pos,(random.nextInt(4)==0 ? Blocks.OBSIDIAN : TCEldritch.OBSIDIAN_TILE.get()).defaultBlockState(),3);
                if (dy>0) level.removeBlock(pos,false);
            }
            if (dx==0 && dz==0) {
                level.setBlock(column.above(),TCEldritch.ALTAR.get().defaultBlockState(),3);
                level.setBlock(column,TCEldritch.OBSIDIAN_TILE.get().defaultBlockState(),3);
                int encounter=random.nextInt(10);
                if (level.getBlockEntity(column.above()) instanceof EldritchMonumentBlockEntity altar) {
                    if (encounter>=1 && encounter<=4) {
                        altar.configureSpawner(true,0);
                        for (Direction side:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
                            int facing=switch(side) { case NORTH->8; case SOUTH->0; case WEST->12; case EAST->4; default->0; };
                            CrimsonBanner.place(level,column.offset(-side.getStepX()*3,1,side.getStepZ()*3),facing);
                        }
                    } else if (encounter==6 || encounter==7) altar.configureSpawner(true,1);
                }
                level.setBlock(column.above(3),TCEldritch.OBELISK.get().defaultBlockState(),3);
                for (int dy=4;dy<=7;dy++) level.setBlock(column.above(dy),TCEldritch.OBELISK_SEGMENT.get().defaultBlockState(),3);
            } else if ((Math.abs(dx)==3 && Math.abs(dz%2)==1 || Math.abs(dz)==3 && Math.abs(dx%2)==1) && Math.abs(dx)!=Math.abs(dz)) {
                level.setBlock(column,TCEldritch.OBSIDIAN_TILE.get().defaultBlockState(),3);
                level.setBlock(column.above(),TCEldritch.CAP.get().defaultBlockState(),3);
            }
        }
        if (level instanceof ServerLevel server) {
            AuraNodeGeneration.createRandom(server,ground.above(2),random,false,true,false);
            return EldritchMazeRuntime.schedule(server,ground.getX()>>4,ground.getZ()>>4,width,height,random.nextLong(),boss);
        }
        long seed=random.nextLong();
        if (level.getBlockEntity(ground.above()) instanceof EldritchMonumentBlockEntity altar)
            altar.deferNaturalMaze(width,height,seed,boss);
        return true;
    }
}
