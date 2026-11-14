package advancedthaumaturgy;

import java.util.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import tc4revival.content.InfusionMatrixBlockEntity;

/** The original stabilizer caps instability at 75% of the initial value. */
public class StabilizerTracker {
    private static final String KEY = "advancedthaumaturgy:stabilizer";
    private static final Set<InfusionMatrixBlockEntity> TRACKED = Collections.newSetFromMap(new WeakHashMap<>());
    public static void monitor(InfusionMatrixBlockEntity matrix) {
        if (!matrix.crafting()) return;
        if (!matrix.getPersistentData().contains(KEY)) matrix.getPersistentData().putInt(KEY, Math.max(0,Math.floorDiv(matrix.instability() * 3, 4)));
        matrix.setChanged(); TRACKED.add(matrix);
    }
    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide() || !(event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) return;
        for (var entity : chunk.getBlockEntities().values()) if (entity instanceof InfusionMatrixBlockEntity matrix && matrix.getPersistentData().contains(KEY)) TRACKED.add(matrix);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        TRACKED.removeIf(matrix -> {
            if (matrix.isRemoved() || matrix.getLevel() == null || !matrix.getLevel().hasChunkAt(matrix.getBlockPos())) return true;
            int cap = matrix.getPersistentData().getInt(KEY);
            if (!matrix.crafting()) {
                matrix.getPersistentData().remove(KEY); matrix.setChanged(); return true;
            }
            if (ATConfig.enabled("wand_upgrades") && matrix.instability() > cap) matrix.addInstability(cap - matrix.instability());
            return false;
        });
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { TRACKED.clear(); }
}
