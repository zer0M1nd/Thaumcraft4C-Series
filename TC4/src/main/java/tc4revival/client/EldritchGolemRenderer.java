package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.EldritchGolemEntity;

public  class EldritchGolemRenderer extends MobRenderer<EldritchGolemEntity,EldritchGolemModel> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/eldritch_golem.png");
    public EldritchGolemRenderer(EntityRendererProvider.Context context) { super(context,new EldritchGolemModel(),.5f); }
    @Override public ResourceLocation getTextureLocation(EldritchGolemEntity entity) { return TEXTURE; }
    @Override protected void scale(EldritchGolemEntity entity,PoseStack pose,float partial) { pose.scale(2.15f,2.15f,2.15f); }
    @Override protected RenderType getRenderType(EldritchGolemEntity entity,boolean visible,boolean translucent,boolean glowing) {
        return visible || translucent ? RenderType.entityTranslucent(TEXTURE) : glowing ? RenderType.outline(TEXTURE) : null;
    }
}
