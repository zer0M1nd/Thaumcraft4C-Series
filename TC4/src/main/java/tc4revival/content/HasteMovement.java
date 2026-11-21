package tc4revival.content;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft")
public  class HasteMovement {
    private HasteMovement() {}
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        int level=EnchantmentHelper.getItemEnchantmentLevel(TCEnchantments.HASTE.get(),player.getItemBySlot(EquipmentSlot.FEET));
        float acceleration=LegacyEnchantmentRules.hasteAcceleration(level,player.zza>0,
                player.getAbilities().flying,player.hasImpulse,player.isInWater());
        if (acceleration>0) player.moveRelative(acceleration,new Vec3(0,0,1));
    }
}
