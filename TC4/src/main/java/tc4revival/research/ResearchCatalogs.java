package tc4revival.research;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import tc4revival.TC4Revival;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;

public  class ResearchCatalogs {
    private static volatile ResearchCatalog catalog;
    private ResearchCatalogs() {}
    public static void addReloadListener(AddReloadListenerEvent event) { event.addListener(new ResearchDefinitions()); }
    public static void stopped(ServerStoppedEvent event) { catalog = null; ResearchDefinitions.clearServer(); }
    public static ResearchCatalog catalog() {
        var current = ResearchDefinitions.server()==null?catalog:ResearchDefinitions.server().catalog();
        if (current == null) throw new IllegalStateException("Research catalog has not loaded");
        return current;
    }
    public static boolean enabled(ResearchDefinition entry) {
        var snapshot=ResearchDefinitions.display();
        if(snapshot!=null && snapshot.json().has("disabledResearch")) {
            for(var key:snapshot.json().getAsJsonArray("disabledResearch"))if(key.getAsString().equals(entry.key()))return false;
            return true;
        }
        return configuredEnabled(entry);
    }
    public static boolean configuredEnabled(ResearchDefinition entry) {
        if(!thaumcraft.api.research.ResearchCategories.enabled(entry.key()))return false;
        return switch (entry.key()) {
            case ResearchGuidance.RESEARCH -> ResearchGuidance.enabled();
            case "CAP_silver" -> tc4revival.content.SilverCaps.available();
            case "MIRROR", "MIRRORHAND", "MIRRORESSENTIA" -> TCConfig.SERVER.allowMirrors.get();
            case "FOCUSWARDING", "WARDEDARCANA" -> TCConfig.SERVER.wardedStone.get();
            default -> true;
        };
    }
    public static ResearchRules rules() { return new ResearchRules(catalog(), ResearchCatalogs::enabled); }
    public static ResearchProgression progression() { return new ResearchProgression(catalog(), ResearchCatalogs::enabled); }
    /** Shipped display metadata for tooltips; authoritative gameplay always uses the loaded server catalog. */
    public static ResearchCatalog bundled() { var snapshot=ResearchDefinitions.display();return snapshot==null?Bundled.VALUE:snapshot.catalog(); }
    private static  class Bundled {
        private static final ResearchCatalog VALUE = read();
        private static ResearchCatalog read() {
            try (var reader = new java.io.InputStreamReader(java.util.Objects.requireNonNull(ResearchCatalogs.class
                    .getResourceAsStream("/data/thaumcraft/research_catalog/legacy.json")), java.nio.charset.StandardCharsets.UTF_8)) {
                var root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                return ResearchCatalog.decode(root.getAsJsonArray("categories"), TCAspects.catalog());
            } catch (java.io.IOException failure) { throw new IllegalStateException("Missing bundled research display data", failure); }
        }
    }
}
