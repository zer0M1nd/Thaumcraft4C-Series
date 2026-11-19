package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Old sword blocking transforms applied through Forge's item extension points. */
public  class LegacySwordPose implements IClientItemExtensions {
    private static final HumanoidModel.ArmPose BLOCK = HumanoidModel.ArmPose.create("TC4_SWORD_BLOCK", false, (model, entity, side) -> {
        var arm = side == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        arm.xRot = arm.xRot * .5f - (float) Math.PI / 10 * 3;
        arm.yRot = 0;
    });
    @Override public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return entity.isUsingItem() && entity.getUsedItemHand() == hand ? BLOCK : HumanoidModel.ArmPose.ITEM;
    }
    @Override public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
            float partialTick, float equipProcess, float swingProcess) {
        var usedArm = player.getUsedItemHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        if (!player.isUsingItem() || usedArm != arm) return false;
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(side * .56f, -.52f - equipProcess * .6f, -.72f);
        pose.translate(side * -.5f, .2f, 0);
        pose.mulPose(Axis.YP.rotationDegrees(side * 30));
        pose.mulPose(Axis.XP.rotationDegrees(-80));
        pose.mulPose(Axis.YP.rotationDegrees(side * 60));
        return true;
    }
}
