package tc4revival.aspect;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** Startup data for the Forge registry; runtime metadata has no Java fallback. */
public  class AspectDefinitions {
    private AspectDefinitions() {}
    private static  class Holder { static final AspectCatalog VALUE=load(); }
    public static AspectCatalog bundled() { return Holder.VALUE; }
    private static AspectCatalog load() {
        String path="/data/thaumcraft/aspect_registry.json";
        try (var stream=AspectDefinitions.class.getResourceAsStream(path)) {
            if(stream==null)throw new IllegalStateException("Missing aspect registry data: "+path);
            return decode(tc4revival.data.AddonDefinitions.collect("aspects", JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonArray()));
        } catch(java.io.IOException error) { throw new IllegalStateException("Cannot load aspect registry",error); }
    }
    public static AspectCatalog decode(com.google.gson.JsonArray rows) {
        var definitions=new ArrayList<Aspect>();
        for(var element:rows) {
            var row=element.getAsJsonObject();var components=new ArrayList<String>();
            for(var component:row.getAsJsonArray("components"))components.add(component.getAsString());
            definitions.add(new Aspect(row.get("tag").getAsString(),row.get("color").getAsInt(),components,
                    row.get("chatColor").getAsString(),row.get("blendFactor").getAsInt(),
                    row.has("texture")?row.get("texture").getAsString():null));
        }
        return new AspectCatalog(definitions,thaumcraft.api.data.StartupDefinitions.rules().allowAspectCycles(),thaumcraft.api.data.StartupDefinitions.rules().aspectAliases());
    }
}
