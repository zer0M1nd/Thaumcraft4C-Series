package thaumicfutures.expansion;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** TC6's neutral 64x64 atlas: 264..271 flames and 457 white core, separately sourced. */
@Mod.EventBusSubscriber(modid="thaumicfutures",value=Dist.CLIENT)
public final class DyedNitorParticles {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumicfutures:textures/misc/tc6_particles.png");
    private static final java.util.List<Mote> PARTICLES=new java.util.ArrayList<>();
    private static ClientLevel current;
    private static boolean prepare(Level level){
        var client=Minecraft.getInstance();if(level==null||level!=client.level)return false;
        if(current!=level){PARTICLES.clear();current=client.level;}return true;
    }
    public static void emit(Level level,BlockPos pos,int color){
        if(!prepare(level))return;
        var client=Minecraft.getInstance();var center=Vec3.atCenterOf(pos);
        if(client.gameRenderer.getMainCamera().getPosition().distanceToSqr(center)>2500)return;
        int interval=switch(client.options.particles().get()){case ALL->1;case DECREASED->2;case MINIMAL->4;};
        if((level.getGameTime()+pos.asLong())%interval==0)add(new Mote(current,center,color,false));
        if((level.getGameTime()+pos.asLong())%10==0)add(new Mote(current,center,0xffffff,true));
    }
    private static void add(Mote mote){if(PARTICLES.size()>=8192)PARTICLES.remove(0);PARTICLES.add(mote);}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){PARTICLES.clear();current=null;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.START||!prepare(Minecraft.getInstance().level)||Minecraft.getInstance().isPaused())return;
        PARTICLES.forEach(Mote::tick);PARTICLES.removeIf(m->!m.isAlive());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||!prepare(Minecraft.getInstance().level))return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        var flames=tc4revival.client.TCRenderTypes.softParticles(TEXTURE);var out=tc4revival.client.TCRenderTypes.buffer(buffers,flames);
        for(var mote:PARTICLES)if(!mote.core)mote.draw(out,event.getCamera(),event.getPartialTick(),event.getPoseStack());
        tc4revival.client.TCRenderTypes.endBatch(buffers,flames);
        var cores=tc4revival.client.TCRenderTypes.softTranslucent(TEXTURE);out=tc4revival.client.TCRenderTypes.buffer(buffers,cores);
        for(var mote:PARTICLES)if(mote.core)mote.draw(out,event.getCamera(),event.getPartialTick(),event.getPoseStack());
        tc4revival.client.TCRenderTypes.endBatch(buffers,cores);
    }
    private static final class Mote extends Particle {
        final boolean core;final float scale;
        Mote(ClientLevel level,Vec3 center,int color,boolean core){
            super(level,center.x,center.y,center.z);this.core=core;
            lifetime=core?10:10+random.nextInt(5);scale=core?1:3+random.nextFloat();
            rCol=(color>>16&255)/255f;gCol=(color>>8&255)/255f;bCol=(color&255)/255f;
            if(!core){x+=random.nextGaussian()*.025;z+=random.nextGaussian()*.025;y+=random.nextGaussian()*.025-.05;}
            else y-=.01;
            xo=x;yo=y;zo=z;xd=core?0:random.nextGaussian()*.0025;yd=core?0:random.nextFloat()*.06;zd=core?0:random.nextGaussian()*.0025;
            hasPhysics=false;friction=.98f;
        }
        @Override public void tick(){super.tick();double jitter=core?.0002:.0025;xd+=random.nextGaussian()*jitter;zd+=random.nextGaussian()*jitter;if(core)yd+=random.nextGaussian()*jitter;}
        @Override public void render(VertexConsumer out,Camera camera,float partial){draw(out,camera,partial,new PoseStack());}
        void draw(VertexConsumer out,Camera camera,float partial,PoseStack pose){
            int sprite=core?457:264+age%8;float u=sprite%64/64f,v=sprite/64/64f;
            float size=.1f*(core?scale:Mth.lerp(Math.min(1,(age+partial)/lifetime),scale,.05f));
            pose.pushPose();var eye=camera.getPosition();pose.translate(Mth.lerp(partial,xo,x)-eye.x,Mth.lerp(partial,yo,y)-eye.y,Mth.lerp(partial,zo,z)-eye.z);pose.mulPose(camera.rotation());
            var matrix=pose.last().pose();float a=core?1:.66f;
            out.vertex(matrix,-size,-size,0).color(rCol,gCol,bCol,a).uv(u,v+1/64f).uv2(240).endVertex();
            out.vertex(matrix,size,-size,0).color(rCol,gCol,bCol,a).uv(u+1/64f,v+1/64f).uv2(240).endVertex();
            out.vertex(matrix,size,size,0).color(rCol,gCol,bCol,a).uv(u+1/64f,v).uv2(240).endVertex();
            out.vertex(matrix,-size,size,0).color(rCol,gCol,bCol,a).uv(u,v).uv2(240).endVertex();pose.popPose();
        }
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
    }
    private DyedNitorParticles(){}
}
