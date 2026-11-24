package thaumcraft.api.config;

import java.util.*;
import java.util.function.*;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Configured recipe visibility after world SERVER configs load. Registrations and saved items stay intact. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft")
public class FeatureRecipes {
    private record Rule(Predicate<ResourceLocation> selects, BooleanSupplier enabled) {}
    private static final Map<ResourceLocation, Rule> RULES = new TreeMap<>();
    private static final Map<RecipeManager, List<Recipe<?>>> RAW = new WeakHashMap<>();
    private static final Set<ResourceLocation> CONFIG_CONDITIONS = new HashSet<>();
    private record Conditions(Map<ResourceLocation, JsonObject> recipes, ICondition.IContext context) {}
    private static final Map<RecipeManager, Conditions> CONDITIONS = new WeakHashMap<>();
    private static boolean ready;

    /** Register during mod setup. All matching rules must permit the recipe. */
    public static synchronized void register(ResourceLocation id, Predicate<ResourceLocation> selects, BooleanSupplier enabled) {
        if (RULES.putIfAbsent(id, new Rule(Objects.requireNonNull(selects), Objects.requireNonNull(enabled))) != null)
            throw new IllegalArgumentException("Duplicate recipe rule " + id);
    }
    public static boolean enabled(ResourceLocation recipe) {
        return RULES.values().stream().allMatch(rule -> !rule.selects.test(recipe) || rule.enabled.getAsBoolean());
    }

    /** Register pure configuration condition types during mod construction, before datapacks load. */
    public static synchronized void deferCondition(ResourceLocation type) { CONFIG_CONDITIONS.add(type); }

    /** Preserve config-dependent recipes until Forge has loaded world settings. Mod/tag conditions still apply. */
    public static Map<ResourceLocation, JsonElement> prepare(RecipeManager manager,
            Map<ResourceLocation, JsonElement> input, ICondition.IContext context) {
        var result = new HashMap<>(input);
        var deferred = new HashMap<ResourceLocation, JsonObject>();
        var metalInputs = tc4revival.content.MetalRecipeIngredients.prepare(context);
        input.forEach((id, element) -> {
            if (!element.isJsonObject()) return;
            var json = metalInputs.normalized(element.getAsJsonObject());
            if (json != element) result.put(id, json);
            if (!json.has("conditions") || !containsDeferred(json.get("conditions"))) return;
            deferred.put(id, json.deepCopy());
            var copy = json.deepCopy();
            var conditions = new JsonArray();
            // FALSE means the recipe cannot exist even when all configurable branches are allowed.
            if (partialAll(json.getAsJsonArray("conditions"), context) == Truth.FALSE) {
                var disabled = new JsonObject(); disabled.addProperty("type", "forge:false"); conditions.add(disabled);
            }
            copy.add("conditions", conditions); result.put(id, copy);
        });
        CONDITIONS.put(manager, new Conditions(Map.copyOf(deferred), context));
        return result;
    }

    private static boolean containsDeferred(JsonElement node) {
        if (node.isJsonArray()) { for (var child : node.getAsJsonArray()) if (containsDeferred(child)) return true; }
        else if (node.isJsonObject()) {
            var object = node.getAsJsonObject();
            if (object.has("type") && CONFIG_CONDITIONS.contains(ResourceLocation.tryParse(object.get("type").getAsString()))) return true;
            for (var child : object.entrySet()) if (containsDeferred(child.getValue())) return true;
        }
        return false;
    }

    // Three-valued evaluation keeps nested NOT/AND/OR correct while config leaves are unknown.
    enum Truth { TRUE, FALSE, UNKNOWN }
    private static Truth partialAll(JsonArray values, ICondition.IContext context) {
        boolean unknown = false;
        for (var element : values) {
            var value = partial(element.getAsJsonObject(), context);
            if (value == Truth.FALSE) return Truth.FALSE;
            unknown |= value == Truth.UNKNOWN;
        }
        return unknown ? Truth.UNKNOWN : Truth.TRUE;
    }
    static Truth partial(JsonObject json, ICondition.IContext context) {
        var type = ResourceLocation.parse(json.get("type").getAsString());
        if (CONFIG_CONDITIONS.contains(type)) return Truth.UNKNOWN;
        if (type.toString().equals("forge:and")) return partialAll(json.getAsJsonArray("values"), context);
        if (type.toString().equals("forge:or")) {
            boolean unknown = false;
            for (var child : json.getAsJsonArray("values")) {
                var value = partial(child.getAsJsonObject(), context);
                if (value == Truth.TRUE) return Truth.TRUE;
                unknown |= value == Truth.UNKNOWN;
            }
            return unknown ? Truth.UNKNOWN : Truth.FALSE;
        }
        if (type.toString().equals("forge:not")) return switch (partial(json.getAsJsonObject("value"), context)) {
            case TRUE -> Truth.FALSE; case FALSE -> Truth.TRUE; case UNKNOWN -> Truth.UNKNOWN;
        };
        // Unknown third-party composite conditions must not be evaluated against unloaded server settings.
        if (containsDeferred(json)) return Truth.UNKNOWN;
        return CraftingHelper.getCondition(json).test(context) ? Truth.TRUE : Truth.FALSE;
    }

    private static boolean conditionEnabled(RecipeManager manager, ResourceLocation id) {
        var conditions = CONDITIONS.get(manager);
        var json = conditions == null ? null : conditions.recipes.get(id);
        return json == null || CraftingHelper.processConditions(json, "conditions", conditions.context);
    }
    /** Called once all vanilla and addon reload listeners have completed; initial startup is deferred. */
    public static void reloaded(RecipeManager manager) {
        RAW.put(manager, List.copyOf(manager.getRecipes()));
        if (ready) apply(manager);
    }
    private static void apply(RecipeManager manager) {
        // Filter the current list: never resurrect recipes removed by another mod's startup/reload handler.
        var current = List.copyOf(manager.getRecipes());
        RAW.computeIfAbsent(manager, key -> current);
        manager.replaceRecipes(current.stream().filter(recipe -> enabled(recipe.getId()) && conditionEnabled(manager, recipe.getId())).toList());
    }
    /** Aspect supplementation can inspect the complete definitions even when acquisition is disabled. */
    public static Collection<Recipe<?>> definitions(RecipeManager manager) {
        return RAW.getOrDefault(manager, List.copyOf(manager.getRecipes()));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void starting(ServerAboutToStartEvent event) {
        ready = true; apply(event.getServer().getRecipeManager());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void sync(OnDatapackSyncEvent event) {
        if (ready && event.getPlayer() == null) apply(event.getPlayerList().getServer().getRecipeManager());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { ready = false; RAW.clear(); CONDITIONS.clear(); }
}
