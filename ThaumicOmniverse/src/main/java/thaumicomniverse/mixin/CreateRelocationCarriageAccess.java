package thaumicomniverse.mixin;
import com.simibubi.create.content.trains.entity.Carriage;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=Carriage.class,remap=false)
public interface CreateRelocationCarriageAccess {
    @Accessor("serialisedPassengers") java.util.Map<Integer,CompoundTag> omniverse$passengers();
}
