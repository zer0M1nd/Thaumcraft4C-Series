package thaumicomniverse.research;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge;

/** Preserve paid-for progress when the former individual research entries are consolidated. */
@Mod.EventBusSubscriber(modid="thaumicomniverse")
public final class EnchantmentResearchMigration {
    private EnchantmentResearchMigration() {}

    public static boolean needsMigration(PlayerKnowledge knowledge) {
        return !knowledge.researchMarkers().contains(UniversalEnchantments.RESEARCH)
                && knowledge.researchMarkers().stream().anyMatch(k->k.startsWith(UniversalEnchantments.PREFIX));
    }
    public static boolean migrate(PlayerKnowledge knowledge) {
        return needsMigration(knowledge) && knowledge.rememberResearch(UniversalEnchantments.RESEARCH);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var data=KnowledgeSavedData.get(player.getServer());
        if(data.snapshot(player.getUUID()).filter(EnchantmentResearchMigration::needsMigration).isPresent())
            data.update(player.getUUID(),player.getRandom()::nextInt,EnchantmentResearchMigration::migrate);
    }
}
