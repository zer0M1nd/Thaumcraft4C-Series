package tc4revival.client;

import java.util.*;
import net.minecraft.network.chat.Component;
import tc4revival.aspect.TCAspects;
import tc4revival.research.*;

/** Display-only view of the server-owned current guidance marker. */
public class ClientResearchGuidance {
    public record View(List<Component> lines, List<ResearchBookHint.Cost> costs, ResearchRequirementData.Entry current) {
        public View { lines = List.copyOf(lines); costs = List.copyOf(costs); }
    }
    private static final View EMPTY = new View(List.of(), List.of(), null);
    private static final Map<String,List<ResearchRequirementData.Entry>> CACHE = new HashMap<>();
    private static ResearchCatalog catalog;
    private static int version = -1;
    public static View view(ResearchDefinition research) {
        return view(research, true);
    }
    public static View view(ResearchDefinition research, boolean bookControls) {
        var knowledge = ClientKnowledge.displaySnapshot().orElse(null);
        var currentCatalog = ResearchCatalogs.bundled();
        if (!ResearchGuidance.enabled() || !ResearchGuidance.blocked(research, knowledge, currentCatalog) || !ClientItemAspects.ready()) return EMPTY;
        var options = ResearchGuidance.options(research, knowledge, currentCatalog, ClientResearchGuidance::rows);
        if (!options.free().isEmpty()) {
            var lines = new ArrayList<Component>();
            for (int i=0;i<options.free().size();i++) lines.add(line(options.free().get(i),1,bookControls&&i==0));
            return new View(lines,List.of(),options.free().get(0));
        }
        if (!ResearchGuidance.eligible(research, knowledge, currentCatalog)) return EMPTY;
        var current=options.current(research.key(),knowledge);
        if(current.isPresent()) {
            var path=current.get();
            var row=path.condition();
            return new View(List.of(line(row,path.depth(),bookControls&&row.requirement().kind()==ResearchRequirements.Kind.RESEARCH)),List.of(),row);
        }
        if(!bookControls||options.paths().isEmpty())return EMPTY;
        var costs = ResearchGuidance.cost(research).stream().map(tag -> {
            var aspect = TCAspects.catalog().require(tag);
            return new ResearchBookHint.Cost(tag, 1, knowledge.knows(aspect), knowledge.poolAmount(aspect));
        }).toList();
        return new View(List.of(Component.translatable("thaumcraft.guidance.listen")), costs, null);
    }
    private static Component line(ResearchRequirementData.Entry row, int depth, boolean jump) {
        var line=row.prerequisite(depth).copy();
        if(jump)line.append(Component.translatable("thaumcraft.guidance.jump"));
        return line;
    }
    public static List<ResearchRequirementData.Entry> rows(ResearchDefinition research) {
        var currentCatalog=ResearchCatalogs.bundled();
        if (catalog != currentCatalog || version != ClientItemAspects.version()) {
            catalog = currentCatalog; version = ClientItemAspects.version(); CACHE.clear();
        }
        return CACHE.computeIfAbsent(research.key(), key -> {
            var data = ClientItemAspects.calculation();
            return ResearchRequirementData.resolve(research, data, data.blocks(), data.entities().researchRules());
        });
    }
}
