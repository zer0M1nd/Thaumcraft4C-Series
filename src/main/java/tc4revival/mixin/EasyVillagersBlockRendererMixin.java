package tc4revival.mixin;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tc4revival.client.LegacyVillagerRenderer;

@Mixin(targets="de.maxhenkel.easyvillagers.blocks.tileentity.render.VillagerRendererBase",remap=false)
public class EasyVillagersBlockRendererMixin {
    @Redirect(method="render",at=@At(value="NEW",target="net/minecraft/client/renderer/entity/VillagerRenderer"))
    private VillagerRenderer thaumcraft$professionSkin(EntityRendererProvider.Context context){
        return new LegacyVillagerRenderer(context);
    }
}
