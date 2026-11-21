package tc4revival.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.ChampionMobs;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class ChampionParticles {
    private static final ResourceLocation ATLAS=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final java.util.ArrayList<Mote> MOTES=new java.util.ArrayList<>();
    private static ClientLevel current;
    @SubscribeEvent public static void living(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Monster mob) || !(mob.level() instanceof ClientLevel level) || !mob.isAlive()) return;
        var kind=ChampionMobs.kind(mob); if (kind==null) return; var r=level.random;
        if (kind==ChampionMobs.Kind.INFESTED) {
            // Keep the champion tick path self-contained; it must not link the slime entity effect helper.
            if (r.nextBoolean()) Minecraft.getInstance().particleEngine.add(new InfestedFragment(level,mob.getX(),mob.getY()+mob.getBbHeight()*.5,mob.getZ()));
            return;
        }
        boolean skip=switch (kind) {
            case ARMORED -> r.nextInt(4)!=0; case MIGHTY -> r.nextFloat()>.3f; case VAMPIRIC -> r.nextFloat()>.2f; case FIERY -> false;
            default -> r.nextBoolean();
        };
        if (skip) return;
        double x=mob.getBoundingBox().minX+r.nextFloat()*mob.getBbWidth(),z=mob.getBoundingBox().minZ+r.nextFloat()*mob.getBbWidth();
        double y=mob.getBoundingBox().minY+r.nextFloat()*mob.getBbHeight()/(kind==ChampionMobs.Kind.BOLD ? 3 : 1);
        if (kind==ChampionMobs.Kind.BOLD) { EldritchSparkParticles.emit(level,x,y,z,.2f,.3f-r.nextFloat()*.1f,0,.8f+r.nextFloat()*.2f,1); return; }
        float red,green,blue,alpha,size; int start,frames,interval=1,life; double dy=0; boolean loop=false;
        switch (kind) {
            case SPINED -> { start=176+r.nextInt(4)*3; frames=3; red=.5f+r.nextFloat()*.2f; green=.1f+r.nextFloat()*.2f; blue=.1f+r.nextFloat()*.2f; alpha=.7f; life=3; size=1.2f+r.nextFloat()*.3f; }
            case ARMORED -> { start=112; frames=9; red=.9f; green=.9f; blue=.9f+r.nextFloat()*.1f; alpha=.7f; life=5+r.nextInt(4); size=.6f+r.nextFloat()*.2f; }
            case MIGHTY -> { start=176+r.nextInt(4)*3; frames=3; red=.8f+r.nextFloat()*.2f; green=.8f+r.nextFloat()*.2f; blue=.8f+r.nextFloat()*.2f; alpha=.7f; life=4+r.nextInt(3); size=1+r.nextFloat()*.3f; }
            case GRIM -> { start=160; frames=10; red=r.nextFloat()*.2f; green=r.nextFloat()*.2f; blue=r.nextFloat()*.2f; alpha=.8f; life=8+r.nextInt(4); size=.6f+r.nextFloat()*.4f; dy=-.02; }
            case WARDED -> { start=21; frames=4; loop=true; red=.5f+r.nextFloat()*.1f; green=.5f+r.nextFloat()*.1f; blue=.5f+r.nextFloat()*.1f; alpha=.6f; life=4+r.nextInt(4); size=.8f+r.nextFloat()*.3f; }
            case WARP -> { start=72; frames=8; loop=true; red=.8f+r.nextFloat()*.2f; green=0; blue=.9f+r.nextFloat()*.1f; alpha=.7f; life=10+r.nextInt(4); size=.6f+r.nextFloat()*.4f; }
            case UNDYING -> { start=21; frames=4; loop=true; red=.1f+r.nextFloat()*.1f; green=.8f+r.nextFloat()*.2f; blue=.1f+r.nextFloat()*.1f; alpha=.9f; life=4+r.nextInt(4); size=.5f+r.nextFloat()*.2f; dy=.03; }
            case FIERY -> { start=160; frames=10; red=.9f+r.nextFloat()*.1f; green=blue=1; alpha=.7f; life=8+r.nextInt(4); size=.7f+r.nextFloat()*.2f; dy=.03; }
            case SICKLY -> { start=1; frames=4; interval=2; red=.2f; green=.6f+r.nextFloat()*.1f; blue=.2f+r.nextFloat()*.1f; alpha=.5f; life=5+r.nextInt(4); size=.9f+r.nextFloat()*.3f; dy=-.02; }
            case VENOMOUS -> { start=147; frames=4; red=.2f; green=.6f+r.nextFloat()*.1f; blue=.2f+r.nextFloat()*.1f; alpha=.7f; life=8+r.nextInt(4); size=.5f+r.nextFloat()*.2f; dy=.02; }
            case VAMPIRIC -> { start=147; frames=4; red=.9f+r.nextFloat()*.1f; green=blue=0; alpha=.9f; life=8+r.nextInt(4); size=.5f+r.nextFloat()*.2f; }
            default -> { return; }
        }
        if (current!=level) { MOTES.clear(); current=level; }
        if (MOTES.size()>=2000) MOTES.remove(0);
        MOTES.add(new Mote(level,x,y,z,dy,red,green,blue,alpha,size,start,frames,interval,life,loop));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return; var mc=Minecraft.getInstance();
        if (current!=mc.level) { MOTES.clear(); current=mc.level; }
        if (current==null || mc.isPaused()) return; MOTES.forEach(Mote::tick); MOTES.removeIf(mote->!mote.isAlive());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || current==null) return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource(); var type=TCRenderTypes.softParticles(ATLAS); var out=TCRenderTypes.buffer(buffers,type);
        MOTES.forEach(mote->mote.renderWorld(out,event.getCamera(),event.getPartialTick(),event.getPoseStack())); TCRenderTypes.endBatch(buffers,type);
    }
    /** Legacy infested champion: a shrinking purple slime-ball fragment, not a registered custom particle ID. */
    private static class InfestedFragment extends TextureSheetParticle {
        private final float u,v;
        InfestedFragment(ClientLevel level,double x,double y,double z) {
            super(level,x,y,z,0,0,0);
            setSprite(Minecraft.getInstance().getItemRenderer().getItemModelShaper()
                    .getItemModel(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SLIME_BALL)).getParticleIcon());
            u=random.nextFloat()*3;v=random.nextFloat()*3;
            rCol=.7f;gCol=0;bCol=1;alpha=.4f;gravity=1;quadSize*=.5f;
            lifetime=(int)(66/(level.random.nextFloat()*.9f+.1f));
        }
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.TERRAIN_SHEET;}
        @Override protected float getU0(){return sprite.getU(u*4);}
        @Override protected float getU1(){return sprite.getU((u+1)*4);}
        @Override protected float getV0(){return sprite.getV(v*4);}
        @Override protected float getV1(){return sprite.getV((v+1)*4);}
        @Override public float getQuadSize(float partial){return quadSize*(1-age/(float)lifetime);}
        @Override public void render(VertexConsumer out,Camera camera,float partial){alpha=.4f*(1-age/(float)lifetime);super.render(out,camera,partial);}
    }
    private static  class Mote extends Particle {
        final float size; final int start,frames,interval; final boolean loop;
        Mote(ClientLevel level,double x,double y,double z,double dy,float r,float g,float b,float alpha,float size,int start,int frames,int interval,int life,boolean loop) {
            super(level,x,y,z); hasPhysics=false; setSize(.1f,.1f); yd=dy; rCol=r; gCol=g; bCol=b; this.alpha=alpha; this.size=size;
            this.start=start; this.frames=frames; this.interval=interval; lifetime=life; this.loop=loop;
        }
        @Override public void tick() { xo=x; yo=y; zo=z; if (age++>=lifetime) { remove(); return; } move(0,yd,0); yd*=.98; }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
        @Override public void render(VertexConsumer out,Camera camera,float partial) { renderWorld(out,camera,partial,new PoseStack()); }
        void renderWorld(VertexConsumer out,Camera camera,float partial,PoseStack worldView) {
            var pose=WorldEffectPose.copy(worldView); var view=camera.getPosition(); pose.translate(x-view.x,Mth.lerp(partial,yo,y)-view.y,z-view.z); pose.mulPose(camera.rotation());
            int frame=start+(loop ? age/interval%frames : (int)Math.min(frames*age/(float)lifetime,frames-1));
            float u=frame%16/16f,v=frame/16/16f,opacity=age<=1 || age>=lifetime-1 ? alpha/2 : alpha;
            ClientFocusEffects.quad(out,pose.last().pose(),size*.1f,u,v,u+.0624375f,v+.0624375f,rCol,gCol,bCol,opacity,getLightColor(partial));
        }
    }
    private ChampionParticles() {}
}
