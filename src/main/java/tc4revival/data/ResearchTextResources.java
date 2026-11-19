package tc4revival.data;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import tc4revival.research.ResearchText;

/** Checks that every picture requested by the legacy book has an external mapping; copies no artwork. */
public  class ResearchTextResources implements DataProvider {
    private final PackOutput output;
    private final net.minecraftforge.common.data.ExistingFileHelper existingFiles;
    public ResearchTextResources(PackOutput output, net.minecraftforge.common.data.ExistingFileHelper existingFiles) {
        this.output = output; this.existingFiles = existingFiles;
    }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var legacy = Path.of(java.util.Objects.requireNonNull(System.getProperty("tc4revival.legacyResources"))).resolve("assets");
        var textures = new TreeSet<String>();
        textures.add("thaumcraft:textures/gui/gui_researchbook.png");
        textures.add("thaumcraft:textures/gui/gui_researchbook_overlay.png");
        textures.add("thaumcraft:textures/gui/gui_research.png");
        textures.add("thaumcraft:textures/misc/nodes.png");
        textures.add("thaumcraft:textures/misc/particles.png");
        try {
            var baseline = Path.of(java.util.Objects.requireNonNull(System.getProperty("tc4revival.researchBaseline")));
            try (var reader = Files.newBufferedReader(baseline.resolve("research-catalog.json"), StandardCharsets.UTF_8)) {
                for (var value : com.google.gson.JsonParser.parseReader(reader).getAsJsonArray()) {
                    var category = value.getAsJsonObject();
                    textures.add(category.get("icon").getAsString()); textures.add(category.get("background").getAsString());
                    for (var entry : category.getAsJsonArray("research")) {
                        var icon = entry.getAsJsonObject().get("icon_resource");
                        if (icon != null && !icon.isJsonNull()) textures.add(icon.getAsString());
                    }
                }
            }
            for (String locale : new String[] {"en_US", "zh_CN"}) {
                for (String line : Files.readAllLines(legacy.resolve("thaumcraft/lang/" + locale + ".lang"), StandardCharsets.UTF_8)) {
                    if (!line.startsWith("tc.research_page.")) continue;
                    int separator = line.indexOf('='); if (separator < 0) continue;
                    for (var part : ResearchText.parse(line.substring(separator + 1)))
                        if (part instanceof ResearchText.Picture picture) textures.add(picture.texture());
                }
            }
            for (String texture : textures) {
                String target = "assets/" + texture.replace(':', '/');
                if (!tc4revival.resources.OriginalResources.mappings().containsKey(target))
                    throw new IllegalStateException("Research picture missing external mapping: " + target);
            }
            return CompletableFuture.completedFuture(null);
        } catch (java.io.IOException failure) { return CompletableFuture.failedFuture(failure); }
    }
    @Override public String getName() { return "TC4 research book and browser artwork"; }
}
