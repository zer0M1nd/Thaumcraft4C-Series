package thaumicfutures.devices;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumicfutures")
public final class CrossbowSafety {
    public static final String OWNER="thaumicfutures:crossbow_owner";
    private static boolean protectedTarget(Entity projectile,Entity target) {
        if(projectile==null)return false;
        if(projectile instanceof Projectile arrow && arrow.getOwner() instanceof AutomatedCrossbow bow && bow.isAlliedTo(target))return true;
        var data=projectile.getPersistentData();if(!data.hasUUID(OWNER))return false;
        var owner=data.getUUID(OWNER);
        if(target.getUUID().equals(owner) || target instanceof OwnableEntity owned && owner.equals(owned.getOwnerUUID()))return true;
        var player=projectile.level().getPlayerByUUID(owner);return player!=null && player.isAlliedTo(target);
    }
    @SubscribeEvent public static void impact(ProjectileImpactEvent event) {
        if(event.getRayTraceResult() instanceof EntityHitResult hit && protectedTarget(event.getProjectile(),hit.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent public static void damage(LivingAttackEvent event) { if(protectedTarget(event.getSource().getDirectEntity(),event.getEntity()))event.setCanceled(true); }
    private CrossbowSafety() {}
}
