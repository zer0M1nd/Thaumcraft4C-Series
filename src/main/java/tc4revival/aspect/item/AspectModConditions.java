package tc4revival.aspect.item;

import com.google.gson.*;
import java.util.function.Predicate;

/** Optional target-mod gates for aspect files or rows, including replacements that use vanilla IDs. */
public final class AspectModConditions {
    private AspectModConditions() {}
    public static boolean loaded(String id) {
        var mods=net.minecraftforge.fml.ModList.get();return mods!=null&&mods.isLoaded(id);
    }
    public static boolean matches(JsonObject json,Predicate<String> loaded) {
        return check(json,"required_mods",loaded,true)&&check(json,"required_any_mods",loaded,false);
    }
    private static boolean check(JsonObject json,String field,Predicate<String> loaded,boolean all) {
        if(!json.has(field))return true;
        var array=json.getAsJsonArray(field);
        if(array.isEmpty())throw new IllegalArgumentException(field+" must not be empty");
        boolean result=all;
        for(var entry:array) {
            if(!entry.isJsonPrimitive()||!entry.getAsJsonPrimitive().isString()||!entry.getAsString().matches("[a-z][a-z0-9_]{1,63}"))
                throw new IllegalArgumentException("Invalid mod ID in "+field);
            boolean present=loaded.test(entry.getAsString());result=all?result&&present:result||present;
        }
        return result;
    }
}
