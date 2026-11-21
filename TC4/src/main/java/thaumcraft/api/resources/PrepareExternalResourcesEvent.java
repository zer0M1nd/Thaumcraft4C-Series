package thaumcraft.api.resources;

import java.util.Map;
import net.minecraftforge.eventbus.api.Event;

/** Client conversion hook, fired on the Forge bus after mapping an addon's validated resources.
 * The mutable output contains only resources owned by the requesting addon. No downloading is needed here.
 */
public class PrepareExternalResourcesEvent extends Event {
    private final String owner;
    private final Map<String,byte[]> resources;
    public PrepareExternalResourcesEvent(String owner,Map<String,byte[]> resources) { this.owner=owner; this.resources=resources; }
    public String owner() { return owner; }
    public Map<String,byte[]> resources() { return resources; }
}
