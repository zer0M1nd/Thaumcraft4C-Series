package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.AuraNodeBlockEntity;
import tc4revival.content.TCItems;

public  class AuraNodeRenderer implements BlockEntityRenderer<AuraNodeBlockEntity> {
    public AuraNodeRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(AuraNodeBlockEntity node, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        thaumcraft.api.client.DeviceRendering.render(new thaumcraft.api.client.DeviceRendering.Context<>(node,partial,pose,buffers,light,overlay),()->renderDefault(node,partial,pose,buffers,light,overlay));
    }
    private void renderDefault(AuraNodeBlockEntity node, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var viewer = Minecraft.getInstance().getCameraEntity(); boolean visible = false; double distance = 64;
        if (viewer instanceof Player player) {
            var helmet = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
            visible = tc4revival.content.RevealingGear.visible(player,true);
            if (!visible && (player.getMainHandItem().is(TCItems.THAUMOMETER.get()) || player.isUsingItem() && player.getUseItem().is(TCItems.THAUMOMETER.get()))) {
                var offset = Vec3.atCenterOf(node.getBlockPos()).subtract(viewer.getEyePosition(partial));
                visible = offset.lengthSqr() < 1e-8 || viewer.getViewVector(partial).dot(offset.normalize()) > Math.cos(.44); distance = 48;
            }
        }
        RevealedNodePass.enqueue(node,pose,distance,visible);
        NodeDrainBeam.render(node, partial, pose, buffers);
    }
}
