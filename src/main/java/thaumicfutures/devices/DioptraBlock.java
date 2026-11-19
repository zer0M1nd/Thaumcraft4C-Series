package thaumicfutures.devices;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public class DioptraBlock extends BaseEntityBlock {
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public DioptraBlock() { super(Properties.copy(Blocks.STONE).strength(2).noOcclusion());registerDefaultState(defaultBlockState().setValue(POWERED,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(POWERED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(POWERED,context.getLevel().hasNeighborSignal(context.getClickedPos())); }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving) {
        if(!level.isClientSide && state.getValue(POWERED)!=level.hasNeighborSignal(pos))level.setBlock(pos,state.cycle(POWERED),3);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!(level.getBlockEntity(pos) instanceof Dioptra tile) || !player.mayBuild())return InteractionResult.PASS;
        var held=player.getItemInHand(hand);boolean insert=Dioptra.shardType(held)!=null;
        if(insert || held.isEmpty() && player.isShiftKeyDown()) {
            if(!level.isClientSide) {
                var old=tile.replaceShard(insert?held.copyWithCount(1):ItemStack.EMPTY);
                if(insert && !player.getAbilities().instabuild)held.shrink(1);
                if(!old.isEmpty() && !player.addItem(old))player.drop(old,false);
                level.playSound(null,pos,tc4revival.content.TCSounds.CRYSTAL.get(),net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);
            }
        } else if(held.isEmpty()) {
            if(level.isClientSide)thaumicfutures.devices.client.DeviceClient.openDioptra(tile);
        } else return InteractionResult.PASS;
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof Dioptra tile)popResource(level,pos,tile.replaceShard(ItemStack.EMPTY));
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) { return level.getBlockEntity(pos) instanceof Dioptra tile && tile.active()?Math.min(15,tile.nodes().size()):0; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new Dioptra(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return level.isClientSide?null:createTickerHelper(type,Devices.DIOPTRA_TILE.get(),(l,p,s,t)->t.tick()); }
}
