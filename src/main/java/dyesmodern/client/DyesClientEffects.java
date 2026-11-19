package dyesmodern.client;

import java.util.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dyesmodern.DyesNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.client.TCRenderTypes;

/** The Ancient Mask's type-5 fractal bolt: purple translucent halo and black core. */
@Mod.EventBusSubscriber(modid="thaumicdyes",value=Dist.CLIENT)
public final class DyesClientEffects {
    private record Beam(DyesLightningGeometry geometry,float width){}
    private static final List<Beam> BOLTS=new ArrayList<>();
    private static Object level;
    public static void receiveMaskBeam(DyesNetwork.MaskBeam packet,Connection connection) {
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.getConnection()==null||mc.getConnection().getConnection()!=connection||!mc.level.dimension().location().equals(packet.dimension()))return;
        maskBeam(packet.sourceId(),packet.targetId());
    }
    public static void maskBeam(int sourceId,int targetId) {
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(level!=mc.level){BOLTS.clear();level=mc.level;}
        if(!(mc.level.getEntity(sourceId) instanceof LivingEntity source)||!(mc.level.getEntity(targetId) instanceof LivingEntity target))return;
        boolean local=source==mc.player;
        double offset=local&&mc.options.getCameraType().isFirstPerson()?.3:0;
        var from=new Vec3(source.getX(),source.getBoundingBox().minY+source.getBbHeight()/2+.75-offset,source.getZ());
        var to=new Vec3(target.getX(),target.getBoundingBox().maxY-.5,target.getZ());
        if(from.distanceToSqr(to)<1e-8)return;
        BOLTS.add(new Beam(new DyesLightningGeometry(from,to,source.getRandom().nextLong(),6,.5f,5),local?.125f:.0625f));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(level!=mc.level){BOLTS.clear();level=mc.level;}
        if(mc.isPaused())return;
        BOLTS.forEach(bolt->bolt.geometry().tick());BOLTS.removeIf(bolt->bolt.geometry().expired());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||BOLTS.isEmpty())return;
        var mc=Minecraft.getInstance();if(level!=mc.level){BOLTS.clear();level=mc.level;return;}
        var camera=event.getCamera().getPosition();var pose=event.getPoseStack().last().pose();
        var view=event.getCamera().getLookVector();var facing=new Vec3(view.x(),view.y(),view.z());
        var buffers=mc.renderBuffers().bufferSource();
        for(int pass=0;pass<2;pass++) {
            var type=TCRenderTypes.softParticle(ResourceLocation.parse("thaumcraft:textures/misc/"+(pass==0?"p_large":"p_small")+".png"));var out=buffers.getBuffer(type);
            for(var beam:BOLTS) {
                var bolt=beam.geometry();double limit=Minecraft.useFancyGraphics()?100:50;if(bolt.origin.distanceToSqr(camera)>limit*limit)continue;
                float fade=Math.max(0,bolt.age)/(float)bolt.lifetime,alpha=pass==0?(1-fade)*.4f:1-fade*.5f;
                for(var edge:bolt.edges) {
                    if(edge.progress>bolt.visibleSegments(event.getPartialTick()))continue;
                    float width=beam.width()*((float)edge.start().distanceTo(camera)/5+1)*(1+edge.brightness)/2;
                    var left=edge.join(true,facing,width);var right=edge.join(false,facing,width);
                    var from=edge.start().subtract(camera);var to=edge.end().subtract(camera);
                    line(out,pose,from,to,left,right,.5f,.5f,alpha*edge.brightness,pass);
                    if(edge.next==null)line(out,pose,to,to.add(edge.direction().normalize().scale(width)),right,right,.5f,0,alpha*edge.brightness,pass);
                    if(edge.previous==null)line(out,pose,from.subtract(edge.direction().normalize().scale(width)),from,left,left,0,.5f,alpha*edge.brightness,pass);
                }
            }
            buffers.endBatch(type);
        }
    }
    private static void line(VertexConsumer out,org.joml.Matrix4f pose,Vec3 from,Vec3 to,Vec3 left,Vec3 right,float u1,float u2,float alpha,int pass) {
        vertex(out,pose,from.subtract(left),u1,0,alpha,pass);vertex(out,pose,to.subtract(right),u2,0,alpha,pass);
        vertex(out,pose,to.add(right),u2,1,alpha,pass);vertex(out,pose,from.add(left),u1,1,alpha,pass);
    }
    private static void vertex(VertexConsumer out,org.joml.Matrix4f pose,Vec3 at,float u,float v,float alpha,int pass) {
        out.vertex(pose,(float)at.x,(float)at.y,(float)at.z).color(pass==0?.6f:0,pass==0?.2f:0,pass==0?.6f:0,alpha).uv(u,v).uv2(0xf000f0).endVertex();
    }
    private DyesClientEffects(){}
}
