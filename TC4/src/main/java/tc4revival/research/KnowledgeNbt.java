package tc4revival.research;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import tc4revival.aspect.AspectCatalog;
import tc4revival.research.PlayerKnowledge.ScanKind;
import tc4revival.research.PlayerKnowledge.WarpKind;

/** Per-player payload, independent of its UUID-based world file container. */
public  class KnowledgeNbt {
    public record ReadResult(PlayerKnowledge knowledge, List<String> problems) {
        public ReadResult { problems = List.copyOf(problems); }
    }

    private KnowledgeNbt() {}

    public static CompoundTag write(PlayerKnowledge knowledge) {
        var root = new CompoundTag();
        var aspects = new ListTag();
        knowledge.knownAspects().forEach((aspect, amount) -> {
            var row = key(aspect.tag());
            row.putInt("amount", amount);
            aspects.add(row);
        });
        root.put("aspects", aspects);
        root.put("research", keys(knowledge.researchMarkers()));
        var guidance = new ListTag();
        knowledge.guidance().forEach((target, condition) -> { var row = key(target); row.putString("condition", condition); guidance.add(row); });
        root.put("guidance", guidance);
        for (ScanKind kind : ScanKind.values()) root.put("scans_" + kind.name().toLowerCase(java.util.Locale.ROOT), keys(knowledge.scannedKeys(kind)));
        for (WarpKind kind : WarpKind.values()) root.putInt("warp_" + kind.name().toLowerCase(java.util.Locale.ROOT), knowledge.warp(kind));
        root.putInt("warp_counter", knowledge.warpCounter());
        root.putInt("research_preferences",knowledge.researchPreferences().packed());
        return root;
    }

    public static ReadResult read(CompoundTag root, AspectCatalog catalog) {
        var knowledge = new PlayerKnowledge(catalog);
        var problems = new ArrayList<String>();
        for (Tag value : list(root, "aspects", problems)) {
            var row = (CompoundTag) value;
            var aspect = row.contains("key", Tag.TAG_STRING) ? catalog.find(row.getString("key")) : java.util.Optional.<tc4revival.aspect.Aspect>empty();
            if (aspect.isEmpty() || !row.contains("amount", Tag.TAG_ANY_NUMERIC)) {
                problems.add("Unknown or malformed knowledge aspect");
                continue;
            }
            knowledge.setPool(aspect.get(), row.getInt("amount"));
        }
        for (String key : readKeys(root, "research", problems)) knowledge.rememberResearch(key);
        for (Tag value : list(root, "guidance", problems)) {
            var row = (CompoundTag)value;
            if (row.contains("key", Tag.TAG_STRING) && row.contains("condition", Tag.TAG_STRING)) knowledge.rememberGuidance(row.getString("key"), row.getString("condition"));
            else problems.add("Invalid guidance entry");
        }
        for (ScanKind kind : ScanKind.values()) {
            for (String key : readKeys(root, "scans_" + kind.name().toLowerCase(java.util.Locale.ROOT), problems)) {
                knowledge.rememberScan(kind, key);
            }
        }
        for (WarpKind kind : WarpKind.values()) {
            knowledge.setWarp(kind, integer(root, "warp_" + kind.name().toLowerCase(java.util.Locale.ROOT), problems));
        }
        knowledge.setWarpCounter(integer(root, "warp_counter", problems));
        knowledge.researchPreferences(ResearchPreferences.unpack(integer(root,"research_preferences",problems)));
        return new ReadResult(knowledge, problems);
    }

    private static int integer(CompoundTag root, String label, List<String> problems) {
        if (root.contains(label) && !root.contains(label, Tag.TAG_INT)) problems.add("Invalid integer: " + label);
        return root.contains(label, Tag.TAG_INT) ? root.getInt(label) : 0;
    }

    private static ListTag list(CompoundTag root, String label, List<String> problems) {
        if (!root.contains(label)) return new ListTag();
        if (!(root.get(label) instanceof ListTag list) || (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)) {
            problems.add("Invalid compound list: " + label);
            return new ListTag();
        }
        return list;
    }

    private static List<String> readKeys(CompoundTag root, String label, List<String> problems) {
        var keys = new ArrayList<String>();
        for (Tag value : list(root, label, problems)) {
            var row = (CompoundTag) value;
            if (row.contains("key", Tag.TAG_STRING)) keys.add(row.getString("key"));
            else problems.add("Missing key in " + label);
        }
        return keys;
    }

    private static ListTag keys(Iterable<String> values) {
        var result = new ListTag();
        values.forEach(value -> result.add(key(value)));
        return result;
    }

    private static CompoundTag key(String value) {
        var result = new CompoundTag();
        result.putString("key", value);
        return result;
    }
}
