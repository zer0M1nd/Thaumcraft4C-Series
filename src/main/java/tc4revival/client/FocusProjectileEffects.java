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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.FrostShardEntity;

@Mod.EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public  class FocusProjectileEffects {
    private static final ResourceLocation ATLAS = ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final ResourceLocation ICE = ResourceLocation.parse("thaumcraft:textures/block/frostshard.png");
    private static final List<Mote> MOTES = new ArrayList<>();
    private static ClientLevel current;
    private FocusProjectileEffects() {}
    private static boolean prepare(Entity entity) {
        var level = Minecraft.getInstance().level;
        if (level == null || entity.level() != level) return false;
        if (current != level) { MOTES.clear(); current = level; }
        return true;
    }
    private static void add(Mote mote) { if (MOTES.size() >= 2000) MOTES.remove(0); MOTES.add(mote); }
    public static void impact(Entity source, BlockState state, int count, boolean ice) {
        if (!prepare(source)) return;
        for (int i = 0; i < count; i++) {
            if (ice) add(new Mote(current, source.position(), 2));
            else current.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), source.getX(), source.getY(), source.getZ(),
                    (current.random.nextFloat() - .5) * 4, .5, (current.random.nextFloat() - .5) * 4);
        }
    }
    public static void frost(FrostShardEntity source) {
        if (!prepare(source)) return;
        float spread = source.damage() / 10;
        int chance = switch (Minecraft.getInstance().options.particles().get()) { case ALL -> 4; case DECREASED -> 2; case MINIMAL -> 0; };
        for (int i = 0; i < source.frost(); i++) {
            var at = source.position().add((current.random.nextFloat() * 2 - 1) * spread,
                    (current.random.nextFloat() * 2 - 1) * spread, (current.random.nextFloat() * 2 - 1) * spread);
            if (current.random.nextInt(6) < chance) add(new Mote(current, at, 0));
        }
    }
    public static void fireTrail(Entity source) {
        if (!prepare(source)) return;
        add(new Mote(current, new Vec3(source.xo, source.yo, source.zo).add((current.random.nextFloat() - current.random.nextFloat()) * .3,
                (current.random.nextFloat() - current.random.nextFloat()) * .3, (current.random.nextFloat() - current.random.nextFloat()) * .3), 1));
    }
    public static void batTrail(Entity source) {
        if (!prepare(source)) return;
        var mote = new Mote(current, new Vec3(source.xo, source.yo + source.getBbHeight() / 2, source.zo).add(jitter(.1f)), 1);
        mote.scale(.5f); add(mote);
    }
    private static Vec3 jitter(float spread) {
        var r = current.random;
        return new Vec3((r.nextFloat() - r.nextFloat()) * spread, (r.nextFloat() - r.nextFloat()) * spread, (r.nextFloat() - r.nextFloat()) * spread);
    }
    public static void pech(Entity source, boolean impact) {
        if (!prepare(source)) return;
        double limit = Minecraft.useFancyGraphics() ? 50 : 25;
        if (Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(source.position()) > limit * limit) return;
        for (int i = 0; i < (impact ? 9 : 3); i++) {
            for (int kind = 3; kind <= (impact ? 5 : 4); kind++) {
                var offset = jitter(impact ? .3f : .2f);
                var center = !impact && kind == 4 ? source.position().add(source.xo, source.yo, source.zo).scale(.5) : source.position();
                var mote = new Mote(current, center.add(offset), kind);
                if (impact) mote.aim(center.add(offset.scale(8)));
                add(mote);
            }
            if (!impact) {
                int chance = switch (Minecraft.getInstance().options.particles().get()) { case ALL -> 4; case DECREASED -> 2; case MINIMAL -> 0; };
                var at = source.position().add(jitter(.1f));
                if (current.random.nextInt(6) < chance) add(new Mote(current, at, 6));
            }
        }
    }
    public static void alumentum(Entity source) {
        if (!prepare(source)) return;
        double range=Minecraft.useFancyGraphics() ? 50 : 25;
        if (Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(source.position())>range*range) return;
        for (int index=0;index<3;index++) {
            add(new Mote(current,source.position().add(jitter(.3f)),8));
            add(new Mote(current,source.position().add(source.xo,source.yo,source.zo).scale(.5).add(jitter(.3f)),8));
            int chance=switch (Minecraft.getInstance().options.particles().get()) { case ALL->4; case DECREASED->2; case MINIMAL->0; };
            var at=source.position().add(jitter(.1f)); if (current.random.nextInt(6)<chance) add(new Mote(current,at,7));
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { MOTES.clear(); current = null; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.START || mc.isPaused()) return;
        if (current != mc.level) { MOTES.clear(); current = mc.level; }
        MOTES.forEach(Mote::tick); MOTES.removeIf(mote -> !mote.isAlive());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || current == null || current != Minecraft.getInstance().level) return;
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        for (int layer = 0; layer < 3; layer++) {
            var type = layer == 1 ? TCRenderTypes.orb(ICE) : layer == 2 ? TCRenderTypes.softTranslucent(ATLAS) : TCRenderTypes.runes(ATLAS);
            var out = TCRenderTypes.buffer(buffers,type);
            for (var mote : MOTES) if ((mote.kind == 2 ? 1 : mote.kind == 6 || mote.kind==8 ? 2 : 0) == layer) mote.render(out, event.getCamera(), event.getPartialTick(), event.getPoseStack());
            TCRenderTypes.endBatch(buffers,type);
        }
    }
    private static  class Mote extends Particle {
        final int kind;
        final float size, jitterU, jitterV;
        Mote(ClientLevel level, Vec3 position, int kind) {
            super(level, position.x, position.y, position.z); this.kind = kind;
            jitterU = random.nextFloat() * .75f; jitterV = random.nextFloat() * .75f;
            size = kind == 1 ? 2 + random.nextFloat() : (random.nextFloat() * .5f + .5f) * (kind == 0 ? .8f : kind >= 3 && kind <= 5 || kind==8 ? .6f : kind == 6 || kind==7 ? 3 : 1);
            lifetime = kind == 1 ? 7 + random.nextInt(5) : kind == 0 || kind == 6 || kind==7 ? 18 : kind >= 3 ? (int) (36 / (random.nextDouble() * .3 + .7)) : (int) (4 / (random.nextDouble() * .9 + .1));
            hasPhysics = kind == 2; friction = kind == 0 ? .908f : .98f; gravity = kind == 0 ? .005f : kind == 2 ? 1 : 0;
            if (kind == 0) { rCol = .8f + random.nextFloat() * .2f; gCol = .8f + random.nextFloat() * .2f; bCol = .8f + random.nextFloat() * .2f; }
            if (kind >= 3 && kind <= 5) {
                gravity = .02f;
                rCol = kind == 5 ? .75f + random.nextFloat() * .25f : .2f;
                gCol = kind == 3 ? .7f + random.nextFloat() * .3f : kind == 5 ? .25f + random.nextFloat() * .25f : .2f;
                bCol = kind == 4 ? .7f + random.nextFloat() * .3f : kind == 5 ? .75f + random.nextFloat() * .25f : .2f;
            }
            if (kind == 6) { gravity = .1f; friction = .908f; rCol = random.nextFloat() * .1f; gCol = random.nextFloat() * .1f; bCol = random.nextFloat() * .1f; }
            if (kind==7) { gravity=0; friction=.908f; rCol=.8f+random.nextFloat()*.2f; gCol=.8f+random.nextFloat()*.2f; bCol=.8f+random.nextFloat()*.2f; }
            if (kind==8) { gravity=.02f; rCol=random.nextFloat()*.1f; gCol=random.nextFloat()*.1f; bCol=random.nextFloat()*.1f; }
            if (kind == 2) {
                xd = (random.nextFloat() - .5) * 4; yd = .5; zd = (random.nextFloat() - .5) * 4;
                rCol = gCol = bCol = .6f; setSize(.1f, .1f);
            }
        }
        void aim(Vec3 target) { hasPhysics = true; xd = (target.x - x) / lifetime; yd = (target.y - y) / lifetime; zd = (target.z - z) / lifetime; }
        @Override public void render(VertexConsumer out, Camera camera, float partial) {
            render(out,camera,partial,new PoseStack());
        }
        private void render(VertexConsumer out, Camera camera, float partial, PoseStack pose) {
            pose.pushPose(); var view = camera.getPosition();
            pose.translate(Mth.lerp(partial, xo, x) - view.x, Mth.lerp(partial, yo, y) - view.y, Mth.lerp(partial, zo, z) - view.z);
            pose.mulPose(camera.rotation());
            boolean sparkle = kind == 0 || kind == 6 || kind==7, wisp = kind >= 3 && kind <= 5 || kind==8;
            float u = sparkle ? age / 6 % 4 / 16f : kind == 1 ? (7 + FireTrailAnimation.frame(age, lifetime)) / 16f : wisp ? 0 : jitterU;
            float v = sparkle ? .25f : kind == 1 ? .5625f : wisp ? .875f : jitterV;
            float step = kind == 2 ? .24975f : wisp ? .125f : .0624375f;
            float radius = size * (wisp ? .5f : .1f) * (sparkle ? (lifetime - age + 1f) / lifetime : wisp ? (lifetime - age) / (float) lifetime : 1);
            int light = sparkle || wisp ? 240 : net.minecraft.client.renderer.LevelRenderer.getLightColor(level, net.minecraft.core.BlockPos.containing(x, y, z));
            ClientFocusEffects.quad(out, pose.last().pose(), radius, u, v, u + step, v + step, rCol, gCol, bCol, kind == 1 ? FireTrailAnimation.alpha(age, lifetime) : wisp ? .5f : 1, light);
            pose.popPose();
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    }
}
