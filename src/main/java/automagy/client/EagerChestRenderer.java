package automagy.client;

import automagy.content.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class EagerChestRenderer implements BlockEntityRenderer<EagerChestBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("automagy:textures/models/eager_chest.png");
    private final ModelPart body, lid;
    public EagerChestRenderer(BlockEntityRendererProvider.Context context) {
        MeshDefinition mesh = new MeshDefinition(); var root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0,19).addBox(1,6,1,14,10,14), PartPose.ZERO);
        var top = root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0,0).addBox(0,-5,-14,14,5,14), PartPose.offset(1,7,15));
        top.addOrReplaceChild("latch", CubeListBuilder.create().texOffs(0,0).addBox(6,-2,-15,2,4,1), PartPose.ZERO);
        var baked = LayerDefinition.create(mesh,64,64).bakeRoot(); body = baked.getChild("body"); lid = baked.getChild("lid");
    }
    @Override public void render(EagerChestBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose(); pose.translate(.5,0,.5);
        pose.mulPose(Axis.YP.rotationDegrees(-tile.getBlockState().getValue(EagerChestBlock.FACING).toYRot())); pose.translate(-.5,1,.5); pose.scale(1,-1,-1);
        float amount = Mth.lerp(partial, tile.previousLid, tile.lid); amount = 1-(1-amount)*(1-amount)*(1-amount);
        lid.xRot = -amount * Mth.HALF_PI;
        var consumer = buffers.getBuffer(RenderType.entityCutout(TEXTURE)); body.render(pose,consumer,light,overlay); lid.render(pose,consumer,light,overlay);
        pose.popPose();
    }
}
