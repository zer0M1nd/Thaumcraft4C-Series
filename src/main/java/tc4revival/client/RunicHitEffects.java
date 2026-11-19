package tc4revival.client;

import java.util.ArrayList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class RunicHitEffects {
    private static final ArrayList<Hit> HITS=new ArrayList<>();
    private static net.minecraft.client.multiplayer.ClientLevel current;
    private static GroupedObjMesh mesh;
    private RunicHitEffects() {}
    public static void reload(net.minecraft.server.packs.resources.ResourceManager resources) { mesh=null; HITS.clear(); }
    public static void receive(tc4revival.network.RunicHitPacket packet,net.minecraft.network.Connection connection) {
        var mc=Minecraft.getInstance(); if (mc.getConnection()==null || mc.getConnection().getConnection()!=connection || mc.level==null) return;
        if (current!=mc.level) { HITS.clear(); current=mc.level; }
        var entity=current.getEntity(packet.source()); if (entity==null) return;
        if(hiddenInFirstPerson(entity))return;
        if (packet.target()>=0) {
            var other=current.getEntity(packet.target()); float yaw=0,pitch=90;
            if (other!=null) {
                var delta=entity.getBoundingBox().getCenter().subtract(other.getBoundingBox().getCenter());
                yaw=(float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90; pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z)));
            }
            add(entity,yaw,pitch);
        } else {
            if (packet.target()==-1 || packet.target()==-3) add(entity,0,90);
            if (packet.target()==-1 || packet.target()==-2) add(entity,0,270);
        }
    }
    private static void add(Entity entity,float yaw,float pitch) { if (HITS.size()<512) HITS.add(new Hit(entity,yaw,pitch,8+current.random.nextInt(4))); }
    private static boolean hiddenInFirstPerson(Entity entity) {
        var mc=Minecraft.getInstance();
        return mc.options.getCameraType().isFirstPerson()
                && (entity==mc.player || entity==mc.getCameraEntity());
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.START) return;
        var mc=Minecraft.getInstance(); if (current!=mc.level) { HITS.clear(); current=mc.level; }
        if (current==null || mc.isPaused()) return;
        HITS.removeIf(hit->hit.entity.isRemoved() || hit.age++>=hit.lifetime);
        for (var hit:HITS) { hit.previous=hit.position; hit.position=hit.entity.getBoundingBox().getCenter(); }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || HITS.isEmpty()) return;
        if (mesh==null) try (var reader=new java.io.InputStreamReader(Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumcraft:models/hemis.obj")).open(),java.nio.charset.StandardCharsets.UTF_8)) { mesh=GroupedObjMesh.read(reader); }
        catch (java.io.IOException failure) { throw new IllegalStateException("Cannot read runic hemisphere",failure); }
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource(); var camera=event.getCamera().getPosition(); float partial=event.getPartialTick();
        var used=new java.util.HashSet<net.minecraft.client.renderer.RenderType>();
        for (var hit:HITS) {
            if (hiddenInFirstPerson(hit.entity) || hit.entity==event.getCamera().getEntity() && !event.getCamera().isDetached()) continue;
            float fade=(hit.age+partial)/hit.lifetime; int frame=Math.min(15,(int)(14*fade)+1);
            var type=TCRenderTypes.runes(ResourceLocation.parse("thaumcraft:textures/models/hemis"+frame+".png")); used.add(type); var out=TCRenderTypes.buffer(buffers,type);
            var pose=WorldEffectPose.copy(event.getPoseStack()); var position=hit.previous.lerp(hit.position,partial).subtract(camera); pose.translate(position.x,position.y,position.z);
            pose.mulPose(Axis.YP.rotationDegrees(180-hit.yaw)); pose.mulPose(Axis.XP.rotationDegrees(-hit.pitch)); float scale=.4f*hit.entity.getBbHeight(); pose.scale(scale,scale,scale);
            float alpha=Math.max(0,Math.min(1,(1-fade)*3));
            for (var group:mesh.groups().values()) for (var vertex:group) out.vertex(pose.last().pose(),vertex.x(),vertex.y(),vertex.z()).color(1f,1f,1f,alpha).uv(vertex.u(),vertex.v()).uv2(220).endVertex();
        }
        used.forEach(buffers::endBatch);
    }
    private static  class Hit {
        final Entity entity; final float yaw,pitch; final int lifetime; int age;
        Vec3 position,previous;
        Hit(Entity entity,float yaw,float pitch,int lifetime) { this.entity=entity; this.yaw=yaw; this.pitch=pitch; this.lifetime=lifetime; position=previous=entity.getBoundingBox().getCenter(); }
    }
}
