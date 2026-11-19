package gadomancymodern.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Preserves the fixed phase and velocity overrides used by the compressor and aura pylon. */
@Mod.EventBusSubscriber(modid="gadomancy",value=Dist.CLIENT)
public class LegacyEssentiaTrails {
    protected static final java.util.List<Trail> PARTICLES=new java.util.ArrayList<>();protected static Object world;
    protected static void reset(){var now=Minecraft.getInstance().level;if(world!=now){world=now;PARTICLES.clear();}}
    public static void add(Vec3 from,Vec3 to,int color,float scale,int phase){reset();var mc=Minecraft.getInstance();if(mc.level==null||mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(from)>gadomancymodern.ClientConfig.limitSquared(Minecraft.useFancyGraphics()?4096:1024)||PARTICLES.size()>=8192)return;PARTICLES.add(new Trail(mc.level,from,to,color,scale,phase));}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;reset();if(Minecraft.getInstance().level==null||Minecraft.getInstance().isPaused())return;PARTICLES.forEach(Trail::tick);PARTICLES.removeIf(p->!p.isAlive());}
    // Draw against opaque geometry before translucent jar glass writes its depth.
    @SubscribeEvent public static void render(RenderLevelStageEvent event){if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;reset();if(PARTICLES.isEmpty())return;var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var type=tc4revival.client.TCRenderTypes.sharpTranslucent(ProtectorEffects.TEXTURE);var out=buffers.getBuffer(type);for(var particle:PARTICLES)particle.render(out,event.getCamera(),event.getPartialTick(),event.getPoseStack());buffers.endBatch(type);}
    public static class Trail extends Particle {
        protected final Vec3 target;protected final int phase;protected float scale;
        public Trail(ClientLevel level,Vec3 from,Vec3 to,int color,float size,int phase){super(level,from.x,from.y,from.z);target=to;this.phase=phase;scale=(Mth.sin(phase/2f)*.1f+1)*size;int base=Math.max(1,(int)(from.distanceTo(to)*30));lifetime=base/2+random.nextInt(base);xd=Mth.sin(phase/10f)*.01+random.nextGaussian()*.01;zd=Mth.sin(phase/10f)*.01+random.nextGaussian()*.01;yd=(.01+Mth.sin(phase/3f)*.001)*(to.y>=from.y?1:-1);if(phase==5){xd=Mth.sin(phase/10f)*.001+random.nextGaussian()*.002;zd=Mth.sin(phase/10f)*.001+random.nextGaussian()*.002;yd=.1+Mth.sin(phase/3f)*.01;}rCol=(color>>16&255)/255f*(.8f+random.nextFloat()*.2f);gCol=(color>>8&255)/255f*(.8f+random.nextFloat()*.2f);bCol=(color&255)/255f*(.8f+random.nextFloat()*.2f);gravity=.2f;hasPhysics=false;}
        @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime){remove();return;}yd+=.01*gravity;move(xd,yd,zd);xd=Mth.clamp(xd*.985,-.05,.05);yd=Mth.clamp(yd*.985,-.05,.05);zd=Mth.clamp(zd*.985,-.05,.05);var delta=target.subtract(x,y,z);double distance=delta.length();if(distance<2)scale*=.98f;if(scale<.2f||distance<1e-8){remove();return;}delta=delta.scale(.01/Math.min(1,distance)/distance);xd+=delta.x;yd+=delta.y;zd+=delta.z;}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
        @Override public void render(VertexConsumer out,Camera camera,float partial){render(out,camera,partial,new PoseStack());}
        /** Level-stage calls supply the view matrix; particle-engine calls retain their own transform. */
        public void render(VertexConsumer out,Camera camera,float partial,PoseStack pose){pose.pushPose();var view=camera.getPosition();pose.translate(Mth.lerp(partial,xo,x)-view.x,Mth.lerp(partial,yo,y)-view.y,Mth.lerp(partial,zo,z)-view.z);pose.mulPose(camera.rotation());float half=.1f*scale*(Mth.sin((age-phase)/5f)*.25f+1);var matrix=pose.last().pose();vertex(out,matrix,-half,-half,.5625f,.125f);vertex(out,matrix,-half,half,.625f,.125f);vertex(out,matrix,half,half,.625f,.0625f);vertex(out,matrix,half,-half,.5625f,.0625f);pose.popPose();}
        protected void vertex(VertexConsumer out,org.joml.Matrix4f matrix,float x,float y,float u,float v){out.vertex(matrix,x,y,0).color(rCol,gCol,bCol,.5f).uv(u,v).uv2(240).endVertex();}
    }
}
