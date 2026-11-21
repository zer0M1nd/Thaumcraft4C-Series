package thaumcraft.api.wands;

import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** A bound identity source; knowledge is resolved live, never copied into an item. */
public interface AutomationCore {
    UUID owner(ItemStack stack);
    default boolean enabled(ItemStack stack) { return true; }
    default Set<String> research(ItemStack stack, ServerLevel level) {
        if(!enabled(stack))return Set.of();
        UUID id = owner(stack);
        return id == null ? Set.of() : tc4revival.research.KnowledgeSavedData.get(level.getServer()).snapshot(id)
                .map(tc4revival.research.PlayerKnowledge::researchMarkers).orElse(Set.of());
    }
}
