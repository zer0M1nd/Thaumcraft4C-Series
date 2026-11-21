package tc4revival.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import tc4revival.aspect.item.ItemAspectJson;

public  class ItemAspectProvider implements DataProvider {
    private final PackOutput output;
    public ItemAspectProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        // Complete authored definitions now live in item_aspects/explicit.json.
        return DataProvider.saveStable(cache, ItemAspectJson.encode(java.util.List.of()),
                output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/item_aspects/vanilla_direct.json"));
    }
    @Override public String getName() { return "TC4 direct vanilla item aspect mappings"; }
}
