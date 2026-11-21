package dev.tc4.resourceadapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Modern destinations and the original edition that supplied each texture. */
record ResourceMapping(List<Rule> rules, String fingerprint) {
    record Rule(String target, String source, String editions, char hint) {}

    static ResourceMapping bundled() throws IOException {
        try (var input = ResourceMapping.class.getResourceAsStream("/adapter/mappings.tsv")) {
            if (input == null) throw new IOException("Missing adapter resource mappings");
            return parse(input.readAllBytes());
        }
    }

    static ResourceMapping parse(byte[] bytes) throws IOException {
        var rules = new ArrayList<Rule>();
        var targets = new HashSet<String>();
        for (String line : new String(bytes, StandardCharsets.UTF_8).split("\\R")) {
            if (line.isBlank() || line.startsWith("#")) continue;
            var fields = line.split("\t", -1);
            if (fields.length != 4 || !fields[0].matches("assets/[a-z0-9_.-]+/[a-z0-9_./-]+\\.png")
                    || !safe(fields[0]) || !fields[1].startsWith("assets/") || !fields[1].endsWith(".png")
                    || !safe(fields[1]) || !targets.add(fields[0] + "\t" + fields[2])
                    || !Set.of("*", "4", "5", "6", "45", "46", "56", "456").contains(fields[2])
                    || !Set.of("-", "4", "5").contains(fields[3])) {
                throw new IOException("Invalid or duplicate adapter mapping: " + line);
            }
            rules.add(new Rule(fields[0], fields[1].toLowerCase(Locale.ROOT), fields[2], fields[3].charAt(0)));
        }
        if (rules.isEmpty()) throw new IOException("Empty adapter mappings");
        return new ResourceMapping(List.copyOf(rules), PackConverter.sha256(bytes));
    }

    List<Rule> matches(Set<String> paths, int format) {
        var present = rules.stream().filter(row -> paths.contains(row.source())).toList();
        // 1.7.10 (TC4) and 1.8.9 (TC5) both use pack_format 1. Distinctive paths are
        // unambiguous even in mixed packs. TC4 is the default for shared paths alone.
        char edition = switch (format) {
            case 1 -> present.stream().anyMatch(r -> r.hint() == '5')
                    && present.stream().noneMatch(r -> r.hint() == '4') ? '5' : '4';
            case 2 -> '5';
            case 3 -> '6';
            default -> throw new IllegalArgumentException("Unsupported legacy pack format " + format);
        };
        var selected = new TreeMap<String, Rule>();
        for (var row : present) {
            char sourceEdition = format == 1 && row.hint() != '-' ? row.hint() : edition;
            if (row.editions().equals("*") || row.editions().indexOf(sourceEdition) >= 0) {
                selected.merge(row.target(), row, (a, b) -> priority(a, edition) <= priority(b, edition) ? a : b);
            }
        }
        return List.copyOf(selected.values());
    }

    private static int priority(Rule rule, char edition) {
        if (rule.editions().equals("*")) return 5;
        return rule.editions().indexOf(edition) >= 0 ? rule.editions().length() : 4;
    }

    static boolean safe(String path) {
        return !path.isEmpty() && !path.startsWith("/") && !path.contains("\\") && !path.contains(":")
                && !path.contains("\0") && java.util.Arrays.stream(path.split("/", -1))
                        .noneMatch(part -> part.isEmpty() || part.equals(".") || part.equals(".."));
    }
}
