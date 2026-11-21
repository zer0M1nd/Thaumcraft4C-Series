package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import tc4revival.content.ArcanePedestalBlockEntity;

public  class ArcanePedestalRenderer implements BlockEntityRenderer<ArcanePedestalBlockEntity> {
    public ArcanePedestalRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ArcanePedestalBlockEntity pedestal,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance(); var viewer=mc.getCameraEntity(); if (viewer==null || pedestal.isEmpty() || pedestal.getLevel()==null) return;
        float ticks=viewer.tickCount+partial; var stack=pedestal.getItem(0).copyWithCount(1);
        pose.pushPose(); pose.translate(.5,1.15+Math.sin(ticks%32767/16f)*.05,.5); pose.mulPose(Axis.YP.rotationDegrees(ticks%360));
        // Ground context supplies the item-entity scale and respects custom wand/focus models.
        float scale=stack.getItem() instanceof net.minecraft.world.item.BlockItem ? 2 : 1; pose.scale(scale,scale,scale);
        mc.getItemRenderer().renderStatic(stack,net.minecraft.world.item.ItemDisplayContext.GROUND,light,overlay,pose,buffers,pedestal.getLevel(),0);
        // Modern generated item models already contain both faces. A second, mirrored
        // copy occupies the same planes and z-fights under Fast graphics.
        pose.popPose();
    }
    public static void sparkles(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,boolean finished) {
        int base=finished ? 10 : 5;
        int count=switch(Minecraft.getInstance().options.particles().get()) { case ALL->base*2; case DECREASED->base; case MINIMAL->0; };
        for (int index=0;index<count;index++) ClientFocusEffects.blockSparkle(level,pos.above(),finished ? -9999 : 0xc000c0,2);
    }
}
