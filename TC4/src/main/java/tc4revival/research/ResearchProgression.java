package tc4revival.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import tc4revival.research.PlayerKnowledge.WarpKind;
import tc4revival.research.ResearchDefinition.Flag;

/** Knowledge mutations for server-validated research actions; never accepts a client's claim of completion. */
public  class ResearchProgression {
    public record Completion(String key, int permanentWarp, int stickyWarp) {}
    private final ResearchCatalog catalog;
    private final Predicate<ResearchDefinition> enabled;
    private final ResearchRules rules;

    public ResearchProgression(ResearchCatalog catalog, Predicate<ResearchDefinition> enabled) {
        this.catalog = java.util.Objects.requireNonNull(catalog);
        this.enabled = java.util.Objects.requireNonNull(enabled);
        rules = new ResearchRules(catalog, enabled);
    }

    public List<String> missingAutomatic(PlayerKnowledge knowledge) {
        return catalog.all().stream().filter(enabled).filter(entry -> !entry.mirror() && entry.has(Flag.AUTO_UNLOCK))
                .map(ResearchDefinition::key).filter(key -> !knowledge.researchMarkers().contains(key)).toList();
    }

    /** Legacy playerLoad completes these entries after loading, without prerequisites or sibling traversal. */
    public List<Completion> unlockAutomatic(PlayerKnowledge knowledge, boolean disableWarp) {
        var result = new ArrayList<Completion>();
        for (String key : missingAutomatic(knowledge)) completeTrusted(knowledge, key, disableWarp).ifPresent(result::add);
        return List.copyOf(result);
    }

    /** Raw legacy completion for trusted callers, including literal clue/unknown markers. No eligibility checks here. */
    public Optional<Completion> completeTrusted(PlayerKnowledge knowledge, String key, boolean disableWarp) {
        if(catalog.find(key).filter(ResearchDefinition::mirror).isPresent())return Optional.empty();
        if (!knowledge.rememberResearch(key)) return Optional.empty();
        int warp = disableWarp ? 0 : catalog.find(key).filter(entry -> !entry.tutorial()).map(ResearchDefinition::warp).orElse(0);
        int sticky = warp / 2;
        int permanent = warp - sticky;
        if (permanent > 0) knowledge.changeWarp(WarpKind.PERMANENT, permanent);
        if (sticky > 0) knowledge.changeWarp(WarpKind.STICKY, sticky);
        if (warp > 0) knowledge.setWarpCounter(knowledge.totalWarp());
        return Optional.of(new Completion(key, permanent, sticky));
    }

    /** After a genuine note or purchase is validated, unlock only its direct eligible siblings in listed order. */
    public List<Completion> completeEligible(PlayerKnowledge knowledge, String key, boolean disableWarp) {
        var entry = catalog.find(key).filter(enabled);
        if (entry.isEmpty() || entry.get().mirror() || rules.completed(knowledge, key) || !rules.prerequisites(knowledge, key)) return List.of();
        var result = new ArrayList<Completion>();
        completeTrusted(knowledge, key, disableWarp).ifPresent(result::add);
        for (String sibling : entry.get().siblings()) {
            if (!rules.completed(knowledge, sibling) && rules.prerequisites(knowledge, sibling)) {
                completeTrusted(knowledge, sibling, disableWarp).ifPresent(result::add);
            }
        }
        return List.copyOf(result);
    }
}
