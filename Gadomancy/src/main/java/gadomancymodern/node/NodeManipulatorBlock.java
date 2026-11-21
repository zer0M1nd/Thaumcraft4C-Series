package gadomancymodern.node;
import gadomancymodern.Gadomancy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public class NodeManipulatorBlock extends tc4revival.content.ArcanePedestalBlock {
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new NodeManipulatorEntity(pos,state);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(!(level.getBlockEntity(pos) instanceof NodeManipulatorEntity tile))return InteractionResult.PASS;if(level.isClientSide)return InteractionResult.SUCCESS;if(!tile.formed)return tile.form(player,hand)?InteractionResult.SUCCESS:InteractionResult.PASS;var held=player.getItemInHand(hand);if(tile.isEmpty()){if(!(held.getItem() instanceof tc4revival.content.CastingWandItem))return InteractionResult.PASS;tile.setItem(0,held.copyWithCount(1));held.shrink(1);}else{var stack=tile.removeItemNoUpdate(0);if(!player.addItem(stack))player.drop(stack,false);tile.sync();}return InteractionResult.SUCCESS;}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved){if(!next.is(this)&&!level.isClientSide&&level.getBlockEntity(pos) instanceof NodeManipulatorEntity tile)tile.breakStructure();super.onRemove(state,level,pos,next,moved);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Gadomancy.MANIPULATOR_BE.get(),(world,pos,block,tile)->tile.tick());}
    @Override public boolean hasAnalogOutputSignal(BlockState state){return false;}
}
