package arcane_engineering.client;

import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

/** Geometry exported from the original 0.2.7 dimensions using Blockbench 5.2.0. */
public class DestabilizerModel {
    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition base = root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 42).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 6.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.0F, 0.0F));

		PartDefinition rest = base.addOrReplaceChild("rest", CubeListBuilder.create().texOffs(12, 28).addBox(-5.0F, 3.0F, -5.0F, 10.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.0F, 0.0F));

		PartDefinition crystal = rest.addOrReplaceChild("crystal", CubeListBuilder.create().texOffs(40, 0).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -3.0F, 0.0F));

		PartDefinition arms = root.addOrReplaceChild("arms", CubeListBuilder.create(), PartPose.offset(0.0F, -8.0F, 0.0F));

		PartDefinition arm_a = arms.addOrReplaceChild("arm_a", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 4.0F, -1.0472F, 0.0F, 0.0F));

		PartDefinition arm_a_tip = arm_a.addOrReplaceChild("arm_a_tip", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.5F, 0.5F, 1.5708F, 0.0F, 0.0F));

		PartDefinition arm_b = arms.addOrReplaceChild("arm_b", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -8.0F, -1.0F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, -5.0F, -1.0472F, -3.1416F, 0.0F));

		PartDefinition arm_b_tip = arm_b.addOrReplaceChild("arm_b_tip", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.5F, 0.0F, 1.5759F, 0.0F, 0.0F));

		PartDefinition arm_c = arms.addOrReplaceChild("arm_c", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, 1.0F, 0.0F, -1.0472F, -1.5708F, 0.0F));

		PartDefinition arm_c_tip = arm_c.addOrReplaceChild("arm_c_tip", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.5F, 0.5F, 1.5708F, 0.0F, 0.0F));

		PartDefinition arm_d = arms.addOrReplaceChild("arm_d", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 1.0F, 0.0F, -1.0472F, 1.5708F, 0.0F));

		PartDefinition arm_d_tip = arm_d.addOrReplaceChild("arm_d_tip", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.5F, 0.5F, 1.5708F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

}
