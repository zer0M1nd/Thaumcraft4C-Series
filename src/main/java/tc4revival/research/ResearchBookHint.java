package tc4revival.research;

import java.util.List;
import tc4revival.aspect.AspectCatalog;

/** Read-only request feedback; the server independently validates every request. */
public record ResearchBookHint(String message, boolean requestable, List<Cost> costs) {
    public record Cost(String aspect, int amount, boolean known, boolean enough) {}
    public ResearchBookHint { costs = List.copyOf(costs); }
    public static ResearchBookHint describe(ResearchBrowser.Node node, int difficulty, PlayerKnowledge knowledge,
            AspectCatalog aspects, boolean paper, boolean ink, boolean existingNote) {
        if(node.research().mirror())return new ResearchBookHint("thaumcraft.research.mirror_jump",false,List.of());
        if (node.state() == ResearchBrowser.State.COMPLETE) return new ResearchBookHint("", false, List.of());
        if (node.original().tutorial()) return new ResearchBookHint(node.state() == ResearchBrowser.State.AVAILABLE
                ? "thaumcraft.research.tutorial.unlock" : "thaumcraft.research.tutorial.preview",
                node.state() == ResearchBrowser.State.AVAILABLE, List.of());
        if (node.state() != ResearchBrowser.State.AVAILABLE) return new ResearchBookHint("tc.researchmissing", false, List.of());
        if (ResearchBookActions.route(node.research(), difficulty) == ResearchBookActions.Route.NOTE)
            return new ResearchBookHint(existingNote ? "tc.research.hasnote" : paper && ink ? "tc.research.getprim" : "tc.research.shortprim",
                    !existingNote && paper && ink, List.of());
        var costs = node.research().aspects().entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(entry -> {
                    var aspect = aspects.require(entry.getKey());
                    return new Cost(entry.getKey(), entry.getValue(), knowledge.knows(aspect), knowledge.poolAmount(aspect) >= entry.getValue());
                }).toList();
        boolean enough = costs.stream().allMatch(cost -> cost.known() && cost.enough());
        return new ResearchBookHint(enough ? "tc.research.purchase" : "tc.research.short", enough, costs);
    }
}
