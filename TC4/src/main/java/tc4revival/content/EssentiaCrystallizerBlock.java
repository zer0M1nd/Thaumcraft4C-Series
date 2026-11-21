package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public  class EssentiaCrystallizerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=DirectionProperty.create("facing");
    public EssentiaCrystallizerBlock(){super(Properties.of().strength(.5f,3).sound(SoundType.METAL).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getClickedFace().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EssentiaCrystallizerBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,TCArcane.ESSENTIA_CRYSTALLIZER_ENTITY.get(),(world,pos,current,tile)->tile.tick());}
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int value){super.triggerEvent(state,level,pos,id,value);return level.getBlockEntity(pos)!=null && level.getBlockEntity(pos).triggerEvent(id,value);}
}
