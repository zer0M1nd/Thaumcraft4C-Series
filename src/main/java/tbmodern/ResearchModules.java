package tbmodern;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import net.minecraftforge.common.ForgeConfigSpec;
import thaumcraft.api.research.ResearchCategories;

/** Recipe and research gates share the base's public enablement hook. Registry identities never disappear. */
public final class ResearchModules {
    public static void initialize() {
        Map<String,ForgeConfigSpec.BooleanValue> flags=Map.of("allowTobacco",Configuration.TOBACCO,"enableBloodyMess",Configuration.BLOODY,
            "enableFlora",Configuration.FLORA,"enableNodeTools",Configuration.NODE_TOOLS,"enableRevolver",Configuration.REVOLVER,"enableBracelets",Configuration.BRACELETS);
        try(var input=Objects.requireNonNull(ResearchModules.class.getResourceAsStream("/tbmodern/research-modules.json"))) {
            var modules=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var entry:modules.entrySet()) {
                var flag=Objects.requireNonNull(flags.get(entry.getKey()),entry.getKey());
                for(var key:entry.getValue().getAsJsonArray())ResearchCategories.registerEnablement(key.getAsString(),()->Configuration.SPEC.isLoaded()?flag.get():flag.getDefault());
            }
        } catch(IOException failure) {throw new IllegalStateException("Cannot read research module assignments",failure);}
    }
    private ResearchModules() {}
}
