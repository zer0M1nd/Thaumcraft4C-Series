package conciliummodern;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
/** Original attached cuboids, exported by Blockbench; no original texture bytes bundled. */
public final class PontifexRobeGeometry {
public static ModelPart bake(){return createBodyLayer().bakeRoot();}
public static LayerDefinition createBodyLayer(){
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Shape1H = head.addOrReplaceChild("Shape1H", CubeListBuilder.create().texOffs(18, 1).addBox(0.0F, 0.0F, 0.0F, 10.0F, 9.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.2F, -9.0F, -4.1F));

		PartDefinition Shape2H = head.addOrReplaceChild("Shape2H", CubeListBuilder.create().texOffs(90, 46).addBox(0.0F, 0.0F, 0.0F, 9.0F, 9.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.7F, -11.0F, -3.0F));

		PartDefinition Shape3H = head.addOrReplaceChild("Shape3H", CubeListBuilder.create().texOffs(95, 50).addBox(0.0F, 0.0F, 0.0F, 7.0F, 5.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.7F, -13.9333F, -2.0F, -0.1745F, 0.0F, 0.0F));

		PartDefinition Shape4H = head.addOrReplaceChild("Shape4H", CubeListBuilder.create().texOffs(38, 0).addBox(0.0F, 0.0F, 0.0F, 5.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.7F, -17.0F, 0.0F, -0.2618F, 0.0F, 0.0F));

		PartDefinition Shape5H = head.addOrReplaceChild("Shape5H", CubeListBuilder.create().texOffs(0, 49).addBox(0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.7F, -18.0F, 2.0F, -0.2618F, 0.0F, 0.0F));

		PartDefinition Shape6H = head.addOrReplaceChild("Shape6H", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.7F, -19.0F, 5.0F, -0.3927F, 0.0F, 0.0F));

		PartDefinition Shape7H = head.addOrReplaceChild("Shape7H", CubeListBuilder.create().texOffs(0, 54).addBox(0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.9F, 0.0F, -4.8F, 0.0F, 0.0F, -0.1134F));

		PartDefinition Shape8H = head.addOrReplaceChild("Shape8H", CubeListBuilder.create().texOffs(38, 11).addBox(0.0F, 0.0F, 0.0F, 9.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.8F, 0.0F, -4.8F));

		PartDefinition Shape9H = head.addOrReplaceChild("Shape9H", CubeListBuilder.create().texOffs(0, 47).addBox(0.0F, 0.0F, 0.0F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.3F, -8.0F, -4.8F));

		PartDefinition Shape10H = head.addOrReplaceChild("Shape10H", CubeListBuilder.create().texOffs(36, 0).addBox(0.0F, 0.0F, 0.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.4F, 0.0F, -4.8F, 0.0F, 0.0F, -1.4573F));

		PartDefinition Shape11H = head.addOrReplaceChild("Shape11H", CubeListBuilder.create().texOffs(0, 45).addBox(0.0F, 0.0F, 0.0F, 1.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.2F, -8.0F, -4.0F, 0.0F, 0.0F, 0.1396F));

		PartDefinition Shape12H = head.addOrReplaceChild("Shape12H", CubeListBuilder.create().texOffs(0, 47).addBox(0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.4F, -0.2F, -4.8F, 0.0175F, 0.0F, 0.1134F));

		PartDefinition Shape13H = head.addOrReplaceChild("Shape13H", CubeListBuilder.create().texOffs(0, 59).addBox(0.0F, 0.0F, 0.0F, 10.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, -1.0F, 2.0F));

		PartDefinition Shape14H = head.addOrReplaceChild("Shape14H", CubeListBuilder.create().texOffs(32, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -8.0F, -4.8F, 0.0F, 0.0F, -0.1047F));

		PartDefinition Shape16H = head.addOrReplaceChild("Shape16H", CubeListBuilder.create().texOffs(90, 52).addBox(0.0F, 0.0F, 0.0F, 10.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.2F, -8.0F, 6.0F, -1.309F, 0.0F, 0.0F));

