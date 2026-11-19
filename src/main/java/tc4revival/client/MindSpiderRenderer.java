package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.MindSpiderEntity;

public  class MindSpiderRenderer extends MobRenderer<MindSpiderEntity,SpiderModel<MindSpiderEntity>> {
    private static final ResourceLocation BODY=ResourceLocation.parse("thaumcraft:textures/models/taint_spider.png");
    private static final RenderType EYES=RenderType.eyes(ResourceLocation.parse("thaumcraft:textures/models/taint_spider_eyes.png"));
    private float opacity;
    public MindSpiderRenderer(EntityRendererProvider.Context context) {
        super(context,new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)),0);
        model=new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)) {
            @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float red,float green,float blue,float alpha) {
                super.renderToBuffer(pose,out,light,overlay,red,green,blue,opacity);
            }
        };
        var eyesModel=new SpiderModel<MindSpiderEntity>(context.bakeLayer(ModelLayers.SPIDER));
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<MindSpiderEntity,SpiderModel<MindSpiderEntity>>(this) {
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,MindSpiderEntity entity,float walk,float amount,float partial,float age,float yaw,float pitch) {
                getParentModel().copyPropertiesTo(eyesModel); eyesModel.prepareMobModel(entity,walk,amount,partial); eyesModel.setupAnim(entity,walk,amount,age,yaw,pitch);
                eyesModel.renderToBuffer(pose,TCRenderTypes.buffer(buffers,EYES),61680,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,1);
            }
        });
    }
    @Override public void render(MindSpiderEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        var camera=Minecraft.getInstance().getCameraEntity();
        if (!entity.viewer().isEmpty() && (camera==null || !entity.viewer().equals(camera.getName().getString()))) return;
        opacity=Math.min(.1f,entity.tickCount/100f); super.render(entity,yaw,partial,pose,buffers,light);
    }
    @Override protected RenderType getRenderType(MindSpiderEntity entity,boolean visible,boolean translucent,boolean glowing) { return TCRenderTypes.ghostModel(BODY); }
    @Override protected void scale(MindSpiderEntity entity,PoseStack pose,float partial) { pose.scale(.3f,.3f,.3f); }
    @Override protected float getFlipDegrees(MindSpiderEntity entity) { return 180; }
    @Override public ResourceLocation getTextureLocation(MindSpiderEntity entity) { return BODY; }
}
