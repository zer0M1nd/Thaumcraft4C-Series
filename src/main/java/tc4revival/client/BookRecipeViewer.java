package tc4revival.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import tc4revival.config.TCConfig;

/** Optional client bridge: loading the book never requires JEI classes. */
public class BookRecipeViewer {
    public interface Viewer {
        boolean show(String research, ItemStack item, InputConstants.Key key);
        Component hint();
        default Optional<Component> modName(String modId) { return Optional.empty(); }
    }
    public static Viewer viewer;
    public static boolean enabled() { return TCConfig.CLIENT.jeiResearch.get(); }
    public static boolean show(String research, ItemStack item, InputConstants.Key key) {
        return enabled() && viewer != null && viewer.show(research, item, key);
    }
    public static Optional<Component> hint() {
        return enabled() && viewer != null ? Optional.of(viewer.hint()) : Optional.empty();
    }
    public static Optional<Component> modName(String modId) {
        return viewer == null ? Optional.empty() : viewer.modName(modId);
    }
    public static Optional<Component> researchModName(tc4revival.research.ResearchDefinition research, String category) {
        boolean sameMod = tc4revival.research.ResearchCatalogs.bundled().categories().stream()
                .anyMatch(tab -> tab.key().equals(category) && tab.modId().equals(research.modId()));
        return sameMod ? Optional.empty() : modName(research.modId());
    }
}
