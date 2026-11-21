package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GantryContraptionEntity.class,remap=false)
public abstract class CreateFocusGantryMixin {
    @Shadow Direction movementAxis;
    @Shadow double axisMotion;
    @Inject(method="checkPinionShaft",at=@At("HEAD"),cancellable=true)
    private void omniverse$gantry(CallbackInfo ci){
        var entity=(GantryContraptionEntity)(Object)this;
        if(entity.level().isClientSide)return;
        var delta=thaumicomniverse.compat.create.NativeRelocation.gantry(entity,movementAxis);
        if(delta!=null){axisMotion=delta.dot(net.minecraft.world.phys.Vec3.atLowerCornerOf(movementAxis.getNormal()));entity.setContraptionMotion(delta);ci.cancel();}
    }
}
