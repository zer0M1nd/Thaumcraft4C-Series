package gadomancymodern.client;

import com.mojang.blaze3d.vertex.*;
import gadomancymodern.automation.BlockProtectorEntity;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Original white FXWisp3 particles, including their UV cell, drift, life and shrinking envelope. */
@Mod.EventBusSubscriber(modid="gadomancy",value=Dist.CLIENT)
public class ProtectorEffects {
    protected static final List<Wisp> PARTICLES=new ArrayList<>();
    protected static ClientLevel current;
    protected static final ResourceLocation TEXTURE=new ResourceLocation("thaumcraft","textures/misc/particles.png");
    protected static void prepare(){var world=Minecraft.getInstance().level;if(current!=world){current=world;PARTICLES.clear();}}
    public static void tick(BlockProtectorEntity tile){prepare();if(current==null||tile.range()==0)return;var random=current.random;var p=tile.getBlockPos();var start=new Vec3(p.getX()+.5,p.getY()+.68,p.getZ()+.5);float sizeMod=1-tile.range()/15f;int quality=switch(Minecraft.getInstance().options.particles().get()){case ALL->2;case DECREASED->1;case MINIMAL->0;};
        if(random.nextInt(9-quality*2)==0)add(start,new Vec3(p.getX()+.3+random.nextFloat()*.4,p.getY()+.68,p.getZ()+.3+random.nextFloat()*.4),.3f-.15f*sizeMod,-.025f);
        if(random.nextInt(15-quality*4)==0)add(start,new Vec3(p.getX()+.4+random.nextFloat()*.2,p.getY()+.68,p.getZ()+.4+random.nextFloat()*.2),.2f-.15f*sizeMod,-.02f);
        for(var entity:current.getEntitiesOfClass(LivingEntity.class,tile.area()))if(!(entity instanceof Player)&&random.nextInt(20)==0){var box=entity.getBoundingBox();double width=box.maxX-box.minX,x=random.nextDouble()*width+box.minX,y=random.nextDouble()*width+box.minY,z=random.nextDouble()*width+box.minZ;
            switch(random.nextInt(5)){case 0->x=box.maxX;case 1->y=box.maxY;case 2->z=box.maxZ;case 3->x=box.minX;case 4->z=box.minZ;}
            add(new Vec3(x,y,z),new Vec3(x+random.nextFloat()*.2,y,z+random.nextFloat()*.2),.2f,-.02f);
        }
    }
    protected static void add(Vec3 from,Vec3 to,float size,float gravity){double visible=Minecraft.useFancyGraphics()?50:25;if(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(from)>gadomancymodern.ClientConfig.limitSquared(visible*visible))return;if(PARTICLES.size()>=2000)PARTICLES.remove(0);PARTICLES.add(new Wisp(current,from,to,size,gravity));}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;prepare();if(current==null||Minecraft.getInstance().isPaused())return;PARTICLES.forEach(Wisp::tick);PARTICLES.removeIf(p->!p.isAlive());}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;prepare();if(PARTICLES.isEmpty())return;var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var type=tc4revival.client.TCRenderTypes.runes(TEXTURE);var out=buffers.getBuffer(type);for(var particle:PARTICLES)particle.render(out,event.getCamera(),event.getPartialTick(),event.getPoseStack());buffers.endBatch(type);}
    public static class Wisp extends Particle {
        protected final float size;
        public Wisp(ClientLevel level,Vec3 from,Vec3 to,float scale,float gravity){super(level,from.x,from.y,from.z);size=(random.nextFloat()*.5f+.5f)*2*scale;lifetime=(int)(36/(random.nextDouble()*.3+.7));this.gravity=gravity;friction=.98f;hasPhysics=true;setSize(.1f,.1f);var motion=to.subtract(from).scale(1.0/lifetime);xd=motion.x;yd=motion.y;zd=motion.z;rCol=.8f+random.nextFloat()*.2f;gCol=.8f+random.nextFloat()*.2f;bCol=.8f+random.nextFloat()*.2f;}
        protected void vertex(VertexConsumer out,org.joml.Matrix4f matrix,float x,float y,float u,float v){out.vertex(matrix,x,y,0).color(rCol,gCol,bCol,.5f).uv(u,v).uv2(240).endVertex();}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
        @Override public void render(VertexConsumer out,Camera camera,float partial){render(out,camera,partial,new PoseStack());}
        /** Level-stage calls supply the view matrix; particle-engine calls retain their own transform. */
        public void render(VertexConsumer out,Camera camera,float partial,PoseStack pose){pose.pushPose();var view=camera.getPosition();pose.translate(Mth.lerp(partial,xo,x)-view.x,Mth.lerp(partial,yo,y)-view.y,Mth.lerp(partial,zo,z)-view.z);pose.mulPose(camera.rotation());float half=size*.5f*(lifetime-age)/lifetime;var matrix=pose.last().pose();vertex(out,matrix,-half,-half,.125f,1);vertex(out,matrix,half,-half,0,1);vertex(out,matrix,half,half,0,.875f);vertex(out,matrix,-half,half,.125f,.875f);pose.popPose();}
    }
}
