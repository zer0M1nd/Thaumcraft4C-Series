package tc4revival.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import tc4revival.aspect.AspectCatalog;
import tc4revival.research.ResearchDefinition.Flag;

/** Book opening and point-purchase proposals; callers commit knowledge and validate their book session. */
public  class ResearchBookActions {
    public enum Route { PURCHASE, NOTE, TUTORIAL }
    public enum Rejection { UNKNOWN, HIDDEN, COMPLETED, PREREQUISITES, NEEDS_NOTE, INVALID_COST, POINTS }
    public record Change(PlayerKnowledge knowledge, List<ResearchProgression.Completion> completions, List<String> discovered) {
        public Change { completions = List.copyOf(completions); discovered = List.copyOf(discovered); }
        public boolean changed() { return !completions.isEmpty() || !discovered.isEmpty(); }
    }
    public record Result(Optional<Change> change, Optional<Rejection> rejection) {
        public boolean accepted() { return change.isPresent(); }
    }
    private final AspectCatalog aspects;
    private final ResearchCatalog catalog;
    private final Predicate<ResearchDefinition> enabled;
    private final ResearchRules rules;
    private final ResearchProgression progression;
    public ResearchBookActions(AspectCatalog aspects, ResearchCatalog catalog, Predicate<ResearchDefinition> enabled) {
        this.aspects = aspects; this.catalog = catalog; this.enabled = enabled;
        rules = new ResearchRules(catalog, enabled); progression = new ResearchProgression(catalog, enabled);
    }
    public static Route route(ResearchDefinition entry, int difficulty) {
        if (difficulty < -1 || difficulty > 1) throw new IllegalArgumentException("Invalid research difficulty");
        if (entry.tutorial()) return Route.TUTORIAL;
        return !entry.aspects().isEmpty() && (difficulty == -1 || difficulty == 0 && entry.has(Flag.SECONDARY))
                ? Route.PURCHASE : Route.NOTE;
    }
    public boolean visible(PlayerKnowledge knowledge, ResearchDefinition entry) {
        return visible(knowledge,entry,rules,enabled);
    }
    public static boolean visible(PlayerKnowledge knowledge,ResearchDefinition entry,ResearchRules rules,Predicate<ResearchDefinition> enabled) {
        if (!enabled.test(entry) || entry.has(Flag.VIRTUAL) || entry.mirror()) return false;
        if (entry.tutorial()) return true;
        if (entry.category().equals("ELDRITCH") && !rules.completed(knowledge, "ELDRITCHMINOR")) return false;
        if (rules.completed(knowledge, entry.key()) || rules.completed(knowledge, "@" + entry.key())) return true;
        return !entry.has(Flag.HIDDEN) && !entry.has(Flag.LOST)
                && (!entry.has(Flag.CONCEALED) || thaumcraft.api.research.ResearchCategories.revealed(entry.key(),
                        key->rules.completed(knowledge,key),rules.prerequisites(knowledge, entry.key())));
    }
    public Result purchase(PlayerKnowledge knowledge, String key, int difficulty, boolean disableWarp) {
        var definition = catalog.find(key).filter(enabled);
        if (definition.isEmpty()) return reject(Rejection.UNKNOWN);
        var entry = definition.orElseThrow();
        if (rules.completed(knowledge, key)) return reject(Rejection.COMPLETED);
        if (!visible(knowledge, entry)) return reject(Rejection.HIDDEN);
        if (!rules.prerequisites(knowledge, key)) return reject(Rejection.PREREQUISITES);
        if (route(entry, difficulty) != Route.PURCHASE) return reject(Rejection.NEEDS_NOTE);
        for (var cost : entry.aspects().entrySet()) {
            if (cost.getValue() <= 0) return reject(Rejection.INVALID_COST);
            if (knowledge.poolAmount(aspects.require(cost.getKey())) < cost.getValue()) return reject(Rejection.POINTS);
        }
        var next = knowledge.copy();
        for (var cost : entry.aspects().entrySet()) next.changePool(aspects.require(cost.getKey()), -cost.getValue().intValue());
        var completed = progression.completeEligible(next, key, disableWarp);
        if (completed.isEmpty()) throw new IllegalStateException("Validated purchase did not complete");
        return new Result(Optional.of(new Change(next, completed, List.of())), Optional.empty());
    }
    /** Reading is always allowed; only an eligible click commits tutorial completion. */
    public Result readTutorial(PlayerKnowledge knowledge, String key) {
        var entry = catalog.find(key).filter(enabled).filter(ResearchDefinition::tutorial).orElse(null);
        if (entry == null || entry.mirror()) return reject(Rejection.UNKNOWN);
        if (rules.completed(knowledge, key)) return reject(Rejection.COMPLETED);
        if (!visible(knowledge, entry)) return reject(Rejection.HIDDEN);
        if (!rules.prerequisites(knowledge, key)) return reject(Rejection.PREREQUISITES);
        var next = knowledge.copy();
        var completion = progression.completeTrusted(next, key, true).orElseThrow();
        return new Result(Optional.of(new Change(next, List.of(completion), List.of())), Optional.empty());
    }
    public Change open(PlayerKnowledge knowledge, boolean cheatBook, boolean allowCheatBook, boolean disableWarp) {
        var next = knowledge.copy();
        var completed = new ArrayList<ResearchProgression.Completion>();
        var discovered = new ArrayList<String>();
        if (cheatBook && allowCheatBook) {
            for (var entry : catalog.all()) if (enabled.test(entry))
                progression.completeTrusted(next, entry.key(), disableWarp).ifPresent(completed::add);
            for (var aspect : aspects.all()) if (!next.knows(aspect)) {
                next.setPool(aspect, (short) 50); discovered.add(aspect.tag());
            }
        } else {
            // One ordered traversal, with live knowledge: a newly completed later entry may extend the chain.
            // Book opening does not use the prerequisite-limited sibling path of a note/purchase.
            for (var entry : catalog.all()) if (enabled.test(entry) && rules.completed(next, entry.key())) {
                for (String sibling : entry.siblings()) if (catalog.find(sibling).map(enabled::test).orElse(true))
                    progression.completeTrusted(next, sibling, disableWarp).ifPresent(completed::add);
            }
        }
        return new Change(next, completed, discovered);
    }
    public Change completeCreative(PlayerKnowledge knowledge,String key) {
        var next=knowledge.copy();var completions=new ArrayList<ResearchProgression.Completion>();
        creativeVisit(next,key,new java.util.HashSet<>(),completions);
        return new Change(next,completions,List.of());
    }
    private void creativeVisit(PlayerKnowledge next,String key,java.util.Set<String> visited,List<ResearchProgression.Completion> out) {
        if(!visited.add(key))return;
        var entry=catalog.find(key);if(entry.isEmpty())return;
        for(var parent:entry.get().parents())creativeVisit(next,parent,visited,out);
        for(var parent:entry.get().hiddenParents())creativeVisit(next,parent,visited,out);
        if(!entry.get().parentsAny().isEmpty()&&entry.get().parentsAny().stream().noneMatch(next.researchMarkers()::contains))
            creativeVisit(next,entry.get().parentsAny().get(0),visited,out);
        progression.completeTrusted(next,key,true).ifPresent(out::add);
    }
    private static Result reject(Rejection reason) { return new Result(Optional.empty(), Optional.of(reason)); }
}
