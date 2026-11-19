package thaumicfutures.compat.bees;

import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

final class LegacyHiveColors {
    static void register(IEventBus bus) {
        bus.addListener((RegisterColorHandlersEvent.Item event) -> {
            event.register((stack,tint) -> tint == 0 ? 11637208 : 3490875, MagicBeeProducts.stack("bee_comb_legacy_forgotten").getItem());
            event.register((stack,tint) -> 8744447, MagicBeeProducts.stack("bee_wax_amnesic").getItem());
        });
    }
}
