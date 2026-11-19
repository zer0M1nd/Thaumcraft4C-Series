package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Modern spell particles retain the purple taint burst without an old slime texture. */
public  class TaintBottleEffects {
    private TaintBottleEffects() {}
    public static void burst(Entity bottle) { burst(bottle, 200, true); }
    static void burst(Entity bottle, int maximum, boolean glass) {
        var mc = Minecraft.getInstance();
        if (bottle.level() != mc.level || mc.level == null) return;
        var level = mc.level;
        int count = switch (mc.options.particles().get()) { case ALL -> maximum; case DECREASED -> maximum / 2; case MINIMAL -> 0; };
        if (mc.getCameraEntity() == null || mc.getCameraEntity().distanceToSqr(bottle) > 64 * 64) count = 0;
        var random = level.random;
        for (int i = 0; i < count; i++) {
            var particle = mc.particleEngine.createParticle(ParticleTypes.WITCH,
                    bottle.getX(), bottle.getY() + random.nextFloat() * bottle.getBbHeight(), bottle.getZ(), 0, 0, 0);
            if (particle == null) continue;
            double dx = random.nextDouble() * 2 - 1, dy = random.nextDouble() * 2 - 1, dz = random.nextDouble() * 2 - 1;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double speed = (random.nextDouble() + random.nextDouble() + 1) * .15;
            if (length > 0) particle.setParticleSpeed(dx / length * speed, dy / length * speed + .1, dz / length * speed);
            particle.setColor(random.nextBoolean() ? .6f : .3f, 0, .3f);
            particle.scale(.5f);
        }
        if (!glass) return;
        for (int i = 0; i < 8; i++) level.addParticle(new ItemParticleOption(ParticleTypes.ITEM,
                new ItemStack(tc4revival.content.TCItems.TAINT_BOTTLE.get())), bottle.getX(), bottle.getY(), bottle.getZ(),
                random.nextGaussian() * .15, random.nextDouble() * .2, random.nextGaussian() * .15);
        level.playLocalSound(bottle.getX(), bottle.getY(), bottle.getZ(), net.minecraft.sounds.SoundEvents.SPLASH_POTION_BREAK,
                net.minecraft.sounds.SoundSource.NEUTRAL, 1, .9f + random.nextFloat() * .1f, false);
    }
}
