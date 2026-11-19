package thaumicomniverse.compat.nodemap;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.aspect.*;
import tc4revival.content.AuraNodeBlockEntity;
import tc4revival.research.*;

/** Only discovered nodes are retained. All world lookups are restricted to loaded chunks. */
@Mod.EventBusSubscriber(modid = "thaumicomniverse")
public final class NodeMapRecords extends SavedData {
    private final Map<String, CompoundTag> nodes = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> discoveries = new HashMap<>();
    private final Map<UUID, Set<String>> scanCache = new HashMap<>();
    private int cacheTick = Integer.MIN_VALUE;
    public static NodeMapRecords get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(NodeMapRecords::load, NodeMapRecords::new, "thaumicomniverse_scanned_nodes");
    }
    private void cache(MinecraftServer server) {
        if (cacheTick == server.getTickCount() / 20) return;
        cacheTick = server.getTickCount() / 20; scanCache.clear();
        for (var player : server.getPlayerList().getPlayers()) scanCache.put(player.getUUID(),
                KnowledgeSavedData.get(server).snapshot(player.getUUID()).map(k -> k.scannedKeys(PlayerKnowledge.ScanKind.PHENOMENON).stream()
                        .filter(key -> key.startsWith("@NODE") || key.startsWith("#NODE"))
                        .map(key -> key.substring(1)).collect(java.util.stream.Collectors.toUnmodifiableSet())).orElse(Set.of()));
    }
    public static void setup(){thaumcraft.api.nodes.NodeObservation.observeLoadedNodes("thaumicomniverse:map",20,NodeMapRecords::observe);}
    public static void observe(AuraNodeBlockEntity node, int lock) {
        if(!(node.getLevel() instanceof ServerLevel)||node.isRemoved())return;
        observe(thaumcraft.api.nodes.NodeApi.stateSnapshot(node));
    }
    private static void observe(thaumcraft.api.nodes.NodeObservation.State node) {
        if (!thaumicomniverse.RevivalFeatures.Group.NODE_MAP.enabled()) return;
        var level=node.level();
        var data = get(level.getServer()); data.cache(level.getServer());
        String key = level.dimension().location() + "@" + node.position().asLong();
        for (var player : data.scanCache.entrySet()) if (player.getValue().contains("NODE" + node.identity())) {
            if (data.discoveries.computeIfAbsent(player.getKey(), unused -> new LinkedHashSet<>()).add(key)) data.setDirty();
        }
        if (!data.nodes.containsKey(key) && data.discoveries.values().stream().noneMatch(seen -> seen.contains(key))) return;
        var row = new CompoundTag(); row.putString("key", key); row.putString("nodeId", node.identity());
        row.putString("dimension", level.dimension().location().toString()); row.putLong("position", node.position().asLong());
        row.put("state", node.saved()); row.put("live", node.live()); row.putInt("lock", node.lock());
        row.putLong("observed", System.currentTimeMillis());
        data.nodes.put(key, row); data.setDirty();
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (!thaumicomniverse.RevivalFeatures.Group.NODE_MAP.enabled()) return;
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 100 != 0) return;
        var server = event.getServer(); var data = get(server); data.cache(server);
        var invalid = new HashSet<String>();
        for (var entry : data.nodes.entrySet()) {
            var row = entry.getValue(); var level = dimension(server, row.getString("dimension"));
            var pos = BlockPos.of(row.getLong("position"));
            if (level != null && level.hasChunkAt(pos)) {
                if (!(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node) || !node.nodeId().equals(row.getString("nodeId"))) invalid.add(entry.getKey());
            }
        }
        if (!invalid.isEmpty()) {
            invalid.forEach(data.nodes::remove); data.discoveries.values().forEach(seen -> seen.removeAll(invalid)); data.setDirty();
        }
        for (var player : server.getPlayerList().getPlayers()) {
            if (!NodeMapNetwork.CHANNEL.isRemotePresent(player.connection.connection)) continue;
            var batch = new ListTag(); boolean reset = true;
            for (var key : data.discoveries.getOrDefault(player.getUUID(), Set.of())) {
                var stored = data.nodes.get(key);
                if (stored == null || !data.scanCache.getOrDefault(player.getUUID(), Set.of()).contains("NODE" + stored.getString("nodeId"))) continue;
                var row = stored.copy(); var level = dimension(server, row.getString("dimension"));
                row.putBoolean("loaded", level != null && level.hasChunkAt(BlockPos.of(row.getLong("position"))));
                row.putLong("serverNow", System.currentTimeMillis()); batch.add(row);
                if (batch.size() == 64) { NodeMapNetwork.send(player, reset, batch); reset = false; batch = new ListTag(); }
            }
            if (reset || !batch.isEmpty()) NodeMapNetwork.send(player, reset, batch);
        }
    }
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) return;
        for (var entity : chunk.getBlockEntities().values()) if (entity instanceof AuraNodeBlockEntity node)
            observe(thaumcraft.api.nodes.NodeApi.stateSnapshot(node));
    }
    private static ServerLevel dimension(MinecraftServer server, String name) {
        var id = ResourceLocation.tryParse(name); return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }
    public static NodeMapRecords load(CompoundTag tag) {
        var result = new NodeMapRecords();
        for (var element : tag.getList("nodes", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) element; if (row.contains("key", Tag.TAG_STRING)) result.nodes.put(row.getString("key"), row.copy());
        }
        for (var element : tag.getList("players", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) element; if (!row.hasUUID("player")) continue;
            var seen = new LinkedHashSet<String>(); for (var key : row.getList("seen", Tag.TAG_STRING)) seen.add(key.getAsString());
            result.discoveries.put(row.getUUID("player"), seen);
        }
        return result;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var rows = new ListTag(); nodes.values().forEach(row -> rows.add(row.copy())); tag.put("nodes", rows);
        var players = new ListTag(); discoveries.forEach((id, keys) -> {
            var row = new CompoundTag(); row.putUUID("player", id); var seen = new ListTag();
            keys.forEach(key -> seen.add(StringTag.valueOf(key))); row.put("seen", seen); players.add(row);
        }); tag.put("players", players); return tag;
    }
}
