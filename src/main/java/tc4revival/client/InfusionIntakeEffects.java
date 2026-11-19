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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.ArcanePedestalBlockEntity;
import tc4revival.content.InfusionMatrixBlockEntity;

/** Visual consumers of synchronized source hints; these never drain containers or mutate the recipe. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class InfusionIntakeEffects {
    private static final ResourceLocation ATLAS=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final List<Intake> PARTICLES=new ArrayList<>();
    private record Stream(BlockPos consumer,BlockPos source,int color) {}
    private static final java.util.Map<Stream,Integer> STREAMS=new java.util.LinkedHashMap<>();
    private static ClientLevel current;
    private static int phase;
    private InfusionIntakeEffects() {}
    private static void world() {
        var level=Minecraft.getInstance().level; if (current!=level) { PARTICLES.clear(); STREAMS.clear(); current=level; phase=0; }
    }
    public static void receive(tc4revival.network.EssentiaSourcePacket packet,net.minecraft.network.Connection connection) {
        var client=Minecraft.getInstance();
        if (client.level==null || client.getConnection()==null || client.getConnection().getConnection()!=connection || !connection.isConnected()
                || !client.level.dimension().location().equals(packet.dimension())) return;
        world(); var key=new Stream(packet.consumer(),packet.source(),packet.color());
        if (!STREAMS.containsKey(key) && STREAMS.size()>=8192) STREAMS.remove(STREAMS.keySet().iterator().next());
        STREAMS.put(key,15);
    }
    public static void emit(InfusionMatrixBlockEntity matrix) {
        world(); if (current==null || matrix.getLevel()!=current || Minecraft.getInstance().isPaused()) return;
        var random=current.random; var target=matrix.getBlockPos().getCenter().add(0,-1,0);
        if(matrix.crafting()) {
            var rune=matrix.getBlockPos().below(2).getCenter();
            EldritchWisps.rune(current,rune.x,rune.y,rune.z,.5f+random.nextFloat()*.2f,.1f,.7f+random.nextFloat()*.3f,25,-.03f);
        }
        int count=switch(Minecraft.getInstance().options.particles().get()) { case ALL->4; case DECREASED->2; case MINIMAL->0; };
        for (var source:matrix.sources()) {
            if (source.expires()<=current.getGameTime()) continue;
            if (source.essentia()) {
                int left=(int)Math.min(15,source.expires()-current.getGameTime());
                float scale=left>5 ? 1 : left*left/25f;
                add(new Intake(current,tc4revival.content.EssentiaSources.visualOrigin(current,source.position()),target,2,ItemStack.EMPTY,source.color(),phase-(left>5 ? 0 : 5-left),scale));
            } else if (source.position().equals(matrix.getBlockPos())) {
                var entity=current.getEntity(source.entityId()); if (entity==null) continue;
                for (int index=0;index<count;index++) {
                    var start=new Vec3(entity.getX()+(random.nextFloat()-random.nextFloat())*entity.getBbWidth(),entity.getBoundingBox().minY+random.nextFloat()*entity.getBbHeight(),entity.getZ()+(random.nextFloat()-random.nextFloat())*entity.getBbWidth());
                    add(new Intake(current,start,target,1,ItemStack.EMPTY,0,phase,1));
                }
            } else if (current.hasChunkAt(source.position()) && current.getBlockEntity(source.position()) instanceof ArcanePedestalBlockEntity pedestal) {
                var stack=pedestal.getItem(0); if (stack.isEmpty()) continue;
                if (random.nextInt(3)==0) add(new Intake(current,jitter(source.position(),false),target,0,ItemStack.EMPTY,0,phase,1));
                else for (int index=0;index<count;index++) add(new Intake(current,jitter(source.position(),!(stack.getItem() instanceof BlockItem)),target,3,stack,0,phase,1));
            }
        }
        if (matrix.crafting() && matrix.instability()>0 && random.nextInt(200)<=matrix.instability()) {
            var start=matrix.getBlockPos().getCenter(); var end=start.add((random.nextFloat()-random.nextFloat())*2,(random.nextFloat()-random.nextFloat())*2,(random.nextFloat()-random.nextFloat())*2);
            ClientFocusEffects.infusionBolt(start,end);
        }
    }
    public static void external(Vec3 from,Vec3 to,int color,float scale,boolean item) {
        world();if(current==null)return;
        ItemStack stack=ItemStack.EMPTY;
        if(item&&current.getBlockEntity(BlockPos.containing(from).below()) instanceof ArcanePedestalBlockEntity pedestal)stack=pedestal.getItem(0);
        add(new Intake(current,from,to,item?(stack.isEmpty()?0:3):2,stack,color,phase,scale));
    }
    private static Vec3 jitter(BlockPos pos,boolean item) {
        var random=current.random;
        return item ? new Vec3(pos.getX()+.4+random.nextFloat()*.2,pos.getY()+1.23+random.nextFloat()*.2,pos.getZ()+.4+random.nextFloat()*.2)
                : new Vec3(pos.getX()+random.nextFloat(),pos.getY()+1+random.nextFloat(),pos.getZ()+random.nextFloat());
    }
    private static void add(Intake particle) {
        var camera=Minecraft.getInstance().getCameraEntity(); int distance=Minecraft.useFancyGraphics() ? 64 : 32;
        if (camera!=null && camera.distanceToSqr(particle.start)<=distance*distance && PARTICLES.size()<8192) PARTICLES.add(particle);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.START) return;
        world(); if (current==null || Minecraft.getInstance().isPaused()) return;
        phase++; PARTICLES.forEach(Intake::tick); PARTICLES.removeIf(particle->!particle.isAlive());
        var streams=STREAMS.entrySet().iterator();
        while (streams.hasNext()) {
            var entry=streams.next(); int left=entry.getValue(); if (left<=0) { streams.remove(); continue; }
            var source=entry.getKey(); var target=source.consumer().getCenter();
            if (current.hasChunkAt(source.consumer()) && current.getBlockEntity(source.consumer()) instanceof InfusionMatrixBlockEntity) target=target.add(0,-1,0);
            add(new Intake(current,tc4revival.content.EssentiaSources.visualOrigin(current,source.source()),target,2,ItemStack.EMPTY,source.color(),phase-(left>5 ? 0 : 5-left),left>5 ? 1 : left*left/25f));
            entry.setValue(left-1);
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || current==null || PARTICLES.isEmpty()) return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        for (int layer=0;layer<3;layer++) {
            int selectedLayer=layer;
            if(PARTICLES.stream().noneMatch(particle->particle.layer()==selectedLayer))continue;
            var type=layer==0 ? TCRenderTypes.runes(ATLAS) : TCRenderTypes.softTranslucent(layer==1 ? ATLAS : net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
            var out=TCRenderTypes.buffer(buffers,type); for (var particle:PARTICLES) if (particle.layer()==layer) particle.render(out,event.getCamera(),event.getPartialTick(),event.getPoseStack()); TCRenderTypes.endBatch(buffers,type);
        }
    }
    private static  class Intake extends Particle {
        final Vec3 start,target;
        final int kind,phase;
        final TextureAtlasSprite sprite;
        final float jitterU,jitterV;
        float size;
        Intake(ClientLevel level,Vec3 start,Vec3 target,int kind,ItemStack stack,int color,int phase,float scale) {
            super(level,start.x,start.y,start.z); this.start=start; this.target=target; this.kind=kind; this.phase=phase; hasPhysics=kind!=2;
            int base=Math.max(1,(int)(start.distanceTo(target)*(kind==2 ? 30 : 3))); lifetime=base/2+random.nextInt(base);
            size=kind==2 ? (Mth.sin(phase/2f)*.1f+1)*scale : kind==3 ? random.nextFloat()*.3f+.4f : random.nextFloat()*.5f+.5f;
            float motion=kind==3 && !(stack.getItem() instanceof BlockItem) ? .03f : .01f;
            xd=random.nextGaussian()*motion; yd=random.nextGaussian()*motion; zd=random.nextGaussian()*motion;
            sprite=kind==3 ? sprite(stack) : null; jitterU=random.nextFloat()*3; jitterV=random.nextFloat()*3;
            if (kind==2) {
                xd=Mth.sin(phase/4f)*.015f+random.nextGaussian()*.002f; yd=.1f+Mth.sin(phase/3f)*.01f; zd=Mth.sin(phase/2f)*.015f+random.nextGaussian()*.002f;
                rCol=(color>>16&255)/255f*(.8f+random.nextFloat()*.2f); gCol=(color>>8&255)/255f*(.8f+random.nextFloat()*.2f); bCol=(color&255)/255f*(.8f+random.nextFloat()*.2f);
            } else if (kind==3) {
                int tint=stack.getItem() instanceof BlockItem ? 0xffffff : Minecraft.getInstance().getItemColors().getColor(stack,0);
                rCol=.6f*(tint>>16&255)/255f; gCol=.6f*(tint>>8&255)/255f; bCol=.6f*(tint&255)/255f;
            } else { rCol=kind==1 ? .2f : .4f+random.nextFloat()*.2f; gCol=kind==1 ? .6f+random.nextFloat()*.3f : .2f; bCol=kind==1 ? .3f : .6f+random.nextFloat()*.3f; }
        }
        private TextureAtlasSprite sprite(ItemStack stack) {
            var mc=Minecraft.getInstance();
            if (stack.getItem() instanceof BlockItem item) {
                var state=item.getBlock().defaultBlockState(); var model=mc.getBlockRenderer().getBlockModel(state);
                var quads=model.getQuads(state,Direction.from3DDataValue(random.nextInt(6)),random);
                if (!quads.isEmpty()) return quads.get(0).getSprite(); return model.getParticleIcon();
            }
            return mc.getItemRenderer().getModel(stack,level,null,0).getParticleIcon();
        }
        int layer() { return kind==3 ? 2 : kind==2 ? 1 : 0; }
        @Override public void tick() {
            xo=x; yo=y; zo=z;
            if (age++>=lifetime || kind!=2 && BlockPos.containing(x,y,z).equals(BlockPos.containing(target))) { remove(); return; }
            if (kind==2) yd+=.002;
            // Essentia is a visual stream, not a colliding object. Modern collision handling
            // can permanently stop a particle against the jar lid or the altar platform.
            if(kind!=2)escape(); move(xd,yd,zd); xd*=.985; yd*=.985; zd*=.985;
            var delta=target.subtract(x,y,z); double distance=delta.length();
            if (distance<1.0e-8) { remove(); return; }
            if (kind==2) {
                xd=Mth.clamp(xd,-.05,.05); yd=Mth.clamp(yd,-.05,.05); zd=Mth.clamp(zd,-.05,.05);
                if (distance<2) size*=.98f; if (size<.2f) { remove(); return; }
                double force=.01/(distance*Math.min(1,distance)); xd+=delta.x*force; yd+=delta.y*force; zd+=delta.z*force;
            } else {
                double force=distance<4 ? .6 : .3; if (distance<4) size*=.9f;
                xd=Mth.clamp((float)(xd+delta.x/distance*force),-.35f,.35f); yd=Mth.clamp((float)(yd+delta.y/distance*force),-.35f,.35f); zd=Mth.clamp((float)(zd+delta.z/distance*force),-.35f,.35f);
            }
        }
        private void escape() {
            var pos=BlockPos.containing(x,y,z); var state=level.getBlockState(pos);
            if (state.isAir() || !level.getFluidState(pos).isEmpty() || kind==2 && !state.isRedstoneConductor(level,pos)) return;
            Direction chosen=null; double nearest=9999;
            for (var side:List.of(Direction.WEST,Direction.EAST,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH)) {
                var next=pos.relative(side); if (level.getBlockState(next).isRedstoneConductor(level,next)) continue;
                double fraction=switch(side.getAxis()) { case X->x-pos.getX(); case Y->y-pos.getY(); case Z->z-pos.getZ(); };
                double distance=side.getAxisDirection()==Direction.AxisDirection.NEGATIVE ? fraction : 1-fraction;
                if (distance<nearest) { nearest=distance; chosen=side; }
            }
            float force=random.nextFloat()*.05f+.025f,other=(random.nextFloat()-random.nextFloat())*.1f;
            if (chosen!=null) { xd=chosen.getStepX()==0 ? other : chosen.getStepX()*force; yd=chosen.getStepY()==0 ? other : chosen.getStepY()*force; zd=chosen.getStepZ()==0 ? other : chosen.getStepZ()*force; }
        }
        @Override public void render(VertexConsumer out,Camera camera,float partial) {
            render(out,camera,partial,new PoseStack());
        }
        private void render(VertexConsumer out,Camera camera,float partial,PoseStack pose) {
            // This custom pass runs outside ParticleEngine: retain the level's
            // view transform as well as the camera-relative particle position.
            pose.pushPose(); var view=camera.getPosition(); pose.translate(Mth.lerp(partial,xo,x)-view.x,Mth.lerp(partial,yo,y)-view.y,Mth.lerp(partial,zo,z)-view.z); pose.mulPose(camera.rotation());
            float u,v,U,V,radius=.1f*size,alpha=1; int light=240;
            if (kind==3) { u=sprite.getU(jitterU*4); U=sprite.getU((jitterU+1)*4); v=sprite.getV(jitterV*4); V=sprite.getV((jitterV+1)*4); light=getLightColor(partial); }
            else if (kind==2) { u=.5625f; U=.625f; v=.0625f; V=.125f; radius*=Mth.sin((age-phase)/5f)*.25f+1; alpha=.5f; }
            else { u=age%4/16f; U=u+.0624375f; v=.25f; V=.3124375f; radius*=Mth.sin(age/3f)*.5f+1; }
            ClientFocusEffects.quad(out,pose.last().pose(),radius,u,v,U,V,rCol,gCol,bCol,alpha,light);
            pose.popPose();
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    }
}
