package tc4revival.client.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import tc4revival.aspect.Aspect;

/** Localized descriptions can contain several names, e.g. "Order, Regularity, Purity". */
final class AspectNames {
    private record Name(Aspect aspect, String latin, List<String> translated, List<JecPronunciation> pinyin) {}
    private final List<Name> names = new ArrayList<>();

    AspectNames(Collection<Aspect> aspects, Function<Aspect, String> translation,
                Function<Aspect, String> chinese, Function<String, JecPronunciation> spell) {
        for (var aspect : aspects) {
            var translated = aliases(translation.apply(aspect));
            var pinyin = aliases(chinese.apply(aspect)).stream().map(spell)
                    .filter(java.util.Objects::nonNull).toList();
            names.add(new Name(aspect, aspect.displayName().toLowerCase(Locale.ROOT), translated, pinyin));
        }
    }

    private static List<String> aliases(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[,，、;；]"))
                .map(String::strip).filter(s -> !s.isEmpty()).distinct().toList();
    }

    List<Aspect> find(String term) {
        var exact = new ArrayList<Aspect>();
        var partial = new ArrayList<Aspect>();
        for (var name : names) {
            if (name.latin.equals(term) || name.aspect.tag().equals(term) || name.translated.contains(term)
                    || name.pinyin.stream().anyMatch(p -> p.matches(term, true))) {
                exact.add(name.aspect);
            } else if (exact.isEmpty() && (name.latin.startsWith(term) || name.aspect.tag().startsWith(term)
                    || name.translated.stream().anyMatch(s -> s.contains(term))
                    || name.pinyin.stream().anyMatch(p -> p.matches(term, false)))) {
                partial.add(name.aspect);
            }
        }
        return List.copyOf(exact.isEmpty() ? partial : exact);
    }
}
