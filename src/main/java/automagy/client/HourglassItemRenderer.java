package automagy.client;

import automagy.content.Content;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;

public final class HourglassItemRenderer extends BlockEntityWithoutLevelRenderer {
    public HourglassItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()); }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose(); pose.translate(.5, .5, .5); pose.scale(.42f, .42f, .42f);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        HourglassRenderer.draw(pose, buffers, light, overlay, stack.is(Content.MAGIC_HOURGLASS.get().asItem()), 1, false, 5);
        pose.popPose();
    }
}