		PartDefinition hat = partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cloth = body.addOrReplaceChild("cloth", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Shape19B = cloth.addOrReplaceChild("Shape19B", CubeListBuilder.create().texOffs(0, 56).addBox(0.0F, 0.0F, 0.0F, 8.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.5F, 18.0F, 4.0F, -0.576F, 1.5708F, 0.0F));

		PartDefinition Shape20B = cloth.addOrReplaceChild("Shape20B", CubeListBuilder.create().texOffs(0, 49).addBox(0.0F, 0.0F, 0.0F, 8.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.2F, 11.6F, 4.0F, -0.2182F, 1.5708F, 0.0F));

		PartDefinition Shape21B = cloth.addOrReplaceChild("Shape21B", CubeListBuilder.create().texOffs(0, 56).addBox(0.0F, 0.0F, 0.0F, 8.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.5F, 18.0F, -4.0F, -0.5236F, -1.5708F, 0.0F));

		PartDefinition Shape22B = cloth.addOrReplaceChild("Shape22B", CubeListBuilder.create().texOffs(0, 49).addBox(0.0F, 0.0F, 0.0F, 8.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.2F, 11.6F, -4.0F, -0.2182F, -1.5708F, 0.0F));

		PartDefinition Shape28B = cloth.addOrReplaceChild("Shape28B", CubeListBuilder.create().texOffs(25, 28).addBox(0.0F, 0.0F, 0.0F, 6.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4667F, 10.0F, 5.0F, 0.2182F, 0.0F, 0.0F));

		PartDefinition Shape29B = cloth.addOrReplaceChild("Shape29B", CubeListBuilder.create().texOffs(33, 26).addBox(0.0F, 0.0F, 0.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, 9.5F, 3.5F, 0.1309F, 0.0F, 0.0F));

		PartDefinition Shape30B = cloth.addOrReplaceChild("Shape30B", CubeListBuilder.create().texOffs(10, 41).addBox(0.0F, 0.0F, 0.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 10.0F, -4.5F));

		PartDefinition Shape31B = cloth.addOrReplaceChild("Shape31B", CubeListBuilder.create().texOffs(88, 20).addBox(0.0F, 0.0F, 0.0F, 10.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 10.0F, -4.0F));

		PartDefinition FrontclothR1 = cloth.addOrReplaceChild("FrontclothR1", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 11.0F, -2.9F, -0.1047F, 0.0F, 0.0F));

