package thaumicomniverse;

import com.google.gson.JsonParser;
import java.nio.file.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** New recipe assets must choose a feature group rather than silently bypass all profiles. */
class FeatureCoverageTest {
    @Test void everyOwnGameplayRecipeHasAGroupOrExplicitSharedDependency() throws Exception {
        var root=Path.of("src/main/resources/data/thaumicomniverse/recipes");
        var missing=new TreeSet<String>();
        try(var paths=Files.walk(root)) {
            for(var path:paths.filter(p->p.toString().endsWith(".json")).toList()) {
                String id=root.relativize(path).toString().replace('\\','/').replaceFirst("\\.json$","");
                if(id.equals("collab/primordial_pearl_from_fragments"))continue; // crops OR productive bees, never an unconditional recipe
                if(RevivalFeatures.recipeGroup(ResourceLocation.parse("thaumicomniverse:"+id))==null)missing.add(id);
            }
        }
        assertEquals(Set.of(),missing,"Recipes need feature ownership");
    }
    @Test void everyResearchEntryBelongsToAnEffectiveGroup() throws Exception {
        var root=Path.of("src/main/resources/data/thaumicomniverse/research_catalog");
        var missing=new TreeSet<String>();
        try(var paths=Files.walk(root)) {
            for(var path:paths.filter(p->p.toString().endsWith(".json")).toList()) {
                var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                for(var category:json.getAsJsonArray("categories"))for(var entry:category.getAsJsonObject().getAsJsonArray("research")) {
                    String key=entry.getAsJsonObject().get("key").getAsString();
                    var row=entry.getAsJsonObject();
                    // Layout-only nodes own no gameplay: mirrors and auto-unlocked guide stubs.
                    if(row.has("mirrorOf") || row.has("isTutorial") && row.get("isTutorial").getAsBoolean() || (row.has("isStub")&&row.get("isStub").getAsBoolean()&&row.has("isAutoUnlock")&&row.get("isAutoUnlock").getAsBoolean()))continue;
                    if(RevivalFeatures.researchGroup(key)==null)missing.add(key);
                }
            }
        }
        assertEquals(Set.of(),missing,"Research nodes need feature ownership");
    }
}
