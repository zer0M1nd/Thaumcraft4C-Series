package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.content.CrystalClusterBlock;
import tc4revival.content.ShardType;

/** Uses the base port's six-face attachment and crystal collision geometry. */
public final class DestabilizedCrystalBlock extends CrystalClusterBlock {
    public DestabilizedCrystalBlock() { super(ShardType.BALANCED); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new DestabilizedCrystalBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        return createTickerHelper(type,ConciliumBlocks.CRYSTAL_TILE.get(),DestabilizedCrystalBlockEntity::tick);
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var stack=new net.minecraft.world.item.ItemStack(this);
        var tile=builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if(tile instanceof DestabilizedCrystalBlockEntity crystal)
            net.minecraft.world.item.BlockItem.setBlockEntityData(stack,ConciliumBlocks.CRYSTAL_TILE.get(),crystal.saveWithoutMetadata());
        return java.util.List.of(stack);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,hit.getDirection(),player.getItemInHand(hand)))return InteractionResult.FAIL;
        if(level.getBlockEntity(pos) instanceof DestabilizedCrystalBlockEntity crystal)
            crystal.insertCrystal(player,player.getItemInHand(hand));
        return InteractionResult.CONSUME;
    }
}
