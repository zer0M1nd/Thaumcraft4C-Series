package ttmodern;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class TabletBlock extends BaseEntityBlock implements tc4revival.content.WandInteractable {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public TabletBlock(){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3,50).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return Block.box(0,0,0,16,2,16);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new TabletEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Tinkerer.TABLET_ENTITY.get(),(world,pos,s,entity)->entity.tick());}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(player.getItemInHand(hand).getItem() instanceof tc4revival.content.CastingWandItem)return rotate(level,pos,player);
        if(player instanceof net.minecraft.server.level.ServerPlayer server&&level.getBlockEntity(pos) instanceof TabletEntity entity)
            net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,p)->new TabletMenu(id,inventory,entity),net.minecraft.network.chat.Component.translatable("block.thaumic_tinkerer.dynamism_tablet")),pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private InteractionResult rotate(Level level,BlockPos pos,Player player){
        if(!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof TabletEntity entity){
            if(entity.idle())level.setBlock(pos,entity.getBlockState().cycle(FACING),3);
            else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("ttmisc.animationTablet.notRotatable"),true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context){return context.getPlayer()==null?InteractionResult.PASS:rotate(context.getLevel(),context.getClickedPos(),context.getPlayer());}
    @Override public java.util.Optional<ItemStack> onWandUse(Level level,ItemStack wand,Player player){return java.util.Optional.empty();}
    @Override public void onWandUseTick(ItemStack wand,Player player,int remaining){}
    @Override public void onWandRelease(ItemStack wand,Level level,Player player,int remaining){}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof TabletEntity entity){entity.stopBreaking();if(!level.isClientSide){var stack=entity.items.extractItem(0,Integer.MAX_VALUE,false);Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),stack);}}
        super.onRemove(state,level,pos,next,moving);
    }
}