		PartDefinition FrontclothR2 = cloth.addOrReplaceChild("FrontclothR2", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 7.5F, 1.7F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 11.0F, -2.9F, -0.3316F, 0.0F, 0.0F));

		PartDefinition FrontclothL1 = cloth.addOrReplaceChild("FrontclothL1", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 11.0F, -2.9F, -0.1047F, 0.0F, 0.0F));

		PartDefinition FrontclothL2 = cloth.addOrReplaceChild("FrontclothL2", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 7.5F, 1.7F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 11.0F, -2.9F, -0.3316F, 0.0F, 0.0F));

		PartDefinition ClothBackR1 = cloth.addOrReplaceChild("ClothBackR1", CubeListBuilder.create().texOffs(0, 46).addBox(0.0F, 0.0F, 0.0F, 4.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, 11.5F, 2.9F, 0.1047F, 0.0F, 0.0F));

		PartDefinition ClothBackR2 = cloth.addOrReplaceChild("ClothBackR2", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 7.8F, -0.9F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, 11.5F, 2.9F, 0.2269F, 0.0F, 0.0F));

		PartDefinition ClothBackR3 = cloth.addOrReplaceChild("ClothBackR3", CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 7.8F, -0.9F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, 11.5F, 2.9F, 0.2269F, 0.0F, 0.0F));

		PartDefinition ClothBackL1 = cloth.addOrReplaceChild("ClothBackL1", CubeListBuilder.create().texOffs(0, 46).addBox(0.0F, 0.0F, 0.0F, 4.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.1047F, 0.0F, 0.0F));

		PartDefinition ClothBackL2 = cloth.addOrReplaceChild("ClothBackL2", CubeListBuilder.create().texOffs(0, 0).addBox(3.0F, 7.8F, -0.9F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.2269F, 0.0F, 0.0F));

		PartDefinition ClothBackL3 = cloth.addOrReplaceChild("ClothBackL3", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 7.8F, -0.9F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.2269F, 0.0F, 0.0F));

		PartDefinition armor = body.addOrReplaceChild("armor", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Shape25B = armor.addOrReplaceChild("Shape25B", CubeListBuilder.create().texOffs(51, 23).addBox(0.0F, 0.0F, 0.0F, 10.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 1.0F, -5.0F, 0.1309F, 0.0F, 0.0F));

		PartDefinition Shape26B = armor.addOrReplaceChild("Shape26B", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 0.0F, -3.8F, 0.0F, 0.0F, 0.1309F));

		PartDefinition Shape27B = armor.addOrReplaceChild("Shape27B", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 0.0F, -3.8F, 0.0F, 0.0F, -0.1309F));

		PartDefinition Shape32B = armor.addOrReplaceChild("Shape32B", CubeListBuilder.create().texOffs(52, 27).addBox(0.0F, 0.0F, 0.0F, 8.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 5.0F, -3.9F));

		PartDefinition Shape33B = armor.addOrReplaceChild("Shape33B", CubeListBuilder.create().texOffs(27, 35).addBox(0.0F, 0.0F, 0.0F, 10.0F, 11.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 0.0F, -3.5F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));

		PartDefinition Shape35RH = right_arm.addOrReplaceChild("Shape35RH", CubeListBuilder.create().texOffs(0, 55).addBox(6.5F, -3.0F, 0.0F, 0.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-8.4F, 11.0F, -2.5F));

		PartDefinition Shape36RH = right_arm.addOrReplaceChild("Shape36RH", CubeListBuilder.create().texOffs(0, 48).addBox(6.5F, -3.0F, 0.0F, 4.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-8.4F, 0.0F, -2.5F));

		PartDefinition Shape37RH = right_arm.addOrReplaceChild("Shape37RH", CubeListBuilder.create().texOffs(33, 42).addBox(2.5F, -6.0F, 0.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.3F, -1.9F, -2.5F, 0.0F, 0.0F, 0.6545F));

		PartDefinition Shape38RH = right_arm.addOrReplaceChild("Shape38RH", CubeListBuilder.create().texOffs(33, 34).addBox(2.5F, -6.0F, 0.0F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.5F, -1.3F, -2.5F, 0.0F, 0.0F, 0.6545F));

		PartDefinition Shape39RH = right_arm.addOrReplaceChild("Shape39RH", CubeListBuilder.create().texOffs(34, 45).addBox(2.5F, -6.0F, 0.0F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.5F, 0.3F, -2.5F, 0.0F, 0.0F, 0.6545F));

		PartDefinition Shape40RH = right_arm.addOrReplaceChild("Shape40RH", CubeListBuilder.create().texOffs(34, 45).addBox(2.5F, -6.0F, 0.0F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.6F, 2.1F, -2.5F, 0.0F, 0.0F, 0.6545F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));

		PartDefinition Shape52LH = left_arm.addOrReplaceChild("Shape52LH", CubeListBuilder.create().texOffs(0, 55).addBox(-6.5F, -3.0F, 0.0F, 0.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(8.4F, 11.0F, -2.5F));

		PartDefinition Shape53LH = left_arm.addOrReplaceChild("Shape53LH", CubeListBuilder.create().texOffs(0, 48).addBox(-6.5F, -3.0F, 0.0F, 4.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(4.4F, 0.0F, -2.5F));

		PartDefinition Shape54LH = left_arm.addOrReplaceChild("Shape54LH", CubeListBuilder.create().texOffs(33, 42).addBox(-2.5F, -6.0F, 0.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.4F, -1.3F, -2.5F, 0.0F, 0.0F, -0.6545F));

		PartDefinition Shape55LH = left_arm.addOrReplaceChild("Shape55LH", CubeListBuilder.create().texOffs(34, 45).addBox(-2.5F, -6.0F, 0.0F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.6F, 0.8F, -2.5F, 0.0F, 0.0F, -0.6545F));

		PartDefinition Shape56LH = left_arm.addOrReplaceChild("Shape56LH", CubeListBuilder.create().texOffs(33, 34).addBox(-2.5F, -6.0F, 0.0F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.6F, -0.7F, -2.5F, 0.0F, 0.0F, -0.6545F));

		PartDefinition Shape57LH = left_arm.addOrReplaceChild("Shape57LH", CubeListBuilder.create().texOffs(34, 45).addBox(-2.5F, -6.0F, 0.0F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.7F, 2.5F, -2.5F, 0.0F, 0.0F, -0.6545F));

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));

		PartDefinition rightBoot = right_leg.addOrReplaceChild("rightBoot", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Shape41RL = rightBoot.addOrReplaceChild("Shape41RL", CubeListBuilder.create().texOffs(63, 24).addBox(2.0F, -13.0F, 0.0F, 5.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.5F, 22.1F, -4.5F));

		PartDefinition Shape42RL = rightBoot.addOrReplaceChild("Shape42RL", CubeListBuilder.create().texOffs(29, 53).addBox(2.0F, -13.0F, -1.0F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.5F, 18.1F, -1.5F));

		PartDefinition Shape43RL = rightBoot.addOrReplaceChild("Shape43RL", CubeListBuilder.create().texOffs(33, 57).addBox(2.0F, -13.0F, 0.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5F, 18.5F, -2.8F, -0.3927F, 0.0F, 0.0F));

		PartDefinition Shape45RL = right_leg.addOrReplaceChild("Shape45RL", CubeListBuilder.create().texOffs(0, 27).addBox(2.0F, -13.0F, -0.5F, 4.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 12.0F, -2.0F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));

		PartDefinition leftBoot = left_leg.addOrReplaceChild("leftBoot", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Shape46LL = leftBoot.addOrReplaceChild("Shape46LL", CubeListBuilder.create().texOffs(63, 24).addBox(-2.0F, -13.0F, 0.0F, 5.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.5F, 22.1F, -4.5F));

		PartDefinition Shape47LL = leftBoot.addOrReplaceChild("Shape47LL", CubeListBuilder.create().texOffs(29, 53).addBox(-2.0F, -13.0F, 0.0F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.5F, 18.1F, -2.5F));

		PartDefinition Shape48LL = leftBoot.addOrReplaceChild("Shape48LL", CubeListBuilder.create().texOffs(33, 57).addBox(-2.0F, -13.0F, 0.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 18.5F, -2.8F, -0.3927F, 0.0F, 0.0F));

		PartDefinition Shape50LL = left_leg.addOrReplaceChild("Shape50LL", CubeListBuilder.create().texOffs(0, 27).addBox(-2.0F, -13.0F, -0.5F, 4.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 12.0F, -2.0F));

		return LayerDefinition.create(meshdefinition, 128, 64);
	
}
}
