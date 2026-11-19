package tc4revival.research;

import java.util.*;
import java.util.function.Predicate;
import tc4revival.aspect.AspectCatalog;
import tc4revival.config.TCConfig;

/** One paid, unfinished prerequisite per target. The server alone commits the choice and cost. */
public class ResearchGuidance {
    public static final String RESEARCH = "REVIVAL_LISTENING_GUIDANCE";
    public static boolean enabled() { return TCConfig.SERVER.revival.listeningGuidance.get(); }
    public static List<String> cost(ResearchDefinition research) {
        var tags = new LinkedHashSet<String>();
        if (research.has(ResearchDefinition.Flag.SECONDARY)) {
            research.aspects().entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                    .limit(3).forEach(e -> tags.add(e.getKey()));
        } else if (research.noteLayout().isPresent()) {
            research.noteLayout().get().initialGrid().values().stream().filter(ResearchNote.Cell::fixed).forEach(cell -> tags.add(cell.aspect()));
        } else {
            tags.addAll(research.aspects().keySet());
            tags.addAll(research.noteSeeds().inner());
            tags.addAll(research.noteSeeds().negative());
        }
        return List.copyOf(tags);
    }
    public static boolean eligible(ResearchDefinition target, PlayerKnowledge player, ResearchCatalog catalog) {
        if (player == null || !enabled() || !player.researchMarkers().contains(RESEARCH) || target.mirror() || target.tutorial()) return false;
        return blocked(target, player, catalog);
    }
    public static boolean blocked(ResearchDefinition target, PlayerKnowledge player, ResearchCatalog catalog) {
        if (player == null || target.mirror() || target.tutorial() || !ResearchCatalogs.enabled(target)) return false;
        var rules = new ResearchRules(catalog, ResearchCatalogs::enabled);
        return !rules.completed(player, target.key()) && (!rules.prerequisites(player, target.key())
                || !ResearchBookActions.visible(player,target,rules,ResearchCatalogs::enabled));
    }
    public record Path(ResearchRequirementData.Entry root, ResearchRequirementData.Entry condition, int depth) {
        public Path(ResearchRequirementData.Entry root, ResearchRequirementData.Entry condition) { this(root, condition, 1); }
    }
    /** Scope scan clues to their research, so a completed clue cannot reveal another branch for free. */
    public static String id(ResearchRequirementData.Entry row) {
        String owner = row.requirement().kind() == ResearchRequirements.Kind.RESEARCH ? "" : row.owner();
        return "v2:" + owner.length() + ":" + owner + ResearchRequirements.id(row.requirement());
    }
    public record Options(List<ResearchRequirementData.Entry> free, List<Path> paths) {
        public Options { free = List.copyOf(free); paths = List.copyOf(paths); }
        /** Retained for callers of the original line-based projection. */
        public List<ResearchRequirementData.Entry> connected() { return free; }
        public Optional<Path> current(String target, PlayerKnowledge player) {
            String marker = player == null ? null : player.guidance().get(target);
            return paths.stream().filter(path -> id(path.condition()).equals(marker)).min(Comparator.comparingInt(Path::depth));
        }
    }
    /** Convert the old direct-parent marker once, before the opening snapshot is sent to the client. */
    public static boolean migrate(PlayerKnowledge player, ResearchCatalog catalog,
            java.util.function.Function<ResearchDefinition,List<ResearchRequirementData.Entry>> resolve) {
        boolean changed = false;
        for (var saved : Map.copyOf(player.guidance()).entrySet()) {
            if (saved.getValue().startsWith("v2:")) continue;
            var target = catalog.find(saved.getKey());
            if (target.isEmpty()) continue;
            var options = options(target.get(), player, catalog, resolve);
            var choice = options.paths().stream().filter(path -> ResearchRequirements.id(path.condition().requirement()).equals(saved.getValue())).findFirst();
            if (choice.isEmpty()) choice = options.paths().stream().filter(path -> ResearchRequirements.id(path.root().requirement()).equals(saved.getValue())).findFirst();
            if (choice.isPresent()) {
                player.rememberGuidance(saved.getKey(), id(choice.get().condition()));
                changed = true;
            }
        }
        return changed;
    }
    /** Same-page, researchable direct parents are free; paid clues stop at actionable leaves. */
    public static Options options(ResearchDefinition target, PlayerKnowledge player, ResearchCatalog catalog,
            java.util.function.Function<ResearchDefinition,List<ResearchRequirementData.Entry>> resolve) {
        if (!blocked(target, player, catalog)) return new Options(List.of(), List.of());
        var context = new Context(player, catalog, resolve);
        var direct = context.missing(target);
        var free = direct.stream().filter(row -> row.requirement().kind() == ResearchRequirements.Kind.RESEARCH
                && catalog.find(context.canonical(row.requirement().key()))
                        .filter(parent -> parent.category().equals(target.category()) && context.researchable(parent)).isPresent()).toList();
        var paths = new ArrayList<Path>();
        for (var root : direct) paths.addAll(context.expand(target, root));
        return new Options(free, paths);
    }
    private static final class Context {
        final PlayerKnowledge player;
        final ResearchCatalog catalog;
        final ResearchRules rules;
        final java.util.function.Function<ResearchDefinition,List<ResearchRequirementData.Entry>> resolve;
        final Map<String,List<ResearchRequirementData.Entry>> rows = new HashMap<>();
        Context(PlayerKnowledge player, ResearchCatalog catalog,
                java.util.function.Function<ResearchDefinition,List<ResearchRequirementData.Entry>> resolve) {
            this.player=player;this.catalog=catalog;this.resolve=resolve;rules=new ResearchRules(catalog,ResearchCatalogs::enabled);
        }
        String canonical(String key) { return catalog.find(key).map(r->catalog.original(key).key()).orElse(key); }
        boolean researchable(ResearchDefinition research) {
            return ResearchBookActions.visible(player,research,rules,ResearchCatalogs::enabled)
                    && rules.prerequisites(player,research.key()) && !research.has(ResearchDefinition.Flag.VIRTUAL)
                    && !research.has(ResearchDefinition.Flag.AUTO_UNLOCK) && !research.has(ResearchDefinition.Flag.STUB);
        }
        List<ResearchRequirementData.Entry> missing(ResearchDefinition research) {
            var all = rows.computeIfAbsent(research.key(), key -> resolve.apply(research));
            boolean anyParent = all.stream().anyMatch(row -> row.requirement().alternative()
                    && row.requirement().kind()==ResearchRequirements.Kind.RESEARCH && row.complete(player));
            boolean anyScan = all.stream().anyMatch(row -> row.requirement().alternative()
                    && row.requirement().kind()!=ResearchRequirements.Kind.RESEARCH && row.complete(player));
            var result = all.stream().filter(row -> !row.complete(player))
                    .filter(row -> !row.requirement().alternative() || !(row.requirement().kind()==ResearchRequirements.Kind.RESEARCH?anyParent:anyScan))
                    .filter(row -> switch(row.requirement().kind()) {
                        case ITEM, ENTITY, ASPECT -> research.has(ResearchDefinition.Flag.HIDDEN) || research.has(ResearchDefinition.Flag.LOST);
                        default -> true;
                    }).toList();
            if (!result.isEmpty() || researchable(research) || rules.completed(player,research.key())) return result;
            // Addon/event unlocks without a scan definition still have a non-navigable clue.
            String key = (research.has(ResearchDefinition.Flag.HIDDEN)||research.has(ResearchDefinition.Flag.LOST)?"clue/":"")+research.key();
            var requirement = new ResearchRequirements.Requirement(ResearchRequirements.Kind.SPECIAL,key,Optional.empty(),false);
            return List.of(new ResearchRequirementData.Entry(requirement,List.of(),List.of(),"",research.key()));
        }
        List<Path> expand(ResearchDefinition target, ResearchRequirementData.Entry root) {
            var visited = new HashSet<>(Set.of(target.key()));
            var result = new LinkedHashMap<String,Path>();
            var pending = new ArrayDeque<Path>();
            pending.add(new Path(root, root, 1));
            // Breadth-first traversal assigns the shortest depth when branches converge.
            while (!pending.isEmpty()) {
                var path = pending.removeFirst();
                var row = path.condition();
                if (row.complete(player)) continue;
                if (row.requirement().kind() != ResearchRequirements.Kind.RESEARCH) {
                    result.putIfAbsent(id(row), path);
                    continue;
                }
                String key = canonical(row.requirement().key());
                var definition = catalog.find(key).filter(ResearchCatalogs::enabled);
                if (definition.isEmpty() || !visited.add(key)) continue;
                if (researchable(definition.get())) result.putIfAbsent(id(row), path);
                else for (var child : missing(definition.get())) pending.addLast(new Path(root, child, path.depth() + 1));
            }
            return List.copyOf(result.values());
        }
    }
    public static ResearchBookActions.Result listen(ResearchDefinition target, PlayerKnowledge player, ResearchCatalog catalog,
            AspectCatalog aspects, java.util.function.Function<ResearchDefinition,List<ResearchRequirementData.Entry>> resolve,
            java.util.function.IntUnaryOperator random) {
        if (!eligible(target,player,catalog)) return rejected();
        var options=options(target,player,catalog,resolve);
        if (!options.free().isEmpty() || options.current(target.key(),player).isPresent()) return rejected();
        var candidates=new LinkedHashMap<String,ResearchRequirementData.Entry>();
        options.paths().forEach(path->candidates.putIfAbsent(id(path.condition()),path.condition()));
        if(candidates.isEmpty())return rejected();
        var cost=cost(target);
        if(cost.stream().anyMatch(tag->!player.knows(aspects.require(tag))||player.poolAmount(aspects.require(tag))<1))
            return new ResearchBookActions.Result(Optional.empty(),Optional.of(ResearchBookActions.Rejection.POINTS));
        var next=player.copy();cost.forEach(tag->next.changePool(aspects.require(tag),-1));
        next.rememberGuidance(target.key(),new ArrayList<>(candidates.keySet()).get(random.applyAsInt(candidates.size())));
        return new ResearchBookActions.Result(Optional.of(new ResearchBookActions.Change(next,List.of(),List.of())),Optional.empty());
    }
    /** Compatibility projection for callers supplying the original direct requirement list. */
    public static Optional<ResearchRequirements.Requirement> current(String target, PlayerKnowledge player,
            List<ResearchRequirements.Requirement> rows, Predicate<ResearchRequirements.Requirement> complete) {
        if (player == null) return Optional.empty();
        String chosen = player.guidance().get(target);
        return rows.stream().filter(row -> ResearchRequirements.id(row).equals(chosen) && !complete.test(row))
                .filter(row -> !row.alternative() || rows.stream().noneMatch(other -> other.alternative()
                        && (other.kind()==ResearchRequirements.Kind.RESEARCH)==(row.kind()==ResearchRequirements.Kind.RESEARCH) && complete.test(other)))
                .findFirst();
    }
    public static boolean revealed(String target, ResearchRequirements.Requirement row, boolean complete, PlayerKnowledge player) {
        return !enabled() || complete || player != null && ResearchRequirements.id(row).equals(player.guidance().get(target));
    }
    public static ResearchBookActions.Result listen(ResearchDefinition target, PlayerKnowledge player, ResearchCatalog catalog,
            AspectCatalog aspects, List<ResearchRequirements.Requirement> rows, Predicate<ResearchRequirements.Requirement> complete) {
        return listen(target,player,catalog,aspects,rows,complete,java.util.concurrent.ThreadLocalRandom.current()::nextInt);
    }
    public static ResearchBookActions.Result listen(ResearchDefinition target, PlayerKnowledge player, ResearchCatalog catalog,
            AspectCatalog aspects, List<ResearchRequirements.Requirement> rows, Predicate<ResearchRequirements.Requirement> complete,
            java.util.function.IntUnaryOperator random) {
        if (!eligible(target, player, catalog) || current(target.key(), player, rows, complete).isPresent()) return rejected();
        // An OR group that is already satisfied does not need a second clue.
        boolean anyParent = rows.stream().anyMatch(r -> r.alternative() && r.kind() == ResearchRequirements.Kind.RESEARCH && complete.test(r));
        boolean anyScan = rows.stream().anyMatch(r -> r.alternative() && r.kind() != ResearchRequirements.Kind.RESEARCH && complete.test(r));
        var candidates = rows.stream().filter(r -> !complete.test(r))
                .filter(r -> !r.alternative() || !(r.kind() == ResearchRequirements.Kind.RESEARCH ? anyParent : anyScan)).toList();
        if (candidates.isEmpty()) return rejected();
        var cost = cost(target);
        if (cost.stream().anyMatch(tag -> !player.knows(aspects.require(tag)) || player.poolAmount(aspects.require(tag)) < 1))
            return new ResearchBookActions.Result(Optional.empty(), Optional.of(ResearchBookActions.Rejection.POINTS));
        var next = player.copy();
        cost.forEach(tag -> next.changePool(aspects.require(tag), -1));
        next.rememberGuidance(target.key(), ResearchRequirements.id(candidates.get(random.applyAsInt(candidates.size()))));
        return new ResearchBookActions.Result(Optional.of(new ResearchBookActions.Change(next, List.of(), List.of())), Optional.empty());
    }
    private static ResearchBookActions.Result rejected() {
        return new ResearchBookActions.Result(Optional.empty(), Optional.of(ResearchBookActions.Rejection.PREREQUISITES));
    }
}
