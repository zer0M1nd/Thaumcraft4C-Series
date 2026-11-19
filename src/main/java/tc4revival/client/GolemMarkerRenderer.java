package tc4revival.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;
import tc4revival.config.TCConfig;

/** Original face decals and animated script ribbons, using camera-relative modern buffers. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class GolemMarkerRenderer {
    private static final ResourceLocation HOME=ResourceLocation.parse("thaumcraft:textures/misc/home.png");
    private static final ResourceLocation MARK=ResourceLocation.parse("thaumcraft:textures/misc/mark.png");
    private static final ResourceLocation AIR=ResourceLocation.parse("thaumcraft:textures/blocks/empty.png");
    private static final ResourceLocation SCRIPT=ResourceLocation.parse("thaumcraft:textures/misc/script.png");
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc=Minecraft.getInstance(); if (mc.player==null || mc.level==null) return;
        var held=mc.player.getMainHandItem();
        if (!(held.getItem() instanceof GolemBellItem) && !(held.getItem() instanceof GolemPlacerItem)) return;
        if (!held.hasTag() || !held.getTag().contains("markers")) return;
        var tag=held.getTag(); GolemEntity golem=null;
        if (held.getItem() instanceof GolemBellItem) { golem=GolemBellItem.selected(held,mc.level); if (golem==null) return; }
        var pose=event.getPoseStack(); var view=event.getCamera().getPosition();
        var buffers=mc.renderBuffers().bufferSource(); float time=System.nanoTime()/30000000L;
        pose.pushPose(); pose.translate(-view.x,-view.y,-view.z);
        if (golem!=null) {
            var previous=golem.getPosition(event.getPartialTick()).add(0,.15,0);
            var lines=TCRenderTypes.buffer(buffers,net.minecraft.client.renderer.RenderType.lines());
            for (var node:golem.visibleRoute()) {
                var next=Vec3.atBottomCenterOf(node).add(0,.15,0);
                var direction=next.subtract(previous); var normal=direction.normalize();
                if (direction.lengthSqr()>0.0001) {
                    lines.vertex(pose.last().pose(),(float)previous.x,(float)previous.y,(float)previous.z).color(64,230,255,230).normal(pose.last().normal(),(float)normal.x,(float)normal.y,(float)normal.z).endVertex();
                    lines.vertex(pose.last().pose(),(float)next.x,(float)next.y,(float)next.z).color(64,230,255,230).normal(pose.last().normal(),(float)normal.x,(float)normal.y,(float)normal.z).endVertex();
                }
                net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose,lines,next.x-.055,next.y-.055,next.z-.055,next.x+.055,next.y+.055,next.z+.055,1,.75f,.15f,1);
                previous=next;
            }
        }
        var frames=tag.getCompound("MovingMarkerFrames");
        if (golem!=null && tag.contains("golemhomex") && tag.contains("golemhomeface")) {
            var home=new BlockPos(tag.getInt("golemhomex"),tag.getInt("golemhomey"),tag.getInt("golemhomez")); int side=tag.getInt("golemhomeface");
            var binding=thaumcraft.api.golems.GolemAnchor.load(tag.getCompound("golemhomeanchor"));
            if(binding!=null&&frames.contains(binding.id().toString())){
                var frame=thaumcraft.api.golems.GolemLocationVisuals.resolve(mc.level,frames.getCompound(binding.id().toString()),event.getPartialTick());
                if(frame!=null)movingDecal(pose,TCRenderTypes.buffer(buffers,TCRenderTypes.runes(HOME)),frame,-1,time,.65f);
            }else if (side>=0 && side<6 && mc.player.distanceToSqr(home.getX(),home.getY(),home.getZ())<4096)
                decal(pose,TCRenderTypes.buffer(buffers,TCRenderTypes.runes(HOME)),home,Direction.from3DDataValue(side),-1,time,.65f,false);
        }
        for (var value:tag.getList("markers",Tag.TAG_COMPOUND)) {
            var marker=GolemMarker.load((net.minecraft.nbt.CompoundTag)value).orElse(null);
            if (marker==null || !marker.dimension().equals(mc.level.dimension().location())) continue;
            if(marker.anchor()!=null&&frames.contains(marker.anchor().id().toString())){
                var frame=thaumcraft.api.golems.GolemLocationVisuals.resolve(mc.level,frames.getCompound(marker.anchor().id().toString()),event.getPartialTick());
                if(frame!=null&&frame.center().distanceToSqr(mc.player.position())<4096){
                    movingDecal(pose,TCRenderTypes.buffer(buffers,TCRenderTypes.runes(MARK)),frame,marker.color(),time,.4f);
                    int quality=TCConfig.CLIENT.golemLinkQuality();if(golem!=null&&quality>3)ribbon(pose,TCRenderTypes.buffer(buffers,TCRenderTypes.runes(SCRIPT)),marker,golem,event.getPartialTick(),time,quality,frame);
                }continue;
            }
            var adjacent=marker.pos().relative(marker.face());
            if (mc.player.distanceToSqr(adjacent.getX(),adjacent.getY(),adjacent.getZ())>=4096) continue;
            decal(pose,TCRenderTypes.buffer(buffers,TCRenderTypes.runes(MARK)),adjacent,marker.face(),marker.color(),time,.4f,false);
            if (mc.level.isEmptyBlock(marker.pos())) for (var face:Direction.values())
                decal(pose,TCRenderTypes.buffer(buffers,TCRenderTypes.runes(AIR)),marker.pos().relative(face),face,marker.color(),time,.98f,true);
            int quality=TCConfig.CLIENT.golemLinkQuality();
            if (golem!=null && quality>3) ribbon(pose,TCRenderTypes.buffer(buffers,TCRenderTypes.runes(SCRIPT)),marker,golem,event.getPartialTick(),time,quality);
        }
        pose.popPose();
        TCRenderTypes.endBatch(buffers,net.minecraft.client.renderer.RenderType.lines());
        for (var texture:new ResourceLocation[]{HOME,MARK,AIR,SCRIPT}) TCRenderTypes.endBatch(buffers,TCRenderTypes.runes(texture));
    }
    private static void movingDecal(PoseStack pose,VertexConsumer out,thaumcraft.api.golems.GolemLocationVisuals.Frame frame,int color,float time,float scale){
        var normal=frame.normal().normalize();var right=normal.cross(frame.up()).normalize();var up=right.cross(normal).normalize();var center=frame.center().add(normal.scale(.51));
        var rgb=color(color,time,0,true);
        for(int i=0;i<4;i++){double x=i==0||i==3?-.5:.5,y=i<2?.5:-.5;var point=center.add(right.scale(x*scale)).add(up.scale(y*scale));vertex(out,pose,point.x,point.y,point.z,(float)(x+.5),(float)(y+.5),rgb,1);}
    }
    private static float[] color(int color,float time,float phase,boolean pulse) {
        if (color>=0) { int rgb=TallowCandleBlock.COLORS[color]; return new float[]{(rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f}; }
        if (!pulse) return new float[]{1,1,1};
        return new float[]{Mth.sin(time%32767/12+phase)*.2f+.8f,Mth.sin(time%32767/14+phase)*.2f+.8f,Mth.sin(time%32767/16+phase)*.2f+.8f};
    }
    private static void decal(PoseStack pose,VertexConsumer out,BlockPos pos,Direction face,int color,float time,float scale,boolean air) {
        var rgb=color(color,time,face.get3DDataValue(),true); float epsilon=air?-.01f:.01f;
        pose.pushPose(); pose.translate(pos.getX()+.5+face.getStepX()*epsilon,pos.getY()+.5+face.getStepY()*epsilon,pos.getZ()+.5+face.getStepZ()*epsilon);
        pose.mulPose(new org.joml.Quaternionf().rotationAxis((float)Math.PI/2,-face.getStepY(),face.getStepX(),-face.getStepZ()));
        pose.translate(0,0,face.getStepZ()<0?.5:-.5); pose.scale(scale,scale,scale);
        vertex(out,pose,-.5,.5,0,0,1,rgb,1); vertex(out,pose,.5,.5,0,1,1,rgb,1);
        vertex(out,pose,.5,-.5,0,1,0,rgb,1); vertex(out,pose,-.5,-.5,0,0,0,rgb,1); pose.popPose();
    }
    private record Point(Vec3 position,float u,float alpha,float[] rgb) {}
    private static void ribbon(PoseStack pose,VertexConsumer out,GolemMarker marker,GolemEntity golem,float partial,float time,int quality) {
        ribbon(pose,out,marker,golem,partial,time,quality,null);
    }
    private static void ribbon(PoseStack pose,VertexConsumer out,GolemMarker marker,GolemEntity golem,float partial,float time,int quality,thaumcraft.api.golems.GolemLocationVisuals.Frame frame) {
        var origin=new Vec3(Mth.lerp(partial,golem.xo,golem.getX()),Mth.lerp(partial,golem.yo,golem.getY()),Mth.lerp(partial,golem.zo,golem.getZ()));
        var direction=frame==null?Vec3.atLowerCornerOf(marker.face().getNormal()):frame.normal();
        var anchor=(frame==null?Vec3.atLowerCornerOf(marker.pos()):frame.center().subtract(.5,.5,.5)).add(direction.scale(.5));
        float distance=(float)anchor.distanceTo(origin),length=Math.round(distance)*(float)quality;
        // A rounded zero-length link made the original interpolation divide by zero.
        if (length<=0 || !Float.isFinite(length)) return;
        // Extreme user configuration must not overflow the loop counter or allocate billions of vertices.
        int segments=(int)Math.min(65536,length);
        var head=origin.add(0,golem.getBbHeight(),0);
        var far=anchor.add(.5,.5,.5).add(direction.scale(.5)).subtract(head);
        var near=anchor.add(.5,.5,.5).subtract(head);
        Point previous=null;
        for (int segment=0;segment<=segments;segment++) {
            float index=segment==segments?length:segment*(length/segments);
            float fraction=index/length,envelope=1-Math.abs(index-length/2)/(length/2),tail=0;
            float phase=distance*(1-fraction)*quality-time%32767/5;
            int side=marker.face().get3DDataValue();
            var wave=new Vec3(Mth.sin((float)(side*20+anchor.z%16+phase)/4)*.5f*envelope,
                    Mth.sin((float)(side*20+anchor.x%16+phase)/3)*.5f*envelope,
                    Mth.sin((float)(side*20+anchor.y%16+phase)/2)*.5f*envelope);
            var offset=far.add(wave);
            if (index>length-quality/2) {
                float blend=(length-index)/(quality/2f); tail=1-blend;
                offset=offset.scale(blend).add(near.add(wave).scale(tail));
            }
            var point=new Point(head.add(offset.scale(fraction)),(1-fraction)*distance-time*.005f,Math.min(.75f,index*1.5f/length)*(1-tail),color(marker.color(),time,side+index,true));
            if (previous!=null) {
                ribbonVertex(out,pose,previous,-.05,1); ribbonVertex(out,pose,point,-.05,1);
                ribbonVertex(out,pose,point,.05,0); ribbonVertex(out,pose,previous,.05,0);
            }
            previous=point;
        }
    }
    private static void ribbonVertex(VertexConsumer out,PoseStack pose,Point point,double y,float v) { vertex(out,pose,point.position.x,point.position.y+y,point.position.z,point.u,v,point.rgb,point.alpha); }
    private static void vertex(VertexConsumer out,PoseStack pose,double x,double y,double z,float u,float v,float[] rgb,float alpha) {
        out.vertex(pose.last().pose(),(float)x,(float)y,(float)z).color(rgb[0],rgb[1],rgb[2],alpha).uv(u,v).uv2(200).endVertex();
    }
    private GolemMarkerRenderer() {}
}
