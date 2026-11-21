package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class AlchemyBubbleParticles {
    private static final java.util.ArrayList<Bubble> PARTICLES=new java.util.ArrayList<>();
    private static ClientLevel current;
    private AlchemyBubbleParticles() {}
    public static void liquidDeath(net.minecraft.world.level.Level level,BlockPos pos,int metadata,RandomSource random) {
        if (!(level instanceof ClientLevel client)) return;
        if (current!=client) { PARTICLES.clear(); current=client; }
        float size=.075f+random.nextFloat()*.075f;
        var bubble=new Bubble(client,pos.getX()+random.nextFloat(),pos.getY()+.1f+.225f*metadata,pos.getZ()+random.nextFloat(),size,random);
        bubble.setColor(.3f-random.nextFloat()*.1f,0,.4f+random.nextFloat()*.1f);
        if (PARTICLES.size()<4096) PARTICLES.add(bubble);
        if (random.nextInt(50)==0) level.playLocalSound(pos.getX()+random.nextFloat(),pos.getY()+1,pos.getZ()+random.nextFloat(),
                net.minecraft.sounds.SoundEvents.LAVA_POP,net.minecraft.sounds.SoundSource.BLOCKS,.1f+random.nextFloat()*.1f,.9f+random.nextFloat()*.15f,false);
    }
    public static void colored(net.minecraft.world.level.Level level,double x,double y,double z,float size,int color){
        if(!(level instanceof ClientLevel client))return;if(current!=client){PARTICLES.clear();current=client;}
        var bubble=new Bubble(client,x,y,z,size,client.random);bubble.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f);if(PARTICLES.size()<4096)PARTICLES.add(bubble);
    }
    public static void furnace(net.minecraft.world.level.Level level,BlockPos pos,RandomSource random) {
        if (!(level instanceof ClientLevel client)) return;
        if (current!=client) { PARTICLES.clear(); current=client; }
        add(client,pos.getX()+random.nextFloat(),pos.getY()+1,pos.getZ()+random.nextFloat(),random);
        if (random.nextInt(50)==0) {
            double x=pos.getX()+random.nextFloat(),z=pos.getZ()+random.nextFloat();
            level.playLocalSound(x,pos.getY()+1,z,net.minecraft.sounds.SoundEvents.LAVA_POP,net.minecraft.sounds.SoundSource.BLOCKS,
                    .1f+random.nextFloat()*.1f,.9f+random.nextFloat()*.15f,false);
        }
        int x=random.nextInt(2),z=random.nextInt(2);
        add(client,pos.getX()-.6+random.nextFloat()*.2+x*2,pos.getY()+2,pos.getZ()-.6+random.nextFloat()*.2+z*2,random);
    }
    private static void add(ClientLevel level,double x,double y,double z,RandomSource random) {
        if (PARTICLES.size()<4096) PARTICLES.add(new Bubble(level,x,y,z,.06f+random.nextFloat()*.06f,random));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance();
        if (mc.level!=current) { PARTICLES.clear(); current=mc.level; }
        if (current==null || mc.isPaused()) return;
        PARTICLES.forEach(Bubble::tick); PARTICLES.removeIf(particle->!particle.isAlive());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || current==null || PARTICLES.isEmpty()) return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        var type=TCRenderTypes.softTranslucent(new ResourceLocation("thaumcraft","textures/misc/particles.png")); var out=TCRenderTypes.buffer(buffers,type);
        PARTICLES.forEach(particle->particle.renderWorld(out,event.getCamera(),event.getPartialTick(),event.getPoseStack())); TCRenderTypes.endBatch(buffers,type);
    }
    private static  class Bubble extends Particle {
        private final float size;
        private int frame=144;
        Bubble(ClientLevel level,double x,double y,double z,float size,RandomSource random) {
            super(level,x,y,z); this.size=size; lifetime=15+random.nextInt(5); setSize(.01f,.01f);
            xd=yd=zd=0; alpha=.8f; rCol=.6f-random.nextFloat()*.2f; gCol=0; bCol=.6f+random.nextFloat()*.2f;
        }
        @Override public void tick() {
            xo=x; yo=y; zo=z;
            if (age++>=lifetime) remove();
            if (age-1<6) { frame=144+age/2; if (age==5) y+=.1; }
            else if (age<lifetime-4) { yd+=.005; frame=147+age%4/2; }
            else { yd/=2; frame=150-(lifetime-age)/2; }
            y+=yd;
        }
        @Override public void render(VertexConsumer out,Camera camera,float partial) { renderWorld(out,camera,partial,new PoseStack()); }
        void renderWorld(VertexConsumer out,Camera camera,float partial,PoseStack worldView) {
            var pose=WorldEffectPose.copy(worldView); var view=camera.getPosition();
            pose.translate(Mth.lerp(partial,xo,x)-view.x,Mth.lerp(partial,yo,y)-view.y,Mth.lerp(partial,zo,z)-view.z); pose.mulPose(camera.rotation());
            float u=(frame%16)/16f,v=(frame/16)/16f;
            ClientFocusEffects.quad(out,pose.last().pose(),size,u,v,u+.0625f,v+.0625f,rCol,gCol,bCol,alpha,getLightColor(partial));
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    }
}
