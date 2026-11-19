package emtmodern.energy;

import emtmodern.ElectroMagicTools;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public class ElectricCloudBlock extends BaseEntityBlock {
    public ElectricCloudBlock(){super(Properties.of().strength(.1f).noCollission().noOcclusion().lightLevel(state->15).sound(SoundType.WOOL));}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(4.8,4.8,4.8,11.2,11.2,11.2);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ElectricCloudEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?createTickerHelper(type,ElectroMagicTools.CLOUD_BE.get(),(world,pos,block,entity)->emtmodern.client.ElectricCloudEffects.tick(world,pos)):null;}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){emtmodern.client.ElectricCloudEffects.sparkle(level,pos);}
}
