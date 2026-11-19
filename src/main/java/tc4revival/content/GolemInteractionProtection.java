package tc4revival.content;

import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;

/** Legacy EntityInteractEvent guard, before either mob or held-item interaction. */
@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID)
public  class GolemInteractionProtection {
    private GolemInteractionProtection() {}

    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof GolemEntity golem)) return;
        String owner = golem.ownerName();
        if (owner.isEmpty() || owner.equals(event.getEntity().getGameProfile().getName())) return;
        if (!event.getLevel().isClientSide)
            event.getEntity().sendSystemMessage(Component.translatable("thaumcraft.golem.not_master"));
        event.setCanceled(true);
    }
}
