package tc4revival.aspect;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Validated graph used by research combinations, decomposition and aspect discovery. */
public  class AspectCatalog {
    public enum Reduction { SUM, MAXIMUM }
    private final Map<String, Aspect> byTag = new LinkedHashMap<>();
    private final Map<Pair, Aspect> combinations = new HashMap<>();

    private final Map<String,String> aliases;
    private final boolean allowCycles;
    private final thaumcraft.api.aspects.AspectGraphService graph;
    public AspectCatalog(Collection<Aspect> definitions) { this(definitions,false,Map.of()); }
    public AspectCatalog(Collection<Aspect> definitions,boolean allowCycles,Map<String,String> aliases) {
        this.aliases=Map.copyOf(aliases);
        this.allowCycles=allowCycles;
        for (Aspect aspect : definitions) {
            if (byTag.putIfAbsent(aspect.tag(), aspect) != null) {
                throw new IllegalArgumentException("Duplicate aspect " + aspect.tag());
            }
        }
        var completed = new HashSet<String>();
        for (Aspect aspect : byTag.values()) {
            if(allowCycles) { for(var child:aspect.components())require(child); }
            else validate(aspect, new HashSet<>(), completed);
            if (!aspect.isPrimal()) {
                var pair = Pair.of(aspect.components().get(0), aspect.components().get(1));
                combinations.putIfAbsent(pair, aspect); // first registered result wins, as in TC4
            }
        }
        aliases.keySet().forEach(this::require);
        var edges=new LinkedHashMap<String,List<String>>();
        byTag.forEach((id,aspect)->edges.put(id,aspect.components().stream().map(this::canonical).toList()));
        graph=new thaumcraft.api.aspects.AspectGraphService(edges);
    }

    private String canonical(String tag) {
        var path=new HashSet<String>();
        while(aliases.containsKey(tag)) { if(!path.add(tag))throw new IllegalArgumentException("Cyclic alias " + tag);tag=aliases.get(tag); }
        return tag;
    }
    private void validate(Aspect aspect, Set<String> path, Set<String> completed) {
        if (completed.contains(aspect.tag())) return;
        if(path.size()>256)throw new IllegalArgumentException("Aspect graph exceeds safe depth");
        if (!path.add(aspect.tag())) throw new IllegalArgumentException("Cyclic aspect " + aspect.tag());
        for (String component : aspect.components()) validate(require(component), path, completed);
        path.remove(aspect.tag());
        completed.add(aspect.tag());
    }

    public List<Aspect> all() { return List.copyOf(byTag.values()); }
    public List<Aspect> primals() { return byTag.values().stream().filter(Aspect::isPrimal).toList(); }
    public List<Aspect> compounds() { return byTag.values().stream().filter(a -> !a.isPrimal()).toList(); }
    public Optional<Aspect> find(String tag) { return Optional.ofNullable(byTag.get(canonical(tag))); }
    public Aspect require(String tag) {
        return find(tag).orElseThrow(() -> new IllegalArgumentException("Unknown aspect " + tag));
    }

    public Optional<Aspect> combine(Aspect first, Aspect second) {
        if (first == null || second == null) return Optional.empty();
        return Optional.ofNullable(combinations.get(Pair.of(first.tag(), second.tag())));
    }

    public boolean areRelated(Aspect first, Aspect second) {
        return first.components().contains(second.tag()) || second.components().contains(first.tag());
    }

    public AspectAmounts toPrimals(AspectAmounts input, Reduction reduction) {
        Map<String,Long> amounts=allowCycles?new java.util.TreeMap<>():new LinkedHashMap<>();
        input.view().forEach((aspect,amount)->amounts.merge(canonical(aspect.tag()),(long)amount,Math::addExact));
        var mode=reduction==Reduction.SUM?thaumcraft.api.aspects.AspectGraphService.Reduction.SUM:thaumcraft.api.aspects.AspectGraphService.Reduction.MAXIMUM;
        var result=new AspectAmounts();
        (allowCycles?graph.reduce(amounts,mode):graph.reduceSigned(amounts,mode)).forEach((tag,amount)->result.add(require(tag),Math.toIntExact(amount)));
        return result;
    }
    public double weightedFold(Aspect aspect,java.util.function.ToDoubleFunction<String> primalWeight,double compoundCap) {
        return graph.weightedFold(canonical(aspect.tag()),primalWeight,compoundCap);
    }

    private record Pair(String first, String second) {
        static Pair of(String a, String b) {
            return a.compareTo(b) <= 0 ? new Pair(a, b) : new Pair(b, a);
        }
    }
}
