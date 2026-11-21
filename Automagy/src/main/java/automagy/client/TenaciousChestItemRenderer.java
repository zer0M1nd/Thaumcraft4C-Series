package automagy.client;

import automagy.content.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;

public final class TenaciousChestItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final TenaciousChestBlockEntity tile = new TenaciousChestBlockEntity(BlockPos.ZERO, Content.TENACIOUS_CHEST.get().defaultBlockState());
    public TenaciousChestItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()); }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(tile, pose, buffers, light, overlay);
    }
}
