package heartwork.content;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;

public class StrawEntity extends PathfinderMob {
    public StrawEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); setNoAi(true); setPersistenceRequired(); }
    public static AttributeSupplier.Builder attributes() { return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,0); }
    @Override protected void registerGoals() {}
    @Override public void tick() {
        // The original training dummy does not run mob AI, movement or gravity.
        if (hurtTime > 0) hurtTime--;
        if (invulnerableTime > 0) invulnerableTime--;
        setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || level().isClientSide || isRemoved()) return false;
        if (amount >= 20) {
            playSound(SoundEvents.BLAZE_HURT,1,1);
            discard();
            return true;
        }
        playSound(SoundEvents.GRASS_STEP,1,1);
        return false;
    }
    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
}
