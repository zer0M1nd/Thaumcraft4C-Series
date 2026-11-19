package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import tc4revival.content.*;

public  class EldritchMonumentRenderer implements BlockEntityRenderer<EldritchMonumentBlockEntity> {
    public EldritchMonumentRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public int getViewDistance() { return 96; }
    @Override public void render(EldritchMonumentBlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (!(entity.getBlockState().getBlock() instanceof EldritchMonumentBlock block)) return;
        boolean outer=entity.getLevel()!=null && entity.getLevel().dimension().equals(TCEldritch.OUTER_LANDS);
        if (block.kind()==EldritchMonumentBlock.Kind.OBELISK && entity.getLevel()!=null) light=LevelRenderer.getLightColor(entity.getLevel(),entity.getBlockPos().above(5));
        draw(block.kind(),outer,entity.eyes(),entity.getBlockPos(),partial,pose,buffers,light,overlay);
    }
    public static void draw(EldritchMonumentBlock.Kind kind,boolean outer,int eyes,BlockPos worldPos,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        String cap=outer ? "obelisk_cap_2" : kind==EldritchMonumentBlock.Kind.ALTAR ? "obelisk_cap_altar" : "obelisk_cap";
        if (kind==EldritchMonumentBlock.Kind.OBELISK || kind==EldritchMonumentBlock.Kind.SEGMENT) {
            var viewer=Minecraft.getInstance().getCameraEntity(); float time=viewer==null ? 0 : viewer.tickCount+partial;
            float bob=worldPos==null ? 0 : (float)Math.sin(time/10)*.1f+.1f;
            boolean near=worldPos!=null && viewer!=null && viewer.distanceToSqr(worldPos.getX()+.5,worldPos.getY()+.5,worldPos.getZ()+.5)<512;
            for (Direction side:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST})
                EldritchPlanes.draw(worldPos==null ? BlockPos.ZERO : worldPos,side,side.getAxisDirection()==Direction.AxisDirection.POSITIVE ? .99f : .01f,
                        0,1,1+bob,4+bob,near,partial,pose,buffers);
            var sideTexture=ResourceLocation.parse("thaumcraft:textures/models/"+(outer ? "obelisk_side_2" : "obelisk_side")+".png");
            pose.pushPose(); pose.translate(.5,1+bob,.5);
            for (int side=0;side<4;side++) {
                pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(side*90)); pose.translate(0,0,-.5);
                var out=buffers.getBuffer(RenderType.entityTranslucent(sideTexture)); var frame=pose.last();
                float[][] corners={{-.5f,3,0,1},{.5f,3,1,1},{.5f,0,1,0},{-.5f,0,0,0}};
                for (float[] vertex:corners) out.vertex(frame.pose(),vertex[0],vertex[1],0).color(1f,1f,1f,1f).uv(vertex[2],vertex[3])
                        .overlayCoords(overlay).uv2(light).normal(frame.normal(),0,0,-1).endVertex();
                pose.popPose();
            }
            pose.popPose();
            cap(cap,1+bob,90,pose,buffers,light,overlay); cap(cap,4+bob,-90,pose,buffers,light,overlay);
        } else {
            cap(cap,0,-90,pose,buffers,light,overlay);
            pose.pushPose(); pose.translate(.5,0,.5);
            for (int eye=0;eye<eyes;eye++) {
                pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(eye*90)); pose.translate(.46,.2,0);
                pose.mulPose(Axis.YP.rotationDegrees(90)); pose.mulPose(Axis.XP.rotationDegrees(-18));
                framedItem(TCEldritch.ELDRITCH_EYE.get().getDefaultInstance(),pose,buffers,light,overlay); pose.popPose();
            }
            pose.popPose();
        }
    }
    private static void cap(String texture,float y,float angle,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose(); pose.translate(.5,y,.5); pose.mulPose(Axis.XP.rotationDegrees(angle));
        EldritchMeshes.draw("obelisk_cap","Cap",pose,buffers,RenderType.entityTranslucent(ResourceLocation.parse("thaumcraft:textures/models/"+texture+".png")),light,overlay,1);
        pose.popPose();
    }
    static void framedItem(net.minecraft.world.item.ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose(); pose.translate(0,.125,0); pose.scale(.5f,.5f,.5f);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,overlay,pose,buffers,Minecraft.getInstance().level,0);
        pose.popPose();
    }
}
