package tc4revival.research;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import tc4revival.research.ResearchDefinition.Flag;
import tc4revival.research.ResearchDefinition.Stack;

/** Selection rules shared by scans and future research interfaces. No client authority or global mutable cache. */
public  class ResearchRules {
    public sealed interface Clue permits ItemClue, EntityClue {}
    /** Matcher owns an immutable snapshot of the observed item; modern tags/NBT belong in that adapter. */
    public record ItemClue(Predicate<Stack> matches) implements Clue {
        public ItemClue { java.util.Objects.requireNonNull(matches); }
    }
    public record EntityClue(String legacyName) implements Clue {
        public EntityClue { java.util.Objects.requireNonNull(legacyName); }
    }
    private final ResearchCatalog catalog;
    private final Predicate<ResearchDefinition> enabled;
    public ResearchRules(ResearchCatalog catalog) { this(catalog, entry -> true); }
    public ResearchRules(ResearchCatalog catalog, Predicate<ResearchDefinition> enabled) {
        this.catalog = java.util.Objects.requireNonNull(catalog);
        this.enabled = java.util.Objects.requireNonNull(enabled);
    }
    public boolean completed(PlayerKnowledge knowledge, String key) {
        if(!key.startsWith("@") && catalog.find(key).filter(ResearchDefinition::mirror).isPresent())key=catalog.original(key).key();
        return (key.startsWith("@") || catalog.find(key).filter(enabled).isPresent()) && knowledge.researchMarkers().contains(key);
    }
    public boolean prerequisites(PlayerKnowledge knowledge, String key) {
        // Completed knowledge remains valid if a datapack later adds or removes its ancestors.
        // Keep the enabled-definition check in completed: this does not unlock disabled content.
        if (catalog.find(key).filter(enabled).isPresent() && completed(knowledge, key)) return true;
        return catalog.find(key).filter(enabled).map(entry -> entry.parents().stream().allMatch(p->marker(knowledge,p))
                && entry.hiddenParents().stream().allMatch(p->marker(knowledge,p))
                && (entry.parentsAny().isEmpty()||entry.parentsAny().stream().anyMatch(p->marker(knowledge,p)))).orElse(false);
    }
    private boolean marker(PlayerKnowledge knowledge,String key) {
        return knowledge.researchMarkers().contains(catalog.find(key).filter(ResearchDefinition::mirror).map(e->catalog.original(key).key()).orElse(key));
    }
    /** A clue uses actual positive rewards; it does not check prerequisites or complete the research itself. */
    public List<String> clueCandidates(PlayerKnowledge knowledge, Clue clue, Map<String, Integer> rewards) {
        if (clue == null) return List.of(); // nodes do not call legacy createClue
        return catalog.all().stream().filter(enabled).filter(entry->!entry.mirror())
                .filter(entry -> !entry.aspects().isEmpty() && (entry.has(Flag.HIDDEN) || entry.has(Flag.LOST)))
                .filter(entry -> !completed(knowledge, entry.key()) && !completed(knowledge, "@" + entry.key()))
                .filter(entry -> matches(entry, clue) || entry.triggers().aspects().orElse(List.of()).stream()
                        .anyMatch(tag -> rewards.getOrDefault(tag, 0) > 0))
                .map(ResearchDefinition::key).toList();
    }
    public Optional<String> createClue(PlayerKnowledge knowledge, Clue clue, Map<String, Integer> rewards, IntUnaryOperator random) {
        var selected = choose(clueCandidates(knowledge, clue, rewards), random);
        selected.ifPresent(key -> knowledge.rememberResearch("@" + key));
        return selected;
    }
    /** Restore explicit item clues from persisted observations, without scan rewards or research completion. */
    public List<String> restoreItemClues(PlayerKnowledge knowledge, Predicate<Stack> previouslyScanned) {
        var restored=new java.util.ArrayList<String>();
        for(var entry:catalog.all()){
            if(!enabled.test(entry)||entry.mirror()||entry.aspects().isEmpty()
                    ||!(entry.has(Flag.HIDDEN)||entry.has(Flag.LOST))
                    ||completed(knowledge,entry.key())||completed(knowledge,"@"+entry.key()))continue;
            if(entry.triggers().items().orElse(List.of()).stream().anyMatch(previouslyScanned)){
                knowledge.rememberResearch("@"+entry.key());restored.add(entry.key());
            }
        }
        return List.copyOf(restored);
    }
    private static boolean matches(ResearchDefinition entry, Clue clue) {
        if (clue instanceof ItemClue item) return entry.triggers().items().orElse(List.of()).stream().anyMatch(item.matches());
        return entry.triggers().entities().orElse(List.of()).contains(((EntityClue) clue).legacyName());
    }
    public List<String> hiddenCandidates(PlayerKnowledge knowledge) {
        return catalog.all().stream().filter(enabled).filter(entry->!entry.mirror()).filter(entry -> entry.has(Flag.HIDDEN) && !entry.aspects().isEmpty())
                .filter(entry -> !completed(knowledge, entry.key()) && !completed(knowledge, "@" + entry.key())
                        && prerequisites(knowledge, entry.key()) && entry.triggers().present())
                .map(ResearchDefinition::key).toList();
    }
    public Optional<String> findHidden(PlayerKnowledge knowledge, long worldTime) {
        return choose(hiddenCandidates(knowledge), new Random(worldTime / 10L / 5L)::nextInt);
    }
    public List<String> matchingCandidates(PlayerKnowledge knowledge, String aspect, int difficulty) {
        return catalog.all().stream().filter(enabled).filter(entry->!entry.mirror())
                .filter(entry -> difficulty != -1 && !(difficulty == 0 && entry.has(Flag.SECONDARY)))
                .filter(entry -> !entry.tutorial() && !entry.has(Flag.HIDDEN) && !entry.has(Flag.LOST) && !entry.has(Flag.AUTO_UNLOCK)
                        && !entry.has(Flag.VIRTUAL) && !entry.has(Flag.STUB))
                .filter(entry -> !completed(knowledge, entry.key()) && prerequisites(knowledge, entry.key())
                        && entry.aspects().getOrDefault(aspect, 0) > 0)
                .map(ResearchDefinition::key).toList();
    }
    private static Optional<String> choose(List<String> candidates, IntUnaryOperator random) {
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(random.applyAsInt(candidates.size())));
    }
}
