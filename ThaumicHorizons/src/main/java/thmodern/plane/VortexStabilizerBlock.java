package thmodern.plane;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import thmodern.Horizons;
public final class VortexStabilizerBlock extends BaseEntityBlock implements thaumcraft.api.wands.IWandable {
 public static final DirectionProperty FACING=BlockStateProperties.FACING;
 public VortexStabilizerBlock(){super(Properties.of().strength(10).noOcclusion().sound(SoundType.METAL));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
 @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getClickedFace());}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new VortexStabilizerEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Horizons.VORTEX_STABILIZER_ENTITY.get(),(l,p,s,e)->e.tick());}
 @Override public net.minecraft.world.InteractionResult onWandUseFirst(net.minecraft.world.item.ItemStack wand,net.minecraft.world.item.context.UseOnContext context){var player=context.getPlayer();if(player==null||!context.getLevel().mayInteract(player,context.getClickedPos())||!player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),wand))return net.minecraft.world.InteractionResult.FAIL;if(!context.getLevel().isClientSide){if(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof VortexStabilizerEntity entity)entity.release();context.getLevel().setBlock(context.getClickedPos(),context.getLevel().getBlockState(context.getClickedPos()).setValue(FACING,context.getClickedFace()),3);player.swing(context.getHand());}return net.minecraft.world.InteractionResult.SUCCESS;}
}
