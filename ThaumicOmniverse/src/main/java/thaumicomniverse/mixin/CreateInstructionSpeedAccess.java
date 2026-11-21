package thaumicomniverse.mixin;

import java.util.List;
import com.simibubi.create.content.kinetics.transmission.sequencer.InstructionSpeedModifiers;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;

@Mixin(value=InstructionSpeedModifiers.class,remap=false)
public interface CreateInstructionSpeedAccess {
    @Accessor("label") Component omniverse$label();
    @Invoker("getOptions") static List<Component> omniverse$options(){throw new AssertionError();}
}
