package thaumicomniverse.compat.create;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public interface ContraptionAccess {
    java.util.UUID omniverse$frameId();
    MountedArcaneStorage omniverse$storage();
    Map<BlockPos,CompoundTag> omniverse$updates();
}
