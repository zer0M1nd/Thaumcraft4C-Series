package thaumcraft.api.research;

import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.function.UnaryOperator;
import thaumcraft.api.util.ExtensionRegistry;

/** All files are merged before contributors run; validation and publication follow the completed bundle. */
public final class ResearchDataContributors {
    /** BEFORE_PUBLICATION runs on the server after world configuration and reloadable rules are ready.
     * Providers may append changes/replacements here; they must not mutate their input or retain it. */
    public enum Phase { BEFORE_ADDITIONS, AFTER_ADDITIONS, BEFORE_PUBLICATION }
    private record Contributor(Phase phase,UnaryOperator<JsonObject> operation) {}
    private static final ExtensionRegistry<Contributor> CONTRIBUTORS=new ExtensionRegistry<>();
    private ResearchDataContributors() {}
    public static void register(String id,int priority,Phase phase,UnaryOperator<JsonObject> operation){CONTRIBUTORS.register(id,priority,new Contributor(phase,operation));}
    public static JsonObject apply(Phase phase,JsonObject source){
        var result=source.deepCopy();
        for(var entry:CONTRIBUTORS.entries())if(entry.value().phase()==phase){
            try{
                result=Objects.requireNonNull(entry.value().operation().apply(result.deepCopy())).deepCopy();
                if(result.has("categories"))tc4revival.research.ResearchOrigins.annotate(
                        result.getAsJsonArray("categories"),tc4revival.research.ResearchOrigins.namespace(entry.id()));
            }
            catch(RuntimeException failure){throw new IllegalArgumentException("Research contributor failed: "+entry.id(),failure);}
        }
        return result;
    }
}
