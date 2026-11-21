package heartwork.client;

import com.mojang.blaze3d.vertex.*;
import heartwork.content.StrawEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/** Adapted from the verified Blockbench export of the FIX model. */
public class StrawModel extends EntityModel<StrawEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("heartwork", "straw"), "main");
    private final ModelPart straw;
    public StrawModel(ModelPart root) { straw = root.getChild("straw"); }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("straw", CubeListBuilder.create()
                .texOffs(0,0).mirror().addBox(-7,16,-7,14,8,14).mirror(false)
                .texOffs(0,0).mirror().addBox(-7,8,-7,14,8,14).mirror(false)
                .texOffs(0,0).mirror().addBox(-7,0,-7,14,8,14).mirror(false), PartPose.ZERO);
        return LayerDefinition.create(mesh,64,32);
    }
    @Override public void setupAnim(StrawEntity entity,float limbSwing,float limbSwingAmount,float age,float yaw,float pitch) {}
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,float red,float green,float blue,float alpha) {
        straw.render(pose,buffer,light,overlay,red,green,blue,alpha);
    }
}
