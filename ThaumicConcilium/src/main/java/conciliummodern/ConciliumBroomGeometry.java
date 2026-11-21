package conciliummodern;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
/** Original cuboids, UVs and pivots exported through Blockbench. */
public final class ConciliumBroomGeometry {
	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition handle = partdefinition.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-1.0F, -10.0F, -1.0F, 2.0F, 24.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 11.0F, -5.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition bristle1 = partdefinition.addOrReplaceChild("bristle1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-1.0F, 10.0F, 9.0F, 0.1859F, -0.1487F, 0.0F));

		PartDefinition bristle2 = partdefinition.addOrReplaceChild("bristle2", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(1.0F, 12.0F, 9.0F, -0.1487F, 0.1859F, 0.0F));

		PartDefinition bristle3 = partdefinition.addOrReplaceChild("bristle3", CubeListBuilder.create().texOffs(0, 12).mirror().addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(1.0F, 10.0F, 9.0F, 0.2231F, 0.1859F, 0.0F));

		PartDefinition bristle4 = partdefinition.addOrReplaceChild("bristle4", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 10.0F, 9.0F, 0.2231F, 0.0744F, 0.0F));

		PartDefinition bristle5 = partdefinition.addOrReplaceChild("bristle5", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-1.0F, 12.0F, 9.0F, -0.2231F, -0.1487F, 0.0F));

		PartDefinition bristle6 = partdefinition.addOrReplaceChild("bristle6", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 11.0F, 9.0F, -0.0372F, 0.0744F, 0.0F));

		PartDefinition bristle7 = partdefinition.addOrReplaceChild("bristle7", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(1.0F, 11.0F, 9.0F, -0.0372F, 0.2231F, 0.0F));

		PartDefinition bristle8 = partdefinition.addOrReplaceChild("bristle8", CubeListBuilder.create().texOffs(0, 12).mirror().addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-1.0F, 11.0F, 9.0F, -0.0744F, -0.1487F, 0.0F));

		PartDefinition bristle9 = partdefinition.addOrReplaceChild("bristle9", CubeListBuilder.create().texOffs(0, 12).mirror().addBox(-0.5333F, -0.5F, 0.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 12.0F, 9.0F, -0.1859F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

}
