package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.FocalManipulatorBlockEntity;

public  class FocalManipulatorRenderer implements BlockEntityRenderer<FocalManipulatorBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/models/wandtable.png");
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    public FocalManipulatorRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(FocalManipulatorBlockEntity table, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ArcaneWorkbenchRenderer.renderTable(pose, buffers, light, overlay, TEXTURE);
        if (table.isEmpty()) return;
        var mc = Minecraft.getInstance(); float ticks = (mc.getCameraEntity() == null ? 0 : mc.getCameraEntity().tickCount) + partial;
        pose.pushPose(); pose.translate(.5, 1 + Math.sin((Math.sin(ticks / 14) * .2 + .2)) * .1 + .1, .5);
        pose.mulPose(Axis.YP.rotationDegrees(ticks % 360)); pose.scale(.5f, .5f, .5f);
        mc.getItemRenderer().renderStatic(table.getItem(0), ItemDisplayContext.FIXED, light, overlay, pose, buffers, table.getLevel(), 0);
        pose.popPose();
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer == null) itemRenderer = new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
            @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                ArcaneWorkbenchRenderer.renderTable(pose, buffers, light, overlay, TEXTURE);
            }
        };
        return itemRenderer;
    }
}
