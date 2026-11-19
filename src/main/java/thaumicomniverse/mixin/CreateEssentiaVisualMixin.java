package thaumicomniverse.mixin;

import com.simibubi.create.content.contraptions.render.ContraptionVisual;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.engine_room.flywheel.lib.model.baked.ForgeBlockModelBuilder;
import thaumicomniverse.compat.create.MovingEssentiaRendering;

/** Filter at the stable Flywheel builder boundary, before either version bakes its model. */
@Mixin(value=ContraptionVisual.class,remap=false)
public abstract class CreateEssentiaVisualMixin {
    @Redirect(method={"setupModel","setupStructure"},at=@At(value="NEW",target="dev/engine_room/flywheel/lib/model/baked/ForgeBlockModelBuilder"))
    private ForgeBlockModelBuilder omniverse$deferVesselShells(BlockAndTintGetter level,Iterable<BlockPos> positions) {
        var filtered=new java.util.ArrayList<BlockPos>();
        for(var pos:positions)if(!MovingEssentiaRendering.inlineBody(level.getBlockEntity(pos)))filtered.add(pos);
        return new ForgeBlockModelBuilder(level,filtered);
    }
}
