package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import tc4revival.client.*;

/** One frame-interpolated orbit per synced receiver, independent of packet and particle cadence. */
public final class KineticFocusEffects {
    private static final Map<KineticBlockEntity,Orbit> ORBITS=new WeakHashMap<>();
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private record Orbit(Direction face,double angle,double tick,float speed){
        double angleAt(double time){return angle+(time-tick)*speed*.3*face.getAxisDirection().getStep();}
    }
    public static void update(KineticBlockEntity tile){
        var view=(KineticFocusView)tile;var face=view.omniverse$driveFace();float speed=view.omniverse$driveSpeed();
        var level=tile.getLevel();
        if(level==null||face==null||speed==0){ORBITS.remove(tile);return;}
        double now=level.getGameTime()+Minecraft.getInstance().getFrameTime();
        var previous=ORBITS.get(tile);double angle=previous==null?0:previous.angleAt(now)%360;
        ORBITS.put(tile,new Orbit(face,angle,now,speed));
    }
    public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();var level=mc.level;if(level==null){ORBITS.clear();return;}
        var buffers=mc.renderBuffers().bufferSource();var type=TCRenderTypes.runes(TEXTURE);var out=buffers.getBuffer(type);
        var camera=event.getCamera();var eye=camera.getPosition();var pose=event.getPoseStack();
        double time=level.getGameTime()+event.getPartialTick();
        for(var iterator=ORBITS.entrySet().iterator();iterator.hasNext();){
            var entry=iterator.next();var tile=entry.getKey();var orbit=entry.getValue();
            if(tile.isRemoved()||tile.getLevel()!=level||level.getBlockEntity(tile.getBlockPos())!=tile){iterator.remove();continue;}
            if(tile.getBlockPos().getCenter().distanceToSqr(eye)>96*96)continue;
            var normal=Vec3.atLowerCornerOf(orbit.face.getNormal());
            var u=normal.cross(orbit.face.getAxis()==Direction.Axis.Y?new Vec3(0,0,1):new Vec3(0,1,0)).normalize();var v=normal.cross(u);
            var center=tile.getBlockPos().getCenter().add(normal.scale(.57));
            // A short continuous trail makes high-speed direction legible without tick-spaced flashes.
            for(int trail=0;trail<8;trail++){
                double angle=Math.toRadians(orbit.angleAt(time-trail/32d));
                var radial=u.scale(Math.cos(angle)*.32).add(v.scale(Math.sin(angle)*.32));
                for(int sign:new int[]{-1,1}){
                    var at=center.add(radial.scale(sign)).subtract(eye);
                    pose.pushPose();pose.translate(at.x,at.y,at.z);pose.mulPose(camera.rotation());
                    float radius=trail==0?.17f:.10f,alpha=trail==0?.5f:.12f*(1-trail/8f);
                    var matrix=pose.last().pose();
                    out.vertex(matrix,-radius,-radius,0).color(.643f,.87f,1,alpha).uv(0,1).uv2(240).endVertex();
                    out.vertex(matrix,radius,-radius,0).color(.643f,.87f,1,alpha).uv(.125f,1).uv2(240).endVertex();
                    out.vertex(matrix,radius,radius,0).color(.643f,.87f,1,alpha).uv(.125f,.875f).uv2(240).endVertex();
                    out.vertex(matrix,-radius,radius,0).color(.643f,.87f,1,alpha).uv(0,.875f).uv2(240).endVertex();
                    pose.popPose();
                }
            }
        }
        buffers.endBatch(type);
    }
    private KineticFocusEffects(){}
}
