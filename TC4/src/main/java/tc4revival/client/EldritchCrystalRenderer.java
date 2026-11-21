package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.EldritchCrystalBlock;
import tc4revival.content.EldritchCrystalBlockEntity;

public  class EldritchCrystalRenderer implements BlockEntityRenderer<EldritchCrystalBlockEntity> {
    private static final ResourceLocation BASE=ResourceLocation.parse("thaumcraft:textures/block/crust.png"),CRYSTAL=ResourceLocation.parse("thaumcraft:textures/models/vcrystal.png");
    public EldritchCrystalRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(EldritchCrystalBlockEntity crystal,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        draw(crystal.getBlockState().getValue(EldritchCrystalBlock.FACING),crystal.hashCode()%4,pose,buffers,light,overlay);
    }
    public static void draw(Direction facing,int rotation,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose(); pose.translate(.5,.5,.5);
        switch(facing) {
            case DOWN->pose.mulPose(Axis.XP.rotationDegrees(90)); case UP->pose.mulPose(Axis.XP.rotationDegrees(-90));
            case NORTH->pose.mulPose(Axis.YP.rotationDegrees(180)); case WEST->pose.mulPose(Axis.YP.rotationDegrees(-90));
            case EAST->pose.mulPose(Axis.YP.rotationDegrees(90)); default->{}
        }
        pose.translate(0,0,-.5); pose.mulPose(Axis.ZP.rotationDegrees(90*rotation));
        EldritchMeshes.draw("vcrystal","Base",pose,buffers,RenderType.entityCutoutNoCull(BASE),light,overlay,1);
        var player=Minecraft.getInstance().player; float tick=player==null ? 0 : player.tickCount;
        int glow=(int)(210*((float)Math.sin(tick/6)*.075f+.925f));
        EldritchMeshes.draw("vcrystal","Crystal",pose,buffers,RenderType.entityTranslucent(CRYSTAL),glow,overlay,.7f);
        pose.popPose();
    }
}
