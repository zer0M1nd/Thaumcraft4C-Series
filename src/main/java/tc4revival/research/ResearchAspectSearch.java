package tc4revival.research;

import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Function;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectCatalog;

/** Search seeds may be undiscovered; results always remain within the caller's visible aspects. */
public class ResearchAspectSearch {
    public static List<Aspect> filter(List<Aspect> visible, AspectCatalog catalog, String input, int strategy,
            Function<Aspect, String> translated, BiPredicate<String, String> translatedMatcher) {
        String query = input.strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return visible;
        boolean related = query.startsWith("~");
        String term = related ? query.substring(1) : query;
        if (term.isEmpty()) return visible;
        boolean latin = term.chars().allMatch(c -> c >= 'a' && c <= 'z');
        var candidates = related ? catalog.all() : visible;
        var matches = candidates.stream().filter(aspect -> {
            String original = aspect.displayName().toLowerCase(Locale.ROOT);
            return strategy != 1 && (latin ? original.startsWith(term) : original.contains(term))
                    || strategy != 0 && translatedMatcher.test(translated.apply(aspect), term);
        }).toList();
        // Exact names take precedence within the selected search mode.
        var exact = matches.stream().filter(aspect ->
                strategy != 1 && aspect.displayName().equalsIgnoreCase(term)
                        || strategy != 0 && translated.apply(aspect).strip().equalsIgnoreCase(term)).toList();
        var selected = exact.isEmpty() ? matches : exact;
        return visible.stream().filter(aspect -> related
                ? selected.stream().anyMatch(seed -> aspect.equals(seed) || catalog.areRelated(aspect, seed))
                : selected.contains(aspect)).toList();
    }
}
