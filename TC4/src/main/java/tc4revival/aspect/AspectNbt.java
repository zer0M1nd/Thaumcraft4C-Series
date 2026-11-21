package tc4revival.aspect;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Keeps the legacy key/amount layout, with diagnostics for damaged or unknown entries. */
public  class AspectNbt {
    public static final String DEFAULT_LABEL = "Aspects";
    public record ReadResult(AspectAmounts amounts, List<String> problems) {
        public ReadResult { problems = List.copyOf(problems); }
    }

    private AspectNbt() {}

    public static void write(CompoundTag target, AspectAmounts amounts) { write(target, DEFAULT_LABEL, amounts); }

    public static void write(CompoundTag target, String label, AspectAmounts amounts) {
        var rows = new ListTag();
        amounts.view().forEach((aspect, amount) -> {
            var row = new CompoundTag();
            row.putString("key", aspect.tag());
            row.putInt("amount", amount);
            rows.add(row);
        });
        target.put(label, rows);
    }

    public static ReadResult read(CompoundTag source, AspectCatalog catalog) { return read(source, DEFAULT_LABEL, catalog); }

    public static ReadResult read(CompoundTag source, String label, AspectCatalog catalog) {
        var quantities = new AspectAmounts();
        var problems = new ArrayList<String>();
        if (!source.contains(label)) return new ReadResult(quantities, problems);
        if (!(source.get(label) instanceof ListTag rows)
                || (!rows.isEmpty() && rows.getElementType() != Tag.TAG_COMPOUND)) {
            return new ReadResult(quantities, List.of("Expected a compound list at " + label));
        }
        for (int index = 0; index < rows.size(); index++) {
            var row = rows.getCompound(index);
            if (!row.contains("key", Tag.TAG_STRING) || !row.contains("amount", Tag.TAG_INT)) {
                problems.add("Invalid key or amount at " + index);
                continue;
            }
            var aspect = catalog.find(row.getString("key"));
            if (aspect.isEmpty()) {
                problems.add("Unknown aspect at " + index + ": " + row.getString("key"));
                continue;
            }
            try {
                quantities.add(aspect.get(), row.getInt("amount"));
            } catch (ArithmeticException overflow) {
                problems.add("Quantity overflow at " + index);
            }
        }
        return new ReadResult(quantities, problems);
    }
}
