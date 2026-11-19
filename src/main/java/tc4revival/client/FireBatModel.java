package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.content.FireBatEntity;

public  class FireBatModel extends EntityModel<FireBatEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.parse("thaumcraft:fire_bat"), "main");
    private final ModelPart head, body, right, left, rightTip, leftTip;
    public FireBatModel(ModelPart root) {
        head = root.getChild("head"); body = root.getChild("body"); right = body.getChild("right"); left = body.getChild("left");
        rightTip = right.getChild("tip"); leftTip = left.getChild("tip");
    }
    public static LayerDefinition layer() {
        var mesh = new MeshDefinition(); var root = mesh.getRoot();
        var head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -3, 6, 6, 6), PartPose.ZERO);
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(24, 0).addBox(-4, -6, -2, 3, 4, 1), PartPose.ZERO);
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(1, -6, -2, 3, 4, 1), PartPose.ZERO);
        var body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-3, 4, -3, 6, 12, 6)
                .texOffs(0, 34).addBox(-5, 16, 0, 10, 6, 1), PartPose.ZERO);
        for (boolean mirrored : new boolean[] { false, true }) {
            var wing = body.addOrReplaceChild(mirrored ? "left" : "right", CubeListBuilder.create().texOffs(42, 0).mirror(mirrored)
                    .addBox(mirrored ? 2 : -12, 1, 1.5f, 10, 16, 1), PartPose.ZERO);
            wing.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(24, 16).mirror(mirrored)
                    .addBox(mirrored ? 0 : -8, 1, 0, 8, 12, 1), PartPose.offset(mirrored ? 12 : -12, 1, 1.5f));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }
    @Override public void setupAnim(FireBatEntity bat, float limb, float amount, float age, float yaw, float pitch) {
        head.xRot = pitch * Mth.DEG_TO_RAD;
        head.yRot = bat.hanging() ? Mth.PI - yaw * Mth.DEG_TO_RAD : yaw * Mth.DEG_TO_RAD;
        head.zRot = bat.hanging() ? Mth.PI : 0; head.setPos(0, bat.hanging() ? -2 : 0, 0);
        if (bat.hanging()) {
            right.setPos(-3, 0, 3); left.setPos(3, 0, 3); body.xRot = Mth.PI;
            right.xRot = -Mth.PI / 20; right.yRot = -Mth.PI * .4f; rightTip.yRot = -Mth.PI * .55f;
            left.xRot = right.xRot;
        } else {
            right.setPos(0, 0, 0); left.setPos(0, 0, 0); body.xRot = Mth.PI / 4 + Mth.cos(age * .1f) * .15f; body.yRot = 0;
            right.xRot=left.xRot=0;
            right.yRot = Mth.cos(age * 1.3f) * Mth.PI / 4; rightTip.yRot = right.yRot / 2;
        }
        left.yRot = -right.yRot; leftTip.yRot = -rightTip.yRot;
    }
    @Override public void renderToBuffer(PoseStack pose, VertexConsumer out, int light, int overlay, float red, float green, float blue, float alpha) {
        head.render(pose, out, light, overlay, red, green, blue, alpha); body.render(pose, out, light, overlay, red, green, blue, alpha);
    }
}
