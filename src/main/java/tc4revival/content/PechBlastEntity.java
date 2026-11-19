package tc4revival.content;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

public  class PechBlastEntity extends ThrowableProjectile {
    private float strength;
    private int duration;
    private boolean nightshade;
    public PechBlastEntity(EntityType<? extends PechBlastEntity> type, Level level) { super(type, level); }
    public PechBlastEntity(Level level, LivingEntity owner, int strength, int duration, boolean nightshade) { this(level, owner, (float) strength, duration, nightshade); }
    public PechBlastEntity(Level level, LivingEntity owner, float strength, int duration, boolean nightshade) {
        this(TCEntities.PECH_BLAST.get(), level); FocusProjectiles.launch(this, owner, 1.5f, 0);
        this.strength = strength; this.duration = duration; this.nightshade = nightshade;
    }
    @Override protected void defineSynchedData() {}
    @Override protected float getGravity() { return .025f; }
    @Override public void tick() {
        if (level().isClientSide) tc4revival.client.FocusProjectileEffects.pech(this, false);
        super.tick(); if (tickCount > 500) discard();
    }
    @Override protected void onHit(HitResult hit) {
        if (isRemoved()) return;
        if (level().isClientSide) { tc4revival.client.FocusProjectileEffects.pech(this, true); return; }
        for (var target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2))) {
            if (target == getOwner() || net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().equals("thaumcraft:pech")) continue;
            target.hurt(level().damageSources().thrown(this, getOwner()), strength + 2);
            int amplifier = FocusPotency.discrete(strength, random);
            int selected = nightshade ? -1 : random.nextInt(3);
            if (selected < 0 || selected == 0) target.addEffect(new MobEffectInstance(MobEffects.POISON, 100 + duration * 40, amplifier));
            if (selected < 0 || selected == 1) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100 + duration * 40, amplifier + 1));
            if (selected < 0 || selected == 2) target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100 + duration * 40, amplifier));
        }
        discard();
    }
    // As in TC4, only base projectile data survives a chunk reload.
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
