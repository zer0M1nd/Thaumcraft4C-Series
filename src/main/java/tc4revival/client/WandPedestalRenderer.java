package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.WandPedestalBlockEntity;

public  class WandPedestalRenderer implements BlockEntityRenderer<WandPedestalBlockEntity> {
    public WandPedestalRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(WandPedestalBlockEntity pedestal, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (pedestal.isEmpty()) return;
        var mc = Minecraft.getInstance(); var viewer = mc.getCameraEntity(); float ticks = (viewer == null ? 0 : viewer.tickCount) + partial;
        float bob = (float) Math.sin((ticks % 32767) / 16) * .05f;
        pose.pushPose(); pose.translate(.5, 1.25 + bob, .5); pose.mulPose(Axis.YP.rotationDegrees(ticks % 360));
        mc.getItemRenderer().renderStatic(pedestal.getItem(0), ItemDisplayContext.GROUND, light, overlay, pose, buffers, pedestal.getLevel(), 0);
        pose.popPose();
        if (pedestal.drainPosition() != null) {
            Vec3 origin = Vec3.atLowerCornerOf(pedestal.getBlockPos());
            NodeDrainBeam.between(origin, origin.add(.5, 1.65 - bob * 2, .5), Vec3.atCenterOf(pedestal.drainPosition()),
                    pedestal.drainColor(), Math.min(ticks, 10) / 10, pose, buffers);
        }
    }
    @Override public boolean shouldRenderOffScreen(WandPedestalBlockEntity pedestal) { return true; }
}
