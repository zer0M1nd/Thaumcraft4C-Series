package conciliummodern;

import java.util.ArrayList;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.*;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** FXEssentiaTrail's original fixed phase, tint, atlas tile, steering and collision escape. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,value=Dist.CLIENT)
public final class CrimsonDrainEffects {
    private static final ResourceLocation ATLAS=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final ArrayList<Trail> TRAILS=new ArrayList<>();
    private static ClientLevel current;
    private static void world(){var next=Minecraft.getInstance().level;if(current!=next){TRAILS.clear();current=next;}}
    public static void receive(CrimsonDrainNetwork.Trail packet,Connection connection){
        var mc=Minecraft.getInstance();world();
        if(current==null||mc.getConnection()==null||mc.getConnection().getConnection()!=connection||!connection.isConnected()||!current.dimension().location().equals(packet.dimension()))return;
        local(current,packet.from(),packet.to(),packet.phase(),packet.count(),packet.color(),packet.scale());
    }
    public static void local(net.minecraft.world.level.Level level,Vec3 from,Vec3 to,int phase,int count,int color,float scale){
        world();var mc=Minecraft.getInstance();if(current==null||current!=level)return;
        if(!Double.isFinite(from.lengthSqr())||!Double.isFinite(to.lengthSqr())||from.distanceToSqr(to)>4096||(phase!=1&&phase!=3&&phase!=5)||count<1||count>5||!Float.isFinite(scale)||scale<=0||scale>16)return;
        int distance=Minecraft.useFancyGraphics()?64:32;
        if(mc.getCameraEntity()==null||mc.getCameraEntity().position().distanceToSqr(from)>distance*distance)return;
        for(int i=0;i<count;i++){if(TRAILS.size()>=4096)TRAILS.remove(0);TRAILS.add(new Trail(current,from,to,phase,color,scale));}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.START)return;world();if(current==null||Minecraft.getInstance().isPaused())return;TRAILS.forEach(Trail::tick);TRAILS.removeIf(trail->!trail.isAlive());}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;world();if(current==null||TRAILS.isEmpty())return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var type=tc4revival.client.TCRenderTypes.softParticle(ATLAS);var out=buffers.getBuffer(type);
        for(var trail:TRAILS)trail.draw(event.getPoseStack(),out,event.getCamera(),event.getPartialTick());buffers.endBatch(type);
    }
    private static final class Trail extends Particle {
        private final Vec3 target;
        private final int phase;
        private float size;
        Trail(ClientLevel level,Vec3 from,Vec3 target,int phase,int color,float scale){
            super(level,from.x,from.y,from.z);this.target=target;this.phase=phase;hasPhysics=true;
            size=(Mth.sin(phase/2f)*.1f+1)*scale;int base=Math.max(1,(int)((float)from.distanceTo(target)*30));lifetime=base/2+random.nextInt(base);
            xd=Mth.sin(phase/4f)*.015f+random.nextGaussian()*.002f;yd=.1f+Mth.sin(phase/3f)*.01f;zd=Mth.sin(phase/2f)*.015f+random.nextGaussian()*.002f;
            rCol=(color>>16&255)/255f*(.8f+random.nextFloat()*.2f);gCol=(color>>8&255)/255f*(.8f+random.nextFloat()*.2f);bCol=(color&255)/255f*(.8f+random.nextFloat()*.2f);
        }
        @Override public void tick(){
            xo=x;yo=y;zo=z;if(age++>=lifetime){remove();return;}yd+=.002;escape();move(xd,yd,zd);
            xd=Mth.clamp((float)(xd*.985),-.05f,.05f);yd=Mth.clamp((float)(yd*.985),-.05f,.05f);zd=Mth.clamp((float)(zd*.985),-.05f,.05f);
            var delta=target.subtract(x,y,z);double distance=delta.length();if(distance<2)size*=.98f;
            if(size<.2f||distance<1e-8){remove();return;}double force=.01/(distance*Math.min(1,distance));xd+=delta.x*force;yd+=delta.y*force;zd+=delta.z*force;
        }
        // Unlike vanilla modern Particle.move, a blocked Y step must not permanently stop a legacy trail.
        @Override public void move(double mx,double my,double mz){
            var moved=Entity.collideBoundingBox(null,new Vec3(mx,my,mz),getBoundingBox(),level,java.util.List.of());
            setBoundingBox(getBoundingBox().move(moved));setLocationFromBoundingbox();onGround=my!=moved.y&&my<0;
            if(mx!=moved.x)xd=0;if(my!=moved.y)yd=0;if(mz!=moved.z)zd=0;
        }
        private void escape(){
            var pos=BlockPos.containing(x,y,z);if(!solid(pos)||!level.getFluidState(pos).isEmpty())return;
            Direction chosen=null;double nearest=9999;
            for(var direction:new Direction[]{Direction.WEST,Direction.EAST,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH}){
                if(solid(pos.relative(direction)))continue;
                double fraction=switch(direction.getAxis()){case X->x-pos.getX();case Y->y-pos.getY();case Z->z-pos.getZ();};
                double distance=direction.getAxisDirection()==Direction.AxisDirection.NEGATIVE?fraction:1-fraction;
                if(distance<nearest){nearest=distance;chosen=direction;}
            }
            float push=random.nextFloat()*.05f+.025f,side=(random.nextFloat()-random.nextFloat())*.1f;
            if(chosen!=null){xd=chosen.getStepX()==0?side:chosen.getStepX()*push;yd=chosen.getStepY()==0?side:chosen.getStepY()*push;zd=chosen.getStepZ()==0?side:chosen.getStepZ()*push;}
        }
        private boolean solid(BlockPos pos){return !level.isEmptyBlock(pos)&&level.getBlockState(pos).isRedstoneConductor(level,pos);}
        void draw(PoseStack pose,VertexConsumer out,Camera camera,float partial){
            var at=new Vec3(Mth.lerp(partial,xo,x),Mth.lerp(partial,yo,y),Mth.lerp(partial,zo,z)).subtract(camera.getPosition());
            float half=.1f*size*(Mth.sin((age-phase)/5f)*.25f+1);
            pose.pushPose();pose.translate(at.x,at.y,at.z);pose.mulPose(camera.rotation());
            var matrix=pose.last().pose();vertex(out,matrix,-half,-half,.5625f,.125f);vertex(out,matrix,-half,half,.625f,.125f);
            vertex(out,matrix,half,half,.625f,.0625f);vertex(out,matrix,half,-half,.5625f,.0625f);pose.popPose();
        }
        private void vertex(VertexConsumer out,org.joml.Matrix4f matrix,float x,float y,float u,float v){out.vertex(matrix,x,y,0).color(rCol,gCol,bCol,.5f).uv(u,v).uv2(0xf000f0).endVertex();}
        @Override public void render(VertexConsumer out,Camera camera,float partial){} // Rendered in the shared buffered stage above.
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
    }
    private CrimsonDrainEffects(){}
}
