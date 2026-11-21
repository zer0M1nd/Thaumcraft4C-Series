package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class MagnetBlock extends BaseEntityBlock implements tc4revival.content.WandInteractable {
    public static final BooleanProperty PULLING=BooleanProperty.create("pulling");
    public final boolean mob;
    public MagnetBlock(boolean mob){super(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(1.7f,1).noOcclusion());this.mob=mob;registerDefaultState(stateDefinition.any().setValue(PULLING,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PULLING);}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return Block.box(1,0,1,15,2,15);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MagnetEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Tinkerer.MAGNET_ENTITY.get(),(world,pos,s,entity)->entity.tick());}
    private InteractionResult open(Level level,BlockPos pos,Player player){
        if(!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(mob&&player instanceof net.minecraft.server.level.ServerPlayer server&&level.getBlockEntity(pos) instanceof MagnetEntity entity){
            net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,p)->new MagnetMenu(id,inventory,entity),net.minecraft.network.chat.Component.translatable("block.thaumic_tinkerer.mob_magnet")),pos);
            level.playSound(null,pos,tc4revival.content.TCSounds.KEY.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,.5f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(player.getItemInHand(hand).getItem() instanceof tc4revival.content.CastingWandItem)return open(level,pos,player);
        if(!level.isClientSide){level.setBlock(pos,state.cycle(PULLING),3);level.playSound(null,pos,net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,.5f);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context){return context.getPlayer()==null?InteractionResult.PASS:open(context.getLevel(),context.getClickedPos(),context.getPlayer());}
    @Override public java.util.Optional<ItemStack> onWandUse(Level level,ItemStack wand,Player player){return java.util.Optional.empty();}
    @Override public void onWandUseTick(ItemStack wand,Player player,int remaining){}
    @Override public void onWandRelease(ItemStack wand,Level level,Player player,int remaining){}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!level.isClientSide&&!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof MagnetEntity entity){var stack=entity.filter.extractItem(0,Integer.MAX_VALUE,false);if(!stack.isEmpty())Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),stack);}super.onRemove(state,level,pos,next,moving);}
}
