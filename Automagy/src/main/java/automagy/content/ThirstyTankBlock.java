package automagy.content;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class ThirstyTankBlock extends BaseEntityBlock implements thaumcraft.api.wands.IWandable {
 public ThirstyTankBlock(){super(Properties.of().strength(2.5f).noOcclusion().sound(SoundType.STONE));}
 @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState s){return new ThirstyTankBlockEntity(pos,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,Content.THIRSTY_TANK_TILE.get(),ThirstyTankBlockEntity::tick);}
 @Override public boolean hasAnalogOutputSignal(BlockState s){return true;}
 @Override public int getAnalogOutputSignal(BlockState s,Level l,BlockPos pos){return l.getBlockEntity(pos) instanceof ThirstyTankBlockEntity t?t.comparator():0;}
 @Override public InteractionResult onWandUseFirst(net.minecraft.world.item.ItemStack wand,net.minecraft.world.item.context.UseOnContext context){var player=context.getPlayer();if(player==null||!player.mayBuild()||!player.isShiftKeyDown())return InteractionResult.PASS;var level=context.getLevel();if(!level.isClientSide&&level.getBlockEntity(context.getClickedPos()) instanceof ThirstyTankBlockEntity tank){int glyph=tank.remove(context.getClickedFace());if(glyph>0)Block.popResource(level,context.getClickedPos(),new net.minecraft.world.item.ItemStack(Content.TANK_GLYPHS.get(glyph).get()));else if(glyph<0){tank.rejectCapacityRemoval(player);player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.automagy.tank.capacity"),true);}}return InteractionResult.sidedSuccess(level.isClientSide);}
 @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(!player.mayBuild())return InteractionResult.FAIL;if(!level.isClientSide&&level.getBlockEntity(pos) instanceof ThirstyTankBlockEntity tank){var held=player.getItemInHand(hand);for(var entry:Content.TANK_GLYPHS.entrySet())if(held.is(entry.getValue().get())){if(tank.install(hit.getDirection(),entry.getKey())&&!player.getAbilities().instabuild)held.shrink(1);return InteractionResult.CONSUME;}if(TankBottles.interact(tank,player,hand))return InteractionResult.CONSUME;if(!net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,level,pos,hit.getDirection()))player.displayClientMessage(net.minecraft.network.chat.Component.literal(tank.tank.getFluidAmount()+" / "+tank.tank.getCapacity()+" mB"),true);}return InteractionResult.sidedSuccess(level.isClientSide);}
}
