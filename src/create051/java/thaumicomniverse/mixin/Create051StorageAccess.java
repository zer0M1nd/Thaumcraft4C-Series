package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.BlockPos;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=MountedStorageManager.class,remap=false)
public interface Create051StorageAccess {
    @Accessor("storage") Map<BlockPos,MountedStorage> omniverse$items();
    @Accessor("fluidStorage") Map<BlockPos,MountedFluidStorage> omniverse$fluids();
}
