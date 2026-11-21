package emtmodern.client;

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
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="emt",value=Dist.CLIENT)
public class ElectricCloudEffects {
    protected static final List<Mote> PARTICLES=new ArrayList<>();protected static ClientLevel current;
    protected static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    protected static void prepare(){var world=Minecraft.getInstance().level;if(current!=world){current=world;PARTICLES.clear();}}
    protected static void add(Mote mote){if(PARTICLES.size()>=2000)PARTICLES.remove(0);PARTICLES.add(mote);}
    public static void tick(Level world,BlockPos pos){prepare();if(world!=current||current==null)return;var random=world.random;var center=pos.getCenter();
        if(random.nextInt(4)==0)add(new Mote(current,center,center.add(random.nextFloat()*.2-.1,0,random.nextFloat()*.2-.1),.25f,0));
        if(random.nextInt(12)==0){bolt(world,pos);if(random.nextInt(2)==0)bolt(world,pos);if(random.nextInt(4)==0)bolt(world,pos);}
        if(random.nextInt(3)==0){var at=center.add(random.nextInt(40)/160f-.125,0,random.nextInt(40)/160f-.125);var mote=new Mote(current,at,at,.4f+random.nextInt(40)/100f,1);mote.color(random.nextInt(40)/100f,0,1);add(mote);}
    }
    protected static void bolt(Level world,BlockPos pos){var r=world.random;tc4revival.client.ClientFocusEffects.infusionBolt(pos.getCenter(),new Vec3(pos.getX()+r.nextInt(40)/20f-.5,pos.getY()+r.nextInt(40)/20f-.5,pos.getZ()+r.nextInt(40)/20f-.5));}
    public static void sparkle(Level world,BlockPos pos){prepare();if(world!=current||current==null)return;var r=world.random;var from=pos.getCenter();add(new Mote(current,from,from.add((r.nextFloat()-r.nextFloat())/3,(r.nextFloat()-r.nextFloat())/3,(r.nextFloat()-r.nextFloat())/3),1,2));}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;prepare();if(current==null||Minecraft.getInstance().isPaused())return;PARTICLES.forEach(Mote::tick);PARTICLES.removeIf(p->!p.isAlive());}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;prepare();if(PARTICLES.isEmpty())return;var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var type=tc4revival.client.TCRenderTypes.runes(TEXTURE);var out=buffers.getBuffer(type);for(var particle:PARTICLES)particle.render(out,event.getCamera(),event.getPartialTick(),event.getPoseStack());buffers.endBatch(type);}
    public static class Mote extends Particle {
        protected final float size;protected final int kind;
        public Mote(ClientLevel level,Vec3 from,Vec3 to,float scale,int kind){super(level,from.x,from.y,from.z);this.kind=kind;size=(random.nextFloat()*.5f+.5f)*2*scale;lifetime=kind==2?9:(int)(36/(random.nextDouble()*.3+.7));gravity=kind==2?.05f:-.02f;friction=kind==2?.908f:.98f;hasPhysics=kind!=2;setSize(kind==2?.01f:.1f,kind==2?.01f:.1f);var motion=to.subtract(from).scale(1.0/lifetime);xd=motion.x;yd=motion.y;zd=motion.z;if(kind==2){rCol=.8f+random.nextFloat()*.2f;gCol=.8f+random.nextFloat()*.2f;bCol=.8f+random.nextFloat()*.2f;}else{rCol=.5f+random.nextFloat()*.3f;gCol=.5f+random.nextFloat()*.3f;bCol=.2f;}double max=Minecraft.useFancyGraphics()?50:25;if(kind!=2&&Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(from)>max*max)lifetime=0;}
        public void color(float r,float g,float b){rCol=r;gCol=g;bCol=b;}
        protected void vertex(VertexConsumer out,org.joml.Matrix4f matrix,float x,float y,float u,float v){out.vertex(matrix,x,y,0).color(rCol,gCol,bCol,kind==2?1:.5f).uv(u,v).uv2(240).endVertex();}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
        @Override public void render(VertexConsumer out,Camera camera,float partial){render(out,camera,partial,new PoseStack());}
        // RenderLevelStageEvent supplies the world-to-view rotation; a fresh stack loses it.
        public void render(VertexConsumer out,Camera camera,float partial,PoseStack pose){pose.pushPose();var view=camera.getPosition();pose.translate(Mth.lerp(partial,xo,x)-view.x,Mth.lerp(partial,yo,y)-view.y,Mth.lerp(partial,zo,z)-view.z);pose.mulPose(camera.rotation());float envelope=kind==1?age/(float)Math.max(1,lifetime/2):(lifetime-age+(kind==2?1:0))/(float)Math.max(1,lifetime);if(kind==1&&envelope>1)envelope=2-envelope;float half=size*(kind==2?.1f:.5f)*Math.max(0,envelope),u=kind==2?age/3%4/16f:0,v=kind==2?.25f:.875f,du=kind==2?.0624375f:.125f;var matrix=pose.last().pose();vertex(out,matrix,-half,-half,u+du,v+du);vertex(out,matrix,half,-half,u,v+du);vertex(out,matrix,half,half,u,v);vertex(out,matrix,-half,half,u+du,v);pose.popPose();}
    }
}
