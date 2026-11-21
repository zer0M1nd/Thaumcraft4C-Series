package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.EssentiaTubeBlockEntity;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class EssentiaVentParticles {
    private static final ResourceLocation ATLAS=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final HashSet<EssentiaTubeBlockEntity> TUBES=new HashSet<>();
    private static final ArrayList<Vent> PARTICLES=new ArrayList<>();
    private static ClientLevel current;
    private EssentiaVentParticles() {}
    public static void maintain(EssentiaTubeBlockEntity tube) { if (tube.clientVent()>0) TUBES.add(tube); }
    public static void emit(net.minecraft.world.level.Level level,double x,double y,double z,double dx,double dy,double dz,int color) {
        emit(level,x,y,z,dx,dy,dz,color,1);
    }
    public static void emit(net.minecraft.world.level.Level level,double x,double y,double z,double dx,double dy,double dz,int color,float scale) {
        var mc=Minecraft.getInstance(); if (level!=mc.level || !(level instanceof ClientLevel client) || mc.getCameraEntity()==null) return;
        if (current!=client) { TUBES.clear(); PARTICLES.clear(); current=client; }
        int range=Minecraft.useFancyGraphics() ? 50 : 25;
        if (PARTICLES.size()<4096 && mc.getCameraEntity().distanceToSqr(x,y,z)<=range*range) PARTICLES.add(new Vent(client,x,y,z,dx,dy,dz,color,scale));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance();
        if (current!=mc.level) { TUBES.clear(); PARTICLES.clear(); current=mc.level; }
        if (current==null || mc.isPaused()) return;
        PARTICLES.forEach(Vent::tick); PARTICLES.removeIf(particle->!particle.isAlive());
        TUBES.removeIf(tube->tube.isRemoved() || tube.getLevel()!=current || tube.clientVent()<=0);
        var camera=mc.getCameraEntity(); if (camera==null) return;
        int distance=Minecraft.useFancyGraphics() ? 50 : 25;
        for (var tube:TUBES) {
            if (PARTICLES.size()>=4096 || camera.distanceToSqr(tube.getBlockPos().getCenter())>distance*distance) continue;
            var heading=new java.util.Random(tube.hashCode()*4); float pitch=heading.nextFloat()*360*Mth.DEG_TO_RAD,yaw=heading.nextFloat()*360*Mth.DEG_TO_RAD;
            double x=-Mth.sin(yaw)*Mth.cos(pitch),y=-Mth.sin(pitch);
            // TC4 supplied X for both horizontal coordinates of this effect.
            PARTICLES.add(new Vent(current,tube,x/5,y/5,x/5));
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || current==null) return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource(); var type=TCRenderTypes.softTranslucent(ATLAS); var out=TCRenderTypes.buffer(buffers,type);
        PARTICLES.forEach(particle->particle.renderWorld(out,event.getCamera(),event.getPartialTick(),event.getPoseStack())); TCRenderTypes.endBatch(buffers,type);
    }
    private static  class Vent extends Particle {
        private float size; private float maximum=1;
        Vent(ClientLevel level,EssentiaTubeBlockEntity tube,double dx,double dy,double dz) {
            this(level,tube.getBlockPos().getX()+.5,tube.getBlockPos().getY()+.5,tube.getBlockPos().getZ()+.5,dx,dy,dz,tube.ventColor());
        }
        Vent(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,int color) {
            super(level,x,y,z);
            hasPhysics=false; size=random.nextFloat()*.1f+.05f;
            rCol=(color>>16&255)/255f; gCol=(color>>8&255)/255f; bCol=(color&255)/255f;
            double length=Math.sqrt(dx*dx+dy*dy+dz*dz); if (length<1.0e-8) { dx=0; dy=1; dz=0; length=1; }
            xd=(dx/length+scatter())*.125; yd=(dy/length+scatter())*.125; zd=(dz/length+scatter())*.125;
        }
        Vent(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,int color,float scale) {
            this(level,x,y,z,dx,dy,dz,color); size*=scale; maximum=scale;
        }
        private double scatter() { return random.nextGaussian()*(random.nextBoolean()?1:-1)*.0075f*5; }
        @Override public void tick() {
            xo=x; yo=y; zo=z; age++; if (size>maximum) { remove(); return; }
            yd+=.0025; move(xd,yd,zd); xd*=.8500000190734863; yd*=.8500000190734863; zd*=.8500000190734863;
            if (size<maximum) size*=1.15f;
        }
        @Override public void render(VertexConsumer out,Camera camera,float partial) { renderWorld(out,camera,partial,new PoseStack()); }
        void renderWorld(VertexConsumer out,Camera camera,float partial,PoseStack worldView) {
            var pose=WorldEffectPose.copy(worldView); var view=camera.getPosition();
            pose.translate(Mth.lerp(partial,xo,x)-view.x,Mth.lerp(partial,yo,y)-view.y,Mth.lerp(partial,zo,z)-view.z); pose.mulPose(camera.rotation());
            float u=((int)(1+size/maximum*4)%16)/16f;
            ClientFocusEffects.quad(out,pose.last().pose(),.3f*size,u,0,u+.0624375f,.0624375f,rCol,gCol,bCol,.4f*Math.max(0,1-size/maximum),getLightColor(partial));
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    }
}
