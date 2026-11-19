package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public  class WandItemExtension implements IClientItemExtensions {
    @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { return WandRenderer.instance(); }
    @Override public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
            float partialTick, float equip, float swing) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float root = (float) Math.sin(Math.sqrt(swing) * Math.PI);
        pose.translate(side * (-0.4f * root), 0.2f * Math.sin(Math.sqrt(swing) * Math.PI * 2), -0.2f * Math.sin(swing * Math.PI));
        pose.translate(side * .56, -.52 - equip * .6, -.72);
        pose.mulPose(Axis.YP.rotationDegrees(side * (45 - (float) Math.sin(swing * swing * Math.PI) * 20)));
        pose.mulPose(Axis.ZP.rotationDegrees(side * -20 * root));
        pose.mulPose(Axis.XP.rotationDegrees(-80 * root));
        pose.scale(.4f, .4f, .4f);
        // Cancels modern bow-specific drawing; wand casting animates its own mesh.
        return true;
    }
}
