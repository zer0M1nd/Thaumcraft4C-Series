package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public  class HungryChestBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public HungryChestBlock() { super(Properties.of().strength(2.5f).sound(SoundType.WOOD).noOcclusion()); registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var player=context.getPlayer(); var pos=context.getClickedPos();
        if (player!=null && Math.abs(player.getX()-pos.getX())<2 && Math.abs(player.getZ()-pos.getZ())<2) {
            if (player.getEyeY()-pos.getY()>2) return defaultBlockState().setValue(FACING,Direction.UP);
            if (pos.getY()-player.getEyeY()>0) return defaultBlockState().setValue(FACING,Direction.DOWN);
        }
        return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Block.box(1,0,1,15,14,15); }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Block.box(1,0,1,15,15,15); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new HungryChestBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return createTickerHelper(type,TCArcane.HUNGRY_CHEST_ENTITY.get(),HungryChestBlockEntity::tick); }
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int value) { super.triggerEvent(state,level,pos,id,value); var entity=level.getBlockEntity(pos); return entity!=null && entity.triggerEvent(id,value); }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HungryChestBlockEntity chest) player.openMenu(chest);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) { return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos)); }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof HungryChestBlockEntity chest) { Containers.dropContents(level,pos,chest); level.updateNeighbourForOutputSignal(pos,this); }
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if (level.isClientSide || !(entity instanceof ItemEntity item) || !item.isAlive() || !(level.getBlockEntity(pos) instanceof HungryChestBlockEntity chest)) return;
        var original=item.getItem(); var remainder=GolemItemTransfer.insert(new net.minecraftforge.items.wrapper.InvWrapper(chest),original,true);
        // setItem emits the shared eating event for both ground items and automation.
        if (remainder.isEmpty()) item.discard(); else item.setItem(remainder);
        chest.setChanged();
    }
}
