package tc4revival.client;

import com.google.gson.JsonParser;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpecialBlockItemTransformsTest {
    @Test void customBlockItemsHaveBoundedTransformsForEveryDisplayLocation() throws Exception {
        var items=Path.of("src/generated/resources/assets/thaumcraft/models/item");
        int checked=0;
        try(var files=Files.list(items)) {
            for(var path:files.filter(p->p.toString().endsWith(".json")).toList()) {
                var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                boolean custom=json.has("parent")&&json.get("parent").getAsString().endsWith("builtin/entity");
                String name=path.getFileName().toString();
                // Handheld instruments implement their own context transforms inside their renderer.
                if(!custom||Set.of("casting_wand.json","thaumometer.json").contains(name))continue;
                var display=json.getAsJsonObject("display");assertNotNull(display,name);
                for(String mode:List.of("gui","ground","fixed","firstperson_righthand","firstperson_lefthand","thirdperson_righthand","thirdperson_lefthand")) {
                    assertTrue(display.has(mode),name+":"+mode);
                    var scale=display.getAsJsonObject(mode).getAsJsonArray("scale");assertNotNull(scale,name+":"+mode);
                    for(var component:scale)assertTrue(component.getAsFloat()>0&&component.getAsFloat()<=.625f,name+":"+mode);
                }
                checked++;
            }
        }
        assertTrue(checked>=40,"Audit must include the custom block catalog");
    }
}
