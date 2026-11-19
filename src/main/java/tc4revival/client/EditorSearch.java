package tc4revival.client;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.world.item.Item;
/** The optional JEI plugin installs this bridge; no JEI classes load without the mod. */
public class EditorSearch {
    public static Function<String,Set<Item>> search;
}
