package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/** Extract only the old asset-index lookup data; no legacy rendering code enters the port. */
public  class ConnectedTextureProvider implements DataProvider {
    private final PackOutput output;
    public ConnectedTextureProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        try { return generate(cache); } catch (java.io.IOException error) { return CompletableFuture.failedFuture(error); }
    }
    private CompletableFuture<?> generate(CachedOutput cache) throws java.io.IOException {
        var references=Path.of(System.getProperty("tc4revival.legacyResources","references/resources")).toAbsolutePath().getParent();
        var text = Files.readString(references.resolve("deobfuscated_src/thaumcraft/client/lib/UtilsFX.java"));
        var match = Pattern.compile("connectedTextureRefByID\\s*=\\s*new int\\[\\]\\s*\\{([^}]+)}").matcher(text);
        if (!match.find()) throw new java.io.IOException("Missing connected-texture asset indices");
        var indices = new JsonArray();
        for (String value : match.group(1).split(",")) {
            int index = Integer.parseInt(value.trim()); if (index < 0 || index >= 47) throw new java.io.IOException("Invalid connected texture index"); indices.add(index);
        }
        if (indices.size() != 256) throw new java.io.IOException("Expected 256 neighbour masks");
        var root = new JsonObject(); root.add("indices", indices);
        return DataProvider.saveStable(cache, root, output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("thaumcraft/connected_textures.json"));
    }
    @Override public String getName() { return "TC4 connected texture asset indices"; }
}
