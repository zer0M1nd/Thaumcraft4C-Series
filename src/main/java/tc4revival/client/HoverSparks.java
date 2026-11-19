package tc4revival.client;

import java.util.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class HoverSparks {
    private static final Map<Player,Long> NEXT=new WeakHashMap<>();
    private static final List<LightningGeometry> BOLTS=new ArrayList<>();
    private static Object world;
    private HoverSparks() {}
    public static void emit(Player player) {
        if (Minecraft.getInstance().isPaused()) return;
        if (world!=player.level()) { NEXT.clear(); BOLTS.clear(); world=player.level(); }
        long now=net.minecraft.Util.getMillis(); if (now<=NEXT.getOrDefault(player,0L)) return;
        NEXT.put(player,now+50+player.getRandom().nextInt(50));
        double y=player.getEyeY()-.45-(player.isCrouching()?.075:0);
        var center=new Vec3(player.getX(),y,player.getZ());
        float yaw=player.yBodyRot-90-player.getRandom().nextInt(180),pitch=-80+player.getRandom().nextInt(160);
        var hit=player.level().clip(new ClipContext(center,center.add(Vec3.directionFromRotation(pitch,yaw).scale(6)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if (hit.getType()==HitResult.Type.MISS) return;
        double angle=Math.toRadians(player.yBodyRot+90);
        var start=center.add(-Math.cos(angle)*.5,0,-Math.sin(angle)*.5);
        if (start.distanceToSqr(hit.getLocation())<1e-8) return;
        BOLTS.add(new LightningGeometry(start,hit.getLocation(),player.getRandom().nextLong(),1,2,3));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance();
        if (world!=mc.level) { BOLTS.clear(); NEXT.clear(); world=mc.level; }
        if (mc.isPaused()) return;
        BOLTS.removeIf(LightningGeometry::expired); BOLTS.forEach(LightningGeometry::tick);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || BOLTS.isEmpty()) return;
        var camera=event.getCamera().getPosition(); var pose=event.getPoseStack().last().pose();
        var look=event.getCamera().getLookVector(); var facing=new Vec3(look.x(),look.y(),look.z());
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        for (int pass=0;pass<2;pass++) {
            var type=TCRenderTypes.softParticles(ResourceLocation.parse("thaumcraft:textures/misc/"+(pass==0?"p_large":"p_small")+".png")); var out=TCRenderTypes.buffer(buffers,type);
            for (var bolt:BOLTS) {
                float fade=Math.max(0,bolt.age)/(float)bolt.lifetime;
                float alpha=pass==0 ? (1-fade)*.2f : 1-fade*.2f;
                for (var edge:bolt.edges) {
                    if (edge.progress>bolt.visibleSegments(event.getPartialTick())) continue;
                    float width=.015f*((float)edge.start().distanceTo(camera)/5+1)*(1+edge.brightness)/2;
                    var left=edge.join(true,facing,width); var right=edge.join(false,facing,width);
                    var from=edge.start().subtract(camera); var to=edge.end().subtract(camera);
                    line(out,pose,from,to,left,right,.5f,.5f,alpha*edge.brightness);
                    if (edge.next==null) line(out,pose,to,to.add(edge.direction().normalize().scale(width)),right,right,.5f,0,alpha*edge.brightness);
                    if (edge.previous==null) line(out,pose,from.subtract(edge.direction().normalize().scale(width)),from,left,left,0,.5f,alpha*edge.brightness);
                }
            }
            TCRenderTypes.endBatch(buffers,type);
        }
    }
    private static void line(VertexConsumer out,org.joml.Matrix4f pose,Vec3 from,Vec3 to,Vec3 left,Vec3 right,float u1,float u2,float alpha) {
        vertex(out,pose,from.subtract(left),u1,0,alpha); vertex(out,pose,to.subtract(right),u2,0,alpha);
        vertex(out,pose,to.add(right),u2,1,alpha); vertex(out,pose,from.add(left),u1,1,alpha);
    }
    private static void vertex(VertexConsumer out,org.joml.Matrix4f pose,Vec3 at,float u,float v,float alpha) {
        out.vertex(pose,(float)at.x,(float)at.y,(float)at.z).color(.75f,1,1,alpha).uv(u,v).uv2(0xf000f0).endVertex();
    }
}
