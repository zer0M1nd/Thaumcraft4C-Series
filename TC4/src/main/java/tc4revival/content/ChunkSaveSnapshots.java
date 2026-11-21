package tc4revival.content;

import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Isolate vanilla heightmap arrays before asynchronous chunk storage. */
@Mod.EventBusSubscriber(modid = "thaumcraft")
public final class ChunkSaveSnapshots {
    private ChunkSaveSnapshots() {}

    @SubscribeEvent
    public static void save(ChunkDataEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        // ChunkSerializer wraps Heightmap.getRawData() without cloning its long[].
        // Terrain edits continue changing those arrays after ChunkMap queues the save.
        var data = event.getData();
        if (data.contains("Heightmaps", Tag.TAG_COMPOUND))
            data.put("Heightmaps", data.getCompound("Heightmaps").copy());
    }
}
