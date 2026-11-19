package tc4revival.research;

import com.google.gson.*;
import java.util.function.Predicate;

/** Player-dependent predicates are retained in the synchronized snapshot. */
public final class BookVisibility {
    private BookVisibility() {}
    public static boolean visible(JsonObject component,Predicate<String> completed) {
        return !component.has("when") || test(component.getAsJsonObject("when"),completed);
    }
    public static boolean test(JsonObject condition,Predicate<String> completed) {
        if(condition.has("research"))return completed.test(condition.get("research").getAsString());
        if(condition.has("not"))return !test(condition.getAsJsonObject("not"),completed);
        if(condition.has("all"))return condition.getAsJsonArray("all").asList().stream().allMatch(e->test(e.getAsJsonObject(),completed));
        if(condition.has("any"))return condition.getAsJsonArray("any").asList().stream().anyMatch(e->test(e.getAsJsonObject(),completed));
        if(condition.has("value"))return condition.get("value").getAsBoolean();
        throw new IllegalArgumentException("Unknown book visibility predicate "+condition);
    }
}
