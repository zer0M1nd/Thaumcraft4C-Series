package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=Contraption.class,remap=false)
public interface Create051ContraptionAccess {
    @Accessor("storage") MountedStorageManager omniverse$legacyStorage();
}
