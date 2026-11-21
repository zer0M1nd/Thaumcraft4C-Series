package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.TravelingTrunkEntity;

public  class TravelingTrunkRenderer extends MobRenderer<TravelingTrunkEntity,TravelingTrunkModel> {
    private static final String[] UPGRADES={"air","earth","fire","water","order","entropy"};
    public TravelingTrunkRenderer(EntityRendererProvider.Context context) {
        super(context,new TravelingTrunkModel(),.5f);
        addLayer(new RenderLayer<TravelingTrunkEntity,TravelingTrunkModel>(this) {
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,TravelingTrunkEntity entity,float walk,float amount,float partial,float age,float yaw,float pitch) {
                pose.pushPose(); getParentModel().knob().translateAndRotate(pose);
                pose.translate(-.075,-.115,-.94301); pose.scale(.15f,.15f,.15f);
                var texture=ResourceLocation.parse("thaumcraft:textures/item/golem_upgrade_"+(entity.upgrade()<0?"empty":UPGRADES[entity.upgrade()])+".png");
                var out=buffers.getBuffer(RenderType.entityTranslucent(texture));
                for (int index=0;index<4;index++) {
                    float x=index==1||index==2?1:0,y=index>=2?1:0;
                    out.vertex(pose.last().pose(),x,y,0).color(255,255,255,255).uv(1-x,1-y).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,0,1).endVertex();
                }
                pose.popPose();
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(TravelingTrunkEntity entity) { return ResourceLocation.parse("thaumcraft:textures/models/"+(entity.anger()>0?"trunkangry":"trunk")+".png"); }
    @Override protected void scale(TravelingTrunkEntity entity,PoseStack pose,float partial) {
        // Native movement supplies a small airborne stretch; grounded storage stays at its base scale.
        float horizontal=(float)(1/1.4),size=(float)(entity.upgrade()==1?2/1.33:2/1.5);
        float stretch=entity.onGround()?1:1+net.minecraft.util.Mth.clamp((float)entity.getDeltaMovement().y,-.15f,.15f);
        pose.scale(horizontal*size/stretch,.5f/horizontal*size*stretch,horizontal*size/stretch); pose.translate(-.5,.5,-.5);
    }
}
