package tc4revival.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public  class TCTextureMetadataProvider implements DataProvider {
    private final PackOutput output;
    public TCTextureMetadataProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        // Exact old metadata: all ten 16px frames, one tick each, no interpolation.
        var root = new JsonObject();
        root.add("animation", new JsonObject());
        var lens = new JsonObject();
        var texture = new JsonObject();
        texture.addProperty("blur", true);
        lens.add("texture", texture);
        // Model sprites outside modern block/ and item/ directories need explicit atlas sources.
        // Keep entity/GUI sheets out of the block atlas unless a baked model uses them.
        var atlas = new JsonObject();
        var sources = new JsonArray();
        for (String sprite : java.util.List.of("blocks/woodplain", "blocks/empty",
                "blocks/manapod_stem_0", "blocks/manapod_stem_1", "blocks/manapod_stem_2",
                "blocks/paving_stone_travel", "blocks/paving_stone_warding",
                "items/mana_bean", "models/trunk", "models/table", "models/restable")) {
            var entry = new JsonObject();
            entry.addProperty("type", "minecraft:single");
            entry.addProperty("resource", "thaumcraft:" + sprite);
            sources.add(entry);
        }
        atlas.add("sources", sources);
        var base = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK);
        var materialMetadata = new java.util.ArrayList<CompletableFuture<?>>();
        // Original metadata is provided by the external resource mapping at runtime.
        materialMetadata.addAll(java.util.List.of(DataProvider.saveStable(cache, root, base.resolve("thaumcraft/textures/item/shard_balanced.png.mcmeta")),
                DataProvider.saveStable(cache, lens, base.resolve("thaumcraft/textures/models/scanscreen.png.mcmeta")),
                DataProvider.saveStable(cache, atlas, base.resolve("minecraft/atlases/blocks.json"))));
        return CompletableFuture.allOf(materialMetadata.toArray(CompletableFuture[]::new));
    }
    @Override public String getName() { return "TC4 texture metadata and atlas sources"; }
}
