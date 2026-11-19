package automagy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;

public final class RemoteComparatorItemRenderer extends BlockEntityWithoutLevelRenderer {
    public RemoteComparatorItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();pose.translate(.5,.3,.5);
        if(context==ItemDisplayContext.GUI)pose.mulPose(Axis.XP.rotationDegrees(55));
        RemoteComparatorRenderer.draw(pose,buffers,light,overlay,false,false);pose.popPose();
    }
}
