package dyesmodern.client.geometry;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
/** Final 1.10.5 cuboids and UVs exported by Blockbench; texture pixels remain external. */
public final class DyesArcherInner {
public static LayerDefinition createBodyLayer(){
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Helmet1 = head.addOrReplaceChild("Helmet1", CubeListBuilder.create().texOffs(2, 1).addBox(-5.5F, -6.0F, -5.5F, 11.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Helmet = head.addOrReplaceChild("Helmet", CubeListBuilder.create().texOffs(48, 0).addBox(-4.5F, -5.0F, -4.5F, 9.0F, 5.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Helmet2 = head.addOrReplaceChild("Helmet2", CubeListBuilder.create().texOffs(0, 13).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 3.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition hat = partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Mbelt = body.addOrReplaceChild("Mbelt", CubeListBuilder.create().texOffs(16, 55).addBox(-4.0F, 7.0F, -3.0F, 8.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition MbeltB = body.addOrReplaceChild("MbeltB", CubeListBuilder.create().texOffs(16, 55).addBox(-4.0F, 7.0F, -4.0F, 8.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

		PartDefinition MbeltR = body.addOrReplaceChild("MbeltR", CubeListBuilder.create().texOffs(16, 36).addBox(-5.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition MbeltL = body.addOrReplaceChild("MbeltL", CubeListBuilder.create().texOffs(16, 36).addBox(4.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition FrontclothR1 = body.addOrReplaceChild("FrontclothR1", CubeListBuilder.create().texOffs(108, 38).addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 11.0F, -2.9F, -0.1047F, 0.0F, 0.0F));

		PartDefinition FrontclothR2 = body.addOrReplaceChild("FrontclothR2", CubeListBuilder.create().texOffs(108, 47).addBox(0.0F, 7.5F, 1.7F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 11.0F, -2.9F, -0.3316F, 0.0F, 0.0F));

		PartDefinition FrontclothL1 = body.addOrReplaceChild("FrontclothL1", CubeListBuilder.create().texOffs(108, 38).mirror().addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 11.0F, -2.9F, -0.1047F, 0.0F, 0.0F));

		PartDefinition FrontclothL2 = body.addOrReplaceChild("FrontclothL2", CubeListBuilder.create().texOffs(108, 47).mirror().addBox(0.0F, 7.5F, 1.7F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 11.0F, -2.9F, -0.3316F, 0.0F, 0.0F));

		PartDefinition ClothBackR1 = body.addOrReplaceChild("ClothBackR1", CubeListBuilder.create().texOffs(118, 16).mirror().addBox(0.0F, 0.0F, 0.0F, 4.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-4.0F, 11.5F, 2.9F, 0.1047F, 0.0F, 0.0F));

		PartDefinition ClothBackR2 = body.addOrReplaceChild("ClothBackR2", CubeListBuilder.create().texOffs(123, 9).addBox(0.0F, 7.8F, -0.9F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, 11.5F, 2.9F, 0.2269F, 0.0F, 0.0F));

		PartDefinition ClothBackR3 = body.addOrReplaceChild("ClothBackR3", CubeListBuilder.create().texOffs(120, 12).mirror().addBox(1.0F, 7.8F, -0.9F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-4.0F, 11.5F, 2.9F, 0.2269F, 0.0F, 0.0F));

		PartDefinition ClothBackL1 = body.addOrReplaceChild("ClothBackL1", CubeListBuilder.create().texOffs(118, 16).addBox(0.0F, 0.0F, 0.0F, 4.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.1047F, 0.0F, 0.0F));

		PartDefinition ClothBackL2 = body.addOrReplaceChild("ClothBackL2", CubeListBuilder.create().texOffs(123, 9).mirror().addBox(3.0F, 7.8F, -0.9F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.2269F, 0.0F, 0.0F));

		PartDefinition ClothBackL3 = body.addOrReplaceChild("ClothBackL3", CubeListBuilder.create().texOffs(120, 12).addBox(0.0F, 7.8F, -0.9F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.2269F, 0.0F, 0.0F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));

		PartDefinition ShoulderR = right_arm.addOrReplaceChild("ShoulderR", CubeListBuilder.create().texOffs(16, 45).addBox(-3.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition GauntletstrapR1 = right_arm.addOrReplaceChild("GauntletstrapR1", CubeListBuilder.create().texOffs(84, 58).addBox(-1.5F, 3.5F, -2.5F, 3.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition ShoulderR5 = right_arm.addOrReplaceChild("ShoulderR5", CubeListBuilder.create().texOffs(0, 38).addBox(-3.3F, -2.0F, 3.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.6458F));

		PartDefinition ShoulderR1 = right_arm.addOrReplaceChild("ShoulderR1", CubeListBuilder.create().texOffs(0, 39).addBox(-4.3F, -2.0F, -3.0F, 1.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.6458F));

		PartDefinition ShoulderR4 = right_arm.addOrReplaceChild("ShoulderR4", CubeListBuilder.create().texOffs(0, 38).addBox(-3.3F, -2.0F, -4.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.6458F));

		PartDefinition ShoulderR3 = right_arm.addOrReplaceChild("ShoulderR3", CubeListBuilder.create().texOffs(0, 50).addBox(-3.3F, 3.0F, -3.0F, 1.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.6458F));

		PartDefinition GauntletstrapR2 = right_arm.addOrReplaceChild("GauntletstrapR2", CubeListBuilder.create().texOffs(84, 58).addBox(-1.5F, 6.5F, -2.5F, 3.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition ShoulderR2 = right_arm.addOrReplaceChild("ShoulderR2", CubeListBuilder.create().texOffs(0, 58).addBox(-4.3F, 3.0F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.6458F));

		PartDefinition GauntletR = right_arm.addOrReplaceChild("GauntletR", CubeListBuilder.create().texOffs(100, 53).addBox(-3.5F, 3.5F, -2.5F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));

		PartDefinition ShoulderL3 = left_arm.addOrReplaceChild("ShoulderL3", CubeListBuilder.create().texOffs(0, 50).addBox(2.3F, 3.0F, -3.0F, 1.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6458F));

		PartDefinition ShoulderL2 = left_arm.addOrReplaceChild("ShoulderL2", CubeListBuilder.create().texOffs(0, 58).addBox(3.3F, 3.0F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6458F));

		PartDefinition GauntletL = left_arm.addOrReplaceChild("GauntletL", CubeListBuilder.create().texOffs(114, 53).addBox(1.5F, 3.5F, -2.5F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition ShoulderL = left_arm.addOrReplaceChild("ShoulderL", CubeListBuilder.create().texOffs(16, 45).mirror().addBox(-1.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition GauntletstrapL2 = left_arm.addOrReplaceChild("GauntletstrapL2", CubeListBuilder.create().texOffs(84, 58).addBox(-1.5F, 6.5F, -2.5F, 3.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition GauntletstrapL1 = left_arm.addOrReplaceChild("GauntletstrapL1", CubeListBuilder.create().texOffs(84, 58).addBox(-1.5F, 3.5F, -2.5F, 3.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition ShoulderL1 = left_arm.addOrReplaceChild("ShoulderL1", CubeListBuilder.create().texOffs(0, 39).addBox(3.3F, -2.0F, -3.0F, 1.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6458F));

		PartDefinition ShoulderL4 = left_arm.addOrReplaceChild("ShoulderL4", CubeListBuilder.create().texOffs(0, 38).addBox(2.3F, -2.0F, -4.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6458F));

		PartDefinition ShoulderL5 = left_arm.addOrReplaceChild("ShoulderL5", CubeListBuilder.create().texOffs(0, 38).addBox(2.3F, -2.0F, 3.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6458F));

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));

		PartDefinition BackpanelR3 = right_leg.addOrReplaceChild("BackpanelR3", CubeListBuilder.create().texOffs(84, 3).addBox(0.0F, 2.5F, -2.5F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396F));

		PartDefinition BackpanelR1 = right_leg.addOrReplaceChild("BackpanelR1", CubeListBuilder.create().texOffs(84, 10).addBox(-3.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396F));

		PartDefinition BackpanelR4 = right_leg.addOrReplaceChild("BackpanelR4", CubeListBuilder.create().texOffs(102, 1).addBox(-2.0F, 2.5F, -2.5F, 2.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396F));

		PartDefinition BackpanelR2 = right_leg.addOrReplaceChild("BackpanelR2", CubeListBuilder.create().texOffs(104, 9).addBox(-3.0F, 2.5F, -2.5F, 1.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));

		PartDefinition BackpanelL1 = left_leg.addOrReplaceChild("BackpanelL1", CubeListBuilder.create().texOffs(104, 9).addBox(2.0F, 2.5F, -2.5F, 1.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396F));

		PartDefinition BackpanelL2 = left_leg.addOrReplaceChild("BackpanelL2", CubeListBuilder.create().texOffs(84, 10).addBox(-2.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396F));

		PartDefinition BackpanelL3 = left_leg.addOrReplaceChild("BackpanelL3", CubeListBuilder.create().texOffs(84, 3).addBox(-2.0F, 2.5F, -2.5F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396F));

		PartDefinition BackpanelL4 = left_leg.addOrReplaceChild("BackpanelL4", CubeListBuilder.create().texOffs(102, 1).addBox(0.0F, 2.5F, -2.5F, 2.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396F));

		return LayerDefinition.create(meshdefinition, 128, 64);
	
}
}
