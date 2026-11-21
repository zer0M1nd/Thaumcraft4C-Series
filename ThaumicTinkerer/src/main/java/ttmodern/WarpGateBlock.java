package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class WarpGateBlock extends BaseEntityBlock {
    public WarpGateBlock(){super(Properties.copy(Blocks.STONE).strength(5,2000).noOcclusion());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new WarpGateEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world,BlockState state,BlockEntityType<T> type){return world.isClientSide?null:createTickerHelper(type,Tinkerer.WARP_GATE_ENTITY.get(),(w,p,s,e)->e.tick());}
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.getItemInHand(hand).is(Tinkerer.SKY_PEARL.get())&&!SkyPearlItem.bound(player.getItemInHand(hand)))return InteractionResult.PASS;
        if(!world.isClientSide&&world.getBlockEntity(pos) instanceof WarpGateEntity gate)gate.open(player,false);return InteractionResult.sidedSuccess(world.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level world,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&world.getBlockEntity(pos) instanceof WarpGateEntity gate){for(int i=0;i<10;i++)Containers.dropItemStack(world,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,gate.items.getStackInSlot(i));world.updateNeighbourForOutputSignal(pos,this);}super.onRemove(state,world,pos,next,moving);}
}
