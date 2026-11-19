package automagy.client;

import automagy.content.RedcrystalBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;

public final class RedcrystalItemRenderer extends BlockEntityWithoutLevelRenderer {
    public RedcrystalItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof RedcrystalBlock block)) return;
        pose.pushPose(); pose.translate(.5,.3,.5);
        if (context==ItemDisplayContext.GUI) pose.mulPose(Axis.XP.rotationDegrees(55));
        RedcrystalRenderer.draw(pose,buffers,light,overlay,block.kind,false,63,Direction.UP); pose.popPose();
    }
}
