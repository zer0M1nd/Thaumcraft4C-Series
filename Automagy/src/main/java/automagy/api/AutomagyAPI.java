package automagy.api;

import net.minecraft.resources.ResourceLocation;

/** Registration entry points for modern Automagy integrations. Register during common setup,
 * before servers load. Block lists use data-pack tags rather than mutable global lists. */
public final class AutomagyAPI {
    private AutomagyAPI() {}
    /** Lower priorities are tried first; equal priorities retain registration order.
     * The first provider accepting the target wins. Duplicate identifiers are rejected. */
    public static void registerInventariumContentsProvider(ResourceLocation id,int priority,InventariumProvider provider){
        automagy.content.InventariumContents.register(id,priority,provider);
    }
}
