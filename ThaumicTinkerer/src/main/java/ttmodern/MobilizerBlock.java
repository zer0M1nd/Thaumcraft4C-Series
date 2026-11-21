package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class MobilizerBlock extends BaseEntityBlock {
    private final boolean relay;
    public MobilizerBlock(boolean relay) { super(Properties.copy(Blocks.IRON_BLOCK).strength(3,10));this.relay=relay; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return relay?new MobilizerRelayEntity(pos,state):new MobilizerEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world,BlockState state,BlockEntityType<T> type) {
        if(world.isClientSide)return null;
        return relay?createTickerHelper(type,Tinkerer.MOBILIZER_RELAY_ENTITY.get(),(w,p,s,e)->e.tick()):createTickerHelper(type,Tinkerer.MOBILIZER_ENTITY.get(),(w,p,s,e)->e.tick());
    }
    @Override public void setPlacedBy(Level world,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack) {
        if(placer instanceof net.minecraft.world.entity.player.Player player&&world.getBlockEntity(pos) instanceof MobilizerEntity mover)mover.owner(player.getUUID());
    }
}
