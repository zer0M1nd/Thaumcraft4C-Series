package forgottenrelics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.*;

public final class RelicProjectiles {
    public static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ForgottenRelics.ID);
    public static final RegistryObject<EntityType<RelicProjectile>> SPELL =
            TYPES.register(
                    "spell",
                    () ->
                            EntityType.Builder.<RelicProjectile>of(
                                            RelicProjectile::new, MobCategory.MISC)
                                    .sized(.25f, .25f)
                                    .clientTrackingRange(12)
                                    .updateInterval(1)
                                    .build("forgottenrelics:spell"));

    private RelicProjectiles() {}

    public static RelicProjectile fire(ServerPlayer p, Relic relic, BlockPos lock) {
        var kind =
                switch (relic) {
                    case MISSILE_TOME -> RelicProjectile.Kind.MISSILE;
                    case CRIMSON_SPELL -> RelicProjectile.Kind.CRIMSON;
                    case ELDRITCH_SPELL -> RelicProjectile.Kind.ELDRITCH;
                    case CHAOS_TOME -> RelicProjectile.Kind.CHAOS;
                    case APOTHEOSIS -> RelicProjectile.Kind.BABYLON;
                    case LUNAR_FLARES -> RelicProjectile.Kind.LUNAR;
                    case THUNDERPEAL -> RelicProjectile.Kind.THUNDER;
                    default -> throw new IllegalArgumentException(relic.name());
                };
        var projectile = new RelicProjectile(SPELL.get(), p.level());
        projectile.setOwner(p);
        projectile.kind(kind);
        projectile.variant(p.getRandom().nextInt(12));
        projectile.setPos(p.getEyePosition().add(p.getLookAngle().scale(.75)));
        projectile.setDeltaMovement(
                p.getLookAngle().scale(kind == RelicProjectile.Kind.CRIMSON ? .25 : 1.5));
        if (kind == RelicProjectile.Kind.BABYLON) {
            var side =
                    new Vec3(p.getLookAngle().z, 0, -p.getLookAngle().x)
                            .normalize()
                            .scale(
                                    (2 + p.getRandom().nextDouble() * 10)
                                            * (p.getRandom().nextBoolean() ? 1 : -1));
            projectile.setPos(
                    p.position().add(side).add(0, .5 + p.getRandom().nextDouble() * 8, 0));
            projectile.setDeltaMovement(Vec3.ZERO);
        }
        if (kind == RelicProjectile.Kind.LUNAR)
            Relics.sound(p.level(), p.position(), RelicSounds.STARFALL.get());
        if (kind == RelicProjectile.Kind.LUNAR) {
            projectile.lock = lock;
            projectile.setPos(
                    lock.getX() + .5,
                    Math.min(p.level().getMaxBuildHeight() + 64, lock.getY() + 100),
                    lock.getZ() + .5);
            projectile.setDeltaMovement(0, -4, 0);
        }
        p.level().addFreshEntity(projectile);
        return projectile;
    }

    public static void soul(ServerPlayer p, LivingEntity source) {
        var projectile = new RelicProjectile(SPELL.get(), p.level());
        projectile.setOwner(p);
        projectile.kind(RelicProjectile.Kind.SOUL);
        projectile.setPos(source.getEyePosition());
        projectile.setDeltaMovement(source.getLookAngle());
        p.level().addFreshEntity(projectile);
    }
}
