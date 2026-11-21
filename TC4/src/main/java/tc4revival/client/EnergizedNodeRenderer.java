package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.AuraNodeType;
import tc4revival.content.EnergizedNodeBlockEntity;

public  class EnergizedNodeRenderer implements BlockEntityRenderer<EnergizedNodeBlockEntity> {
    private static final ResourceLocation NODES = ResourceLocation.parse("thaumcraft:textures/misc/nodes.png");
    private static final ResourceLocation RING = ResourceLocation.parse("thaumcraft:textures/items/lightningringv.png");
    public EnergizedNodeRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(EnergizedNodeBlockEntity node, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        thaumcraft.api.client.DeviceRendering.render(new thaumcraft.api.client.DeviceRendering.Context<>(node,partial,pose,buffers,light,overlay),()->renderDefault(node,partial,pose,buffers,light,overlay));
    }
    private void renderDefault(EnergizedNodeBlockEntity node, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderNode(node.getBlockPos(), node.aura(), node.nodeType(), node.nodeModifier(), true, false, 64, pose, buffers);
        var mc = Minecraft.getInstance(); long nano = System.nanoTime();
        pose.pushPose(); pose.translate(.5, .5, .5); pose.mulPose(mc.gameRenderer.getMainCamera().rotation());
        int ringFrame = Math.floorMod(nano / 40_000_000 + (long) (node.getBlockPos().getX() - mc.gameRenderer.getMainCamera().getPosition().x), 16);
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.runes(RING)), pose.last().pose(), .33f, ringFrame / 16f, 0, (ringFrame + 1) / 16f, 1, 1, 1, 1, .9f, 220);
        pose.popPose();
    }
    static void renderNode(net.minecraft.core.BlockPos position, tc4revival.aspect.AspectAmounts aura, AuraNodeType type, tc4revival.content.AuraNodeModifier modifier,
            boolean visible, boolean throughBlocks, double viewDistance, PoseStack pose, MultiBufferSource buffers) {
        aura=thaumcraft.api.client.ClientAspectViews.project(aura,thaumcraft.api.aspects.AspectViews.Purpose.NODE);
        var mc = Minecraft.getInstance(); var viewer = mc.getCameraEntity(); if (viewer == null) return;
        double distance = viewer.position().distanceTo(Vec3.atCenterOf(position));
        long nano = System.nanoTime(), time = nano / 5_000_000;
        int frame = Math.floorMod(nano / 40_000_000 + position.getX(), 32);
        pose.pushPose(); pose.translate(.5, .5, .5); pose.mulPose(mc.gameRenderer.getMainCamera().rotation());
        int count = aura.aspects().size();
        if (count == 0 || !visible) strip(pose, buffers, frame, 1, 0, .5f, .1f, 0xffffff, false, false);
        else if (distance <= viewDistance) {
            float alpha = (float) (1 - distance / viewDistance);
            if (modifier != null) alpha *= switch (modifier) {
                case BRIGHT -> 1.5f; case PALE -> .66f; case FADING -> (float) Math.sin(viewer.tickCount / 3f) * .25f + .33f;
            };
            int index = 0; float angle = 0; double sum = 0;
            for (var aspect : aura.aspects()) {
                int amount = NodeVisualSize.amount(aura.amount(aspect)); sum += amount;
                // A fourteen-aspect node must not create nonfinite render vertices.
                float period = index == 14 ? 1 : 14 - index;
                float size = .2f + ((float) Math.sin(viewer.tickCount / period) * .25f + .5f) * (amount / 50f);
                int rotationPeriod = 5000 + 500 * index++;
                angle = Math.floorMod(time, rotationPeriod) / (float) rotationPeriod * (float) (Math.PI * 2);
                boolean translucent = aspect.blendFactor() == 771;
                strip(pose, buffers, frame, 0, angle, size, alpha * (translucent ? 1.5f : 1) / Math.max(1, count / 2f), aspect.color(), translucent, throughBlocks);
            }
            int row = switch (type) { case NORMAL -> 1; case UNSTABLE -> 6; case DARK -> 2; case TAINTED -> 5; case PURE -> 4; case HUNGRY -> 3; };
            float size = (float) (.1 + sum / count / 150);
            if (type == AuraNodeType.HUNGRY) size *= .75f;
            strip(pose, buffers, frame, row, type == AuraNodeType.UNSTABLE ? 0 : angle, size, alpha, 0xffffff,
                    type == AuraNodeType.DARK || type == AuraNodeType.TAINTED, throughBlocks);
        }
        pose.popPose();
    }
    private static void strip(PoseStack pose, MultiBufferSource buffers, int frame, int row, float angle, float radius, float alpha, int rgb, boolean translucent, boolean throughBlocks) {
        pose.pushPose(); pose.mulPose(Axis.ZP.rotation(angle));
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,throughBlocks ? TCRenderTypes.revealedNode(NODES, translucent)
                : TCRenderTypes.node(NODES,translucent)), pose.last().pose(),
                radius, frame / 32f, row / 32f, (frame + 1) / 32f, (row + 1) / 32f,
                (rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f, Math.min(1, alpha), 220);
        pose.popPose();
    }
}
