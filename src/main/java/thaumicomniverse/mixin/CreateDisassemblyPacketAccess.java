package thaumicomniverse.mixin;

import com.simibubi.create.content.contraptions.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value=ContraptionDisassemblyPacket.class,remap=false)
public interface CreateDisassemblyPacketAccess {
    @Accessor("entityID") int omniverse$entity();
    @Accessor("transform") StructureTransform omniverse$transform();
}
