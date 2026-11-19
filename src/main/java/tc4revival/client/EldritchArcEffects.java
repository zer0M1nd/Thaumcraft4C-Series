package tc4revival.client;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Brief ballistic electrical arcs, distinct from the branched wand lightning effect. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class EldritchArcEffects {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/beamh.png");
    private static final ArrayList<Arc> ARCS=new ArrayList<>();
    private static ClientLevel current;
    private static  class Arc {
        final Vec3 origin; final ArrayList<Vec3> points=new ArrayList<>(); final float r,g,b,length; int age;
        Arc(Level level,Vec3 origin,Vec3 end,float r,float g,float b,double height) {
            this.origin=origin; this.r=r; this.g=g; this.b=b;
            var target=end.subtract(origin); length=(float)target.length(); double horizontal=target.horizontalDistance();
            double peak=height+Math.max(0,target.y),vy=Math.sqrt(Math.max(0,peak*.115));
            double slopeTerm=2*(peak+Math.sqrt(Math.max(0,peak*(peak-target.y))));
            Vec3 velocity=horizontal>1e-9 && slopeTerm>1e-9 ? new Vec3(target.x*vy/slopeTerm,vy,target.z*vy/slopeTerm) : target;
            double step=velocity.lengthSqr(); Vec3 point=Vec3.ZERO; points.add(point);
            for (int index=0;index<50 && point.distanceToSqr(target)>step;index++) {
                point=point.add(velocity); var random=level.random;
                points.add(point.add((random.nextDouble()-random.nextDouble())*.25,(random.nextDouble()-random.nextDouble())*.25,(random.nextDouble()-random.nextDouble())*.25));
                velocity=velocity.add(0,-.115/1.9,0);
            }
            points.add(target);
        }
    }
    public static void emit(Level level,Vec3 from,Vec3 to,float r,float g,float b,float height) {
        var mc=Minecraft.getInstance(); if (level!=mc.level || !(level instanceof ClientLevel client) || from.distanceToSqr(to)<1e-9) return;
        if (current!=client) { ARCS.clear(); current=client; }
        if (ARCS.size()>=2000) ARCS.remove(0); ARCS.add(new Arc(level,from,to,r,g,b,height));
        ClientFocusEffects.arcEndpoint(level,to,r,g,b);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return; var mc=Minecraft.getInstance();
        if (current!=mc.level) { ARCS.clear(); current=mc.level; }
        if (mc.isPaused()) return; ARCS.removeIf(arc->arc.age++>=1);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || current==null) return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource(); var type=TCRenderTypes.softParticles(TEXTURE); var out=TCRenderTypes.buffer(buffers,type);
        var pose=event.getPoseStack(); var camera=event.getCamera().getPosition();
        for (var arc:ARCS) {
            pose.pushPose(); var offset=arc.origin.subtract(camera); pose.translate(offset.x,offset.y,offset.z);
            for (Vec3 width:new Vec3[]{new Vec3(0,.25,0),new Vec3(.25,0,.25)}) for (int index=1;index<arc.points.size();index++) {
                var first=arc.points.get(index-1); var second=arc.points.get(index);
                Vec3[] corners={first.subtract(width),first.add(width),second.add(width),second.subtract(width)};
                for (int vertex=0;vertex<4;vertex++) {
                    var at=corners[vertex]; float u=(vertex<2 ? index-1 : index)/arc.length,v=vertex==0 || vertex==3 ? 1 : 0;
                    out.vertex(pose.last().pose(),(float)at.x,(float)at.y,(float)at.z).color(arc.r,arc.g,arc.b,.8f).uv(u,v).uv2(200).endVertex();
                }
            }
            pose.popPose();
        }
        TCRenderTypes.endBatch(buffers,type);
    }
    private EldritchArcEffects() {}
}
