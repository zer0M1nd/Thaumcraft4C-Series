package thaumicomniverse.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.EssentiaTubeBlockEntity;

/** Brass rendering and servo animation belong to Omniverse. */
public class ConvenientTubeRenderer extends tc4revival.client.EssentiaTubeRenderer {
    public ConvenientTubeRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected ResourceLocation textureId(String texture) {
        return texture.equals("pipe_filter_core") ? super.textureId(texture)
            : ResourceLocation.parse("thaumicomniverse:block/brass_"+(texture.equals("servo_metal")?"pipe_1":texture));
    }
    @Override protected float[] textureUv(String texture) { return texture.equals("servo_metal") ? new float[]{7,7,9,9} : null; }
    @Override protected void renderAttachments(EssentiaTubeBlockEntity tube,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(tube instanceof thaumicomniverse.content.ConvenientTubeBlockEntity active)for(var side:Direction.values())if(active.servo(side)) {
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(side.getRotation());pose.translate(0,.32,0);
            cube(pose,buffers,light,overlay,"servo_metal",-.8f,-3,-.8f,.8f,0,.8f,0xffffff);
            pose.mulPose(Axis.YP.rotationDegrees(((tube.getLevel()==null?0:tube.getLevel().getGameTime()%3600)+partial)*2));
            cube(pose,buffers,light,overlay,"servo_metal",-2,-1,-2,2,1,2,0xffffff);
            for(int tooth=0;tooth<8;tooth++){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(tooth*45));cube(pose,buffers,light,overlay,"servo_metal",1.5f,-.7f,-.7f,3,.7f,.7f,0xffffff);pose.popPose();}
            pose.popPose();
        }
    }
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if(itemRenderer==null)itemRenderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()) {
            final ConvenientTubeRenderer renderer=new ConvenientTubeRenderer(null);
            @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
                if(stack.getItem() instanceof tc4revival.content.EssentiaTubeItem item)
                    renderer.render((EssentiaTubeBlockEntity)((tc4revival.content.EssentiaTubeBlock)item.getBlock()).newBlockEntity(net.minecraft.core.BlockPos.ZERO,item.getBlock().defaultBlockState()),0,pose,buffers,light,overlay);
            }
        };
        return itemRenderer;
    }
}
