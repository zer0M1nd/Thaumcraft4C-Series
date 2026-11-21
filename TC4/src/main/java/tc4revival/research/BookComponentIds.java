package tc4revival.research;

import com.google.gson.*;
import java.util.Set;

/** Validate explicit and derived text anchors before accepting a definition snapshot. */
public final class BookComponentIds {
    private BookComponentIds() {}
    public static void add(Set<String> ids,String id) {
        if(id==null||id.isBlank()||!ids.add(id))throw new IllegalArgumentException("Invalid or duplicate book component id "+id);
    }
    public static void text(Set<String> ids,JsonElement node,String identity) {
        if(node==null||node.isJsonNull())return;
        if(node.isJsonObject()&&node.getAsJsonObject().has("id"))identity=node.getAsJsonObject().get("id").getAsString();
        add(ids,identity);
        if(node.isJsonArray()) {
            int i=0;for(var child:node.getAsJsonArray())text(ids,child,identity+"/"+i++);
        }else if(node.isJsonObject()) {
            var object=node.getAsJsonObject();
            if(object.has("localized")) {
                var alternatives=object.getAsJsonObject("localized");
                if(!alternatives.has("en_us"))throw new IllegalArgumentException("Localized book text requires en_us fallback");
                var reserved=new java.util.HashSet<String>();
                for(var alternative:alternatives.entrySet()) {
                    var branch=new java.util.HashSet<>(ids);
                    text(branch,alternative.getValue(),identity+"/localized");
                    reserved.addAll(branch);
                }
                ids.addAll(reserved);
            }
            for(String key:java.util.List.of("extra","with"))if(object.has(key)) {
                int i=0;for(var child:object.getAsJsonArray(key))text(ids,child,identity+"/"+key+"/"+i++);
            }
        }
    }
}
