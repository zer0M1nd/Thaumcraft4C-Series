package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

public final class FunnelBlock extends BaseEntityBlock {
    public FunnelBlock(){super(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3,8).noOcclusion());}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return Block.box(0,0,0,16,2,16);}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return net.minecraft.world.phys.shapes.Shapes.block();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new FunnelEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Tinkerer.FUNNEL_ENTITY.get(),(world,pos,s,entity)->entity.tick());}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!(level.getBlockEntity(pos) instanceof FunnelEntity entity))return InteractionResult.PASS;
        if(!level.isClientSide) {
            if(!entity.inventory.getStackInSlot(0).isEmpty()) {var stack=entity.remove();if(!player.getInventory().add(stack))player.drop(stack,false);}
            else if(entity.inventory.isItemValid(0,player.getItemInHand(hand)))entity.inventory.setStackInSlot(0,player.getItemInHand(hand).split(1));
            else return InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof FunnelEntity entity)Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),entity.remove());super.onRemove(state,level,pos,next,moving);}
}
