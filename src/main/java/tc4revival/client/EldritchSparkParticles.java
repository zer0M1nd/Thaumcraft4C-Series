package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Stationary eight-frame electrical sparks from the second legacy particle atlas. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class EldritchSparkParticles {
    private static final ResourceLocation ATLAS=ResourceLocation.parse("thaumcraft:textures/misc/particles2.png");
    private static final java.util.ArrayList<Spark> SPARKS=new java.util.ArrayList<>();
    private static ClientLevel current;
    private static final org.joml.Matrix4f VIEW=new org.joml.Matrix4f();
    public static void emit(Level level,double x,double y,double z,float size,float red,float green,float blue,float alpha) {
        var mc=Minecraft.getInstance(); if (level!=mc.level || !(level instanceof ClientLevel client)) return;
        if (current!=client) { SPARKS.clear(); current=client; }
        if (SPARKS.size()>=2000) SPARKS.remove(0);
        SPARKS.add(new Spark(client,x,y,z,size,red,green,blue,alpha));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return; var mc=Minecraft.getInstance();
        if (current!=mc.level) { SPARKS.clear(); current=mc.level; }
        if (current==null || mc.isPaused()) return;
        SPARKS.forEach(Spark::tick); SPARKS.removeIf(spark->!spark.isAlive());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || current==null) return;
        VIEW.set(event.getPoseStack().last().pose());
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource(); var type=TCRenderTypes.softParticles(ATLAS); var out=TCRenderTypes.buffer(buffers,type);
        SPARKS.forEach(spark->spark.render(out,event.getCamera(),event.getPartialTick())); TCRenderTypes.endBatch(buffers,type);
    }
    private static  class Spark extends Particle {
        final float size; final int row; final boolean flip;
        Spark(ClientLevel level,double x,double y,double z,float size,float red,float green,float blue,float opacity) {
            super(level,x,y,z); this.size=size; rCol=red; gCol=green; bCol=blue; alpha=opacity;
            lifetime=5+level.random.nextInt(5); row=level.random.nextInt(3); flip=level.random.nextBoolean(); setSize(.01f,.01f);
        }
        @Override public void tick() { xo=x; yo=y; zo=z; if (age++>=lifetime) remove(); }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
        @Override public void render(VertexConsumer out,Camera camera,float partial) {
            var pose=new PoseStack(); pose.last().pose().set(VIEW); var at=camera.getPosition(); pose.translate(x-at.x,y-at.y,z-at.z); pose.mulPose(camera.rotation());
            float u=(int)(age/(float)lifetime*7)/8f,v=row/8f;
            ClientFocusEffects.quad(out,pose.last().pose(),size,flip ? u+.125f : u,v,flip ? u : u+.125f,v+.125f,rCol,gCol,bCol,alpha,getLightColor(partial));
        }
    }
    private EldritchSparkParticles() {}
}
