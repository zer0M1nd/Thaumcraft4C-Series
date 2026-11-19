package thaumicfutures.devices;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.google.gson.*;

/** Remap legacy resource names without changing the original model geometry or UVs. */
public final class DeviceResources {
    public static void convert(Map<String,byte[]> output) {
        for(var entry:output.entrySet())if(entry.getKey().startsWith("assets/thaumicfutures/models/block/tc5/") && entry.getKey().endsWith(".json")) {
            var json=JsonParser.parseString(new String(entry.getValue(),StandardCharsets.UTF_8)).getAsJsonObject();
            if(json.has("textures"))for(var texture:json.getAsJsonObject("textures").entrySet()) {
                var value=texture.getValue().getAsString();
                if(value.startsWith("thaumcraft:blocks/"))value=value.replace("thaumcraft:blocks/","thaumicfutures:block/tc5/");
                else if(value.startsWith("blocks/"))value="minecraft:block/"+value.substring(7);
                texture.setValue(new JsonPrimitive(value));
            }
            if(!json.has("parent"))json.addProperty("parent","minecraft:block/block");
            else { var parent=json.get("parent").getAsString();if(parent.startsWith("thaumcraft:block/"))json.addProperty("parent",parent.replace("thaumcraft:block/","thaumicfutures:block/tc5/")); }
            json.addProperty("render_type","minecraft:cutout");entry.setValue(json.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
    private DeviceResources() {}
}
