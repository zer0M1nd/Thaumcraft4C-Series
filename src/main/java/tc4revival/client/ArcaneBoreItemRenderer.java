package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import tc4revival.content.*;

public  class ArcaneBoreItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static ArcaneBoreItemRenderer instance;
    private final ArcaneBoreRenderer boreRenderer=new ArcaneBoreRenderer(null);
    private final ArcaneBoreBaseRenderer baseRenderer=new ArcaneBoreBaseRenderer(null);
    private final ArcaneBoreBlockEntity bore=new ArcaneBoreBlockEntity(BlockPos.ZERO,TCArcane.ARCANE_BORE.get().defaultBlockState());
    private final ArcaneBoreBaseBlockEntity base=new ArcaneBoreBaseBlockEntity(BlockPos.ZERO,TCArcane.ARCANE_BORE_BASE.get().defaultBlockState());
    private ArcaneBoreItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
    public static ArcaneBoreItemRenderer instance() { if (instance==null) instance=new ArcaneBoreItemRenderer(); return instance; }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        boolean foundation=stack.is(TCItems.ARCANE_BORE_BASE.get());
        pose.pushPose(); pose.translate(.5,.5,.5); pose.mulPose(Axis.YP.rotationDegrees(180)); pose.translate(-.5,foundation?-.5:-.75,-.5);
        if (foundation) baseRenderer.render(base,0,pose,buffers,light,overlay); else boreRenderer.render(bore,0,pose,buffers,light,overlay);
        pose.popPose();
    }
}
