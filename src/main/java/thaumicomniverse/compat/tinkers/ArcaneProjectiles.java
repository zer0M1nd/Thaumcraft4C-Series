package thaumicomniverse.compat.tinkers;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.*;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;

/** Remember the launcher's traits on the projectile; changing the held tool cannot grant them retroactively. */
public final class ArcaneProjectiles {
    public static final String IMBUED="thaumicomniverse:imbued",APOCALYPSE="thaumicomniverse:apocalypse";
    private static final java.lang.reflect.Method CONTEXT_PROJECTILE=contextProjectile();
    private ArcaneProjectiles() {}
    private static java.lang.reflect.Method contextProjectile() {
        try { return ToolAttackContext.class.getMethod("getProjectile"); }
        catch(NoSuchMethodException absent) { return null; } // 3.10 contexts represent melee attacks only.
    }
    public static boolean tagged(ToolAttackContext context) {
        if(CONTEXT_PROJECTILE==null)return false;
        try { return tagged((Projectile)CONTEXT_PROJECTILE.invoke(context)); }
        catch(ReflectiveOperationException failure) { throw new IllegalStateException("Cannot read TConstruct attack projectile",failure); }
    }
    public static boolean tagged(Projectile projectile) {
        return projectile!=null && (projectile.getPersistentData().contains(IMBUED)||projectile.getPersistentData().contains(APOCALYPSE));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void damage(LivingHurtEvent event) {
        if(!ArcaneMaterials.enabled() || event.getAmount()<=0 || !(event.getSource().getDirectEntity() instanceof Projectile projectile)
                || !(projectile.getOwner() instanceof Player player) || player.level().isClientSide)return;
        var data=projectile.getPersistentData();float damage=event.getAmount();int level=data.getInt(IMBUED);
        if(level>0 && ArcaneMaterials.vis(player,ArcaneMaterials.costs(10,level),true,false))damage*=1+.3f*level;
        if(data.getBoolean(APOCALYPSE) && ArcaneMaterials.flying(player))damage*=1.4f;
        event.setAmount(damage);
    }
}
