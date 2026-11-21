package temodern.client;

import appeng.api.client.StorageCellModels;
import appeng.items.storage.BasicStorageCell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import temodern.TE;

/** Shares AE2's live cell status for inventory LEDs and drive LEDs. */
@Mod.EventBusSubscriber(modid = TE.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class StorageCellRendering {
    public static ResourceLocation driveModel(String tier) {
        return ResourceLocation.fromNamespaceAndPath(TE.ID, "block/drive/essentia_cell_" + tier);
    }

    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        // The model registry is persistent, unlike the resource reload events below.
        event.enqueueWork(() -> TE.CELLS.forEach((tier, item) ->
                StorageCellModels.registerModel(item.get(), driveModel(tier))));
    }

    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) {
        TE.CELLS.keySet().forEach(tier -> event.register(driveModel(tier)));
    }

    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event) {
        event.register(BasicStorageCell::getColor,
                TE.CELLS.values().stream().map(item -> item.get()).toArray(Item[]::new));
    }
}
