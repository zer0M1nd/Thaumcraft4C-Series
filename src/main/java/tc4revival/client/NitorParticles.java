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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public  class NitorParticles {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final List<Mote> PARTICLES = new ArrayList<>();
    private static ClientLevel current;
    private NitorParticles() {}
    private static boolean prepare(Level level) {
        var mc = Minecraft.getInstance(); if (level == null || level != mc.level) return false;
        if (current != mc.level) { PARTICLES.clear(); current = mc.level; } return true;
    }
    public static void emit(Level level, BlockPos pos, boolean sparkle) {
        emit(level,pos,sparkle,-1);
    }
    public static void emit(Level level,BlockPos pos,boolean sparkle,int color) {
        if (!prepare(level)) return; var mc = Minecraft.getInstance(); var center = Vec3.atCenterOf(pos);
        int range = Minecraft.useFancyGraphics() ? 50 : 25;
        if (mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(center) > range * range) return;
        int quality = switch (mc.options.particles().get()) { case ALL -> 2; case DECREASED -> 1; case MINIMAL -> 0; };
        if (sparkle) add(new Mote(current, center, 2,color));
        else {
            if (level.random.nextInt(9 - quality) == 0) add(new Mote(current, center, 0,color));
            if (level.random.nextInt(15 - quality * 2) == 0) add(new Mote(current, center, 1,color));
        }
    }
    private static void add(Mote mote) { if (PARTICLES.size() >= 2000) PARTICLES.remove(0); PARTICLES.add(mote); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { PARTICLES.clear(); current = null; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !prepare(Minecraft.getInstance().level) || Minecraft.getInstance().isPaused()) return;
        PARTICLES.forEach(Mote::tick); PARTICLES.removeIf(mote -> !mote.isAlive());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !prepare(Minecraft.getInstance().level)) return;
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource(); var type = TCRenderTypes.runes(TEXTURE); var out = TCRenderTypes.buffer(buffers,type);
        for (var mote : PARTICLES) mote.render(out, event.getCamera(), event.getPartialTick(), event.getPoseStack());
        TCRenderTypes.endBatch(buffers,type);
    }
    private static  class Mote extends Particle {
        private final float size;
        private final boolean sparkle;
        Mote(ClientLevel level, Vec3 position, int kind,int color) {
            super(level, position.x, position.y, position.z); sparkle = kind == 2;
            size = (random.nextFloat() * .5f + .5f) * 2 * (kind == 0 ? .5f : kind == 1 ? .25f : 1);
            lifetime = sparkle ? 9 : (int) (36 / (random.nextDouble() * .3 + .7));
            gravity = sparkle ? .05f : kind == 0 ? -.025f : -.02f;
            hasPhysics = !sparkle; friction = sparkle ? .908f : .98f; setSize(sparkle ? .01f : .1f, sparkle ? .01f : .1f);
            if (sparkle) {
                rCol = .8f + random.nextFloat() * .2f; gCol = .8f + random.nextFloat() * .2f; bCol = .8f + random.nextFloat() * .2f;
                xd = (random.nextFloat() - random.nextFloat()) / 3 / lifetime;
                yd = (random.nextFloat() - random.nextFloat()) / 3 / lifetime;
                zd = (random.nextFloat() - random.nextFloat()) / 3 / lifetime;
            } else {
                rCol = kind == 0 ? .7f + random.nextFloat() * .3f : .5f + random.nextFloat() * .3f;
                gCol = kind == 0 ? .2f : .5f + random.nextFloat() * .3f; bCol = .2f;
                double spread = kind == 0 ? .4 : .2;
                xd = (random.nextDouble() - .5) * spread / lifetime; zd = (random.nextDouble() - .5) * spread / lifetime;
            }
            if(color!=-1){float brightness=.8f+random.nextFloat()*.2f;rCol=(color>>16&255)/255f*brightness;gCol=(color>>8&255)/255f*brightness;bCol=(color&255)/255f*brightness;}
        }
        @Override public void render(VertexConsumer out, Camera camera, float partial) {
            render(out,camera,partial,new PoseStack());
        }
        private void render(VertexConsumer out, Camera camera, float partial, PoseStack pose) {
            pose.pushPose(); var view = camera.getPosition();
            pose.translate(Mth.lerp(partial, xo, x) - view.x, Mth.lerp(partial, yo, y) - view.y, Mth.lerp(partial, zo, z) - view.z);
            pose.mulPose(camera.rotation()); float u = sparkle ? age / 3 % 4 / 16f : 0;
            float radius = size * (sparkle ? .1f : .5f) * (lifetime - age) / lifetime;
            ClientFocusEffects.quad(out, pose.last().pose(), radius, u, sparkle ? .25f : .875f, u + (sparkle ? .0624375f : .125f),
                    sparkle ? .3124375f : 1, rCol, gCol, bCol, sparkle ? 1 : .5f, 240);
            pose.popPose();
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    }
}
