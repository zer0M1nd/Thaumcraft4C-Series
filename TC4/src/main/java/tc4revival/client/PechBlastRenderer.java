package tc4revival.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.PechBlastEntity;

/** TC4's curse projectile is represented entirely by its trail and burst particles. */
public  class PechBlastRenderer extends EntityRenderer<PechBlastEntity> {
    public PechBlastRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = .1f; }
    @Override public ResourceLocation getTextureLocation(PechBlastEntity entity) { return ResourceLocation.parse("thaumcraft:textures/misc/particles.png"); }
}
