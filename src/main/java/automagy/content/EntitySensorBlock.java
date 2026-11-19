package automagy.content;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.Tags;
import net.minecraftforge.network.NetworkHooks;

public final class EntitySensorBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    public static final IntegerProperty MASK=IntegerProperty.create("mask",0,63);
    public EntitySensorBlock(){super(Properties.of().strength(2.5f).sound(SoundType.STONE).lightLevel(state->state.getValue(LIT)?9:0));registerDefaultState(stateDefinition.any().setValue(LIT,false).setValue(MASK,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(LIT,MASK);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EntitySensorBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Content.ENTITY_SENSOR_TILE.get(),EntitySensorBlockEntity::tick);}
    @Override public boolean isSignalSource(BlockState state){return true;}
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return level.getBlockEntity(pos) instanceof EntitySensorBlockEntity sensor?Math.min(15,sensor.count()):0;}
    @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return getSignal(state,level,pos,side);}
    @Override public boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return getSignal(state,level,pos,Direction.UP);}
    public static boolean blocked(Level level,BlockPos neighbor,Direction outward){
        if(!level.hasChunkAt(neighbor))return true;var state=level.getBlockState(neighbor);
        if(state.isAir()||state.canBeReplaced()||state.is(Tags.Blocks.GLASS)||state.is(Tags.Blocks.GLASS_PANES))return false;
        if(state.is(Blocks.REDSTONE_WIRE))return outward==Direction.UP;
        if(state.getBlock() instanceof RedcrystalBlock)return state.getValue(RedcrystalBlock.FACE)==outward;
        return true;
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!player.mayBuild())return InteractionResult.FAIL;
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof EntitySensorBlockEntity sensor)NetworkHooks.openScreen((ServerPlayer)player,sensor,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof EntitySensorBlockEntity sensor)sensor.dropContents();
        super.onRemove(state,level,pos,next,moving);
        if(!state.is(next.getBlock()))for(Direction dir:Direction.values())level.updateNeighborsAt(pos.relative(dir),this);
    }
}
