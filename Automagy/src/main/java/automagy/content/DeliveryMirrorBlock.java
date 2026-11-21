package automagy.content;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class DeliveryMirrorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty SOCKETED=BooleanProperty.create("socketed");
    public final boolean multi;
    public DeliveryMirrorBlock(boolean multi){super(Properties.of().strength(1.5f).noOcclusion().sound(SoundType.GLASS));this.multi=multi;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP).setValue(SOCKETED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,SOCKETED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getClickedFace());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return switch(state.getValue(FACING)){
        case UP->box(1,0,1,15,2,15);case DOWN->box(1,14,1,15,16,15);
        case NORTH->box(1,1,14,15,15,16);case SOUTH->box(1,1,0,15,15,2);
        case WEST->box(14,1,1,16,15,15);case EAST->box(0,1,1,2,15,15);};}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!player.mayBuild())return InteractionResult.PASS;
        if(level.getBlockEntity(pos) instanceof DeliveryMirrorBlockEntity mirror){
            var held=player.getItemInHand(hand);
            if(held.getItem() instanceof CrystalEyeItem&&!multi){if(!level.isClientSide)mirror.installEye(player,hand);return InteractionResult.sidedSuccess(level.isClientSide);}
            if(held.getItem() instanceof tc4revival.content.CastingWandItem){
                if(!level.isClientSide){if(multi){boolean linked=mirror.core()!=null||InventariumBlockEntity.nearest(level,pos,true).map(core->core.linkMemory(pos)).orElse(false);player.displayClientMessage(net.minecraft.network.chat.Component.translatable(linked?"message.automagy.scribe.linked":"message.automagy.scribe.no_core"),true);}else if(player.isShiftKeyDown())mirror.removeEye(player);}
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){
        if(!level.isClientSide&&entity instanceof ItemEntity item&&item.isAlive()&&!item.isOnPortalCooldown()&&level.getBlockEntity(pos) instanceof DeliveryMirrorBlockEntity mirror){
            var rest=mirror.insert(item.getItem(),false);if(rest.getCount()!=item.getItem().getCount()){if(rest.isEmpty())item.discard();else item.setItem(rest);}
        }
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof DeliveryMirrorBlockEntity mirror)mirror.removed();super.onRemove(state,level,pos,next,moving);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new DeliveryMirrorBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Content.DELIVERY_MIRROR_TILE.get(),DeliveryMirrorBlockEntity::tick);}
}
