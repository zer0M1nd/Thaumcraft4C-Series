package tmmodern;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The original FXWispEG boot trail: drifting dark motes, not vanilla smoke. */
@Mod.EventBusSubscriber(modid="taintedmagic",value=Dist.CLIENT)
public class VoidwalkerMotes {
    private static final java.util.ArrayList<Mote> ACTIVE=new java.util.ArrayList<>();
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final org.joml.Matrix4f VIEW=new org.joml.Matrix4f();
    private static ClientLevel world;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(world!=mc.level){ACTIVE.clear();world=mc.level;}if(world==null||mc.isPaused())return;
        ACTIVE.forEach(Mote::tick);ACTIVE.removeIf(m->!m.isAlive());
        for(var p:world.players())if(p.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof TMArmor.Boots){
            var v=p.getDeltaMovement();if((Math.abs(v.x)+Math.abs(v.z)+Math.abs(v.y*.5)>.1||!p.onGround())&&world.random.nextInt(3)==0){
                var camera=mc.getCameraEntity();int range=Minecraft.useFancyGraphics()?50:25;
                if(camera!=null&&camera.distanceToSqr(p)<range*range&&ACTIVE.size()<2000)ACTIVE.add(new Mote(world,p));
            }
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||ACTIVE.isEmpty())return;
        VIEW.set(e.getPoseStack().last().pose());var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        var type=tc4revival.client.TCRenderTypes.sharpTranslucent(TEXTURE);var out=buffers.getBuffer(type);
        for(var m:ACTIVE)m.render(out,e.getCamera(),e.getPartialTick());buffers.endBatch(type);
    }
    public static class Mote extends Particle {
        private final Entity owner;private final float size;
        public Mote(ClientLevel l,Entity owner){super(l,owner.getX()+(l.random.nextDouble()-l.random.nextDouble())*.5,owner.getBoundingBox().minY+.05+(l.random.nextDouble()-l.random.nextDouble())*.1,owner.getZ()+(l.random.nextDouble()-l.random.nextDouble())*.5);this.owner=owner;xo=x;yo=y;zo=z;xd=random.nextGaussian()*.03;yd=-.05;zd=random.nextGaussian()*.03;size=(random.nextFloat()*.5f+.5f)*.8f;lifetime=(int)(40/(random.nextDouble()*.3+.7));setSize(.01f,.01f);rCol=random.nextFloat()*.05f;gCol=random.nextFloat()*.05f;bCol=random.nextFloat()*.05f;}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
        @Override public void tick(){xo=x;yo=y;zo=z;if(!onGround){var v=owner.getDeltaMovement();setPos(x+v.x,y,z+v.z);}escape();move(xd,yd,zd);xd*=.98;yd*=.98;zd*=.98;if(onGround){xd*=.85;zd*=.85;}if(age++>=lifetime)remove();}
        private void escape(){var p=BlockPos.containing(x,y,z);if(!level.getBlockState(p).isSolidRender(level,p)||level.containsAnyLiquid(getBoundingBox()))return;Direction side=null;double best=9999;for(var d:Direction.values()){var q=p.relative(d);if(level.getBlockState(q).isSolidRender(level,q))continue;double fraction=switch(d.getAxis()){case X->x-p.getX();case Y->y-p.getY();case Z->z-p.getZ();};double distance=d.getAxisDirection()==Direction.AxisDirection.POSITIVE?1-fraction:fraction;if(distance<best){best=distance;side=d;}}if(side==null)return;float speed=random.nextFloat()*.05f+.025f,drift=(random.nextFloat()-random.nextFloat())*.1f;xd=side.getAxis()==Direction.Axis.X?side.getStepX()*speed:drift;yd=side.getAxis()==Direction.Axis.Y?side.getStepY()*speed:drift;zd=side.getAxis()==Direction.Axis.Z?side.getStepZ()*speed:drift;}
        @Override public void render(VertexConsumer out,Camera camera,float partial){var pose=new PoseStack();pose.last().pose().set(VIEW);var at=camera.getPosition();pose.translate(net.minecraft.util.Mth.lerp(partial,xo,x)-at.x,net.minecraft.util.Mth.lerp(partial,yo,y)-at.y,net.minecraft.util.Mth.lerp(partial,zo,z)-at.z);pose.mulPose(camera.rotation());float u=age%13/16f,fade=(float)(1-Math.min(1024,camera.getPosition().distanceToSqr(x,y,z))/1024);TMClient.quad(out,pose.last().pose(),size*.5f,u,.1875f,u+.0624375f,.2499375f,rCol,gCol,bCol,.2f*(1-age/(float)lifetime)*fade);}
    }
}
