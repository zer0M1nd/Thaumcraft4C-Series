package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

/** Original six-box model dimensions, UVs, inflation and lying pose. */
public final class NetherExplorerRenderer extends EntityRenderer<NetherExplorer> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumicconcilium:textures/entity/nether_explorer.png");
    private final ModelPart model;
    public NetherExplorerRenderer(EntityRendererProvider.Context context){
        super(context);var mesh=new MeshDefinition();var root=mesh.getRoot();var inflation=new CubeDeformation(.5f);
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8,inflation),PartPose.ZERO);
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,16).addBox(-4,0,-2,8,12,4,inflation),PartPose.ZERO);
        root.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(40,16).addBox(-1,-2,-1,2,12,2,inflation),PartPose.offset(-5,2,0));
        root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(40,16).mirror().addBox(-1,-2,-1,2,12,2,inflation),PartPose.offset(5,2,0));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(0,16).addBox(-1,0,-1,2,12,2,inflation),PartPose.offset(-2,12,0));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(0,16).mirror().addBox(-1,0,-1,2,12,2,inflation),PartPose.offset(2,12,0));
        model=LayerDefinition.create(mesh,64,32).bakeRoot();
    }
    @Override public ResourceLocation getTextureLocation(NetherExplorer entity){return TEXTURE;}
    @Override public void render(NetherExplorer entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.translate(0,.1,0);pose.mulPose(Axis.YP.rotationDegrees(yaw));pose.mulPose(Axis.XP.rotationDegrees(90));
        model.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
}
