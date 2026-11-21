package heartwork;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Legacy achievement descriptions use HTML breaks; native text components use newlines. */
final class HeartworkLanguages {
    static void convert(Map<String,byte[]> resources) {
        for(String locale:new String[]{"en_us","zh_cn"}) {
            String path="assets/heartwork/lang/"+locale+".json";
            byte[] bytes=resources.get(path);if(bytes==null)continue;
            var json=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            for(String key:java.util.List.copyOf(json.keySet()))if(key.startsWith("advancements.heartwork."))
                json.addProperty(key,json.get(key).getAsString().replace("<br>","\n"));
            resources.put(path,new Gson().toJson(json).getBytes(StandardCharsets.UTF_8));
        }
    }
}
