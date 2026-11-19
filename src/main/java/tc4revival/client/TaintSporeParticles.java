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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.TCSounds;
import tc4revival.content.TaintSporeEntity;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class TaintSporeParticles {
    private static final ResourceLocation ATLAS=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final List<Fly> FLIES=new ArrayList<>();
    private static final List<Long> BUZZES=new ArrayList<>();
    private static ClientLevel current;
    private TaintSporeParticles() {}
    public static void maintain(TaintSporeEntity spore) { maintain(spore,spore.sporeSize()/3,.1f,10,0); }
    public static void maintainHive(tc4revival.content.TaintHiveEntity hive) { maintain(hive,(500-hive.spawnCounter())/25,.1f,10,0); }
    public static void maintainSwarm(tc4revival.content.TaintSwarmEntity swarm) {
        int particles=switch(Minecraft.getInstance().options.particles().get()) {case ALL->25;case DECREASED->12;case MINIMAL->0;};
        maintain(swarm,Math.max(particles,10),.22f,15,.08f);
    }
    public static void clear(net.minecraft.world.entity.LivingEntity target) { FLIES.removeIf(fly->fly.target==target); }
    private static void maintain(net.minecraft.world.entity.LivingEntity target,int desired,float speed,float turn,float gravity) {
        if(!(target.level() instanceof ClientLevel level))return;
        if(current!=level) { FLIES.clear();BUZZES.clear();current=level; }
        long count=FLIES.stream().filter(fly->fly.target==target && fly.isAlive()).count();
        if(count<desired && FLIES.size()<4096)FLIES.add(new Fly(level,target,speed,turn,gravity));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.START) return;
        if (Minecraft.getInstance().level!=current) { FLIES.clear(); BUZZES.clear(); current=null; return; }
        if (Minecraft.getInstance().isPaused()) return;
        FLIES.forEach(Fly::tick); FLIES.removeIf(fly->!fly.isAlive());
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { FLIES.clear(); BUZZES.clear(); current=null; }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || current==null) return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource(); var type=TCRenderTypes.softTranslucent(ATLAS);
        var out=TCRenderTypes.buffer(buffers,type); FLIES.forEach(fly->fly.renderWorld(event.getPoseStack(),out,event.getCamera(),event.getPartialTick())); TCRenderTypes.endBatch(buffers,type);
    }
    private static  class Fly extends Particle {
        final net.minecraft.world.entity.LivingEntity target;
        final float speed,turnSpeed,gravity;
        final float size;
        int dying;
        float yaw,pitch;
        Fly(ClientLevel level,net.minecraft.world.entity.LivingEntity target,float speed,float turnSpeed,float gravity) {
            super(level,target.getX()+jitter(level,2),target.getY()+jitter(level,2),target.getZ()+jitter(level,2));
            this.target=target;this.speed=speed;this.turnSpeed=turnSpeed;this.gravity=gravity; size=1+random.nextFloat()*.5f;
            rCol=.8f+level.random.nextFloat()*.2f; gCol=level.random.nextFloat()*.4f; bCol=1-level.random.nextFloat()*.2f;
            xd=jitter(level,.2f); yd=jitter(level,.2f); zd=jitter(level,.2f); hasPhysics=true;
            var camera=Minecraft.getInstance().getCameraEntity();
            double visible=Minecraft.getInstance().options.graphicsMode().get()==net.minecraft.client.GraphicsStatus.FAST?32:64;
            if(camera!=null && camera.distanceToSqr(x,y,z)>visible*visible)remove();
        }
        private static float jitter(ClientLevel level,float scale) { return (level.random.nextFloat()-level.random.nextFloat())*scale; }
        @Override public void tick() {
            xo=x; yo=y; zo=z; age++;
            boolean dead=target.isRemoved() || target.deathTime>0;
            yd+=dead?-gravity/2:gravity;
            if (dead && ++dying>50) { remove(); return; }
            escape(); move(xd,yd,zd); xd*=.985; yd*=.985; zd*=.985;
            if (!dead) {
                double dx=target.getX()-x,dz=target.getZ()-z;
                double dy=target.getBoundingBox().getCenter().y-getBoundingBox().getCenter().y;
                boolean approach=target.distanceToSqr(x,y,z)>target.getBbWidth() && target.hurtTime<=0;
                float yawStep=turnSpeed/2+random.nextInt((int)(turnSpeed/2)),pitchStep=turnSpeed/2+random.nextInt((int)(turnSpeed/2));
                yaw=turn(yaw,(float)Math.toDegrees(Math.atan2(dz,dx))-90,approach ? yawStep : -yawStep);
                pitch=turn(pitch,(float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))),approach ? pitchStep : -pitchStep);
                var heading=new Vec3(-Mth.sin(yaw*Mth.DEG_TO_RAD)*Mth.cos(pitch*Mth.DEG_TO_RAD),
                        -Mth.sin(pitch*Mth.DEG_TO_RAD),Mth.cos(yaw*Mth.DEG_TO_RAD)*Mth.cos(pitch*Mth.DEG_TO_RAD)).normalize();
                xd=(heading.x+scatter())*speed; yd=(heading.y+scatter())*speed; zd=(heading.z+scatter())*speed;
            }
            if (BUZZES.size()<3 && random.nextInt(50)==0 && level.getNearestPlayer(x,y,z,8,false)!=null) {
                level.playLocalSound(x,y,z,TCSounds.FLY.get(),SoundSource.MASTER,.03f,.5f+random.nextFloat()*.4f,false);
                BUZZES.add(System.nanoTime()+1500000L);
            }
            if (BUZZES.size()>=3 && BUZZES.get(0)<System.nanoTime()) BUZZES.remove(0);
        }
        private double scatter() { return random.nextGaussian()*(random.nextBoolean() ? -1 : 1)*.0075f*15; }
        private static float turn(float current,float target,float limit) {
            float delta=Mth.wrapDegrees(target-current);
            delta=Mth.clamp(delta,-Math.abs(limit),Math.abs(limit));
            if(limit<0)delta=-delta;
            return current+delta;
        }
        private void escape() {
            var pos=BlockPos.containing(x,y,z); var state=level.getBlockState(pos);
            if (state.isAir() || ResourceLocation.parse("thaumcraft:taint_fibres").equals(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()))
                    || level.containsAnyLiquid(getBoundingBox())) return;
            Direction chosen=null; double nearest=9999;
            for (Direction side:List.of(Direction.WEST,Direction.EAST,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH)) {
                var other=pos.relative(side); if (level.getBlockState(other).isRedstoneConductor(level,other)) continue;
                double fraction=switch(side.getAxis()) { case X->x-pos.getX(); case Y->y-pos.getY(); case Z->z-pos.getZ(); };
                double distance=side.getAxisDirection()==Direction.AxisDirection.NEGATIVE ? fraction : 1-fraction;
                if (distance<nearest) { nearest=distance; chosen=side; }
            }
            float force=random.nextFloat()*.05f+.025f,other=(random.nextFloat()-random.nextFloat())*.1f;
            if (chosen==null) return;
            xd=chosen.getStepX()!=0 ? force*chosen.getStepX() : other;
            yd=chosen.getStepY()!=0 ? force*chosen.getStepY() : other;
            zd=chosen.getStepZ()!=0 ? force*chosen.getStepZ() : other;
        }
        @Override public void render(VertexConsumer out,Camera camera,float partial) {
            renderWorld(new PoseStack(),out,camera,partial);
        }
        private void renderWorld(PoseStack pose,VertexConsumer out,Camera camera,float partial) {
            pose.pushPose(); var view=camera.getPosition();
            pose.translate(Mth.lerp(partial,xo,x)-view.x,Mth.lerp(partial,yo,y)-view.y,Mth.lerp(partial,zo,z)-view.z); pose.mulPose(camera.rotation());
            float u=(7+age%8)/16f, radius=.1f*size*(1+.25f*Mth.sin(age/3f));
            float hurt=target.hurtTime<=0 ? 1 : .5f;
            ClientFocusEffects.quad(out,pose.last().pose(),radius,u,.25f,u+.0624375f,.3124375f,rCol,gCol*hurt,bCol*hurt,(50-dying)/50f,240);
            pose.popPose();
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    }
}
