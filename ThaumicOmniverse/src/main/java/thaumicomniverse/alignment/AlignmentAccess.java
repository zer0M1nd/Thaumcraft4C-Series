package thaumicomniverse.alignment;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import tc4revival.content.CastingWandItem;

/** Journey supplies optional progression; Omniverse itself only requires a casting wand. */
public final class AlignmentAccess {
    public static boolean wandAllowed(ItemStack wand) {
        if (!(wand.getItem() instanceof CastingWandItem)) return false;
        if (!ModList.get().isLoaded("thaumicjourney")) return true;
        try {
            return (int) Class.forName("thaumicjourney.rules.JourneyTiers")
                    .getMethod("wand", ItemStack.class).invoke(null, wand) >= 4;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Journey wand-tier integration unavailable", e);
        }
    }
    public static boolean canStart(ServerPlayer player, ItemStack wand) {
        if (!thaumicomniverse.RevivalFeatures.Group.ALIGNMENT.enabled() || !wandAllowed(wand)) return false;
        return !ModList.get().isLoaded("thaumicjourney") ||
                tc4revival.research.KnowledgeSavedData.get(player.server).snapshot(player.getUUID())
                        .map(k -> k.researchMarkers().contains("thaumicjourney:t4")).orElse(false);
    }
    private AlignmentAccess() {}
}
