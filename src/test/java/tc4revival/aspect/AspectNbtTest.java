package tc4revival.aspect;

import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AspectNbtTest {
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());

    @Test void roundTripRetainsLabelsZerosNegativesAndOrder() {
        var original = new AspectAmounts().add(catalog.require("terra"), 8)
                .add(catalog.require("aer"), 0).add(catalog.require("ordo"), -1);
        var tag = new CompoundTag();
        tag.putString("owner", "test");
        AspectNbt.write(tag, "stored", original);
        var loaded = AspectNbt.read(tag, "stored", catalog);
        assertTrue(loaded.problems().isEmpty());
        assertEquals(original.view(), loaded.amounts().view());
        assertEquals(original.aspects(), loaded.amounts().aspects());
        assertEquals("test", tag.getString("owner"));
        assertFalse(tag.contains("Aspects"));
    }

    @Test void duplicateRowsAccumulateAndUnknownRowsDoNotCreateNullAspects() {
        var tag = new CompoundTag();
        var rows = new ListTag();
        rows.add(row("aer", 2));
        rows.add(row("missing", 9));
        rows.add(row("aer", 3));
        rows.add(row("terra", 0));
        tag.put("Aspects", rows);
        var loaded = AspectNbt.read(tag, catalog);
        assertEquals(1, loaded.problems().size());
        assertEquals(5, loaded.amounts().amount(catalog.require("aer")));
        assertEquals(List.of(catalog.require("aer"), catalog.require("terra")), loaded.amounts().aspects());
    }

    @Test void malformedTagsAreReportedWithoutCrashingOrCoercingStrings() {
        var tag = new CompoundTag();
        tag.putString("Aspects", "broken");
        assertEquals(1, AspectNbt.read(tag, catalog).problems().size());
        var rows = new ListTag();
        var wrong = row("aer", 2);
        wrong.putString("amount", "3");
        rows.add(wrong);
        rows.add(new CompoundTag());
        tag.put("Aspects", rows);
        var loaded = AspectNbt.read(tag, catalog);
        assertEquals(2, loaded.problems().size());
        assertTrue(loaded.amounts().isEmpty());
        assertTrue(AspectNbt.read(new CompoundTag(), catalog).problems().isEmpty());
    }

    @Test void overflowingDuplicateCannotWrapIntoNegativeQuantities() {
        var rows = new ListTag();
        rows.add(row("aer", Integer.MAX_VALUE));
        rows.add(row("aer", 1));
        var tag = new CompoundTag();
        tag.put("Aspects", rows);
        var loaded = AspectNbt.read(tag, catalog);
        assertEquals(Integer.MAX_VALUE, loaded.amounts().amount(catalog.require("aer")));
        assertEquals(1, loaded.problems().size());
    }

    private static CompoundTag row(String key, int amount) {
        var row = new CompoundTag();
        row.putString("key", key);
        row.putInt("amount", amount);
        return row;
    }
}
