package tc4revival.aspect.item;

import java.util.*;
import java.util.function.BiPredicate;

/** Enumerates membership without probing every authored tag for each item. */
@FunctionalInterface
public interface ItemTagLookup extends BiPredicate<String, String> {
    Collection<String> tags(String item);

    @Override default boolean test(String item, String tag) { return tags(item).contains(tag); }

    /** Capture the client's synchronized tags before calculation moves to a worker thread. */
    static ItemTagLookup snapshot(Map<String, ? extends Collection<String>> tagMembers) {
        var mutable = new HashMap<String, Set<String>>();
        tagMembers.forEach((tag, items) -> items.forEach(item -> mutable.computeIfAbsent(item, key -> new HashSet<>()).add(tag)));
        var byItem = new HashMap<String, Set<String>>();
        mutable.forEach((item, tags) -> byItem.put(item, Set.copyOf(tags)));
        var frozen = Map.copyOf(byItem);
        return item -> frozen.getOrDefault(item, Set.of());
    }
}
