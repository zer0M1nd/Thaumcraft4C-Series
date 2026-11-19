package tc4revival.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.TCArcane;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class NodeHighlight {
    private NodeHighlight() {}
    @SubscribeEvent public static void block(RenderHighlightEvent.Block event) {
        var level=net.minecraft.client.Minecraft.getInstance().level;
        if(level!=null) {
            var state=level.getBlockState(event.getTarget().getBlockPos());
            if(state.is(TCArcane.ENERGIZED_NODE.get()) || state.is(TCArcane.AURA_NODE.get())) event.setCanceled(true);
        }
    }
}
