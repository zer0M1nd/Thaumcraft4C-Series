package thaumicomniverse.alignment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Reuses the original arcane pedestal geometry and display renderer. */
public class AlignmentPedestalBlock extends tc4revival.content.ArcanePedestalBlock {
    public final int sign;public final boolean stacking,core;
    public AlignmentPedestalBlock(int sign,boolean stacking,boolean core) {this.sign=sign;this.stacking=stacking;this.core=core;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return core?new AlignmentCoreEntity(pos,state):new AlignmentPedestalEntity(pos,state);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(core && level.getBlockEntity(pos) instanceof AlignmentCoreEntity altar) {
            if(player.getItemInHand(hand).getItem() instanceof tc4revival.content.CastingWandItem)return InteractionResult.PASS;
            if(altar.running() || !player.isShiftKeyDown() && (player.getItemInHand(hand).isEmpty() || !altar.isEmpty())) {
                if(player instanceof net.minecraft.server.level.ServerPlayer server && level.mayInteract(player,pos))net.minecraftforge.network.NetworkHooks.openScreen(server,altar,pos);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if(stacking && level.getBlockEntity(pos) instanceof AlignmentPedestalEntity pedestal && pedestal.isEmpty() && !player.getItemInHand(hand).isEmpty()) {
            if(!level.mayInteract(player,pos))return InteractionResult.FAIL;
            if(!level.isClientSide) {pedestal.setItem(0,player.getItemInHand(hand).split(Math.min(pedestal.getMaxStackSize(),player.getItemInHand(hand).getCount())));player.getInventory().setChanged();}
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state,level,pos,player,hand,hit);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!next.is(this)&&!level.isClientSide&&level.getBlockEntity(pos) instanceof AlignmentCoreEntity altar)altar.refund();
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,net.minecraft.world.level.block.Block block,BlockPos from,boolean moving) {
        if(core && level.getBlockEntity(pos) instanceof AlignmentCoreEntity altar && altar.running())return;
        super.neighborChanged(state,level,pos,block,from,moving);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return core&&!level.isClientSide?createTickerHelper(type,AlignmentContent.CORE_ENTITY.get(),(l,p,s,entity)->entity.tick()):null;
    }
}
