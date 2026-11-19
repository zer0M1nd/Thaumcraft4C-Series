package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.CrucibleBlockEntity;
import tc4revival.content.TCSounds;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class CrucibleEffects {
    private static final ResourceLocation TEXTURE=new ResourceLocation("thaumcraft","textures/misc/particles.png");
    private static final List<Bubble> BUBBLES=new ArrayList<>();
    private static ClientLevel current;
    private CrucibleEffects() {}
    private static boolean prepare(Level level) {
        var mc=Minecraft.getInstance(); if (current!=mc.level) { BUBBLES.clear(); current=mc.level; }
        return level!=null && level==current;
    }
    private static int particleCount(int base) {
        return switch(Minecraft.getInstance().options.particles().get()) { case ALL -> base*2; case DECREASED -> base; case MINIMAL -> 0; };
    }
    public static void spawnBubble(net.minecraft.client.multiplayer.ClientLevel level,thaumcraft.api.client.ThaumicParticles.BubbleSpec spec){
        add(level,spec.x(),spec.y(),spec.z(),spec.age(),spec.color(),spec.froth(),spec.speed(),spec.alpha());
    }
    private static void add(Level level,double x,double y,double z,int age,int color,int froth,double speed,float alpha) {
        if (!prepare(level)) return;
        int range=Minecraft.useFancyGraphics() ? 50 : 25;
        if (Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(x,y,z)>range*range) return;
        if (BUBBLES.size()>=4000) BUBBLES.remove(0);
        BUBBLES.add(new Bubble(current,x,y,z,age,color,froth,speed,alpha));
    }
    public static void goo(Level level,BlockPos pos,int metadata) {
        if (level.random.nextInt(50-particleCount(10))<=metadata)
            add(level,pos.getX()+level.random.nextFloat(),pos.getY()+metadata*.125,pos.getZ()+level.random.nextFloat(),0,0xff007f,0,.002,.25f);
    }
    public static void pureWater(Level level,BlockPos pos,int metadata) {
        add(level,pos.getX()+level.random.nextFloat(),pos.getY()+.125*(8-metadata),pos.getZ()+level.random.nextFloat(),0,0xffffff,0,0,.25f);
    }
    public static void elementalBubble(Level level,double x,double y,double z) { add(level,x,y,z,1,0x5454ff,0,0,1); }
    public static void elementalBlockBubble(Level level,BlockPos pos) {
        for (int i=0;i<particleCount(1);i++) {
            elementalBubble(level,pos.getX(),pos.getY()+level.random.nextFloat(),pos.getZ()+level.random.nextFloat());
            elementalBubble(level,pos.getX()+1,pos.getY()+level.random.nextFloat(),pos.getZ()+level.random.nextFloat());
            elementalBubble(level,pos.getX()+level.random.nextFloat(),pos.getY()+level.random.nextFloat(),pos.getZ());
            elementalBubble(level,pos.getX()+level.random.nextFloat(),pos.getY()+level.random.nextFloat(),pos.getZ()+1);
        }
    }
    public static void soap(net.minecraft.world.entity.player.Player player,boolean completed) {
        var level=player.level(); var random=level.random;
        for (int i=0;i<particleCount(completed ? 20 : 5);i++) {
            float width=completed ? 1.5f : 1;
            add(level,(float)player.getX()-.5f+random.nextFloat()*width,player.getBoundingBox().minY+random.nextFloat()*player.getBbHeight(),
                    (float)player.getZ()-.5f+random.nextFloat()*width,1,completed ? 0xffb2e5 : 0xffcce5,0,0,1);
        }
    }
    public static void tick(CrucibleBlockEntity crucible) {
        var level=crucible.getLevel(); if (!prepare(level) || crucible.contents().water()<=0) return;
        var pos=crucible.getBlockPos(); var contents=crucible.contents(); double height=pos.getY()+contents.fluidHeight();
        if (contents.boiling()) {
            add(level,pos.getX()+.2+level.random.nextFloat()*.6,height,pos.getZ()+.2+level.random.nextFloat()*.6,-4,0x7f7fb2,1,-.001,1);
            if (contents.aspects().total()>100) for (int i=0;i<2;i++) {
                add(level,pos.getX(),pos.getY()+1,pos.getZ()+level.random.nextFloat(),-4,0x7f7fb2,2,-.005,1);
                add(level,pos.getX()+1,pos.getY()+1,pos.getZ()+level.random.nextFloat(),-4,0x7f7fb2,2,-.005,1);
                add(level,pos.getX()+level.random.nextFloat(),pos.getY()+1,pos.getZ(),-4,0x7f7fb2,2,-.005,1);
                add(level,pos.getX()+level.random.nextFloat(),pos.getY()+1,pos.getZ()+1,-4,0x7f7fb2,2,-.005,1);
            }
        }
        var aspects=contents.aspects().aspects();
        if (level.random.nextInt(6)==0 && !aspects.isEmpty()) {
            int color=aspects.get(level.random.nextInt(aspects.size())).color();
            double x=pos.getX()+(5+level.random.nextInt(22))/32.0+.015625, z=pos.getZ()+(5+level.random.nextInt(22))/32.0+.015625;
            level.random.nextInt(10); // Old delay field was assigned even though unused afterwards.
            add(level,x,height+.05,z,1,color,0,.002,1);
        }
    }
    public static void event(CrucibleBlockEntity crucible,int id,int strength) {
        var level=crucible.getLevel(); if (!prepare(level)) return;
        var pos=crucible.getBlockPos();
        if (id==1) {
            // The existing block-sparkle implementation uses the original node atlas.
            NitorParticles.emit(level,pos,true); return;
        }
        level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,TCSounds.SPILL.get(),SoundSource.BLOCKS,.2f,1,false);
        var aspects=crucible.contents().aspects().aspects();
        for (int burst=0;burst<10;burst++) {
            level.random.nextInt(22); level.random.nextInt(22);
            for (int i=0;i<particleCount(1);i++) {
                double x=pos.getX()+.2+level.random.nextFloat()*.6,z=pos.getZ()+.2+level.random.nextFloat()*.6;
                int color=aspects.isEmpty() ? 0xffffff : aspects.get(level.random.nextInt(aspects.size())).color();
                add(level,x,pos.getY()+.1+crucible.contents().fluidHeight(),z,3,color,0,.003*strength,1);
            }
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { current=null; BUBBLES.clear(); }
    @SubscribeEvent public static void tickParticles(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.START || !prepare(Minecraft.getInstance().level) || Minecraft.getInstance().isPaused()) return;
        BUBBLES.forEach(Bubble::tick); BUBBLES.removeIf(bubble->!bubble.isAlive());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || !prepare(Minecraft.getInstance().level)) return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource(); var type=TCRenderTypes.orb(TEXTURE); var out=TCRenderTypes.buffer(buffers,type);
        for (var bubble : BUBBLES) bubble.render(out,event.getCamera(),event.getPartialTick(),event.getPoseStack());
        TCRenderTypes.endBatch(buffers,type);
    }
    private static  class Bubble extends Particle {
        private int frame=16;
        private final double acceleration;
        private final float radius;
        Bubble(ClientLevel level,double x,double y,double z,int extraAge,int color,int froth,double acceleration,float alpha) {
            super(level,x,y,z); this.acceleration=acceleration; this.alpha=alpha;
            float scale=(random.nextFloat()*.5f+.5f)*2*(random.nextFloat()*.3f+.2f);
            xd=(random.nextDouble()*2-1)*.02; yd=random.nextDouble()*.02; zd=(random.nextDouble()*2-1)*.02;
            lifetime=(int)(extraAge+2+8/(random.nextDouble()*.8+.2));
            if (froth!=0) { scale*=.75f; lifetime=froth==1 ? 4+random.nextInt(3) : 12+random.nextInt(12); xd/=5; yd/=10; zd/=5; }
            radius=.1f*scale; rCol=(color>>16&255)/255f; gCol=(color>>8&255)/255f; bCol=(color&255)/255f;
            hasPhysics=false; setSize(.02f,.02f);
        }
        @Override public void tick() {
            xo=x; yo=y; zo=z; yd+=acceleration;
            if (acceleration>0) { xd+=(level.random.nextFloat()-level.random.nextFloat())*.01; zd+=(level.random.nextFloat()-level.random.nextFloat())*.01; }
            setPos(x+xd,y+yd,z+zd); xd*=.85; yd*=.85; zd*=.85;
            if (lifetime--<=0) remove(); else if (lifetime<=2) frame++;
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
        @Override public void render(VertexConsumer out,Camera camera,float partial) {
            render(out,camera,partial,new PoseStack());
        }
        private void render(VertexConsumer out,Camera camera,float partial,PoseStack pose) {
            pose.pushPose(); var eye=camera.getPosition();
            pose.translate(Mth.lerp(partial,xo,x)-eye.x,Mth.lerp(partial,yo,y)-eye.y,Mth.lerp(partial,zo,z)-eye.z); pose.mulPose(camera.rotation());
            float u=frame%16/16f,v=frame/16/16f;
            ClientFocusEffects.quad(out,pose.last().pose(),radius,u,v,u+.0624375f,v+.0624375f,rCol,gCol,bCol,alpha,240);
            pose.popPose();
        }
    }
}
