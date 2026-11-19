package thaumicfutures.autocaster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.api.wands.WandActors;

/** Preserve real projectile attribution and protect excluded targets, including delayed effects. */
@Mod.EventBusSubscriber(modid="thaumicfutures")
public final class AutocasterEffects {
    private static final String CASTER="thaumicfutures:autocaster",OWNER="thaumicfutures:autocaster_owner";
    public static boolean belongsTo(Entity entity,AutocasterEntity turret) {
        return entity.getPersistentData().hasUUID(CASTER) && entity.getPersistentData().getUUID(CASTER).equals(turret.getUUID());
    }
    private static AutocasterEntity caster(Entity entity) {
        entity=WandActors.resolve(entity);
        if(entity instanceof AutocasterEntity turret)return turret;
        if(entity!=null && entity.level() instanceof ServerLevel level && entity.getPersistentData().hasUUID(CASTER)
                && level.getEntity(entity.getPersistentData().getUUID(CASTER)) instanceof AutocasterEntity turret)return turret;
        return null;
    }
    private static boolean denied(Entity source,LivingEntity target) {
        var turret=caster(source);if(turret!=null)return !turret.canAttack(target);
        if(source==null)return false;
        var data=source.getPersistentData();
        // A projectile can outlive its source chunk. Its owner remains protected after reload.
        return data.hasUUID(OWNER) && (data.getUUID(OWNER).equals(target.getUUID())
                || target instanceof OwnableEntity owned && data.getUUID(OWNER).equals(owned.getOwnerUUID()));
    }
    @SubscribeEvent public static void spawned(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide)return;
        var entity=event.getEntity();var turret=AutocasterCombat.casting();
        if(entity instanceof Projectile projectile) {
            var resolved=caster(projectile.getOwner());if(resolved!=null) { turret=resolved;projectile.setOwner(turret); }
        }
        if(turret!=null && (entity instanceof Projectile || entity instanceof Mob) && entity!=turret) {
            entity.getPersistentData().putUUID(CASTER,turret.getUUID());
            if(turret.getOwnerUUID()!=null)entity.getPersistentData().putUUID(OWNER,turret.getOwnerUUID());
            if(entity instanceof Mob summoned)summoned.setTarget(turret.getTarget());
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public static void damage(LivingAttackEvent event) {
        var turret=AutocasterCombat.casting();var source=event.getSource();
        if(turret!=null && !turret.canAttack(event.getEntity()) || denied(source.getEntity(),event.getEntity())
                || denied(source.getDirectEntity(),event.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent public static void impact(ProjectileImpactEvent event) {
        if(event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target
                && (denied(event.getProjectile(),target) || denied(event.getProjectile().getOwner(),target)))event.setCanceled(true);
    }
    @SubscribeEvent public static void target(LivingChangeTargetEvent event) {
        if(event.getNewTarget()!=null && denied(event.getEntity(),event.getNewTarget()))event.setNewTarget(null);
    }
    @SubscribeEvent public static void effect(MobEffectEvent.Applicable event) {
        var turret=AutocasterCombat.casting();
        if(turret!=null && !turret.canAttack(event.getEntity()) && event.getEffectInstance().getEffect().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL)
            event.setResult(Event.Result.DENY);
    }
    private AutocasterEffects() {}
}
