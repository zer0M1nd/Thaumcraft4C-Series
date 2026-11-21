package tc4revival.resources;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.repository.Pack;

/** Keep the required foundation ordered without changing the relative order of user packs. */
public  class ExternalResourceOrder {
    private ExternalResourceOrder() {}
    public static List<Pack> normalize(List<Pack> selected) {
        Pack external = selected.stream().filter(p -> p.getId().equals("thaumcraft_original_resources")).findFirst().orElse(null);
        Pack translation = selected.stream().filter(p -> p.getId().equals("thaumcraft_legacy_translations")).findFirst().orElse(null);
        Pack official = selected.stream().filter(p -> p.getId().equals("thaumcraft_original_translations")).findFirst().orElse(null);
        if (external == null && translation == null && official == null) return selected;
        var result = new ArrayList<Pack>();
        if (translation != null) result.add(translation);
        if (official != null) result.add(official);
        if (external == null) {
            selected.stream().filter(p -> p != translation && p != official).forEach(result::add);
            return List.copyOf(result);
        }
        selected.stream().filter(p -> p.getId().equals("vanilla")).forEach(result::add);
        result.add(external);
        selected.stream().filter(p->p.getId().startsWith("thaumcraft_original_resources/")).forEach(result::add);
        selected.stream().filter(p -> p.getId().equals("mod_resources")).forEach(result::add);
        selected.stream().filter(p -> p != translation && p != official && p != external && !p.getId().startsWith("thaumcraft_original_resources/") && !p.getId().equals("vanilla") && !p.getId().equals("mod_resources"))
                .forEach(result::add);
        return List.copyOf(result);
    }
}
