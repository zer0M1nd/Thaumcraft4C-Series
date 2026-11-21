package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Dark homing motes, orb impact wisps and the guardian's drifting shroud. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class EldritchWisps {
    private static final ResourceLocation ATLAS=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final ArrayList<Particle> ACTIVE=new ArrayList<>();
    private static ClientLevel current;
    private static final org.joml.Matrix4f VIEW=new org.joml.Matrix4f();
    private EldritchWisps() {}
    private static Wisp create(Level level,double x,double y,double z,float size,boolean shroud,Entity target) {
        var mc=Minecraft.getInstance();
        if (!(level instanceof ClientLevel client) || mc.level!=client) return null;
        if (current!=client) { ACTIVE.clear(); current=client; }
        var viewer=mc.getCameraEntity(); int range=Minecraft.useFancyGraphics() ? 50 : 25;
        if (viewer==null || viewer.distanceToSqr(x,y,z)>range*range) return null;
        if (ACTIVE.size()>=4000) ACTIVE.remove(0);
        var wisp=new Wisp(client,x,y,z,size,shroud,target); ACTIVE.add(wisp); return wisp;
    }
    public static void homing(Level level,double x,double y,double z,Entity target) {
        var wisp=create(level,x,y,z,.4f,false,target); if (wisp!=null) wisp.setPull(1);
    }
    public static void shroud(Entity owner) {
        shroud(owner,.22);
    }
    public static void shroud(Entity owner,double heightFraction) {
        var level=owner.level();
        double x=owner.getX()+(level.random.nextFloat()-level.random.nextFloat())*.2f;
        double z=owner.getZ()+(level.random.nextFloat()-level.random.nextFloat())*.2f;
        int count=switch(Minecraft.getInstance().options.particles().get()) { case ALL->2; case DECREASED->1; case MINIMAL->0; };
        for (int i=0;i<count;i++) create(level,x,owner.getY()+heightFraction*owner.getBbHeight(),z,.4f,true,owner);
    }
    public static void impact(Entity orb) {
        var level=orb.level();
        for (int i=0;i<30;i++) {
            float dx=(level.random.nextFloat()-level.random.nextFloat())*.3f;
            float dy=(level.random.nextFloat()-level.random.nextFloat())*.3f;
            float dz=(level.random.nextFloat()-level.random.nextFloat())*.3f;
            var wisp=create(level,orb.getX()+dx,orb.getY()+dy,orb.getZ()+dz,.3f,false,null);
            if (wisp!=null) wisp.scatter(dx,dy,dz);
        }
    }
    public static void spawnSpiral(Entity owner,int remainingTicks) {
        var mc=Minecraft.getInstance(); if (!(owner.level() instanceof ClientLevel level) || mc.level!=level) return;
        if (current!=level) { ACTIVE.clear(); current=level; }
        float radius=Math.max(1,owner.getBbHeight()*(150-remainingTicks)/150f);
        for (int i=0;i<33;i++) {
            if (ACTIVE.size()>=4000) ACTIVE.remove(0);
            ACTIVE.add(new Spiral(level,owner.getX(),owner.getBoundingBox().minY+radius/2,owner.getZ(),radius,
                    level.random.nextInt(360),Mth.floor(owner.getBoundingBox().minY)-1));
        }
    }
    public static void rune(Level world,double x,double y,double z,float red,float green,float blue,int duration) {
        rune(world,x,y,z,red,green,blue,duration,0);
    }
    public static void rune(Level world,double x,double y,double z,float red,float green,float blue,int duration,float gravity) {
        var mc=Minecraft.getInstance(); if (!(world instanceof ClientLevel level) || mc.level!=level) return;
        if (current!=level) { ACTIVE.clear(); current=level; }
        if (ACTIVE.size()>=4000) ACTIVE.remove(0);
        ACTIVE.add(new Rune(level,x,y,z,red,green,blue,duration,gravity));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return; var mc=Minecraft.getInstance();
        if (current!=mc.level) { ACTIVE.clear(); current=mc.level; }
        if (current==null || mc.isPaused()) return;
        ACTIVE.forEach(Particle::tick); ACTIVE.removeIf(wisp->!wisp.isAlive());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || current==null) return;
        VIEW.set(event.getPoseStack().last().pose());
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource(); var type=TCRenderTypes.sharpTranslucent(ATLAS);
        for (Particle particle:ACTIVE) {
            var selected=particle instanceof Rune ? TCRenderTypes.runes(ATLAS) : type;
            particle.render(TCRenderTypes.buffer(buffers,selected),event.getCamera(),event.getPartialTick());
        }
        TCRenderTypes.endBatch(buffers,type); TCRenderTypes.endBatch(buffers,TCRenderTypes.runes(ATLAS));
    }
    private static  class Wisp extends Particle {
        private final Entity target;
        private final boolean shroud;
        private final float size;
        void setPull(float value) { gravity=value; }
        void scatter(float dx,float dy,float dz) { xd=dx*7/lifetime; yd=dy*7/lifetime; zd=dz*7/lifetime; gravity=.02f; }
        Wisp(ClientLevel level,double x,double y,double z,float scale,boolean shroud,Entity target) {
            super(level,x,y,z); this.target=target; this.shroud=shroud;
            size=(random.nextFloat()*.5f+.5f)*2*scale;
            lifetime=(int)((shroud ? 40 : 36)/(random.nextDouble()*.3+.7));
            float tint=shroud ? .05f : .1f;
            rCol=random.nextFloat()*tint; gCol=random.nextFloat()*tint; bCol=random.nextFloat()*tint;
            setSize(shroud ? .01f : .1f,shroud ? .01f : .1f);
            if (shroud) { xd=random.nextGaussian()*.03; yd=-.05; zd=random.nextGaussian()*.03; }
        }
        @Override public void tick() {
            xo=x; yo=y; zo=z;
            if (!shroud && age++>=lifetime) remove();
            if (shroud && target!=null && !onGround) {
                Vec3 drift=target.getDeltaMovement(); setPos(x+drift.x,y,z+drift.z);
            }
            if (!shroud) yd-=.04*gravity;
            escapeSolid(); move(xd,yd,zd);
            if (!shroud && target!=null) {
                Vec3 delta=new Vec3(target.getX()-x,target.getY()+target.getBbHeight()/2-y,target.getZ()-z);
                Vec3 acceleration=delta.lengthSqr()>1e-12 ? delta.normalize().scale(.2) : Vec3.ZERO;
                xd=Mth.clamp((float)(xd*.985+acceleration.x),-.2f,.2f);
                yd=Mth.clamp((float)(yd*.985+acceleration.y),-.2f,.2f);
                zd=Mth.clamp((float)(zd*.985+acceleration.z),-.2f,.2f);
            } else {
                xd*=.98f; yd*=.98f; zd*=.98f;
                if (onGround) { xd*=shroud ? .85f : .7f; zd*=shroud ? .85f : .7f; }
            }
            if (shroud && age++>=lifetime) remove();
        }
        private void escapeSolid() {
            BlockPos pos=BlockPos.containing(x,y,z);
            if (!level.getBlockState(pos).isSolidRender(level,pos) || level.containsAnyLiquid(getBoundingBox())) return;
            Direction exit=null; double best=Double.MAX_VALUE;
            for (Direction side:new Direction[]{Direction.WEST,Direction.EAST,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH}) {
                BlockPos neighbor=pos.relative(side); if (level.getBlockState(neighbor).isSolidRender(level,neighbor)) continue;
                double fraction=switch(side.getAxis()) { case X->x-pos.getX(); case Y->y-pos.getY(); case Z->z-pos.getZ(); };
                double distance=side.getAxisDirection()==Direction.AxisDirection.POSITIVE ? 1-fraction : fraction;
                if (distance<best) { best=distance; exit=side; }
            }
            float speed=random.nextFloat()*.05f+.025f,sideways=(random.nextFloat()-random.nextFloat())*.1f;
            if (exit==null) return;
            xd=exit.getAxis()==Direction.Axis.X ? exit.getStepX()*speed : sideways;
            yd=exit.getAxis()==Direction.Axis.Y ? exit.getStepY()*speed : sideways;
            zd=exit.getAxis()==Direction.Axis.Z ? exit.getStepZ()*speed : sideways;
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
        @Override public void render(VertexConsumer out,Camera camera,float partial) {
            var pose=new PoseStack(); pose.last().pose().set(VIEW); var at=camera.getPosition();
            pose.translate(Mth.lerp(partial,xo,x)-at.x,Mth.lerp(partial,yo,y)-at.y,Mth.lerp(partial,zo,z)-at.z); pose.mulPose(camera.rotation());
            float remaining=1-age/(float)lifetime,half=size*.5f*(shroud ? 1 : remaining);
            var viewer=Minecraft.getInstance().getCameraEntity();
            float fade=viewer==null ? 0 : (float)(1-Math.min(1024,viewer.distanceToSqr(x,y,z))/1024);
            float u=shroud ? age%13/16f : 0,v=shroud ? .1875f : .875f,span=shroud ? .0624375f : .125f;
            ClientFocusEffects.quad(out,pose.last().pose(),half,u+span,v,u,v+span,rCol,gCol,bCol,shroud ? .2f*remaining*fade : .5f,240);
        }
    }
    private static  class Spiral extends Particle {
        private final float radius,size;
        private final int start,floor;
        Spiral(ClientLevel level,double x,double y,double z,float radius,int start,int floor) {
            super(level,x,y,z); this.radius=radius; this.start=start; this.floor=floor;
            lifetime=20+level.random.nextInt(10); size=(random.nextFloat()*.5f+.5f)*2;
            rCol=34/255f; gCol=17/255f; bCol=47/255f; setSize(.01f,.01f);
        }
        @Override public void tick() { alpha=(lifetime-age)/(float)lifetime; if (age++>=lifetime) remove(); }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
        @Override public void render(VertexConsumer out,Camera camera,float partial) {
            double progress=(age+partial)/lifetime,azimuth=Math.toRadians(start+720*progress),elevation=Math.toRadians(90-180*progress);
            var at=camera.getPosition(); var pose=new PoseStack(); pose.last().pose().set(VIEW);
            pose.translate(x-Math.sin(azimuth)*Math.cos(elevation)*radius-at.x,
                    Math.max(y-Math.sin(elevation)*radius,floor+.1)-at.y,z+Math.cos(azimuth)*Math.cos(elevation)*radius-at.z);
            pose.mulPose(camera.rotation()); int frame=(int)(1+age/(float)lifetime*4); float u=frame%16/16f,v=frame/16/16f;
            ClientFocusEffects.quad(out,pose.last().pose(),.15f*size,u+.0624375f,v,u,v+.0624375f,rCol,gCol,bCol,.66f*alpha,getLightColor(partial));
        }
    }
    private static  class Rune extends Particle {
        final float rotation,size,u; final double offsetX,offsetY;
        Rune(ClientLevel level,double x,double y,double z,float red,float green,float blue,int duration,float gravity) {
            super(level,x,y,z); lifetime=duration*3; rCol=red==0 ? 1 : red; gCol=green; bCol=blue;
            this.gravity=gravity;xd=yd=zd=0;
            rotation=random.nextInt(4)*90; u=random.nextInt(16)/16f; offsetX=random.nextFloat()*.2; offsetY=-.3+random.nextFloat()*.6;
            size=(float)(1+random.nextGaussian()*.1); alpha=0; setSize(.01f,.01f);
        }
        @Override public void tick() {
            xo=x;yo=y;zo=z;
            alpha=age<=lifetime/5f ? age/(lifetime/5f) : (lifetime-age)/(float)lifetime;
            if (age++>=lifetime) remove();
            yd-=.04*gravity;x+=xd;y+=yd;z+=zd;
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
        @Override public void render(VertexConsumer out,Camera camera,float partial) {
            var pose=new PoseStack(); pose.last().pose().set(VIEW); var at=camera.getPosition(); pose.translate(Mth.lerp(partial,xo,x)-at.x,Mth.lerp(partial,yo,y)-at.y,Mth.lerp(partial,zo,z)-at.z);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation)); pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90));
            pose.translate(offsetX,offsetY,-.51);
            float half=.15f*size; float[][] vertices={{-half,half,u+.0624375f,.4374375f},{half,half,u+.0624375f,.375f},
                    {half,-half,u,.375f},{-half,-half,u,.4374375f}};
            for (float[] vertex:vertices) out.vertex(pose.last().pose(),vertex[0],vertex[1],0).color(rCol,gCol,bCol,alpha/2).uv(vertex[2],vertex[3]).uv2(240).endVertex();
        }
    }
}
