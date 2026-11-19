package thaumicomniverse.mixin;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumicomniverse.compat.create.PowerFocusSessions;
@Mixin(value=KineticBlockEntity.class,remap=false)
public abstract class CreateFocusKineticsMixin implements thaumicomniverse.compat.create.KineticFocusView {
    @Unique private float omniverse$driveSpeed;
    @Unique private net.minecraft.core.Direction omniverse$driveFace;
    public float omniverse$driveSpeed(){return omniverse$driveSpeed;}
    public net.minecraft.core.Direction omniverse$driveFace(){return omniverse$driveFace;}
    @Inject(method="write",at=@At("TAIL"))
    private void omniverse$save(net.minecraft.nbt.CompoundTag tag,boolean client,CallbackInfo ci){
        PowerFocusSessions.write((KineticBlockEntity)(Object)this,tag,client);
    }
    @Inject(method="read",at=@At("TAIL"))
    private void omniverse$load(net.minecraft.nbt.CompoundTag tag,boolean client,CallbackInfo ci){
        if(!client)return;
        omniverse$driveSpeed=tag.getFloat("OmniDriveSpeed");
        int face=tag.contains("OmniDriveFace")?tag.getInt("OmniDriveFace"):-1;
        omniverse$driveFace=face<0?null:net.minecraft.core.Direction.from3DDataValue(face);
        thaumicomniverse.compat.create.KineticFocusEffects.update((KineticBlockEntity)(Object)this);
    }
    @Inject(method="getGeneratedSpeed",at=@At("RETURN"),cancellable=true)
    private void omniverse$generated(CallbackInfoReturnable<Float> cir){
        cir.setReturnValue(PowerFocusSessions.generated((KineticBlockEntity)(Object)this,cir.getReturnValue()));
    }
    @Inject(method="isSource",at=@At("RETURN"),cancellable=true)
    private void omniverse$source(CallbackInfoReturnable<Boolean> cir){
        if(PowerFocusSessions.generated((KineticBlockEntity)(Object)this,0)!=0)cir.setReturnValue(true);
    }
}
