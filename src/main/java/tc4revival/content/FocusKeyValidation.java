package tc4revival.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Every wand focus contributes one string to a single focus-selection namespace: the sorting key built from
 * {@link WandFocusItem#sortingPrefix()} plus its applied upgrades. {@code FocusSelection} keeps those keys in a map
 * and the wheel/pouch/backpack all address a focus by that key, so two items that share a prefix silently shadow
 * each other and one of them can never be selected. The original 1.7.10 mods had the same unqualified namespace
 * (and Thaumic Horizons' liquefaction focus reused the excavation prefix); this check turns that class of collision
 * into a startup failure with the offending items named.
 */
public final class FocusKeyValidation {
    private FocusKeyValidation() {}

    public record Entry(String itemId, String key) {}

    public static void setup(FMLCommonSetupEvent event) { verifyRegisteredFoci(); }

    public static void verifyRegisteredFoci() {
        var entries = new ArrayList<Entry>();
        var unreadable = new ArrayList<String>();
        for (var item : ForgeRegistries.ITEMS) {
            if (!(item instanceof WandFocusItem focus)) continue;
            var id = String.valueOf(ForgeRegistries.ITEMS.getKey(item));
            try { entries.add(new Entry(id, focus.sortingKey(new ItemStack(item)))); }
            catch (RuntimeException | LinkageError error) { unreadable.add(id + " (" + error.getClass().getSimpleName() + ")"); }
        }
        if (!unreadable.isEmpty())
            tc4revival.TC4Revival.LOGGER.warn("Could not read sorting keys for wand foci {}; they are excluded from the uniqueness check", unreadable);
        var duplicates = duplicates(entries);
        if (!duplicates.isEmpty()) throw new IllegalStateException(describe(duplicates));
        tc4revival.TC4Revival.LOGGER.info("Verified {} registered wand foci have distinct sorting keys", entries.size());
    }

    /** Items grouped by key; only keys claimed by two or more distinct items are returned. */
    public static Map<String, List<String>> duplicates(List<Entry> entries) {
        var owners = new LinkedHashMap<String, LinkedHashSet<String>>();
        for (var entry : entries) owners.computeIfAbsent(entry.key(), ignored -> new LinkedHashSet<>()).add(entry.itemId());
        var duplicates = new LinkedHashMap<String, List<String>>();
        owners.forEach((key, items) -> { if (items.size() > 1) duplicates.put(key, List.copyOf(items)); });
        return duplicates;
    }

    public static String describe(Map<String, List<String>> duplicates) {
        var detail = duplicates.entrySet().stream()
                .map(entry -> "'" + entry.getKey() + "' is shared by " + String.join(", ", entry.getValue()))
                .collect(Collectors.joining("; "));
        return "Duplicate wand focus sorting keys: " + detail
                + ". Every focus item must return a unique sortingPrefix(); the focus wheel and the focus pouch/backpack"
                + " selection use this key as the focus identity, so a duplicate hides one focus entirely.";
    }
}
