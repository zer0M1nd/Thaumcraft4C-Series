package tc4revival.research;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import tc4revival.aspect.AspectCatalog;

/** Old field names; reject a damaged board as a whole so missing anchors cannot become free success. */
public  class ResearchNoteNbt {
    public record ReadResult(Optional<ResearchNote> note, List<String> problems) {
        public ReadResult { problems = List.copyOf(problems); }
    }
    private ResearchNoteNbt() {}
    public static void write(CompoundTag target, ResearchNote note) {
        target.putString("key", note.key());
        target.putInt("color", note.color());
        target.putBoolean("complete", note.complete());
        target.putInt("copies", note.copies());
        if (note.layoutSize() > 0) target.putInt("layoutSize", note.layoutSize());
        else target.remove("layoutSize");
        var grid = new ListTag();
        note.grid().forEach((hex, cell) -> {
            var row = new CompoundTag();
            row.putByte("hexq", (byte) hex.q());
            row.putByte("hexr", (byte) hex.r());
            row.putByte("type", (byte) cell.type());
            if (cell.aspect() != null) row.putString("aspect", cell.aspect());
            grid.add(row);
        });
        target.put("hexgrid", grid);
    }
    public static ReadResult read(CompoundTag source, AspectCatalog aspects) {
        if (source == null) return new ReadResult(Optional.empty(), List.of());
        int layoutSize = source.getInt("layoutSize");
        if (source.contains("layoutSize") && (!source.contains("layoutSize", Tag.TAG_INT)
                || layoutSize < 1 || layoutSize > 5)) return invalid("Invalid authored layout size");
        if (source.contains("hexgrid") && (!(source.get("hexgrid") instanceof ListTag list)
                || (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND))) return invalid("Invalid hexgrid list");
        var rows = source.getList("hexgrid", Tag.TAG_COMPOUND);
        if (rows.size() > 61) return invalid("Oversized research grid");
        var positions = new HashMap<String, ResearchHex>();
        var cells = new HashMap<ResearchHex, ResearchNote.Cell>();
        for (int index = 0; index < rows.size(); index++) {
            var row = rows.getCompound(index);
            if (!row.contains("hexq", Tag.TAG_BYTE) || !row.contains("hexr", Tag.TAG_BYTE)
                    || !row.contains("type", Tag.TAG_BYTE)) return invalid("Invalid cell fields at " + index);
            var hex = new ResearchHex(row.getByte("hexq"), row.getByte("hexr"));
            String aspect = row.getString("aspect");
            if (!aspect.isEmpty() && aspects.find(aspect).isEmpty()) return invalid("Unknown aspect at " + index);
            try {
                var cell = new ResearchNote.Cell(row.getByte("type"), aspect.isEmpty() ? null : aspect);
                positions.put(hex.legacyKey(), hex);
                cells.put(hex, cell); // Legacy duplicates use the last entry.
            } catch (IllegalArgumentException failure) { return invalid("Invalid cell at " + index); }
        }
        var grid = new LinkedHashMap<ResearchHex, ResearchNote.Cell>();
        positions.values().forEach(hex -> grid.put(hex, cells.get(hex)));
        try {
            return new ReadResult(Optional.of(new ResearchNote(source.getString("key"), source.getInt("color"),
                    source.getBoolean("complete"), source.getInt("copies"), grid, layoutSize)), List.of());
        } catch (IllegalArgumentException failure) { return invalid(failure.getMessage()); }
    }
    private static ReadResult invalid(String reason) { return new ReadResult(Optional.empty(), List.of(reason)); }
}
