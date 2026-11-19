package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.TaintacleEntity;
import tc4revival.content.GiantTaintacleEntity;

public  class TaintacleRenderer<T extends TaintacleEntity> extends MobRenderer<T,TaintacleModel<T>> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/taintacle.png");
    public TaintacleRenderer(EntityRendererProvider.Context context,int length,float shadow) { super(context,new TaintacleModel<>(length),shadow); }
    @Override public ResourceLocation getTextureLocation(T entity) { return TEXTURE; }
    @Override protected void scale(T entity,PoseStack pose,float partial) { if (entity instanceof GiantTaintacleEntity) pose.scale(1.33f,1.33f,1.33f); }
    @Override protected RenderType getRenderType(T entity,boolean visible,boolean translucent,boolean glowing) {
        return visible || translucent ? RenderType.entityTranslucent(TEXTURE) : glowing ? RenderType.outline(TEXTURE) : null;
    }
}
