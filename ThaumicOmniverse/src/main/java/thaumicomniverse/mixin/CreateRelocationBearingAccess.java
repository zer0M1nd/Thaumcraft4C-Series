package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=MechanicalBearingBlockEntity.class,remap=false)
public interface CreateRelocationBearingAccess {
    @Accessor("movedContraption") void omniverse$detach(ControlledContraptionEntity entity);
}
