package gadomancymodern.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.particle.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Port of Gadomancy's ten-render-frame dispatcher and TC4 FXBubble, including alpha and atlas animation. */
@Mod.EventBusSubscriber(modid="gadomancy",value=Dist.CLIENT)
public class DisguiseBubbles {
    protected static net.minecraft.client.multiplayer.ClientLevel current;
    protected static final java.util.List<Emission> EMITTERS=new java.util.ArrayList<>();
    protected static final java.util.List<Bubble> BUBBLES=new java.util.ArrayList<>();
    protected static class Emission{Vec3 point;int frames;Emission(Vec3 point){this.point=point;}}
    protected static boolean prepare(){var level=Minecraft.getInstance().level;if(level!=current){EMITTERS.clear();BUBBLES.clear();current=level;}return level!=null;}
    public static void emit(Vec3 point){if(prepare()&&EMITTERS.size()<2048)EMITTERS.add(new Emission(point));}
    @SubscribeEvent public static void frames(net.minecraftforge.event.TickEvent.RenderTickEvent event){if(event.phase!=net.minecraftforge.event.TickEvent.Phase.START||!prepare()||Minecraft.getInstance().isPaused())return;var iterator=EMITTERS.iterator();while(iterator.hasNext()){var emitter=iterator.next();if(++emitter.frames>10){iterator.remove();continue;}var random=current.random;if(random.nextInt(3)==0&&BUBBLES.size()<4000){double x=emitter.point.x+(random.nextBoolean()?1:-1)*random.nextFloat()*.2,y=emitter.point.y+(random.nextBoolean()?1:-1)*random.nextFloat()*.2,z=emitter.point.z+(random.nextBoolean()?1:-1)*random.nextFloat()*.2;BUBBLES.add(new Bubble(current,x,y,z));}}}
    @SubscribeEvent public static void ticks(net.minecraftforge.event.TickEvent.ClientTickEvent event){if(event.phase==net.minecraftforge.event.TickEvent.Phase.START&&prepare()&&!Minecraft.getInstance().isPaused()){BUBBLES.forEach(Bubble::tick);BUBBLES.removeIf(b->!b.isAlive());}}
    @SubscribeEvent public static void render(net.minecraftforge.client.event.RenderLevelStageEvent event){if(event.getStage()!=net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES||!prepare())return;var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var type=tc4revival.client.TCRenderTypes.orb(net.minecraft.resources.ResourceLocation.parse("thaumcraft:textures/misc/particles.png"));var out=buffers.getBuffer(type);for(var bubble:BUBBLES)bubble.render(out,event.getCamera(),event.getPartialTick(),event.getPoseStack());buffers.endBatch(type);}
    protected static void quad(VertexConsumer out,org.joml.Matrix4f pose,float radius,float u,float v,float alpha){out.vertex(pose,-radius,-radius,0).color(1f,1f,1f,alpha).uv(u+.0624375f,v+.0624375f).uv2(240).endVertex();out.vertex(pose,-radius,radius,0).color(1f,1f,1f,alpha).uv(u+.0624375f,v).uv2(240).endVertex();out.vertex(pose,radius,radius,0).color(1f,1f,1f,alpha).uv(u,v).uv2(240).endVertex();out.vertex(pose,radius,-radius,0).color(1f,1f,1f,alpha).uv(u,v+.0624375f).uv2(240).endVertex();}
    public static class Bubble extends Particle{
        protected int frame=16;protected float radius;
        public Bubble(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z){super(level,x,y,z);alpha=.25f;float scale=(random.nextFloat()*.5f+.5f)*2*(random.nextFloat()*.3f+.2f);radius=.1f*scale;xd=(random.nextDouble()*2-1)*.02;yd=random.nextDouble()*.02;zd=(random.nextDouble()*2-1)*.02;lifetime=(int)(2+8/(random.nextDouble()*.8+.2));hasPhysics=false;setSize(.02f,.02f);}
        @Override public void tick(){xo=x;yo=y;zo=z;setPos(x+xd,y+yd,z+zd);xd*=.85;yd*=.85;zd*=.85;if(lifetime--<=0)remove();else if(lifetime<=2)frame++;}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
        @Override public void render(VertexConsumer out,Camera camera,float partial){render(out,camera,partial,new PoseStack());}
        public void render(VertexConsumer out,Camera camera,float partial,PoseStack pose){pose.pushPose();var eye=camera.getPosition();pose.translate(net.minecraft.util.Mth.lerp(partial,xo,x)-eye.x,net.minecraft.util.Mth.lerp(partial,yo,y)-eye.y,net.minecraft.util.Mth.lerp(partial,zo,z)-eye.z);pose.mulPose(camera.rotation());float u=frame%16/16f,v=frame/16/16f;quad(out,pose.last().pose(),radius,u,v,alpha);pose.popPose();}
    }
}
