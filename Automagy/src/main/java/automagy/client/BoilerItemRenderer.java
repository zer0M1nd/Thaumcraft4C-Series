package automagy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;

public final class BoilerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public BoilerItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();
        // ItemRenderer already applies the model's GUI display transform.
        BoilerRenderer.draw(pose,buffers,light,overlay,false,false);pose.popPose();
    }
}
