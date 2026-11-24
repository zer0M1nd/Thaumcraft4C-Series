package tc4revival.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;

/** Resolve equivalent metal inputs from this reload's Forge tags before recipe and book data are copied. */
public final class MetalRecipeIngredients {
    private static final Map<ICondition.IContext, MetalRecipeIngredients> RELOADS = new WeakHashMap<>();
    private static final ThreadLocal<MetalRecipeIngredients> READING = new ThreadLocal<>();
    private static final Set<String> ADDITIONAL_METALS =
            Set.of("quicksilver", "crimson_alloy", "thaumic_alloy", "neutronized", "dense_iron", "tin");
    private static final Pattern COMPRESSION_TIER = Pattern.compile("_(?:[1-9][0-9]*)x$");
    private final Map<String, String> materials;

    public static synchronized MetalRecipeIngredients prepare(ICondition.IContext context) {
        var index = from(context);
        RELOADS.put(context, index);
        return index;
    }

    public static <T> T read(ICondition.IContext context, Supplier<T> serializer) {
        MetalRecipeIngredients index;
        synchronized (MetalRecipeIngredients.class) { index = RELOADS.get(context); }
        var previous = READING.get();
        if (index == null || index == previous) return serializer.get();
        READING.set(index);
        try {
            return serializer.get();
        } finally {
            if (previous == null) READING.remove(); else READING.set(previous);
        }
    }

    /** Called only from Forge's vanilla ingredient codec. ItemStack results use a different codec. */
    public static JsonObject readIngredient(JsonObject json) {
        var index = READING.get();
        String tag = index == null ? null : index.tagFor(json);
        if (tag == null) return json;
        var value = new JsonObject();
        value.addProperty("tag", tag);
        return value;
    }

    private String tagFor(JsonObject json) {
        if (json.size() != 1 || !json.has("item") || !json.get("item").isJsonPrimitive()) return null;
        var id = ResourceLocation.tryParse(json.get("item").getAsString());
        return id == null ? null : materials.get(id.toString());
    }

    MetalRecipeIngredients(Map<String, String> materials) {
        this.materials = materials;
    }

    public static MetalRecipeIngredients from(ICondition.IContext context) {
        if (context == ICondition.IContext.EMPTY || context == ICondition.IContext.TAGS_INVALID) return new MetalRecipeIngredients(Map.of());
        var tags = context.getAllTags(Registries.ITEM);
        var names = new HashSet<String>();
        for (var id : tags.keySet()) {
            if (id.getNamespace().equals("forge") && id.getPath().startsWith("ingots/"))
                names.add(id.getPath().substring("ingots/".length()));
        }
        names.add("void_metal");
        var materials = new HashMap<String, String>();
        var ambiguous = new HashSet<String>();
        for (var entry : tags.entrySet()) {
            var id = entry.getKey();
            if (!id.getNamespace().equals("forge") || !materialForm(id.getPath(), names)) continue;
            for (var holder : entry.getValue()) {
                holder.unwrapKey().ifPresent(key -> {
                    String item = key.location().toString();
                    String tag = id.toString();
                    if (tag.equals("forge:storage_blocks/void_metal")) tag = "forge:storage_blocks/void";
                    String previous = materials.putIfAbsent(item, tag);
                    if (previous != null && !previous.equals(tag)) ambiguous.add(item);
                });
            }
        }
        // An item in two different material tags cannot safely stand for every member of either one.
        ambiguous.forEach(materials::remove);
        return new MetalRecipeIngredients(materials);
    }

