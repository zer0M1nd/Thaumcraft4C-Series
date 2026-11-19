package thaumicomniverse.alignment;

import com.google.gson.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Composable candidate predicates. Addons may register a namespaced predicate decoder before reload. */
public final class AlignmentPredicates {
    private static final Map<String,java.util.function.Function<JsonObject,Predicate<ItemStack>>> CUSTOM=new TreeMap<>();
    public static synchronized void register(String id,java.util.function.Function<JsonObject,Predicate<ItemStack>> decoder) {
        ResourceLocation.parse(id);if(CUSTOM.putIfAbsent(id,Objects.requireNonNull(decoder))!=null)throw new IllegalArgumentException("Duplicate alignment predicate "+id);
    }
    public static Predicate<ItemStack> decode(JsonObject json) { return decode(json,0); }
    private static Predicate<ItemStack> decode(JsonObject json,int depth) {
        if(depth>16)throw new IllegalArgumentException("Alignment predicate nesting exceeds 16");
        var tests=new ArrayList<Predicate<ItemStack>>();
        if(json.has("all"))for(var child:json.getAsJsonArray("all"))tests.add(decode(child.getAsJsonObject(),depth+1));
        if(json.has("any")) {var alternatives=new ArrayList<Predicate<ItemStack>>();for(var child:json.getAsJsonArray("any"))alternatives.add(decode(child.getAsJsonObject(),depth+1));tests.add(stack->alternatives.stream().anyMatch(p->p.test(stack)));}
        if(json.has("not"))tests.add(decode(json.getAsJsonObject("not"),depth+1).negate());
        if(json.has("namespace")) {String namespace=json.get("namespace").getAsString();tests.add(stack->ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace().equals(namespace));}
        if(json.has("id")) {var id=ResourceLocation.parse(json.get("id").getAsString());tests.add(stack->id.equals(ForgeRegistries.ITEMS.getKey(stack.getItem())));}
        if(json.has("regex")) {var regex=Pattern.compile(json.get("regex").getAsString());tests.add(stack->regex.matcher(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()).matches());}
        if(json.has("tag")) {var tag=ItemTags.create(ResourceLocation.parse(json.get("tag").getAsString()));tests.add(stack->stack.is(tag));}
        if(json.has("include")) {var include=ids(json.getAsJsonArray("include"));tests.add(stack->include.contains(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()));}
        if(json.has("exclude")) {var exclude=ids(json.getAsJsonArray("exclude"));tests.add(stack->!exclude.contains(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()));}
        if(json.has("minAspectKinds")) {int minimum=json.get("minAspectKinds").getAsInt();if(minimum<0)throw new IllegalArgumentException("Negative aspect minimum");tests.add(stack->AlignmentDefinitions.aspects(stack).size()>=minimum);}
        if(json.has("minAspectAmount")) {long minimum=json.get("minAspectAmount").getAsLong();if(minimum<0)throw new IllegalArgumentException("Negative aspect minimum");tests.add(stack->AlignmentDefinitions.aspects(stack).values().stream().mapToLong(Long::longValue).sum()>=minimum);}
        if(json.has("custom")) {String id=json.get("custom").getAsString();var decoder=CUSTOM.get(id);if(decoder==null)throw new IllegalArgumentException("Unknown alignment predicate "+id);tests.add(decoder.apply(json));}
        var allowed=Set.of("all","any","not","namespace","id","regex","tag","include","exclude","minAspectKinds","minAspectAmount","custom","data");
        for(String key:json.keySet())if(!allowed.contains(key))throw new IllegalArgumentException("Unknown alignment predicate field "+key);
        return stack->tests.stream().allMatch(test->test.test(stack));
    }
    private static Set<String> ids(JsonArray values) {var result=new HashSet<String>();for(var value:values)result.add(ResourceLocation.parse(value.getAsString()).toString());return result;}
}
