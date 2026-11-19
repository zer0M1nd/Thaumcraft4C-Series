package tc4revival.research;

import java.util.Optional;
import java.util.function.Predicate;

/** Ordered legacy recipe lookup. The caller supplies platform-specific item equality. */
public  class ResearchRecipeLinks {
    public record Target(String research, int sourcePage) {}
    private final ResearchPages pages;
    private final ResearchPageRecipes recipes;
    public ResearchRecipeLinks(ResearchPages pages, ResearchPageRecipes recipes) {
        this.pages = pages; this.recipes = recipes;
    }
    public Optional<Target> find(Predicate<ResearchDefinition.Stack> matches, Predicate<String> complete) {
        return find(matches,complete,false);
    }
    /** Inventory lookup may skip a locked recipe in favor of another known recipe for the same item. */
    public Optional<Target> findCompleted(Predicate<ResearchDefinition.Stack> matches, Predicate<String> complete) {
        return find(matches,complete,true);
    }
    private Optional<Target> find(Predicate<ResearchDefinition.Stack> matches, Predicate<String> complete,boolean skipLocked) {
        for (var entry : pages.entries().entrySet()) {
            if(skipLocked&&!complete.test(entry.getKey()))continue;
            for (int index = 0; index < entry.getValue().size(); index++) {
                var page = entry.getValue().get(index);
                // The old array-of-crucibles branch continues past incomplete matches.
                if (page.type() == ResearchPage.Type.CRUCIBLE_CRAFTING && page.recipe() instanceof ResearchPage.Sequence sequence) {
                    for (var choice : sequence.elements()) {
                        if (!(choice instanceof ResearchPage.Reference reference)) continue;
                        var definition = recipes.require(reference.keys().get(0)).definition();
                        if (definition instanceof ResearchPageRecipes.Crucible crucible
                                && crucible.output() instanceof ResearchPage.Item output && matches.test(output.stack())
                                && complete.test(entry.getKey())) return Optional.of(new Target(entry.getKey(), index));
                    }
                    continue;
                }
                if (page.output().isPresent() && matches.test(page.output().get()))
                    return complete.test(entry.getKey()) ? Optional.of(new Target(entry.getKey(), index)) : Optional.empty();
            }
        }
        return Optional.empty();
    }
}
