package tc4revival.content;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Invoked before the runic shield consumes incoming damage, matching the original shared callback. */
public  class FortressMasks {
    private FortressMasks() {}
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        Entity direct=event.getSource().getDirectEntity();
        if (direct instanceof Player attacker && mask(attacker)==2 && attacker.getRandom().nextFloat()<event.getAmount()/12f) attacker.heal(1);
        if (event.getEntity() instanceof Player victim && direct instanceof LivingEntity attacker
                && mask(victim)==1 && victim.getRandom().nextFloat()<event.getAmount()/10f)
            attacker.addEffect(new MobEffectInstance(MobEffects.WITHER,80));
    }
    private static int mask(Player player) {
        var helmet=player.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.getItem() instanceof FortressArmorItem ? FortressArmorItem.mask(helmet) : -1;
    }
}
