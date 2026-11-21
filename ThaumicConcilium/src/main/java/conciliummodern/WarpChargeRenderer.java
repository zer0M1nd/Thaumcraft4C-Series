package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Two original 16-unit cubes, verified/exported in Blockbench; original atlas stays external. */
public final class WarpChargeRenderer extends EntityRenderer<WarpCharge> {
    private static final ResourceLocation SKIN=ResourceLocation.parse("thaumicconcilium:textures/entity/warp_cube.png");
    private final ModelPart inner,outer;
    public WarpChargeRenderer(EntityRendererProvider.Context context){
        super(context);var mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("inner",CubeListBuilder.create().texOffs(0,0).addBox(-8,-8,-8,16,16,16),PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("outer",CubeListBuilder.create().texOffs(0,32).addBox(-8,-8,-8,16,16,16),PartPose.ZERO);
        var root=LayerDefinition.create(mesh,64,64).bakeRoot();inner=root.getChild("inner");outer=root.getChild("outer");
    }
    @Override public ResourceLocation getTextureLocation(WarpCharge entity){return SKIN;}
    @Override public void render(WarpCharge charge,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.translate(0,.5,0);
        float sine=(float)Math.sin((charge.tickCount+partial)*.075),pulse=.025f*sine;
        float shade=charge.flagellation()?net.minecraft.util.Mth.clamp(1-sine,0,1):1,alpha=.5f+pulse*10;
        pose.scale(1-pulse,1-pulse,1-pulse);
        var out=buffers.getBuffer(RenderType.entityTranslucent(SKIN));
        pose.pushPose();pose.scale(.89856f,.89856f,.89856f);inner.render(pose,out,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,shade,shade,shade,alpha);pose.popPose();
        outer.render(pose,out,light,OverlayTexture.NO_OVERLAY,shade,shade,shade,alpha);pose.popPose();
        super.render(charge,yaw,partial,pose,buffers,light);
    }
}
