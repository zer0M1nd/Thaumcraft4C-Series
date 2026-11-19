package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A solid silverwood trunk with the same node storage and ticking as a free aura node. */
public  class SilverwoodNodeLogBlock extends MagicalLogBlock implements EntityBlock {
    public SilverwoodNodeLogBlock() { super(true); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new AuraNodeBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return type==TCArcane.AURA_NODE_ENTITY.get() ? (world,pos,block,entity)->((AuraNodeBlockEntity)entity).tick() : null;
    }
    @Override public void playerWillDestroy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.player.Player player) {
        // Creative skips normal loot. Survival and tree-felling go through getDrops once.
        if(player.getAbilities().instabuild)AuraNodeBlock.dropEssence(level,pos);
        super.playerWillDestroy(level,pos,state,player);
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var drops=new java.util.ArrayList<>(super.getDrops(state,builder));
        drops.addAll(AuraNodeBlock.essenceDrops(builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY)));
        return drops;
    }
}
