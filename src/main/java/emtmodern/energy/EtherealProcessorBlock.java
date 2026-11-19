package emtmodern.energy;
import emtmodern.ElectroMagicTools;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
public class EtherealProcessorBlock extends WandChargerBlock {
    public static final BooleanProperty ACTIVE=BlockStateProperties.LIT;
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(ACTIVE);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EtherealProcessorEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,ElectroMagicTools.PROCESSOR_BE.get(),EtherealProcessorEntity::tick);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer server&&level.getBlockEntity(pos) instanceof EtherealProcessorEntity machine)net.minecraftforge.network.NetworkHooks.openScreen(server,machine,pos);return InteractionResult.sidedSuccess(level.isClientSide);}
    @Override public void onRemove(BlockState old,Level level,BlockPos pos,BlockState next,boolean moving){if(!old.is(next.getBlock())&&level.getBlockEntity(pos) instanceof EtherealProcessorEntity machine)Containers.dropContents(level,pos,machine);super.onRemove(old,level,pos,next,moving);}
    @Override public java.util.List<ItemStack> getWrenchDrops(Level level,BlockPos pos,BlockState state,BlockEntity entity,Player player,int fortune){return java.util.List.of(new ItemStack(asItem()));}
}
