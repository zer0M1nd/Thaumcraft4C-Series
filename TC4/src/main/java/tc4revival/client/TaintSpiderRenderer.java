package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.TaintSpiderEntity;

public  class TaintSpiderRenderer extends MobRenderer<TaintSpiderEntity,SpiderModel<TaintSpiderEntity>> {
    private static final ResourceLocation BODY=ResourceLocation.parse("thaumcraft:textures/models/taint_spider.png");
    private static final RenderType EYES=RenderType.eyes(ResourceLocation.parse("thaumcraft:textures/models/taint_spider_eyes.png"));
    public TaintSpiderRenderer(EntityRendererProvider.Context context) {
        super(context,new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)),.5f);
        addLayer(new EyesLayer<TaintSpiderEntity,SpiderModel<TaintSpiderEntity>>(this) {
            @Override public RenderType renderType() { return EYES; }
        });
    }
    @Override protected void scale(TaintSpiderEntity entity,PoseStack pose,float partialTick) { pose.scale(.4f,.5f,.4f); }
    @Override protected float getFlipDegrees(TaintSpiderEntity entity) { return 180; }
    @Override public ResourceLocation getTextureLocation(TaintSpiderEntity entity) { return BODY; }
}
