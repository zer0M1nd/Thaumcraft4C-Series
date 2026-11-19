package tc4revival.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import tc4revival.aspect.item.ItemAspectJson;

/** Equipment minima now belong to the complete, maintained item base definitions. */
public class EquipmentAspectProvider implements DataProvider {
    private final PackOutput output;
    public EquipmentAspectProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var directory = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/item_bonus_aspects");
        return CompletableFuture.allOf(
                DataProvider.saveStable(cache, ItemAspectJson.encode(List.of()), directory.resolve("vanilla_equipment.json")),
                DataProvider.saveStable(cache, ItemAspectJson.encode(List.of()), directory.resolve("thaumcraft_equipment.json")));
    }
    @Override public String getName() { return "TC4 retired equipment bonus tables"; }
}
