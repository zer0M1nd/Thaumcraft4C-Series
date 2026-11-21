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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class PrimalOrbEffects {
    private static final List<Wisp> WISPS=new ArrayList<>();
    private static final ResourceLocation ATLAS=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static ClientLevel current;
    private PrimalOrbEffects() {}
    private static boolean prepare(Entity source) {
        if (!(source.level() instanceof ClientLevel level)) return false;
        if (current!=level) { WISPS.clear(); current=level; } return true;
    }
    private static Vec3 jitter(float spread) {
        var r=current.random; return new Vec3((r.nextFloat()-r.nextFloat())*spread,(r.nextFloat()-r.nextFloat())*spread,(r.nextFloat()-r.nextFloat())*spread);
    }
    private static void add(Wisp wisp) { if (WISPS.size()<4096 && wisp.visible()) WISPS.add(wisp); }
    private static void addShrinking(Wisp wisp) {wisp.shrink=true;add(wisp);}
    public static void plant(net.minecraft.world.level.Level world,BlockPos pos,boolean mushroom) {
        if (!(world instanceof ClientLevel level)) return;
        if (current!=level) { WISPS.clear(); current=level; }
        var random=level.random;
        float red=mushroom ? .5f : .3f+random.nextFloat()*.3f;
        float green=mushroom ? .3f : .7f+random.nextFloat()*.3f;
        float blue=mushroom ? .8f : .7f+random.nextFloat()*.3f;
        var at=mushroom ? Vec3.atLowerCornerOf(pos).add(.5+(random.nextFloat()-random.nextFloat())*.4,.3,.5+(random.nextFloat()-random.nextFloat())*.4)
                : Vec3.atCenterOf(pos).add((random.nextFloat()-random.nextFloat())*.1,(random.nextFloat()-random.nextFloat())*.15,(random.nextFloat()-random.nextFloat())*.1);
        var wisp=new Wisp(at,mushroom ? .1f : .2f,-1,null,null,false,mushroom ? .015f : 0);
        wisp.setColor(red,green,blue); wisp.shrink=mushroom; add(wisp);
    }
    public static void trail(Entity source) {
        if (!prepare(source)) return;
        for (int color=0;color<6;color++) addShrinking(new Wisp(source.position().add(jitter(.2f)),.4f,color,source,null,false,0));
        addShrinking(new Wisp(source.position().add(jitter(.2f)),.1f,current.random.nextInt(6),null,null,true,0));
    }
    public static void impact(Entity source) {
        if (!prepare(source)) return;
        for (int group=0;group<6;group++) for (int color=0;color<6;color++) {
            var offset=jitter(.5f); addShrinking(new Wisp(source.position().add(offset),.4f,color,null,source.position().add(offset.scale(10)),false,.05f));
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.START) return;
        if (Minecraft.getInstance().level!=current) { WISPS.clear(); current=null; return; }
        if (!Minecraft.getInstance().isPaused()) { WISPS.forEach(Wisp::tick); WISPS.removeIf(wisp->!wisp.isAlive()); }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { WISPS.clear(); current=null; }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (current==null || event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        // Deferred rendering submits these layers after world composition. Put dark wisps
        // beneath their emissive halo there, so they cannot mask the orb's bright core.
        boolean[] layers=ShaderPackMagicRendering.shouldDefer() ? new boolean[]{true,false} : new boolean[]{false,true};
        for (boolean dark:layers) {
            var type=dark ? TCRenderTypes.softTranslucent(ATLAS) : TCRenderTypes.primalSprite(ATLAS); var out=TCRenderTypes.buffer(buffers,type);
            WISPS.stream().filter(wisp->wisp.dark==dark).forEach(wisp->wisp.renderWorld(out,event.getCamera(),event.getPartialTick(),event.getPoseStack())); TCRenderTypes.endBatch(buffers,type);
        }
    }
    private static  class Wisp extends Particle {
        final Entity target;
        final boolean dark;
        final float size;
        boolean shrink;
        boolean visible() { return lifetime>0; }
        Wisp(Vec3 pos,float scale,int color,Entity target,Vec3 destination,boolean noClip,float gravity) {
            super(current,pos.x,pos.y,pos.z); this.target=target; dark=color==5; this.gravity=gravity; hasPhysics=!noClip; setSize(.1f,.1f);
            size=(random.nextFloat()*.5f+.5f)*2*scale; lifetime=(int)(36/(Math.random()*.3+.7));
            double range=Minecraft.useFancyGraphics() ? 50 : 25;
            if (Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(pos)>range*range) lifetime=0;
            var r=current.random;
            switch(color) {
                case -1->{}
                case 0->{ rCol=.75f+r.nextFloat()*.25f; gCol=.25f+r.nextFloat()*.25f; bCol=.75f+r.nextFloat()*.25f; }
                case 1->{ rCol=.5f+r.nextFloat()*.3f; gCol=.5f+r.nextFloat()*.3f; bCol=.2f; }
                case 2->{ rCol=.2f; gCol=.2f; bCol=.7f+r.nextFloat()*.3f; }
                case 3->{ rCol=.2f; gCol=.7f+r.nextFloat()*.3f; bCol=.2f; }
                case 4->{ rCol=.7f+r.nextFloat()*.3f; gCol=.2f; bCol=.2f; }
                default->{ rCol=r.nextFloat()*.1f; gCol=r.nextFloat()*.1f; bCol=r.nextFloat()*.1f; }
            }
            if (destination!=null && lifetime>0) { var velocity=destination.subtract(pos).scale(1.0/lifetime); xd=velocity.x; yd=velocity.y; zd=velocity.z; }
        }
        @Override public void tick() {
            xo=x; yo=y; zo=z; if (age++>=lifetime) { remove(); return; }
            yd-=.04*gravity; if (hasPhysics) escape(); move(xd,yd,zd);
            if (target!=null) {
                xd*=.985; yd*=.985; zd*=.985;
                var direction=new Vec3(target.getX()-x,target.getY()+target.getBbHeight()/2-y,target.getZ()-z).normalize().scale(.2);
                xd=Mth.clamp((float)(xd+direction.x),-.2f,.2f); yd=Mth.clamp((float)(yd+direction.y),-.2f,.2f); zd=Mth.clamp((float)(zd+direction.z),-.2f,.2f);
            } else { xd*=.98f; yd*=.98f; zd*=.98f; if (onGround) { xd*=.7; zd*=.7; } }
        }
        private void escape() {
            var pos=BlockPos.containing(x,y,z); if (!level.getBlockState(pos).isRedstoneConductor(level,pos) || level.containsAnyLiquid(getBoundingBox())) return;
            Direction nearest=null; double distance=9999;
            for (Direction side:List.of(Direction.WEST,Direction.EAST,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH)) {
                var at=pos.relative(side); if (level.getBlockState(at).isRedstoneConductor(level,at)) continue;
                double fraction=switch(side.getAxis()) { case X->x-pos.getX(); case Y->y-pos.getY(); case Z->z-pos.getZ(); };
                double d=side.getAxisDirection()==Direction.AxisDirection.NEGATIVE ? fraction : 1-fraction;
                if (d<distance) { nearest=side; distance=d; }
            }
            float force=random.nextFloat()*.05f+.025f,other=(random.nextFloat()-random.nextFloat())*.1f;
            if (nearest==null) return;
            xd=nearest.getStepX()!=0 ? force*nearest.getStepX() : other;
            yd=nearest.getStepY()!=0 ? force*nearest.getStepY() : other;
            zd=nearest.getStepZ()!=0 ? force*nearest.getStepZ() : other;
        }
        @Override public void render(VertexConsumer out,Camera camera,float partial) { renderWorld(out,camera,partial,new PoseStack()); }
        void renderWorld(VertexConsumer out,Camera camera,float partial,PoseStack worldView) {
            if (lifetime<=0) return;
            var pose=WorldEffectPose.copy(worldView); var view=camera.getPosition();
            pose.translate(Mth.lerp(partial,xo,x)-view.x,Mth.lerp(partial,yo,y)-view.y,Mth.lerp(partial,zo,z)-view.z); pose.mulPose(camera.rotation());
            float phase=shrink ? (lifetime-age)/(float)lifetime : age/(float)(lifetime/2);
            if (!shrink && phase>1) phase=2-phase;
            ClientFocusEffects.quad(out,pose.last().pose(),.5f*size*phase,0,.875f,.125f,1,rCol,gCol,bCol,.5f,240);
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    }
}
