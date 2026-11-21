package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=ControlledContraptionEntity.class,remap=false)
public interface CreateFocusControllerAccess {
    @Invoker("getController") IControlContraption omniverse$controller();
}
