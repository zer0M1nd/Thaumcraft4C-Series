package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;
@Mixin(value=LinearActuatorBlockEntity.class,remap=false)
public interface CreateFocusLinearAccess {
    @Invoker("getExtensionRange") int omniverse$range();
    @Invoker("toPosition") Vec3 omniverse$position(float offset);
    @Accessor("forceMove") void omniverse$force(boolean value);
}
