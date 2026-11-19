package tc4revival.research;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Direct dependencies, including hidden parents. Scan triggers are alternative clues, not an AND gate. */
public class ResearchRequirements {
    public enum Kind { RESEARCH, ITEM, ENTITY, ASPECT, WARP, SPECIAL }
    public record Requirement(Kind kind, String key, Optional<ResearchDefinition.Stack> item, boolean alternative) {}
    public static String id(Requirement row) {
        return row.kind().name() + ":" + row.key() + row.item().map(s -> "[" + s.key().metadata() + "," + s.ore().orElse("") + "," + s.nbt().orElse("") + "]").orElse("");
    }
    public static String canonical(String key) {
        var catalog = ResearchCatalogs.bundled();
        return catalog.find(key).isPresent() ? catalog.original(key).key() : key;
    }
    public static List<Requirement> collect(ResearchDefinition research) {
        var rows = new LinkedHashSet<Requirement>();
        var required = new LinkedHashSet<>(research.parents());
        required.addAll(research.hiddenParents());
        if (research.category().equals("ELDRITCH") && !research.key().equals("ELDRITCHMINOR")) required.add("ELDRITCHMINOR");
        required.forEach(key -> rows.add(new Requirement(Kind.RESEARCH, key, Optional.empty(), false)));
        research.parentsAny().stream().filter(key -> !required.contains(key)).forEach(key ->
                rows.add(new Requirement(Kind.RESEARCH, key, Optional.empty(), true)));
        var triggers = research.triggers();
        boolean alternative = triggers.items().orElse(List.of()).size() + triggers.entities().orElse(List.of()).size()
                + triggers.aspects().orElse(List.of()).size() > 1;
        triggers.items().orElse(List.of()).forEach(item -> rows.add(new Requirement(Kind.ITEM,
                item.key().item(), Optional.of(item), alternative)));
        triggers.entities().orElse(List.of()).forEach(key -> rows.add(new Requirement(Kind.ENTITY, key, Optional.empty(), alternative)));
        triggers.aspects().orElse(List.of()).forEach(key -> rows.add(new Requirement(Kind.ASPECT, key, Optional.empty(), alternative)));
        switch (research.key()) {
            case "BATHSALTS", "ELDRITCHMINOR", "ELDRITCHMAJOR" -> rows.add(new Requirement(Kind.WARP, research.key(), Optional.empty(), false));
            case "CRIMSON", "ENTEROUTER" -> rows.add(new Requirement(Kind.SPECIAL, research.key(), Optional.empty(), false));
        }
        return List.copyOf(rows);
    }
    public static int warpThreshold(String key) {
        return switch (key) { case "BATHSALTS" -> 11; case "ELDRITCHMINOR" -> 26; case "ELDRITCHMAJOR" -> 51; default -> 0; };
    }
    public record Row<T>(T value, boolean complete) {}
    public record Page<T>(List<Row<T>> rows, int omittedComplete, int omittedIncomplete) {
        public Page { rows = List.copyOf(rows); }
        public boolean truncated() { return omittedComplete + omittedIncomplete > 0; }
    }
    /** Reserve the last line for the omission summary only when a list actually overflows. */
    public static <T> Page<T> page(List<T> values, Predicate<T> complete, int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("A page needs at least one line");
        var rows = new ArrayList<Row<T>>();
        values.forEach(value -> rows.add(new Row<>(value, complete.test(value))));
        rows.sort(java.util.Comparator.comparing(Row::complete));
        int visible = Math.min(rows.size(), rows.size() > capacity ? capacity - 1 : capacity);
        int done = 0, missing = 0;
        for (int i = visible; i < rows.size(); i++) if (rows.get(i).complete()) done++; else missing++;
        return new Page<>(rows.subList(0, visible), done, missing);
    }
}