    static boolean materialForm(String path, Set<String> metals) {
        if (path.equals("quicksilver")) return true;
        int split = path.indexOf('/');
        if (split < 0 || path.indexOf('/', split + 1) >= 0) return false;
        String material = path.substring(split + 1);
        String form = path.substring(0, split);
        if (form.equals("storage_blocks")) {
            // Use the base metal only for classification. The recipe retains its full tagged form and tier.
            material = COMPRESSION_TIER.matcher(material).replaceFirst("");
            if (material.startsWith("raw_")) material = material.substring(4);
        }
        return switch (form) {
            case "ingots" -> true;
            case "nuggets", "plates", "gears", "rods", "wires", "dusts", "raw_materials", "storage_blocks" ->
                    metals.contains(material) || ADDITIONAL_METALS.contains(material);
            default -> false;
        };
    }

    /** Preserve the caller's datapack tree, including unmodified conditions and fixed ItemStack outputs. */
    public JsonObject normalized(JsonObject recipe) {
        if (materials.isEmpty() || !visit(recipe, false)) return recipe;
        var copy = recipe.deepCopy();
        visit(copy, true);
        return copy;
    }

    private boolean visit(JsonObject recipe, boolean write) {
        if (!recipe.has("type") || !recipe.get("type").isJsonPrimitive()) return false;
        String type = recipe.get("type").getAsString();
        boolean changed = false;
        if (type.equals("forge:conditional")) {
            if (recipe.get("recipes") instanceof JsonArray branches)
                for (var branch : branches)
                    if (branch instanceof JsonObject option && option.get("recipe") instanceof JsonObject child)
                        changed |= visit(child, write);
            return changed;
        }
        // These serializers keep JSON for the game client or book. Others use the scoped Forge ingredient codec.
        if (recipe.has("pattern") && recipe.get("key") instanceof JsonObject key)
            for (var entry : key.entrySet()) changed |= ingredient(entry.getValue(), write);
        switch (type) {
            case "minecraft:crafting_shapeless", "thaumcraft:arcane", "witchinggadgets:spinning",
                 "witchinggadgets:photo_developing", "botania:runic_altar" -> changed |= ingredient(recipe.get("ingredients"), write);
            case "minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking",
                 "minecraft:stonecutting", "tconstruct:material", "ic2:metal_former_rolling" -> changed |= ingredient(recipe.get("ingredient"), write);
            case "minecraft:smithing_transform", "minecraft:smithing_trim" -> {
                changed |= ingredient(recipe.get("base"), write);
                changed |= ingredient(recipe.get("addition"), write);
                changed |= ingredient(recipe.get("template"), write);
            }
            case "thaumcraft:crucible" -> changed |= ingredient(recipe.get("catalyst"), write);
            case "bloodmagic:altar" -> changed |= ingredient(recipe.get("input"), write);
            case "alchemistry:dissolver" -> changed |= wrapped(recipe.get("input"), write);
            case "productivebees:bee_conversion" -> changed |= ingredient(recipe.get("item"), write);
            default -> { }
        }
        changed |= wrapped(recipe.get("center"), write);
        if (recipe.get("components") instanceof JsonArray parts)
            for (var part : parts) changed |= wrapped(part, write);
        if (recipe.get("optional_components") instanceof JsonArray optional)
            for (var part : optional)
                if (part instanceof JsonObject entry) changed |= wrapped(entry.get("component"), write);
        return changed;
    }

    private boolean wrapped(JsonElement element, boolean write) {
        return element instanceof JsonObject obj && ingredient(obj.get("ingredient"), write);
    }

    private boolean ingredient(JsonElement element, boolean write) {
        if (element instanceof JsonArray alternatives) {
            boolean changed = false;
            for (var item : alternatives) changed |= ingredient(item, write);
            return changed;
        }
        if (!(element instanceof JsonObject obj)) return false;
        String tag = tagFor(obj);
        if (tag != null) {
            if (write) {
                obj.remove("item");
                obj.addProperty("tag", tag);
            }
            return true;
        }
        return obj.has("type") && obj.get("type").isJsonPrimitive()
                && obj.get("type").getAsString().equals("thaumicfutures:preferred_material")
                && ingredient(obj.get("fallback"), write);
    }
}
