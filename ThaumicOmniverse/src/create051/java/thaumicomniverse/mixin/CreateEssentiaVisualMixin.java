package thaumicomniverse.mixin;

import com.simibubi.create.content.contraptions.render.FlwContraption;
import com.jozufozu.flywheel.core.model.WorldModelBuilder;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import thaumicomniverse.compat.create.MovingEssentiaRendering;
import java.util.Collection;

@Mixin(value=FlwContraption.class,remap=false)
public abstract class CreateEssentiaVisualMixin {
    @Redirect(method="buildLayers",at=@At(value="INVOKE",target="Lcom/jozufozu/flywheel/core/model/WorldModelBuilder;withBlocks(Ljava/util/Collection;)Lcom/jozufozu/flywheel/core/model/WorldModelBuilder;"))
    private WorldModelBuilder omniverse$deferShells(WorldModelBuilder builder,Collection<StructureBlockInfo> blocks) {
        var filtered=new java.util.ArrayList<StructureBlockInfo>();
        var renderWorld=((com.simibubi.create.content.contraptions.render.ContraptionRenderInfo)(Object)this).renderWorld;
        for(var info:blocks)if(!MovingEssentiaRendering.inlineBody(renderWorld.getBlockEntity(info.pos())))filtered.add(info);
        return builder.withBlocks(filtered);
    }
}
