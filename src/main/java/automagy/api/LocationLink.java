package automagy.api;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;

/** An item-held target; consumers must check dimension, range and loaded chunks before use. */
public interface LocationLink {
    Optional<GlobalPos> linkedPosition(ItemStack stack);
}
