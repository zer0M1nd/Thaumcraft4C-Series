package ttmodern;
import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ResearchSpacingTest {
    @Test void infusedInkwellHasTwoGridUnitsOfSpace() throws Exception {
        var root=JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/thaumic_tinkerer/research_catalog/main.json"))).getAsJsonObject();
        for(var category:root.getAsJsonArray("categories")){
            var rows=category.getAsJsonObject().getAsJsonArray("research");JsonObject ink=null;
            for(var row:rows)if(row.getAsJsonObject().get("key").getAsString().endsWith(":INFUSED_INKWELL"))ink=row.getAsJsonObject();
            if(ink==null)continue;
            for(var row:rows){var other=row.getAsJsonObject();if(other==ink)continue;
                double dx=ink.get("displayColumn").getAsInt()-other.get("displayColumn").getAsInt(),dy=ink.get("displayRow").getAsInt()-other.get("displayRow").getAsInt();
                assertTrue(dx*dx+dy*dy>=4,"Inkwell too close to "+other.get("key"));}
            return;
        }
        fail("Missing infused inkwell");
    }
}
