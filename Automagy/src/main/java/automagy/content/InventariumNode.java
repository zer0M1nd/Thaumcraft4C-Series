package automagy.content;

import java.util.List;
import net.minecraft.core.BlockPos;

/** Implemented by loaded scribes; the core never owns or teleports their inventory items. */
public interface InventariumNode {
    BlockPos linkedCore();
    void linkCore(BlockPos position);
    List<InventariumContents.Source> sources();
}
