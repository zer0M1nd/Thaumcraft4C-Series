package tc4revival.content;

import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Menu packets can arrive after the corresponding block update or chunk unload. */
public  class MenuBlockEntities {
    private MenuBlockEntities() {}
    public static <T extends BlockEntity> T resolve(Inventory inventory,FriendlyByteBuf data,Class<T> type,
            Block block,BiFunction<BlockPos,BlockState,T> factory) {
        var pos=data.readBlockPos();var existing=inventory.player.level().getBlockEntity(pos);
        if(type.isInstance(existing)&&!existing.isRemoved())return type.cast(existing);
        // Keep the fixed slot layout safe for in-flight synchronization. No level means it cannot be used or ticked.
        var detached=factory.apply(pos,block.defaultBlockState());detached.setRemoved();return detached;
    }
}
