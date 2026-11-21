package automagy.api;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import java.util.Optional;

/** Read-only discovery hook. Implementations return a view; callers must not mutate returned stacks. */
@FunctionalInterface
public interface InventariumProvider {
    Optional<IItemHandler> inventory(BlockEntity tile,Direction side);
    /** Stable value identifying the whole returned view within one level. Equal identities must
     * expose identical slot ordering, even when accessed through different multiblock parts.
     * Defaults to one inventory per block. Do not load chunks to compute this key. */
    default Object identity(BlockEntity tile,Direction side){return tile.getBlockPos().immutable();}
}
