package conciliummodern.client;

import conciliummodern.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class VengefulGolemRenderer extends HumanoidMobRenderer<VengefulGolem,HumanoidModel<VengefulGolem>> {
    public VengefulGolemRenderer(EntityRendererProvider.Context context){
        super(context,new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,32).bakeRoot()),.5f);
        model.head.xScale=.65f;model.head.yScale=.75f;model.head.zScale=.65f;model.hat.visible=false;addLayer(new BrainLayer(this));
    }
    @Override public ResourceLocation getTextureLocation(VengefulGolem entity){return ConciliumGolems.id("textures/entity/vengeful_golem.png");}
    private static final class BrainLayer extends RenderLayer<VengefulGolem,HumanoidModel<VengefulGolem>> {
        private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/golem_decoration.png");
        private final ModelPart brain,jar;
        BrainLayer(RenderLayerParent<VengefulGolem,HumanoidModel<VengefulGolem>> parent){super(parent);var root=ConciliumVengefulGeometry.createBodyLayer().bakeRoot();brain=root.getChild("brain");jar=root.getChild("jar");}
        @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,VengefulGolem entity,float limb,float amount,float partial,float age,float yaw,float pitch){
            if(entity.isInvisible())return;brain.copyFrom(getParentModel().head);jar.copyFrom(getParentModel().head);
            // copyFrom copies scale too in some mappings; the legacy outer scale is applied once below.
            brain.xScale=brain.yScale=brain.zScale=jar.xScale=jar.yScale=jar.zScale=1;
            pose.pushPose();pose.scale(.65f,.75f,.65f);pose.translate(0,-.22,0);
            int overlay=LivingEntityRenderer.getOverlayCoords(entity,0);
            brain.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,overlay);
            jar.render(pose,buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),light,overlay);
            pose.popPose();
        }
    }
}
