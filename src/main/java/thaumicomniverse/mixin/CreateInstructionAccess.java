package thaumicomniverse.mixin;

import com.simibubi.create.content.kinetics.transmission.sequencer.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value=Instruction.class,remap=false)
public interface CreateInstructionAccess {
    @Accessor("instruction") SequencerInstructions omniverse$type();
    @Accessor("instruction") void omniverse$type(SequencerInstructions value);
    @Accessor("value") int omniverse$value();
    @Accessor("value") void omniverse$value(int value);
    @Accessor("speedModifier") InstructionSpeedModifiers omniverse$modifier();
    @Accessor("speedModifier") void omniverse$modifier(InstructionSpeedModifiers value);
}
