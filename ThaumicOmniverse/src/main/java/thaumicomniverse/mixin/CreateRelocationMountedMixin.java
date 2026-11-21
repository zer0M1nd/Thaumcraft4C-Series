package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumicomniverse.compat.create.RelocationTransfer;

/** A stabilized child changing vehicle must not run OrientedContraption's disassembly. */
@Mixin(value=OrientedContraptionEntity.class,remap=false)
public abstract class CreateRelocationMountedMixin extends AbstractContraptionEntity {
    protected CreateRelocationMountedMixin(EntityType<?> type,Level level){super(type,level);}
    @Inject(method={"stopRiding","m_8127_"},at=@At("HEAD"),cancellable=true,remap=false)
    private void omniverse$transferSeat(CallbackInfo ci){
        if(RelocationTransfer.remounting()){super.stopRiding();ci.cancel();}
    }
}
