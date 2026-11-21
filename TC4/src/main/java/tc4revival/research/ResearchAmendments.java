package tc4revival.research;

import com.google.gson.*;
import java.util.*;
import java.util.function.Predicate;

/** Data-pack changes belong to the affected research, regardless of their provider. */
public final class ResearchAmendments {
    public static final String REPLACEMENTS = "text_replacements";
    private ResearchAmendments() {}

    public static JsonObject materialize(JsonObject source, Predicate<String> loaded,
            Predicate<JsonObject> enabled) {
        var result = source.deepCopy();
        var changes = entries(result, "changes", loaded, enabled);
        var replacements = entries(result, "replacements", loaded, enabled);
        var counts = new HashMap<String, Integer>();
        for (var value : result.getAsJsonArray("pages")) {
            var entry = value.getAsJsonObject();
            String research = entry.get("key").getAsString();
            // Mirrors cannot own pages, even when an optional pack refers to them.
            if (mirror(result, research)) continue;
            var pages = new JsonArray();
            for (var change : changes) if (research.equals(change.get("research").getAsString())) {
                var page = new JsonObject();
                page.addProperty("id", "change/" + change.get("id").getAsString());
                page.addProperty("type", "TEXT");
                page.addProperty("amendment", true);
                page.addProperty("amendment_kind", change.get("kind").getAsString());
                page.addProperty("amendment_provider", change.get("provider").getAsString());
                page.add("amendment_provider_name", change.get("provider_name").deepCopy());
                page.add("text", styled(change, change.get("text")));
                if(change.has("summary"))page.add("summary",change.get("summary").deepCopy());
                var suffix = new JsonObject(); suffix.addProperty("text", "\n");
                page.getAsJsonObject("text").getAsJsonArray("extra").add(suffix);
                if (change.has("client_conditions")) page.add("client_conditions", change.get("client_conditions"));
                pages.add(page);
            }
            boolean prepended = !pages.isEmpty();
            int originalIndex = 0;
            for (var original : entry.getAsJsonArray("pages")) {
                var page = original.getAsJsonObject();
                if (prepended && originalIndex++ == 0) page.addProperty("amendment_join_start", true);
                var relevant = new JsonArray();
                for (var rule : replacements) if (research.equals(rule.get("research").getAsString())
                        && hasKey(page.get("text"), rule.getAsJsonArray("keys"))) relevant.add(rule);
                if (!relevant.isEmpty()) page.add(REPLACEMENTS, relevant);
                pages.add(page);
            }
            entry.add("pages", pages); counts.put(research, pages.size());
        }
        for (var category : result.getAsJsonArray("categories"))
            for (var value : category.getAsJsonObject().getAsJsonArray("research")) {
                var entry = value.getAsJsonObject(); var count = counts.get(entry.get("key").getAsString());
                if (count != null) entry.addProperty("pageCount", count);
            }
        result.remove("changes"); result.remove("replacements");
        return result;
    }

    private static boolean mirror(JsonObject data, String key) {
        for (var category : data.getAsJsonArray("categories"))
            for (var value : category.getAsJsonObject().getAsJsonArray("research")) {
                var entry = value.getAsJsonObject();
                if (key.equals(entry.get("key").getAsString()))
                    return entry.has("mirrorOf");
            }
        return false;
    }

    public static boolean hasKey(JsonElement component, JsonArray keys) {
        if (component == null || component.isJsonNull()) return false;
        if (component.isJsonArray()) return component.getAsJsonArray().asList().stream().anyMatch(e -> hasKey(e, keys));
        if (!component.isJsonObject()) return false;
        var json = component.getAsJsonObject();
        if (json.has("translate") && keys.contains(json.get("translate"))) return true;
        // Never replace hover payloads, identifiers or click targets.
        for (String field : List.of("extra", "with", "localized")) if (json.has(field)) {
            var child = json.get(field);
            if (field.equals("localized")) {
                if (child.getAsJsonObject().entrySet().stream().anyMatch(e -> hasKey(e.getValue(), keys))) return true;
            } else if (hasKey(child, keys)) return true;
        }
        return false;
    }

