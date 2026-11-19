package tc4revival.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.item.ItemAspects;

@Mod.EventBusSubscriber(modid = "thaumcraft")
public  class VisRepair {
    private VisRepair() {}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.isCreative() || player.tickCount % 40 != 0) return;
        for (int slot = 0; slot < 9; slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.is(LegacyRepairEnchantment.HOTBAR)) repair(stack, player);
        }
        for (var stack : player.getArmorSlots()) repair(stack, player);
    }
    public static void repair(ItemStack stack, ServerPlayer player) {
        if (!stack.is(LegacyRepairEnchantment.REPAIRABLE) || !stack.isDamaged()) return;
        int level = Math.min(2, stack.getEnchantmentLevel(TCEnchantments.REPAIR.get())); if (level <= 0) return;
        // TC4 uses object tags here, without equipment or enchantment scan bonuses.
        var base = ItemAspects.baseSession().base(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()).orElse(null);
        if (base == null || base.isEmpty()) return;
        var costs = costs(TCAspects.catalog(), base, level);
        if ((!(stack.getItem() instanceof RepairableItem repair) || repair.beforeVisRepair(stack, player, level))
                && WandInventory.consume(player, costs.view())) stack.setDamageValue(Math.max(0, stack.getDamageValue() - level));
    }
    public static AspectAmounts costs(AspectCatalog catalog, AspectAmounts base, int level) {
        var result = new AspectAmounts();
        if (level <= 0) return result;
        int repairLevel = Math.min(2, level);
        catalog.toPrimals(base, AspectCatalog.Reduction.SUM).view().forEach((aspect, amount) -> result.merge(aspect, (int) Math.sqrt(amount * 2.0) * repairLevel));
        return result;
    }
}
