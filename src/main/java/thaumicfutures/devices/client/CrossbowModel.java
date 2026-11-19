package thaumicfutures.devices.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumicfutures.devices.AutomatedCrossbow;

// Made with Blockbench 5.2.0
// Exported for Minecraft version 1.17 or later with Mojang mappings


public class CrossbowModel extends EntityModel<AutomatedCrossbow> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.parse("thaumicfutures:automated_crossbow"), "main");
	private final ModelPart crossbow;
	private final ModelPart basebarr;
	private final ModelPart basebarl;
	private final ModelPart barrel;
	private final ModelPart basebarcross;
	private final ModelPart ammobox;
	private final ModelPart loadbarcross;
	private final ModelPart loadbarl;
	private final ModelPart loadbarr;
	private final ModelPart crossl1;
	private final ModelPart crossl2;
	private final ModelPart crossl3;
	private final ModelPart crossr1;
	private final ModelPart crossr2;
	private final ModelPart crossr3;
	private final ModelPart leg2;
	private final ModelPart tripod;
	private final ModelPart leg3;
	private final ModelPart leg4;
	private final ModelPart leg1;

	public CrossbowModel(ModelPart root) {
		this.crossbow = root.getChild("crossbow");
		this.basebarr = this.crossbow.getChild("basebarr");
		this.basebarl = this.crossbow.getChild("basebarl");
		this.barrel = this.crossbow.getChild("barrel");
		this.basebarcross = this.crossbow.getChild("basebarcross");
		this.ammobox = this.crossbow.getChild("ammobox");
		this.loadbarcross = this.crossbow.getChild("loadbarcross");
		this.loadbarl = this.crossbow.getChild("loadbarl");
		this.loadbarr = this.crossbow.getChild("loadbarr");
		this.crossl1 = this.crossbow.getChild("crossl1");
		this.crossl2 = this.crossbow.getChild("crossl2");
		this.crossl3 = this.crossbow.getChild("crossl3");
		this.crossr1 = this.crossbow.getChild("crossr1");
		this.crossr2 = this.crossbow.getChild("crossr2");
		this.crossr3 = this.crossbow.getChild("crossr3");
		this.leg2 = root.getChild("leg2");
		this.tripod = root.getChild("tripod");
		this.leg3 = root.getChild("leg3");
		this.leg4 = root.getChild("leg4");
		this.leg1 = root.getChild("leg1");
	}

	public static LayerDefinition layer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition crossbow = partdefinition.addOrReplaceChild("crossbow", CubeListBuilder.create().texOffs(28, 14).mirror().addBox(-2.0F, 0.0F, -7.0F, 4.0F, 2.0F, 14.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 10.0F, 0.0F));

		PartDefinition basebarr = crossbow.addOrReplaceChild("basebarr", CubeListBuilder.create().texOffs(40, 23).mirror().addBox(-1.0F, 0.0F, 7.0F, 1.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.1396F, 0.0F));

		PartDefinition basebarl = crossbow.addOrReplaceChild("basebarl", CubeListBuilder.create().texOffs(40, 23).mirror().addBox(0.0F, 0.0F, 7.0F, 1.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.1396F, 0.0F));

		PartDefinition barrel = crossbow.addOrReplaceChild("barrel", CubeListBuilder.create().texOffs(20, 28).mirror().addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition basebarcross = crossbow.addOrReplaceChild("basebarcross", CubeListBuilder.create().texOffs(0, 13).mirror().addBox(-2.0F, 0.5F, 10.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition ammobox = crossbow.addOrReplaceChild("ammobox", CubeListBuilder.create().texOffs(38, 0).mirror().addBox(-2.0F, -5.0F, -6.0F, 4.0F, 5.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition loadbarcross = crossbow.addOrReplaceChild("loadbarcross", CubeListBuilder.create().texOffs(0, 13).mirror().addBox(-2.0F, -8.5F, -0.5F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5585F, 0.0F, 0.0F));

		PartDefinition loadbarl = crossbow.addOrReplaceChild("loadbarl", CubeListBuilder.create().texOffs(0, 15).mirror().addBox(2.0F, -9.0F, -1.0F, 1.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5585F, 0.0F, 0.0F));

		PartDefinition loadbarr = crossbow.addOrReplaceChild("loadbarr", CubeListBuilder.create().texOffs(0, 15).mirror().addBox(-3.0F, -9.0F, -1.0F, 1.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5585F, 0.0F, 0.0F));

		PartDefinition crossl1 = crossbow.addOrReplaceChild("crossl1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, -6.0F, 5.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2443F, 0.0F));

		PartDefinition crossl2 = crossbow.addOrReplaceChild("crossl2", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(4.0F, 0.0F, -5.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2443F, 0.0F));

		PartDefinition crossl3 = crossbow.addOrReplaceChild("crossl3", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(6.0F, 0.0F, -4.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2443F, 0.0F));

		PartDefinition crossr1 = crossbow.addOrReplaceChild("crossr1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-5.0F, 0.0F, -6.0F, 5.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.2443F, 0.0F));

		PartDefinition crossr2 = crossbow.addOrReplaceChild("crossr2", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-7.0F, 0.0F, -5.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.2443F, 0.0F));

		PartDefinition crossr3 = crossbow.addOrReplaceChild("crossr3", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-8.0F, 0.0F, -4.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.2443F, 0.0F));

		PartDefinition leg2 = partdefinition.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(20, 10).mirror().addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5236F, 1.5708F, 0.0F));

		PartDefinition tripod = partdefinition.addOrReplaceChild("tripod", CubeListBuilder.create().texOffs(13, 0).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 12.0F, 0.0F));

		PartDefinition leg3 = partdefinition.addOrReplaceChild("leg3", CubeListBuilder.create().texOffs(20, 10).mirror().addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5236F, 3.1416F, 0.0F));

		PartDefinition leg4 = partdefinition.addOrReplaceChild("leg4", CubeListBuilder.create().texOffs(20, 10).mirror().addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5236F, 4.7124F, 0.0F));

		PartDefinition leg1 = partdefinition.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(20, 10).mirror().addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5236F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 32);
	}

	@Override
	public void setupAnim(AutomatedCrossbow entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        crossbow.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        crossbow.xRot = headPitch * Mth.DEG_TO_RAD;
        float partial = Mth.clamp(ageInTicks - entity.tickCount, 0, 1);
        float flex = Mth.sin(Mth.sqrt(entity.shotProgress(partial)) * Mth.TWO_PI) * .2f;
        crossl1.yRot = crossl2.yRot = crossl3.yRot = -.2f + flex;
        crossr1.yRot = crossr2.yRot = crossr3.yRot = .2f - flex;
        loadbarl.xRot = loadbarr.xRot = loadbarcross.xRot = -.5f
                + Mth.sin(Mth.sqrt(entity.loadProgress(partial)) * Mth.TWO_PI) * .5f;
    }

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		crossbow.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		tripod.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg3.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg4.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg1.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
