package gadomancymodern.client;

import com.mojang.blaze3d.vertex.*;
import gadomancymodern.aura.AuraCoreEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.client.TCRenderTypes;
import java.util.*;

/** Legacy FXFlow / Orbital: six differently tilted paths, with stationary glow trails. */
@Mod.EventBusSubscriber(modid="gadomancy",value=Dist.CLIENT)
public class AuraCoreEffects {
    protected static final Map<AuraCoreEntity,Orbits> CORES=new IdentityHashMap<>();
    protected static final List<Glow> GLOWS=new ArrayList<>();
    protected static Object world;
    protected static final ResourceLocation FLOW=new ResourceLocation("gadomancy","textures/fx/flow_large.png");
    protected static final Map<String,Integer> SUB=Map.of("aer",16711625,"ignis",16755260,"terra",8315743,"aqua",7917516,"ordo",0xffffff,"perditio",0);
    public static void tick(AuraCoreEntity entity){
        resetWorld();var orbits=CORES.computeIfAbsent(entity,Orbits::new);orbits.counter++;
        if(entity.elapsed()>=50)return;
        var types=entity.orbitals();var random=entity.level().random;
        for(int i=0;i<6;i++){
            String type=types.get(i);int color=tc4revival.aspect.TCAspects.catalog().require(type).color();
            var old=orbits.flows[i];var target=orbits.point(i,entity);var next=old.add(target.subtract(old).scale(1.0/Math.max(1,50-entity.elapsed())));orbits.flows[i]=next;
            add(next,color,.2f,27);add(old.lerp(next,.5),color,.16f,24);
            int count=random.nextInt(3)+1;for(int n=0;n<count;n++)add(next.add(offset(random,.2)),random.nextBoolean()?SUB.get(type):color,.1f,18);
        }
    }
    protected static Vec3 offset(net.minecraft.util.RandomSource random,double radius){return new Vec3((random.nextDouble()*2-1)*radius,(random.nextDouble()*2-1)*radius,(random.nextDouble()*2-1)*radius);}
    protected static void resetWorld(){var now=Minecraft.getInstance().level;if(world!=now){world=now;CORES.clear();GLOWS.clear();}}
    protected static void add(Vec3 pos,int color,float scale,int life){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(pos)>gadomancymodern.ClientConfig.distanceSquared())return;
        GLOWS.add(new Glow(pos,color,scale*(1+mc.level.random.nextFloat())*.5f,life));
    }
    public static void addAlpha(Vec3 pos,int color,float scale,int life,int alpha){resetWorld();add(pos,color,scale,life);if(!GLOWS.isEmpty()&&GLOWS.get(GLOWS.size()-1).pos.equals(pos))GLOWS.get(GLOWS.size()-1).alpha=alpha;}
    @SubscribeEvent public static void endTick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;resetWorld();if(Minecraft.getInstance().isPaused())return;
        CORES.keySet().removeIf(e->!e.isAlive());GLOWS.removeIf(glow->++glow.age>=glow.life);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;resetWorld();var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(!mc.isPaused())for(var entry:CORES.entrySet()){
            var core=entry.getKey();if(core.elapsed()<49||mc.player==null||core.distanceToSqr(mc.player)>gadomancymodern.ClientConfig.distanceSquared())continue;var types=core.orbitals();
            for(int i=0;i<6;i++){
                var pos=entry.getValue().point(i,core);String type=types.get(i);int color=tc4revival.aspect.TCAspects.catalog().require(type).color();add(pos,color,.1f,24);
                if(mc.level.random.nextInt(3)==0)add(pos.add(offset(mc.level.random,.2)),mc.level.random.nextBoolean()?SUB.get(type):color,mc.level.random.nextBoolean()?.05f:.075f,18);
            }
        }
        if(GLOWS.isEmpty())return;var buffers=mc.renderBuffers().bufferSource();var type=TCRenderTypes.sharpTranslucent(FLOW);var out=buffers.getBuffer(type);
        var camera=event.getCamera();for(var glow:GLOWS){
            float envelope=glow.age/(float)(glow.life/2);if(envelope>=1)envelope=2-envelope;float half=glow.scale*Math.max(0,envelope);
            // Level-stage vertices need the current view rotation before camera-relative
            // translation. An identity stack puts these world particles behind the camera.
            var pose=event.getPoseStack();pose.pushPose();var pos=glow.pos.subtract(camera.getPosition());pose.translate(pos.x,pos.y,pos.z);pose.mulPose(camera.rotation());var matrix=pose.last().pose();
            int r=glow.color>>16&255,g=glow.color>>8&255,b=glow.color&255;
            vertex(out,matrix,-half,-half,0,1,r,g,b,glow.alpha);vertex(out,matrix,half,-half,1,1,r,g,b,glow.alpha);vertex(out,matrix,half,half,1,0,r,g,b,glow.alpha);vertex(out,matrix,-half,half,0,0,r,g,b,glow.alpha);
            pose.popPose();
        }buffers.endBatch(type);
    }
    protected static void vertex(VertexConsumer out,org.joml.Matrix4f matrix,float x,float y,float u,float v,int r,int g,int b,int alpha){out.vertex(matrix,x,y,0).color(r,g,b,alpha).uv(u,v).uv2(240).endVertex();}
    public static void finish(AuraCoreEntity core,boolean success){
        var level=Minecraft.getInstance().level;if(level==null)return;var center=BlockPos.containing(core.position());var positions=new ArrayList<BlockPos>();positions.add(center);for(var dir:Direction.values())positions.add(center.relative(dir));
        for(var pos:positions){
            if(success){tc4revival.client.ClientFocusEffects.blockSparkle(level,pos,-9999,10);continue;}
            int count=level.random.nextInt(6)+2;for(int i=0;i<count;i++){
                var at=Vec3.atCenterOf(pos).add(offset(level.random,1));var motion=offset(level.random,1).multiply(.01,.1,.01);
                var smoke=Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.SMOKE,at.x,at.y,at.z,motion.x,motion.y,motion.z);if(smoke!=null)smoke.scale(2);
            }
        }
    }
    public static class Glow {
        protected final Vec3 pos;protected final int color,life;protected final float scale;protected int age,alpha=255;
        public Glow(Vec3 pos,int color,float scale,int life){this.pos=pos;this.color=color;this.scale=scale;this.life=life;}
    }
    public static class Orbits {
        protected final Vec3[] axes=new Vec3[6],flows=new Vec3[6];protected final int[] periods=new int[6],offsets=new int[6];protected int counter;
        public Orbits(AuraCoreEntity core){var random=core.level().random;for(int i=0;i<6;i++){
            axes[i]=new Vec3(random.nextDouble()*2-1,random.nextDouble(),random.nextDouble()*2-1).normalize();periods[i]=120+random.nextInt(40);offsets[i]=random.nextInt(80);flows[i]=Vec3.atCenterOf(core.activation());
        }}
        public Vec3 point(int i,AuraCoreEntity core){
            var axis=axes[i];var radial=(axis.z==0?new Vec3(axis.y,-axis.x,0):new Vec3(0,axis.z,-axis.y)).normalize();double angle=Math.PI*2*((counter+offsets[i])%periods[i])/(double)periods[i];
            // Rodrigues rotation retains each random plane rather than flattening all six rings.
            radial=radial.scale(Math.cos(angle)).add(axis.cross(radial).scale(Math.sin(angle)));
            double radius=core.elapsed()<=50?1:Math.max(0,(500-(core.elapsed()-50))/500.0);return core.position().add(radial.scale(radius));
        }
    }
}
