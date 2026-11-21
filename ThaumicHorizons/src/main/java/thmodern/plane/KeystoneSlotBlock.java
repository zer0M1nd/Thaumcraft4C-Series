package thmodern.plane;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class KeystoneSlotBlock extends BaseEntityBlock implements thaumcraft.api.wands.IWandable {
 public static final net.minecraft.world.level.block.state.properties.BooleanProperty HAS_KEY=net.minecraft.world.level.block.state.properties.BooleanProperty.create("has_key");
 public KeystoneSlotBlock(){super(Properties.of().strength(2.5f).noOcclusion().sound(SoundType.STONE));registerDefaultState(stateDefinition.any().setValue(HAS_KEY,false));}
 @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(HAS_KEY);}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new KeystoneSlotEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,GatewayBlocks.SLOT_ENTITY.get(),(l,p,s,e)->e.tick());}
 @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),player.getItemInHand(hand)))return InteractionResult.FAIL;if(level.isClientSide)return InteractionResult.SUCCESS;return level.getBlockEntity(pos) instanceof KeystoneSlotEntity slot&&slot.interact(player,hand)?InteractionResult.CONSUME:InteractionResult.PASS;}
 @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context){if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;return context.getPlayer()!=null&&context.getLevel().getBlockEntity(context.getClickedPos()) instanceof KeystoneSlotEntity slot&&slot.open(context.getPlayer(),wand)?InteractionResult.CONSUME:InteractionResult.FAIL;}
 @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof KeystoneSlotEntity slot)slot.breakSlot();super.onRemove(state,level,pos,next,moving);}
}
