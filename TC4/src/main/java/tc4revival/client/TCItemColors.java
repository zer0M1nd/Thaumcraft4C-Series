package tc4revival.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;
import tc4revival.content.ShardType;
import tc4revival.content.TCItems;

@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public  class TCItemColors {
    private TCItemColors() {}
    @SubscribeEvent public static void register(RegisterColorHandlersEvent.Item event) {
        for (var type : ShardType.values()) {
            event.register((stack, layer) -> layer == 0 ? type.tint() : -1, TCItems.shard(type));
        }
    }
}
