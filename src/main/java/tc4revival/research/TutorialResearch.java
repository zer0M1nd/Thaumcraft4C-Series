package tc4revival.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Tutorial data belongs to its owning mod and follows that mod's optional dependency conditions. */
public final class TutorialResearch {
    private TutorialResearch() {}

    public static JsonObject materialize(JsonObject source, Predicate<JsonObject> condition) {
        return materialize(source, true, condition);
    }

    static JsonObject materialize(JsonObject source, boolean enabled, Predicate<JsonObject> condition) {
        var result = source.deepCopy();
        var removed = new HashSet<String>();
        var present = new HashSet<String>();
        var categories = result.getAsJsonArray("categories");
        for (var tabs = categories.iterator(); tabs.hasNext();) {
            var category = tabs.next().getAsJsonObject();
            boolean skipTab = !enabled && "TUTORIAL".equals(category.get("key").getAsString());
            for (var members = category.getAsJsonArray("research").iterator(); members.hasNext();) {
                var row = members.next().getAsJsonObject();
                String key = row.get("key").getAsString();
                if (skipTab || unavailable(row, enabled, condition)) {
                    removed.add(key);
                    members.remove();
                } else {
                    present.add(key);
                }
            }
            if (skipTab) tabs.remove();
        }
        if (!enabled && !removed.isEmpty()) {
            removeDanglingMirrors(categories, removed);
            present.removeAll(removed);
            for (var tab : categories) {
                for (var member : tab.getAsJsonObject().getAsJsonArray("research")) {
                    var row = member.getAsJsonObject();
                    for (String field : List.of("parents", "parentsHidden", "parentsAny", "siblings")) {
                        var links = row.get(field);
                        if (links != null && links.isJsonArray())
                            links.getAsJsonArray().asList().removeIf(key -> removed.contains(key.getAsString()));
                    }
                    var mirrors = row.get("parentMirrors");
                    if (mirrors != null && mirrors.isJsonObject()) mirrors.getAsJsonObject().entrySet().removeIf(
                            link -> removed.contains(link.getKey()) || removed.contains(link.getValue().getAsString()));
                }
            }
        }
        result.getAsJsonArray("pages").asList().removeIf(value -> removed.contains(value.getAsJsonObject().get("key").getAsString()));
        if (!enabled && !removed.isEmpty()) {
            for (var value : result.getAsJsonArray("pages")) {
                value.getAsJsonObject().getAsJsonArray("pages").asList().removeIf(page -> {
                    var research = page.getAsJsonObject().get("research");
                    return research != null && !research.isJsonNull() && removed.contains(research.getAsString());
                });
            }
        }
        for (var value : categories) {
            for (var member : value.getAsJsonObject().getAsJsonArray("research")) {
                var row = member.getAsJsonObject();
                if (row.has("suggestedResearch")) {
                    var suggestions = new JsonArray();
                    for (var key : row.getAsJsonArray("suggestedResearch"))
                        if (present.contains(key.getAsString())) suggestions.add(key);
                    row.add("suggestedResearch", suggestions);
                }
            }
        }
        ResearchPageAdditions.recount(result);
        return result;
    }

    private static boolean unavailable(JsonObject row, boolean enabled, Predicate<JsonObject> condition) {
        boolean tutorial = row.has("isTutorial") && row.get("isTutorial").getAsBoolean();
        boolean mirror = row.has("tutorialMirror") && row.get("tutorialMirror").getAsBoolean();
        return (tutorial || mirror) && (!enabled || row.has("conditions") && !condition.test(row));
    }

    private static void removeDanglingMirrors(JsonArray categories, Set<String> removed) {
        // A normal addon category can mirror a lesson, including through another mirror.
        boolean changed;
        do {
            changed = false;
            for (var value : categories) {
                for (var rows = value.getAsJsonObject().getAsJsonArray("research").iterator(); rows.hasNext();) {
                    var row = rows.next().getAsJsonObject();
                    var target = row.get("mirrorOf");
                    if (target != null && target.isJsonPrimitive() && removed.contains(target.getAsString())) {
                        removed.add(row.get("key").getAsString());
                        rows.remove();
                        changed = true;
                    }
                }
            }
        } while (changed);
    }
}
