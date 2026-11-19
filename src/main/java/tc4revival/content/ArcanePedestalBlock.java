package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class ArcanePedestalBlock extends BaseEntityBlock {
    public ArcanePedestalBlock() { super(Properties.of().strength(3,15).noOcclusion().requiresCorrectToolForDrops()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.or(box(0,0,0,16,4,16),box(4,4,4,12,12,12),box(2,12,2,14,16,14)); }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return getShape(state,level,pos,context); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ArcanePedestalBlockEntity(pos,state); }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ArcanePedestalBlockEntity pedestal ? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(pedestal) : 0;
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ArcanePedestalBlockEntity pedestal)) return InteractionResult.PASS;
        if (pedestal.isEmpty() && player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!level.mayInteract(player,pos)) return InteractionResult.FAIL;
        float pitch;
        if (!pedestal.isEmpty()) {
            var stack=pedestal.removeItemNoUpdate(0); pedestal.sync();
            var dropped=new net.minecraft.world.entity.item.ItemEntity(level,player.getX(),player.getY(),player.getZ(),stack);
            dropped.setDefaultPickUpDelay(); level.addFreshEntity(dropped); pitch=1.5f;
        } else {
            pedestal.setItem(0,player.getItemInHand(hand).split(1)); player.getInventory().setChanged(); pitch=1.6f;
        }
        level.playSound(null,pos,SoundEvents.ITEM_PICKUP,SoundSource.BLOCKS,.2f,((level.random.nextFloat()-level.random.nextFloat())*.7f+1)*pitch);
        return InteractionResult.CONSUME;
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving) {
        if (!level.isClientSide && level.hasChunkAt(pos.above()) && !level.isEmptyBlock(pos.above()) && level.getBlockEntity(pos) instanceof ArcanePedestalBlockEntity pedestal) {
            Containers.dropContents(level,pos,pedestal); pedestal.clearContent();
        }
    }
    @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        return level.isClientSide?null:(world,pos,block,entity)->{if(entity instanceof ArcanePedestalBlockEntity pedestal)pedestal.extensions().tick();};
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!next.is(state.getBlock())&&level.getBlockEntity(pos) instanceof ArcanePedestalBlockEntity host)
            host.extensions().beforeRemove(moving?thaumcraft.api.devices.DeviceExtensions.Removal.MOVED:thaumcraft.api.devices.DeviceExtensions.Removal.REPLACED);

        if (!next.is(this) && !level.isClientSide && level.getBlockEntity(pos) instanceof ArcanePedestalBlockEntity pedestal) {
            Containers.dropContents(level,pos,pedestal); level.updateNeighbourForOutputSignal(pos,this);
        }
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack) {
        if (stack.hasCustomHoverName() && level.getBlockEntity(pos) instanceof ArcanePedestalBlockEntity pedestal) pedestal.setCustomName(stack.getHoverName().getString());
    }
}
