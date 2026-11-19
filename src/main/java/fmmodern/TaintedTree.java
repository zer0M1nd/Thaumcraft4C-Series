package fmmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class TaintedTree extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty STAGE=IntegerProperty.create("stage",0,1);
    public TaintedTree(){super(Properties.copy(Blocks.OAK_SAPLING).randomTicks());registerDefaultState(stateDefinition.any().setValue(STAGE,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(STAGE);}
    @Override public void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(l.getMaxLocalRawBrightness(p.above())>=9&&r.nextInt(7)==0)advance(s,l,p,r);}
    @Override public boolean isValidBonemealTarget(LevelReader l,BlockPos p,BlockState s,boolean client){return true;}
    @Override public boolean isBonemealSuccess(Level l,RandomSource r,BlockPos p,BlockState s){return r.nextFloat()<.45f;}
    @Override public void performBonemeal(ServerLevel l,RandomSource r,BlockPos p,BlockState s){advance(s,l,p,r);}
    private void advance(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(state.getValue(STAGE)==0)level.setBlock(pos,state.setValue(STAGE,1),4);else grow(level,pos,random);}
    public static boolean grow(ServerLevel level,BlockPos origin,RandomSource random){
        int height=4+random.nextInt(3);
        if(origin.getY()<=level.getMinBuildHeight()||origin.getY()+height+1>=level.getMaxBuildHeight())return false;
        if(!Content.TAINT_SAPLING.get().defaultBlockState().canSurvive(level,origin))return false;
        for(int y=0;y<=height+1;y++){int radius=y==0?0:y>=height-1?2:1;for(var pos:BlockPos.betweenClosed(origin.offset(-radius,y,-radius),origin.offset(radius,y,radius))){if(!level.isLoaded(pos))return false;var state=level.getBlockState(pos);if(!(state.isAir()||state.is(BlockTags.LEAVES)||state.is(Content.TAINT_SAPLING.get())||state.canBeReplaced()))return false;}}
        var placements=new LinkedHashMap<BlockPos,BlockState>();
        for(int y=height-3;y<=height;y++){int delta=y-height,radius=1-delta/2;for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)if(Math.abs(x)!=radius||Math.abs(z)!=radius||(random.nextInt(2)!=0&&delta!=0))placements.put(origin.offset(x,y,z),Content.TAINT_LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE,1));}
        for(int y=0;y<height;y++)placements.put(origin.above(y),Content.TAINT_LOG.get().defaultBlockState());
        // All space checks precede writes: obstructed growth cannot delete the sapling or half-build a tree.
        var soil=level.getBlockState(origin.below());
        if(soil.is(Blocks.GRASS_BLOCK)||soil.is(Blocks.MYCELIUM))level.setBlock(origin.below(),Blocks.DIRT.defaultBlockState(),3);
        placements.forEach((pos,state)->level.setBlock(pos,state,3));return true;
    }
}
