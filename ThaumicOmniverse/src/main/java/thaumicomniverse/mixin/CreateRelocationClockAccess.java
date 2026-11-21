package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=ClockworkBearingBlockEntity.class,remap=false)
public interface CreateRelocationClockAccess {
    @Accessor("hourHand") ControlledContraptionEntity omniverse$hour();
    @Accessor("minuteHand") ControlledContraptionEntity omniverse$minute();
    @Accessor("hourHand") void omniverse$hour(ControlledContraptionEntity value);
    @Accessor("minuteHand") void omniverse$minute(ControlledContraptionEntity value);
    @Accessor("running") void omniverse$running(boolean value);
}
