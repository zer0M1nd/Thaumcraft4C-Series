package thaumcraft.api.wands;

import com.google.gson.*;
import java.util.*;
import java.util.function.*;
import tc4revival.content.WandParts;
/** Config-backed attributes owned by an addon namespace. Register in common setup;
 * values are captured on server data reload and transmitted with the research snapshot. */
public class WandAttributes {
    private static final Map<String,IntSupplier> CAPACITY=new TreeMap<>();
    private static final Map<String,Supplier<Map<String,Float>>> COST=new TreeMap<>();
    private static volatile JsonObject server=new JsonObject(),client=new JsonObject();
    private static void owned(String namespace,String id) {
        if(!id.startsWith(namespace+":") || namespace.equals("thaumcraft") || namespace.isBlank())throw new IllegalArgumentException("Attribute override must own its addon namespace: "+id);
    }
    public static synchronized void registerCapacity(String namespace,String rod,IntSupplier supplier) {
        owned(namespace,rod);WandParts.Rod.require(rod);
        if(CAPACITY.putIfAbsent(rod,Objects.requireNonNull(supplier))!=null)throw new IllegalArgumentException("Duplicate rod override "+rod);
    }
    /** Map keys are aspect tags or '*' for the default cost multiplier. */
    public static synchronized void registerCosts(String namespace,String cap,Supplier<Map<String,Float>> supplier) {
        owned(namespace,cap);WandParts.Cap.require(cap);
        if(COST.putIfAbsent(cap,Objects.requireNonNull(supplier))!=null)throw new IllegalArgumentException("Duplicate cap override "+cap);
    }
    public static synchronized JsonObject refreshServer() {
        var result=new JsonObject();var rods=new JsonObject();var caps=new JsonObject();
        for(var rod:WandParts.Rod.values()){
            var supplier=CAPACITY.get(rod.tag);
            int base=supplier==null?rod.capacity:supplier.getAsInt();
            rods.addProperty(rod.tag,WandPropertyLayers.resolveCapacity(rod.tag,base));
        }
        COST.forEach((id,supplier)->{var values=new JsonObject();supplier.get().forEach((aspect,cost)->{
            if(!Float.isFinite(cost)||cost<0||cost>100)throw new IllegalArgumentException("Invalid cost "+id);
            if(!aspect.equals("*"))tc4revival.aspect.TCAspects.catalog().require(aspect);
            values.addProperty(aspect,cost);
        });caps.add(id,values);});
        result.add("rods",rods);result.add("caps",caps);server=result;return result.deepCopy();
    }
    public static void accept(JsonObject values) { client=values.deepCopy(); }
    public static void clearClient() {client=new JsonObject();}
    private static JsonObject current(){return net.minecraftforge.fml.util.thread.EffectiveSide.get().isServer()?server:client;}
    public static int capacity(String rod,int fallback) {
        var values=current().getAsJsonObject("rods");return values!=null&&values.has(rod)?values.get(rod).getAsInt():fallback;
    }
    public static float cost(String cap,String aspect,float fallback) {
        var values=current().getAsJsonObject("caps");if(values==null||!values.has(cap))return fallback;
        var row=values.getAsJsonObject(cap);return row.has(aspect)?row.get(aspect).getAsFloat():row.has("*")?row.get("*").getAsFloat():fallback;
    }
}
