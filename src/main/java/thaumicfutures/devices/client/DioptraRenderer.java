package thaumicfutures.devices.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.ItemDisplayContext;
import thaumicfutures.devices.Dioptra;

public class DioptraRenderer implements BlockEntityRenderer<Dioptra> {
    public DioptraRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(Dioptra tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();float time=tile.getLevel().getGameTime()+partial;
        if(!tile.shard().isEmpty()) {
            pose.pushPose();pose.translate(.5,1.02,.5);pose.scale(.32f,.32f,.32f);pose.mulPose(Axis.YP.rotationDegrees(time*2));
            mc.getItemRenderer().renderStatic(tile.shard(),ItemDisplayContext.GROUND,light,overlay,pose,buffers,tile.getLevel(),0);pose.popPose();
        }
        if(!tile.active())return;
        int cx=(tile.getBlockPos().getX()>>4)*16+8,cz=(tile.getBlockPos().getZ()>>4)*16+8;
        for(var node:tile.nodes()) {
            pose.pushPose();pose.translate(.5+(node.pos().getX()+.5-cx)/208*.92,1.12+(node.pos().getY()-tile.getLevel().getMinBuildHeight())/(double)tile.getLevel().getHeight()*1.1,.5+(node.pos().getZ()+.5-cz)/208*.92);
            NodeGlyph.world(node,time,pose,buffers,.027f);pose.popPose();
        }
    }
    @Override public int getViewDistance() { return 64; }
}
