package tc4revival.research;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import tc4revival.aspect.recipe.RecipeAspectData.Key;

/** Immutable research metadata. Recipe/text pages are a separate dependency, not fabricated here. */
public record ResearchDefinition(String key, String category, Map<String, Integer> aspects,
        List<String> parents, List<String> hiddenParents, List<String> siblings,
        Set<Flag> flags, int complexity, int warp, Display display, Triggers triggers, List<String> parentsAny,
        NoteSeeds noteSeeds, Reference reference, Optional<ResearchNoteLayout> noteLayout, String modId,
        List<String> suggestedResearch) {
    /** Retain the source-aware constructor used by existing addons. */
    public ResearchDefinition(String key,String category,Map<String,Integer> aspects,List<String> parents,
            List<String> hiddenParents,List<String> siblings,Set<Flag> flags,int complexity,int warp,Display display,
            Triggers triggers,List<String> parentsAny,NoteSeeds noteSeeds,Reference reference,
            Optional<ResearchNoteLayout> noteLayout,String modId) {
        this(key,category,aspects,parents,hiddenParents,siblings,flags,complexity,warp,display,triggers,
                parentsAny,noteSeeds,reference,noteLayout,modId,List.of());
    }
    /** Retain the previous canonical constructor for addon binary compatibility. */
    public ResearchDefinition(String key,String category,Map<String,Integer> aspects,List<String> parents,
            List<String> hiddenParents,List<String> siblings,Set<Flag> flags,int complexity,int warp,Display display,
            Triggers triggers,List<String> parentsAny,NoteSeeds noteSeeds,Reference reference,
            Optional<ResearchNoteLayout> noteLayout) {
        this(key,category,aspects,parents,hiddenParents,siblings,flags,complexity,warp,display,triggers,
                parentsAny,noteSeeds,reference,noteLayout,ResearchOrigins.namespace(key));
    }
    /** Retain the pre-layout constructor for addon binary compatibility. */
    public ResearchDefinition(String key,String category,Map<String,Integer> aspects,List<String> parents,
            List<String> hiddenParents,List<String> siblings,Set<Flag> flags,int complexity,int warp,Display display,
            Triggers triggers,List<String> parentsAny,NoteSeeds noteSeeds,Reference reference) {
        this(key,category,aspects,parents,hiddenParents,siblings,flags,complexity,warp,display,triggers,
                parentsAny,noteSeeds,reference,Optional.empty());
    }
    public record Reference(Optional<String> target, Map<String,String> parentMirrors) {
        public static final Reference EMPTY = new Reference(Optional.empty(), Map.of());
        public Reference { java.util.Objects.requireNonNull(target); parentMirrors=Map.copyOf(parentMirrors); }
    }
    public boolean mirror() { return reference.target().isPresent(); }
    public ResearchDefinition(String key,String category,Map<String,Integer> aspects,List<String> parents,
            List<String> hiddenParents,List<String> siblings,Set<Flag> flags,int complexity,int warp,Display display,
            Triggers triggers,List<String> parentsAny,NoteSeeds noteSeeds) {
        this(key,category,aspects,parents,hiddenParents,siblings,flags,complexity,warp,display,triggers,parentsAny,noteSeeds,Reference.EMPTY);
    }
    /** Optional extra fixed cells. Lists allow the same aspect at more than one location. */
    public record NoteSeeds(List<String> inner, List<String> negative) {
        public static final NoteSeeds EMPTY = new NoteSeeds(List.of(), List.of());
        public NoteSeeds { inner = List.copyOf(inner); negative = List.copyOf(negative); }
        public boolean empty() { return inner.isEmpty() && negative.isEmpty(); }
    }
    /** Retain the pre-extension constructor for addon binary compatibility. */
    public ResearchDefinition(String key,String category,Map<String,Integer> aspects,List<String> parents,
            List<String> hiddenParents,List<String> siblings,Set<Flag> flags,int complexity,int warp,Display display,
            Triggers triggers,List<String> parentsAny) {
        this(key,category,aspects,parents,hiddenParents,siblings,flags,complexity,warp,display,triggers,parentsAny,NoteSeeds.EMPTY);
    }
    /** Existing callers retain the original all-parents behavior. */
    public ResearchDefinition(String key,String category,Map<String,Integer> aspects,List<String> parents,
            List<String> hiddenParents,List<String> siblings,Set<Flag> flags,int complexity,int warp,Display display,Triggers triggers) {
        this(key,category,aspects,parents,hiddenParents,siblings,flags,complexity,warp,display,triggers,List.of());
    }
    public enum Flag { SPECIAL, SECONDARY, ROUND, STUB, VIRTUAL, CONCEALED, HIDDEN, LOST, AUTO_UNLOCK, TUTORIAL }
    public boolean tutorial() { return has(Flag.TUTORIAL); }
    /** Tutorials and mirrors are navigation aids, not gameplay completion requirements. */
    public boolean countsForProgress() { return !mirror() && !tutorial(); }
    public record Stack(Key key, int count, boolean damageable, Optional<String> nbt, Optional<String> ore) {
        /** A fresh mutable tag for a display stack; the immutable observed text remains unchanged. */
        public Optional<net.minecraft.nbt.CompoundTag> tag() { return nbt.map(LegacyStackNbt::read); }
        public Stack {
            java.util.Objects.requireNonNull(key);
            java.util.Objects.requireNonNull(nbt);
            java.util.Objects.requireNonNull(ore);
            if (count < 1) throw new IllegalArgumentException("Research icon/trigger must contain an item");
        }
    }
    public record Icon(Optional<String> resource, Optional<Stack> item) {
        public Icon {
            java.util.Objects.requireNonNull(resource);java.util.Objects.requireNonNull(item);
            if(resource.isPresent()==item.isPresent())throw new IllegalArgumentException("An icon needs one texture or item");
        }
    }
    public record Display(int column, int row, Optional<String> iconResource, Optional<Stack> iconItem, int pageCount,
                          List<Icon> icons) {
        /** Preserve the existing addon constructor and single-icon accessors. */
        public Display(int column,int row,Optional<String> iconResource,Optional<Stack> iconItem,int pageCount) {
            this(column,row,iconResource,iconItem,pageCount,List.of());
        }
        public Optional<Icon> icon(long milliseconds) {
            if(!icons.isEmpty())return Optional.of(icons.get((int)Math.floorMod(milliseconds/1000,icons.size())));
            return iconItem.isPresent()?Optional.of(new Icon(Optional.empty(),iconItem)):
                    iconResource.map(value->new Icon(Optional.of(value),Optional.empty()));
        }
        public Display {
            java.util.Objects.requireNonNull(iconResource);
            java.util.Objects.requireNonNull(iconItem);
            icons=List.copyOf(icons);
            if (pageCount < 0) throw new IllegalArgumentException("Negative page count");
        }
    }
    /** Absent and present-but-empty triggers differ in legacy hidden-research discovery. */
    public record Triggers(Optional<List<Stack>> items, Optional<List<String>> entities, Optional<List<String>> aspects) {
        public Triggers {
            items = items.map(List::copyOf);
            entities = entities.map(List::copyOf);
            aspects = aspects.map(List::copyOf);
        }
        public boolean present() { return items.isPresent() || entities.isPresent() || aspects.isPresent(); }
    }
    public ResearchDefinition {
        if (key == null || key.length()>128 || key.contains("..") || !key.matches("[A-Za-z0-9_:./-]+") || category == null || category.isBlank()) {
            throw new IllegalArgumentException("Invalid research identity");
        }
        aspects = Collections.unmodifiableMap(new LinkedHashMap<>(aspects));
        parents = List.copyOf(parents);
        hiddenParents = List.copyOf(hiddenParents);
        parentsAny = List.copyOf(parentsAny);
        siblings = List.copyOf(siblings);
        flags = Set.copyOf(flags);
        suggestedResearch = List.copyOf(suggestedResearch);
        java.util.Objects.requireNonNull(display);
        java.util.Objects.requireNonNull(triggers);
        java.util.Objects.requireNonNull(noteSeeds);
        java.util.Objects.requireNonNull(reference);
        java.util.Objects.requireNonNull(noteLayout);
        if (modId == null || !modId.matches("[a-z0-9_.-]+"))
            throw new IllegalArgumentException("Invalid research source modid");
        if (complexity < 0 || complexity > 3 || warp < 0) throw new IllegalArgumentException("Invalid research values");
    }
    public boolean has(Flag flag) { return flags.contains(flag); }
}
