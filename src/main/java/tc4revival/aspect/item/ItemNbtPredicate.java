package tc4revival.aspect.item;

import com.google.gson.*;
import java.math.BigDecimal;
import java.util.*;
import net.minecraft.nbt.*;

/** Immutable partial compound predicate. Lists are ordered and exact, numeric NBT types compare by value. */
public final class ItemNbtPredicate {
    public static final ItemNbtPredicate EMPTY = new ItemNbtPredicate(new JsonObject());
    private final JsonObject pattern;
    public ItemNbtPredicate(JsonObject pattern) {
        this.pattern = normalize(pattern).getAsJsonObject();
        if(choice(this.pattern)||existence(this.pattern))throw new IllegalArgumentException("Root NBT predicate must be a compound");
    }
    private static boolean choice(JsonElement value){return value.isJsonObject()&&value.getAsJsonObject().has("$in");}
    private static boolean existence(JsonElement value){return value.isJsonObject()&&value.getAsJsonObject().has("$exists");}
    public boolean isEmpty() { return pattern.size() == 0; }
    public JsonObject json() { return pattern.deepCopy(); }
    public String canonical() { return pattern.toString(); }
    public boolean matches(CompoundTag tag) { return isEmpty() || matches(pattern, tag==null?new CompoundTag():tag); }
    private static boolean matches(JsonElement expected, Tag actual) {
        if(existence(expected))return expected.getAsJsonObject().get("$exists").getAsBoolean()==(actual!=null);
        if (actual == null) return false;
        if(choice(expected))return expected.getAsJsonObject().getAsJsonArray("$in").asList().stream().anyMatch(value->matches(value,actual));
        if (expected.isJsonObject()) {
            if (!(actual instanceof CompoundTag compound)) return false;
            return expected.getAsJsonObject().entrySet().stream().allMatch(e -> matches(e.getValue(), compound.get(e.getKey())));
        }
        if (expected.isJsonArray()) {
            if (!(actual instanceof CollectionTag<?> list) || list.size() != expected.getAsJsonArray().size()) return false;
            for (int i = 0; i < list.size(); i++) if (!matches(expected.getAsJsonArray().get(i), list.get(i))) return false;
            return true;
        }
        var value = expected.getAsJsonPrimitive();
        if (value.isString()) return actual instanceof StringTag && value.getAsString().equals(actual.getAsString());
        if (value.isBoolean()) return actual instanceof ByteTag number && number.getAsByte() == (value.getAsBoolean() ? 1 : 0);
        if (!(actual instanceof NumericTag number)) return false;
        try { return value.getAsBigDecimal().compareTo(new BigDecimal(number.getAsNumber().toString())) == 0; }
        catch (NumberFormatException invalidNumber) { return false; }
    }
    /** Only fields read by this predicate enter cache keys; names, damage and unrelated tags stay ignored. */
    public void project(CompoundTag actual, CompoundTag output) { if (actual != null) project(pattern, actual, output); }
    private static void project(JsonObject selector, CompoundTag actual, CompoundTag output) {
        selector.entrySet().forEach(e -> {
            var value = actual.get(e.getKey());
            if (value == null) return;
            if(existence(e.getValue())){if(!output.contains(e.getKey()))output.putBoolean(e.getKey(),true);return;}
            if (e.getValue().isJsonObject() && !choice(e.getValue()) && value instanceof CompoundTag nested) {
                var selected = output.contains(e.getKey(), Tag.TAG_COMPOUND) ? output.getCompound(e.getKey()) : new CompoundTag();
                project(e.getValue().getAsJsonObject(), nested, selected);
                output.put(e.getKey(), selected);
            } else output.put(e.getKey(), value.copy());
        });
    }
    /** A concrete selector witness for exports/tests; it does not enumerate arbitrary third-party NBT. */
    public CompoundTag example() { return (CompoundTag) example(pattern); }
    private static Tag example(JsonElement json) {
        if(choice(json))return example(json.getAsJsonObject().getAsJsonArray("$in").get(0));
        if (json.isJsonObject()) {
            var result = new CompoundTag();json.getAsJsonObject().entrySet().forEach(e -> {if(existence(e.getValue())){if(e.getValue().getAsJsonObject().get("$exists").getAsBoolean())result.putBoolean(e.getKey(),true);}else result.put(e.getKey(), example(e.getValue()));});return result;
        }
        if (json.isJsonArray()) {
            var result = new ListTag();var array=json.getAsJsonArray();
            boolean numeric=!array.isEmpty();for(var value:array)numeric&=value.isJsonPrimitive()&&value.getAsJsonPrimitive().isNumber();
            boolean integral=numeric;if(numeric)for(var value:array)try{value.getAsBigDecimal().longValueExact();}catch(ArithmeticException invalid){integral=false;}
            for (var value : array) {
                var element = numeric?(integral?LongTag.valueOf(value.getAsLong()):DoubleTag.valueOf(value.getAsDouble())):example(value);
                if(numeric&&!matches(value,element))throw new IllegalArgumentException("NBT number cannot be represented exactly in selector witness");
                if (!result.addTag(result.size(), element)) throw new IllegalArgumentException("NBT lists must have uniform element types");
            }return result;
        }
        var value = json.getAsJsonPrimitive();
        if (value.isString()) return StringTag.valueOf(value.getAsString());
        if (value.isBoolean()) return ByteTag.valueOf(value.getAsBoolean());
        var number = value.getAsBigDecimal();
        try { return IntTag.valueOf(number.intValueExact()); } catch (ArithmeticException notInt) {}
        try { return LongTag.valueOf(number.longValueExact()); } catch (ArithmeticException notLong) {}
        double decimal = number.doubleValue();if (!Double.isFinite(decimal)) throw new IllegalArgumentException("NBT number is not finite");
        return DoubleTag.valueOf(decimal);
    }
    private static JsonElement normalize(JsonElement json) {
        if (json == null || json.isJsonNull()) throw new IllegalArgumentException("Null is not an NBT predicate value");
        if (json.isJsonObject()) {
            if(existence(json)){
                var object=json.getAsJsonObject();var value=object.get("$exists");
                if(object.size()!=1||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("$exists requires a boolean");
                return object.deepCopy();
            }
            if(choice(json)) {
                var object=json.getAsJsonObject();
                if(object.size()!=1||!object.get("$in").isJsonArray()||object.getAsJsonArray("$in").isEmpty())
                    throw new IllegalArgumentException("$in requires a nonempty primitive array");
                var sorted=new TreeMap<String,JsonElement>();for(var value:object.getAsJsonArray("$in")) {
                    if(!value.isJsonPrimitive())throw new IllegalArgumentException("$in values must be strings, numbers or booleans");
                    var normalized=normalize(value);sorted.put(normalized.toString(),normalized);
                }
                var options=new JsonArray();sorted.values().forEach(options::add);
                var result=new JsonObject();result.add("$in",options);return result;
            }
            var result = new JsonObject();json.getAsJsonObject().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> result.add(e.getKey(), normalize(e.getValue())));return result;
        }
        if (json.isJsonArray()) { var result = new JsonArray();json.getAsJsonArray().forEach(e -> result.add(normalize(e)));return result; }
        if (json.getAsJsonPrimitive().isNumber()) return new JsonPrimitive(json.getAsBigDecimal().stripTrailingZeros());
        return json.deepCopy();
    }
    @Override public boolean equals(Object other) { return other instanceof ItemNbtPredicate predicate && pattern.equals(predicate.pattern); }
    @Override public int hashCode() { return pattern.hashCode(); }
}
