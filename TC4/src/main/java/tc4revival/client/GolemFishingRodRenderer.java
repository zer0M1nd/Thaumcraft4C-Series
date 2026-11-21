package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.*;

public  class GolemFishingRodRenderer {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("minecraft:textures/item/fishing_rod.png");
    public static void render(GolemEntity golem,GolemModel model,PoseStack pose,MultiBufferSource buffers,int light) {
        if (golem.configuration().core()!=GolemCoreType.FISH) return;
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(5+90*model.root().getChild("right_arm").xRot/(float)Math.PI));
        pose.translate(-.26875,1.6,-.53); pose.mulPose(Axis.YN.rotationDegrees(90)); pose.mulPose(Axis.ZN.rotationDegrees(30));
        pose.scale(.66f,-.66f,.66f);
        GolemBadges.plate(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,.0625f,16);
        pose.popPose();
    }
    private GolemFishingRodRenderer() {}
}
