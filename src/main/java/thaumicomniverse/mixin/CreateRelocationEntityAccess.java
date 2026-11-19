package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=AbstractContraptionEntity.class,remap=false)
public interface CreateRelocationEntityAccess {
    @Invoker("setContraption") void omniverse$replaceWithHardware(Contraption contraption);
}