    private static List<JsonObject> entries(JsonObject source, String field, Predicate<String> loaded,
            Predicate<JsonObject> enabled) {
        var result = new ArrayList<JsonObject>(); var ids = new HashSet<String>();
        if (!source.has(field)) return result;
        for (var value : source.getAsJsonArray(field)) {
            var entry = value.getAsJsonObject();
            for (String required : List.of("id", "research", "kind", "provider", "provider_name"))
                if (!entry.has(required)) throw new IllegalArgumentException("Missing amendment " + required);
            String id = entry.get("id").getAsString(), kind = entry.get("kind").getAsString();
            if (id.isBlank() || !ids.add(id)) throw new IllegalArgumentException("Duplicate/empty amendment id: " + id);
            if (!Set.of("continuation", "integration").contains(kind))
                throw new IllegalArgumentException("Unknown amendment kind: " + kind);
            if (field.equals("changes") && !entry.has("text")) throw new IllegalArgumentException("Missing amendment text: " + id);
            if (field.equals("replacements") && (!entry.has("keys") || !entry.has("translations")))
                throw new IllegalArgumentException("Missing replacement keys/translations: " + id);
            if (!loaded.test(entry.get("provider").getAsString()) || entry.has("conditions") && !enabled.test(entry)) continue;
            entry.remove("conditions"); result.add(entry);
        }
        result.sort(Comparator.comparingInt((JsonObject e) -> e.get("kind").getAsString().equals("continuation") ? 0 : 1)
                .thenComparingInt(e -> e.has("priority") ? e.get("priority").getAsInt() : 0)
                .thenComparing(e -> e.get("id").getAsString()));
        return result;
    }

    /** Source styling shared by introductory changes and inline corrections. */
    public static JsonObject styled(JsonObject source, JsonElement content) {
        var result = new JsonObject(); result.addProperty("text", "");
        result.addProperty("color", source.get("kind").getAsString().equals("continuation") ? "dark_green" : "blue");
        result.addProperty("strikethrough", false);
        var extra = new JsonArray(); result.add("extra", extra);
        var hover = new JsonObject(); hover.addProperty("action", "show_text");
        var localized = new JsonObject();
        for (String locale : List.of("zh_cn", "en_us")) {
            boolean chinese = locale.equals("zh_cn"), own = source.get("kind").getAsString().equals("continuation");
            var tooltip = new JsonArray();
            if (!own) tooltip.add(chinese ? "来自 " : "Changes from ");
            tooltip.add(source.get("provider_name").deepCopy());
            tooltip.add(own ? (chinese ? " 承续版新增特性" : " Continuation features") : (chinese ? " 的修改" : ""));
            if (source.has("configuration")) tooltip.add((chinese ? "\n可在配置文件中关闭：" : "\nDisable in configuration: ")
                    + source.get("configuration").getAsString());
            localized.add(locale, tooltip);
        }
        var contents = new JsonObject(); contents.add("localized", localized);
        contents.addProperty("color",source.get("kind").getAsString().equals("continuation")?"green":"blue");
        contents.addProperty("underlined",false);
        hover.add("contents", contents);
        result.add("hoverEvent", hover);
        extra.add(provenance(content, result));
        return result;
    }

    private static JsonElement provenance(JsonElement content, JsonObject style) {
        var value = content.deepCopy();
        if (value.isJsonArray()) {
            var out = new JsonArray(); for (var child : value.getAsJsonArray()) out.add(provenance(child, style)); return out;
        }
        if (!value.isJsonObject()) return value;
        var object = value.getAsJsonObject();
        for (String field : List.of("extra", "with")) if (object.has(field)) object.add(field, provenance(object.get(field), style));
        if (object.has("localized")) for (var entry : object.getAsJsonObject("localized").entrySet()) entry.setValue(provenance(entry.getValue(), style));
        object.add("color", style.get("color").deepCopy());
        object.add("hoverEvent", style.get("hoverEvent").deepCopy());
        object.addProperty("strikethrough", false);
        return object;
    }
}
