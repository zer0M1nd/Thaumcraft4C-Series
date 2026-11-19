package tc4revival.research;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import tc4revival.TC4Revival;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.TCAspects;

/** Overworld-owned, server-only knowledge shared across dimensions and player respawns. */
public  class KnowledgeSavedData extends SavedData {
    private static final String FILE_ID = "thaumcraft_knowledge";
    private final AspectCatalog catalog;
    private final Map<UUID, PlayerKnowledge> players = new LinkedHashMap<>();
    private final Map<UUID, Long> revisions = new LinkedHashMap<>();
    private CompoundTag unreadable;

    public KnowledgeSavedData(AspectCatalog catalog) { this.catalog = catalog; }

    public static KnowledgeSavedData get(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Knowledge must be accessed on the server thread");
        return server.overworld().getDataStorage().computeIfAbsent(
                tag -> load(tag, TCAspects.catalog()), () -> new KnowledgeSavedData(TCAspects.catalog()), FILE_ID);
    }

    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var data = get(player.getServer());
            if (data.unreadable == null) {
                data.ensure(player.getUUID(), player.getRandom()::nextInt);
                var progression = ResearchCatalogs.progression();
                // The installed catalog may have gained item-triggered addon research
                // since the item was scanned. Recover it without scanning again.
                data.update(player.getUUID(), player.getRandom()::nextInt, knowledge -> {
                    progression.unlockAutomatic(knowledge, tc4revival.config.TCConfig.SERVER.disableWarp.get());
                    ResearchClues.restoreKnownItems(knowledge);
                    return null;
                });
            }
        }
    }

    public void ensure(UUID player, IntUnaryOperator random) {
        requireWritable();
        if (!players.containsKey(player)) {
            players.put(player, PlayerKnowledge.firstLogin(catalog, random));
            revisions.put(player, 1L);
            setDirty();
        }
    }

    public Optional<PlayerKnowledge> snapshot(UUID player) { return Optional.ofNullable(players.get(thaumcraft.api.wands.WandAutomation.readIdentity(player))).map(PlayerKnowledge::copy); }
    public long revision(UUID player) { return revisions.getOrDefault(player, 0L); }

    /** Commit an already validated transaction only against the snapshot it was computed from. */
    public boolean replace(UUID player, long expectedRevision, PlayerKnowledge next) {
        requireWritable();
        if (!players.containsKey(player) || revision(player) != expectedRevision) return false;
        var owned = next.copy();
        restoreCluesAfterProgress(players.get(player),owned);
        long revision = Math.incrementExact(expectedRevision);
        players.put(player, owned);
        revisions.put(player, revision);
        setDirty();
        return true;
    }

    /** Edit a copy so failed validations or arithmetic cannot persist a partial reward. */
    public <R> R update(UUID player, IntUnaryOperator random, Function<PlayerKnowledge, R> operation) {
        requireWritable();
        var existing = players.get(player);
        var next = existing == null ? PlayerKnowledge.firstLogin(catalog, random) : existing.copy();
        R result = operation.apply(next);
        restoreCluesAfterProgress(existing,next);
        long revision = Math.incrementExact(revision(player));
        players.put(player, next.copy());
        revisions.put(player, revision);
        setDirty();
        return result;
    }

    private static void restoreCluesAfterProgress(PlayerKnowledge before,PlayerKnowledge after){
        if(ResearchDefinitions.server()==null)return; // No authoritative catalog during early load.
        if(before==null || !before.researchMarkers().equals(after.researchMarkers())
                || !before.scannedKeys(PlayerKnowledge.ScanKind.OBJECT).equals(after.scannedKeys(PlayerKnowledge.ScanKind.OBJECT)))
            ResearchClues.restoreKnownItems(after);
    }

    public static KnowledgeSavedData load(CompoundTag root, AspectCatalog catalog) {
        var result = new KnowledgeSavedData(catalog);
        if (!root.contains("version", Tag.TAG_INT) || root.getInt("version") != 1
                || !(root.get("players") instanceof ListTag rows)
                || (!rows.isEmpty() && rows.getElementType() != Tag.TAG_COMPOUND)) {
            return result.preserveUnreadable(root);
        }
        for (Tag value : rows) {
            var row = (CompoundTag) value;
            if (!row.hasUUID("uuid") || !row.contains("knowledge", Tag.TAG_COMPOUND)) {
                return result.preserveUnreadable(root);
            }
            UUID id = row.getUUID("uuid");
            if (result.players.containsKey(id)) return result.preserveUnreadable(root);
            var read = KnowledgeNbt.read(row.getCompound("knowledge"), catalog);
            if (!read.problems().isEmpty()) TC4Revival.LOGGER.warn("Recovered player knowledge {}: {}", id, read.problems());
            result.players.put(id, read.knowledge());
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        if (unreadable != null) return unreadable.copy();
        var rows = new ListTag();
        players.forEach((id, knowledge) -> {
            var row = new CompoundTag();
            row.putUUID("uuid", id);
            row.put("knowledge", KnowledgeNbt.write(knowledge));
            rows.add(row);
        });
        root.putInt("version", 1);
        root.put("players", rows);
        return root;
    }

    private KnowledgeSavedData preserveUnreadable(CompoundTag original) {
        // DimensionDataStorage catches load exceptions and would otherwise create a fresh empty file.
        unreadable = original.copy();
        players.clear();
        TC4Revival.LOGGER.error("TC4 knowledge file has an unsupported or malformed player container; preserving it without changes");
        return this;
    }

    private void requireWritable() {
        if (unreadable != null) throw new IllegalStateException("TC4 knowledge file requires format recovery before editing");
    }
}
