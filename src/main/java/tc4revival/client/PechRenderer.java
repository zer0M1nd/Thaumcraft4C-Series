package tc4revival.client;

import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import tc4revival.content.PechEntity;

public  class PechRenderer extends MobRenderer<PechEntity,PechModel> {
    private static final ResourceLocation[] SKINS=java.util.stream.Stream.of("pech_forage","pech_thaum","pech_stalker").map(s->ResourceLocation.parse("thaumcraft:textures/models/"+s+".png")).toArray(ResourceLocation[]::new);
    public PechRenderer(EntityRendererProvider.Context context) {
        super(context,new PechModel(context.bakeLayer(PechModel.LAYER)),.5f);
        addLayer(new RenderLayer<PechEntity,PechModel>(this) {
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,PechEntity entity,float walk,float amount,float partial,float age,float yaw,float pitch) {
                var item=entity.getMainHandItem(); if (item.isEmpty()) return;
                pose.pushPose(); getParentModel().rightArm.translateAndRotate(pose); pose.translate(-.0625,.3375,.0625);
                if (item.getItem() instanceof net.minecraft.world.item.BlockItem) { pose.translate(0,.1875,-.3125); pose.mulPose(Axis.XP.rotationDegrees(20)); pose.mulPose(Axis.YP.rotationDegrees(45)); pose.scale(-.375f,-.375f,.375f); }
                else if (item.is(net.minecraft.world.item.Items.BOW)) { pose.translate(0,.125,.3125); pose.mulPose(Axis.YP.rotationDegrees(-20)); pose.scale(.625f,-.625f,.625f); pose.mulPose(Axis.XP.rotationDegrees(-100)); pose.mulPose(Axis.YP.rotationDegrees(45)); }
                else if (item.getItem() instanceof net.minecraft.world.item.TieredItem || item.getItem() instanceof tc4revival.content.CastingWandItem || item.is(net.minecraft.world.item.Items.FISHING_ROD)) {
                    if (item.getItem() instanceof tc4revival.content.CastingWandItem) pose.translate(0,-.125,0);
                    if (item.is(net.minecraft.world.item.Items.FISHING_ROD)) { pose.mulPose(Axis.ZP.rotationDegrees(180)); pose.translate(0,-.125,0); }
                    pose.translate(0,.1875,0); pose.scale(.625f,-.625f,.625f); pose.mulPose(Axis.XP.rotationDegrees(-100)); pose.mulPose(Axis.YP.rotationDegrees(45));
                } else { pose.translate(.25,.1875,-.1875); pose.scale(.375f,.375f,.375f); pose.mulPose(Axis.ZP.rotationDegrees(60)); pose.mulPose(Axis.XP.rotationDegrees(-90)); pose.mulPose(Axis.ZP.rotationDegrees(20)); }
                context.getItemInHandRenderer().renderItem(entity,item,net.minecraft.world.item.ItemDisplayContext.NONE,false,pose,buffers,light); pose.popPose();
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(PechEntity entity) { return SKINS[entity.profession()]; }
}
