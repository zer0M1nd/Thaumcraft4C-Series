package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.*;

public  class HungryChestRenderer implements BlockEntityRenderer<HungryChestBlockEntity> {
    private final net.minecraft.client.model.geom.ModelPart chest=TravelingTrunkModel.create().bakeRoot();
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    public HungryChestRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(HungryChestBlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var facing=entity.getBlockState().getValue(HungryChestBlock.FACING);
        float yaw=switch (facing) { case NORTH->180; case WEST->90; case EAST->-90; default->0; };
        float closed=1-net.minecraft.util.Mth.lerp(partial,entity.previousLid,entity.lid);
        chest.getChild("lid").xRot=chest.getChild("knob").xRot=-(1-closed*closed*closed)*(float)Math.PI/2;
        pose.pushPose(); pose.translate(0,1,1); pose.scale(1,-1,-1); pose.translate(.5,.5,.5); pose.mulPose(Axis.YP.rotationDegrees(yaw)); pose.translate(-.5,-.5,-.5);
        chest.render(pose,buffers.getBuffer(RenderType.entityCutout(ResourceLocation.parse("thaumcraft:textures/models/chesthungry.png"))),light,overlay);
        pose.popPose();
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer==null) itemRenderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()) {
            private final HungryChestRenderer renderer=new HungryChestRenderer(null);
            @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
                pose.pushPose(); pose.translate(.5,.5,.5); pose.mulPose(Axis.YP.rotationDegrees(90)); pose.translate(-.5,-.5,-.5);
                renderer.render(new HungryChestBlockEntity(BlockPos.ZERO,TCArcane.HUNGRY_CHEST.get().defaultBlockState()),0,pose,buffers,light,overlay); pose.popPose();
            }
        };
        return itemRenderer;
    }
}
