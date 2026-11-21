package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

/** Vanilla smoke supplies its sprites, animation, physics and resource-pack support. */
public  class MirrorTransferParticles {
    private MirrorTransferParticles() {}
    private static boolean visible(Level level, BlockPos pos) {
        var mc = Minecraft.getInstance();
        return level != null && level == mc.level && !mc.isPaused() && mc.getCameraEntity() != null
                && mc.getCameraEntity().distanceToSqr(pos.getCenter()) <= 64 * 64;
    }
    public static void emit(Level level, BlockPos pos, Direction face) {
        if (!visible(level, pos)) return;
        var mc = Minecraft.getInstance();
        int count = switch (mc.options.particles().get()) { case ALL -> 2; case DECREASED -> 1; case MINIMAL -> 0; };
        var random = level.random;
        for (int i = 0; i < count; i++) {
            var smoke = mc.particleEngine.createParticle(ParticleTypes.SMOKE,
                    pos.getX() + .33 + random.nextFloat() * .33 - face.getStepX() / 2.0,
                    pos.getY() + .33 + random.nextFloat() * .33 - face.getStepY() / 2.0,
                    pos.getZ() + .33 + random.nextFloat() * .33 - face.getStepZ() / 2.0,
                    face.getStepX() * .05, face.getStepY() * .05, face.getStepZ() * .05);
            if (smoke != null) smoke.setColor(0, 0, 0);
        }
    }
    public static void brainFull(Level level, BlockPos pos) {
        if (!visible(level, pos)) return;
        var mc = Minecraft.getInstance();
        if (mc.options.particles().get() == net.minecraft.client.ParticleStatus.MINIMAL) return;
        var random = level.random;
        var smoke = mc.particleEngine.createParticle(ParticleTypes.SMOKE,
                pos.getX() + .3 + random.nextFloat() * .4, pos.getY() + .9,
                pos.getZ() + .3 + random.nextFloat() * .4, 0, 0, 0);
        if (smoke != null) smoke.setColor(0, .4f + random.nextFloat() * .1f, .3f + random.nextFloat() * .2f);
    }
}
