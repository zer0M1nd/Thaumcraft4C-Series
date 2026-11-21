package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.config.TCConfig;
import tc4revival.content.ForestBiomeColors;
import tc4revival.content.TCBiomes;

/** Local preference only: never modify the registry data received from the server. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class ForestBiomePalette {
    private ForestBiomePalette() {}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.START) return;
        var client=Minecraft.getInstance(); var level=client.level;
        if (level==null) { ForestBiomeColors.update(null,false); return; }
        var forest=level.registryAccess().registryOrThrow(Registries.BIOME).get(TCBiomes.MAGICAL_FOREST);
        if (ForestBiomeColors.update(forest,TCConfig.CLIENT.blueMagicalForest.get())) {
            level.clearTintCaches();
            client.levelRenderer.allChanged();
        }
    }
}
