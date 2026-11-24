package tc4revival.client;

import com.google.gson.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.StringDecomposer;
import tc4revival.research.ResearchAmendments;
import tc4revival.config.TCConfig.LegacyTextMode;

/** Exact, counted amendments to visible text; never searches tooltip or click payloads. */
public final class BookTextAmendments {
    public static final String CORRECTION = "thaumcraft:book_correction";
    private record Run(String text, Style style) {}
    private record Edit(int start, int end, Component replacement) {}
    private BookTextAmendments() {}

    public static List<Component> apply(List<Component> input, JsonArray rules, String key, String locale,
            Function<JsonElement, Component> decode, BiConsumer<String, String> warning) {
        return apply(input, rules, key, locale, decode, warning, LegacyTextMode.ALL);
    }

    public static List<Component> apply(List<Component> input, JsonArray rules, String key, String locale,
            Function<JsonElement, Component> decode, BiConsumer<String, String> warning, LegacyTextMode mode) {
        // Every rule is planned against the same original text. Inserted corrections
        // must never become another rule's input (or shift its original coordinates).
        var flattened = input.stream().map(BookTextAmendments::runs).toList();
        var edits = new ArrayList<List<Edit>>();
        for (int i = 0; i < input.size(); i++) edits.add(new ArrayList<>());
        for (var element : rules) {
            var rule = element.getAsJsonObject();
            if (!rule.getAsJsonArray("keys").contains(new JsonPrimitive(key))) continue;
            var translations = rule.getAsJsonObject("translations");
            if (!translations.has(locale)) continue; // A resource-pack translation is never corrected in another language.
            var translation = translations.getAsJsonObject(locale);
            String find = translation.get("find").getAsString();
            int expected = translation.has("count") ? translation.get("count").getAsInt() : 1;
            if (find.isEmpty() || expected < 1) throw new IllegalArgumentException("Invalid replacement: " + rule.get("id"));
            int count = 0;
            var matches = new ArrayList<List<Integer>>();
            for (var runs : flattened) {
                var positions = new ArrayList<Integer>();
                String original = text(runs);
                for (int at = 0; (at = original.indexOf(find, at)) >= 0; at += find.length())
                    if (!amended(runs, at, at + find.length())) positions.add(at);
                matches.add(positions); count += positions.size();
            }
            if (count == 0) continue;
            if (count != expected) {
                warning.accept(rule.get("id").getAsString(), "Expected " + expected + " matches in " + key + ", found " + count);
                continue;
            }
            boolean overlap = false;
            for (int i = 0; i < matches.size(); i++) for (int start : matches.get(i))
                for (var edit : edits.get(i)) if (start < edit.end() && start + find.length() > edit.start()) overlap = true;
            if (overlap) {
                warning.accept(rule.get("id").getAsString(), "Overlapping replacement in " + key); continue;
            }
            var replacement = translation.get("replace");
            Component decorated = decode.apply(replacement);
            if (mode != LegacyTextMode.HIDDEN) {
                var styled = decode.apply(ResearchAmendments.styled(rule, replacement));
                var style = styled.getStyle();
                if (mode == LegacyTextMode.HIGHLIGHT) {
                    var details = style.getHoverEvent().getValue(HoverEvent.Action.SHOW_TEXT).copy();
                    details.append("\n").append(Component.translatable("thaumcraft.book.old_text", find));
                    style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, details));
                }
                decorated = forceStyle(styled, style);
            }
            for (int i = 0; i < matches.size(); i++) for (int start : matches.get(i))
                edits.get(i).add(new Edit(start, start + find.length(), decorated));
        }
        var result = new ArrayList<Component>();
        for (int i = 0; i < input.size(); i++) result.add(edits.get(i).isEmpty() ? input.get(i) : replace(flattened.get(i), edits.get(i), mode));
        return List.copyOf(result);
    }

    private static boolean amended(List<Run> runs, int start, int end) {
        int at = 0;
        for (var run : runs) {
            int next = at + run.text().length();
            if (at < end && next > start && (run.style().isStrikethrough() || CORRECTION.equals(run.style().getInsertion()))) return true;
            at = next;
        }
        return false;
    }
    private static List<Run> runs(Component source) {
        var result = new ArrayList<Run>();
        source.visit((style, text) -> {
            var buffer = new StringBuilder(); Style[] current = {style};
            StringDecomposer.iterateFormatted(text, style, (offset, s, code) -> {
                if (!s.equals(current[0])) {
                    if (!buffer.isEmpty()) result.add(new Run(buffer.toString(), current[0]));
                    buffer.setLength(0); current[0] = s;
                }
                buffer.appendCodePoint(code); return true;
            });
            if (!buffer.isEmpty()) result.add(new Run(buffer.toString(), current[0]));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
    private static String text(List<Run> runs) {
        var out = new StringBuilder(); for (var run : runs) out.append(run.text()); return out.toString();
    }
    private static Component forceStyle(Component source, Style provenance) {
        var out = Component.empty();
        for (var run : runs(source)) out.append(Component.literal(run.text()).withStyle(run.style()
                .withColor(provenance.getColor()).withHoverEvent(provenance.getHoverEvent())
                .withStrikethrough(false).withInsertion(CORRECTION)));
        return out;
    }
    private static Component replace(List<Run> runs, List<Edit> edits, LegacyTextMode mode) {
        String text = text(runs); var out = Component.empty(); int cursor = 0;
        edits.sort(Comparator.comparingInt(Edit::start));
        for (var edit : edits) {
            slice(out, runs, cursor, edit.start(), false);
            if (mode == LegacyTextMode.ALL) slice(out, runs, edit.start(), edit.end(), true);
            var replacement = edit.replacement().copy();
            if (mode != LegacyTextMode.ALL) {
                // A replacement inside a styled link keeps that link. A match that
                // crosses unrelated links cannot safely inherit either target.
                Style common = null;
                int offset = 0;
                for (var run : runs) {
                    int end = offset + run.text().length();
                    if (end > edit.start() && offset < edit.end()) {
                        if (common == null) common = run.style();
                        else if (!common.equals(run.style())) { common = null; break; }
                    }
                    if (end >= edit.end()) break;
                    offset = end;
                }
                if (common != null) replacement.withStyle(common);
            }
            out.append(replacement); cursor = edit.end();
        }
        slice(out, runs, cursor, text.length(), false); return out;
    }
    private static void slice(MutableComponent out, List<Run> runs, int from, int to, boolean strike) {
        int offset = 0;
        for (var run : runs) {
            int start = Math.max(0, from - offset), end = Math.min(run.text().length(), to - offset);
            if (end > start) out.append(Component.literal(run.text().substring(start, end))
                    .withStyle(strike ? run.style().withStrikethrough(true) : run.style()));
            offset += run.text().length(); if (offset >= to) break;
        }
    }
}
