package thaumicomniverse.mixin;

import java.util.List;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;

@Mixin(value=SequencerInstructions.class,remap=false)
public interface CreateInstructionDefinitionAccess {
    @Accessor("translationKey") String omniverse$title();
    @Accessor("parameterKey") String omniverse$parameter();
    @Accessor("hasValueParameter") boolean omniverse$hasValue();
    @Accessor("hasSpeedParameter") boolean omniverse$hasSpeed();
    @Accessor("background") AllGuiTextures omniverse$background();
    @Accessor("maxValue") int omniverse$max();
    @Accessor("shiftStep") int omniverse$shift();
    @Accessor("defaultValue") int omniverse$defaultValue();
    @Invoker("formatValue") String omniverse$format(int value);
    @Invoker("getOptions") static List<Component> omniverse$options(){throw new AssertionError();}
}
