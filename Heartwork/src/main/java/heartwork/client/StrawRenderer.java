package heartwork.client;

import heartwork.content.StrawEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public class StrawRenderer extends MobRenderer<StrawEntity,StrawModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("heartwork:textures/entity/slash_straw.png");
    public StrawRenderer(EntityRendererProvider.Context context) { super(context,new StrawModel(context.bakeLayer(StrawModel.LAYER)),0); }
    @Override public ResourceLocation getTextureLocation(StrawEntity entity) { return TEXTURE; }
}
