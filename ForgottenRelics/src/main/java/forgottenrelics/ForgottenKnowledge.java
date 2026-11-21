package forgottenrelics;

import com.google.gson.*;

import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import tc4revival.research.*;

import java.util.*;

/** The server inspects completed scans, then reveals one random eligible forgotten clue. */
public final class ForgottenKnowledge {
    private static volatile Map<String, List<JsonObject>> triggers = load();
    private static List<String> lastOverrides = List.of();
    private static Map<String, List<JsonObject>> configured = Map.of();
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private ForgottenKnowledge() {}

    private static Map<String, List<JsonObject>> load() {
        var result = new LinkedHashMap<String, List<JsonObject>>();
        try (var stream =
                        ForgottenKnowledge.class.getResourceAsStream(
                                "/data/forgottenrelics/forgotten_knowledge.json");
                var reader =
                        new java.io.InputStreamReader(
                                Objects.requireNonNull(stream),
                                java.nio.charset.StandardCharsets.UTF_8)) {
            result.putAll(parse(JsonParser.parseReader(reader).getAsJsonObject()));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot load original forgotten knowledge triggers", exception);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, List<JsonObject>> parse(JsonObject json) {
        var result = new LinkedHashMap<String, List<JsonObject>>();
        for (var entry : json.entrySet()) {
            var items = new ArrayList<JsonObject>();
            for (var element : entry.getValue().getAsJsonArray()) {
                var item = element.getAsJsonObject();
                if (!item.has("item")
                        || ResourceLocation.tryParse(item.get("item").getAsString()) == null)
                    throw new JsonParseException(
                            "Invalid forgotten knowledge item for " + entry.getKey());
                items.add(item.deepCopy());
            }
            result.put(entry.getKey(), List.copyOf(items));
        }
        return Collections.unmodifiableMap(result);
    }

    public static void registerReload(net.minecraftforge.event.AddReloadListenerEvent event) {
        event.addListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                        manager -> {
                            var resource =
                                    manager.getResource(
                                                    ForgottenRelics.id("forgotten_knowledge.json"))
                                            .orElseThrow();
                            try (var reader = resource.openAsReader()) {
                                triggers = parse(JsonParser.parseReader(reader).getAsJsonObject());
                                lastOverrides = List.of();
                                configured = Map.of();
                            } catch (java.io.IOException e) {
                                throw new IllegalStateException(
                                        "Cannot reload forgotten knowledge", e);
                            }
                        });
    }

    static Map<String, List<JsonObject>> withOverrides(
            Map<String, List<JsonObject>> original, List<String> overrides) {
        var result = new LinkedHashMap<>(original);
        for (var expression : overrides) {
            int bracket = expression.indexOf('[');
            if (bracket <= 0 || !expression.endsWith("]"))
                throw new IllegalArgumentException(
                        "Expected ResearchKey[modid:item,...]: " + expression);
            String key = expression.substring(0, bracket).trim();
            if (!key.contains(":")) key = ForgottenRelics.ID + ":" + key;
            if (!original.containsKey(key))
                throw new IllegalArgumentException("Unknown forgotten knowledge research: " + key);
            var items = new ArrayList<JsonObject>();
            var content = expression.substring(bracket + 1, expression.length() - 1).trim();
            if (!content.isEmpty())
                for (var name : content.split(",")) {
                    var id = ResourceLocation.tryParse(name.trim());
                    if (id == null || !ForgeRegistries.ITEMS.containsKey(id))
                        throw new IllegalArgumentException("Unknown trigger item: " + name);
                    var item = new JsonObject();
                    item.addProperty("item", id.toString());
                    items.add(item);
                }
            result.put(key, List.copyOf(items));
        }
        return Collections.unmodifiableMap(result);
    }

    static Map<String, List<JsonObject>> currentTriggers() {
        if (!RelicsConfig.enabled("forgottenKnowledgeOverridingEnabled")) return triggers;
        var overrides = RelicsConfig.knowledgeOverrides();
        if (configured.isEmpty() || !lastOverrides.equals(overrides)) {
            lastOverrides = List.copyOf(overrides);
            try {
                configured = withOverrides(triggers, overrides);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Rejected forgotten knowledge overrides; using data pack triggers", e);
                configured = triggers;
            }
        }
        return configured;
    }

    public static void tick(ServerPlayer p) {
        double modifier = RelicsConfig.value("revelationModifier");
        if (modifier <= 0
                || p.tickCount % Math.max(1, (int) (600 / modifier)) != 0
                || p.getRandom().nextDouble() > Math.min(1, .1 * modifier)) return;
        var data = KnowledgeSavedData.get(p.server);
        var knowledge = data.snapshot(p.getUUID()).orElse(null);
        if (knowledge == null) return;
        var candidates = new ArrayList<String>();
        for (var entry : currentTriggers().entrySet()) {
            String key = entry.getKey();
            if (knowledge.researchMarkers().contains(key)
                    || knowledge.researchMarkers().contains("@" + key)) continue;
            if (key.endsWith(":FalseJustice") && !RelicsConfig.enabled("falseJusticeEnabled")
                    || key.endsWith(":VoidGrimoire")
                            && !RelicsConfig.enabled("voidGrimoireEnabled")) continue;
            if (entry.getValue().stream().allMatch(item -> scanned(knowledge, item)))
                candidates.add(key);
        }
        var apotheosis =
                p.server
                        .getAdvancements()
                        .getAdvancement(new ResourceLocation("botania", "challenge/king_key"));
        if (apotheosis != null
                && p.getAdvancements().getOrStartProgress(apotheosis).isDone()
                && !knowledge.researchMarkers().contains("@forgottenrelics:Apotheosis")
                && !knowledge.researchMarkers().contains("forgottenrelics:Apotheosis"))
            candidates.add("forgottenrelics:Apotheosis");
        if (candidates.isEmpty()) return;
        reveal(p, candidates.get(p.getRandom().nextInt(candidates.size())));
    }

    static void reveal(ServerPlayer p, String key) {
        var data = KnowledgeSavedData.get(p.server);
        var knowledge = data.snapshot(p.getUUID()).orElse(null);
        if (knowledge != null
                && (knowledge.researchMarkers().contains(key)
                        || knowledge.researchMarkers().contains("@" + key))) return;
        data.update(
                p.getUUID(),
                p.getRandom()::nextInt,
                k -> {
                    ResearchCatalogs.progression().completeTrusted(k, "@" + key, true);
                    return null;
                });
        p.displayClientMessage(
                Component.translatable(
                        "message.forgottenrelics.revelation",
                        Component.translatable("tc.research_name." + key)),
                false);
        Relics.burst(p.level(), p.position());
    }

    public static void advancement(
            net.minecraftforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p
                && event.getAdvancement()
                        .getId()
                        .equals(ResourceLocation.parse("botania:challenge/king_key")))
            reveal(p, "forgottenrelics:Apotheosis");
    }

    private static boolean scanned(PlayerKnowledge knowledge, JsonObject item) {
        var id = ResourceLocation.tryParse(item.get("item").getAsString());
        if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) return false;
        var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id));
        if (item.has("nbt"))
            try {
                stack.setTag(TagParser.parseTag(item.get("nbt").getAsString()));
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                return false;
            }
        return knowledge.scanned(PlayerKnowledge.ScanKind.OBJECT, ScanIdentity.item(stack));
    }
}
