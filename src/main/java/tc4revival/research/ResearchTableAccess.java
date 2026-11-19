package tc4revival.research;

import java.util.function.Predicate;
import tc4revival.config.CoreRevivalFeatures;
import tc4revival.config.TCConfig;

/** Shared permission calculation for the real screen and server automation. */
public class ResearchTableAccess {
    public static final String FIRST = "REVIVAL_RESEARCH_ASSISTANCE";
    public static final String SECOND = "REVIVAL_ASPECT_RELATIONS";
    public static final String THIRD = "REVIVAL_RESEARCH_CONNECTIONS";
    public record Access(boolean basic, boolean sorting, boolean search, boolean refill,
                         boolean random, boolean related, boolean connect) {}
    public static Access resolve(CoreRevivalFeatures features, Predicate<String> completed) {
        boolean first = !features.firstResearch.get() || completed.test(FIRST);
        boolean second = !features.secondResearch.get() || completed.test(SECOND);
        boolean third = !features.thirdResearch.get() || completed.test(THIRD);
        return new Access(features.basicShortcuts.get(), features.tableSorting.get(),
                features.aspectSearch.get() && first, features.autoRefill.get() && first,
                features.randomResearch.get() && first, features.relatedSearch.get() && second,
                features.autoConnect.get() && third);
    }
    public static Access resolve(PlayerKnowledge knowledge) {
        return resolve(TCConfig.SERVER.revival, key -> knowledge != null && knowledge.researchMarkers().contains(key));
    }
    public static Access server(net.minecraft.server.level.ServerPlayer player) {
        return resolve(KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).orElse(null));
    }
    public static boolean queryAllowed(String query, Access access) {
        if (query.isBlank()) return true;
        return query.stripLeading().startsWith("~") ? access.related() : access.search();
    }
    public static void register() {
        var features = TCConfig.SERVER.revival;
        thaumcraft.api.research.ResearchCategories.registerEnablement(FIRST, () -> features.stageEnabled(1));
        thaumcraft.api.research.ResearchCategories.registerEnablement(SECOND, () -> features.stageEnabled(2));
        thaumcraft.api.research.ResearchCategories.registerEnablement(THIRD, () -> features.stageEnabled(3));
        thaumcraft.api.research.ResearchPrerequisites.conditional(net.minecraft.resources.ResourceLocation.parse("thaumcraft:revival_stage_two"), SECOND, FIRST, () -> features.stageEnabled(1));
        thaumcraft.api.research.ResearchPrerequisites.conditional(net.minecraft.resources.ResourceLocation.parse("thaumcraft:revival_stage_three"), THIRD, SECOND, () -> features.stageEnabled(2));
    }
}
