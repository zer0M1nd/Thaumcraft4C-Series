package heartwork.content;

import heartwork.Heartwork;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Heartwork.ID)
public final class ToolRepairEvents {
    private ToolRepairEvents() {}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 800 != 0) return;
        var inventory=event.player.getInventory();
        for (int slot=0;slot<inventory.getContainerSize();slot++) {
            var stack=inventory.getItem(slot);
            if (!stack.isDamaged()) continue;
            var id=ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null || !id.getNamespace().equals(Heartwork.ID)) continue;
            if (id.getPath().startsWith("power_steel_") || (id.getPath().startsWith("highpower_steel_") && event.player.tickCount % 1600 == 0))
                stack.setDamageValue(stack.getDamageValue()-1);
        }
    }
}
