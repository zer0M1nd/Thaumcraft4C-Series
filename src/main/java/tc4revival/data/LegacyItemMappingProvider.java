package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.TC4Revival;

/** DataFixer runs at generation time only. The game consumes the reviewed, fixed-version mapping. */
public  class LegacyItemMappingProvider implements DataProvider {
    private final PackOutput output;
    public LegacyItemMappingProvider(PackOutput output) { this.output = output; }

    @Override public CompletableFuture<?> run(CachedOutput cache) {
        try {
            return DataProvider.saveStable(cache, definition(), output.getOutputFolder(PackOutput.Target.DATA_PACK)
                    .resolve("thaumcraft/legacy_item_mappings/vanilla.json"));
        } catch (IOException failure) { return CompletableFuture.failedFuture(failure); }
    }
    public static JsonObject definition() throws IOException {
            var directory = Path.of(System.getProperty("tc4revival.legacyBaseline"));
            var definitions = JsonParser.parseString(Files.readString(directory.resolve("item-definitions.json"))).getAsJsonArray();
            Map<String, List<Candidate>> candidates = new TreeMap<>();
            for (var value : definitions) {
                var definition = value.getAsJsonObject();
                String oldId = definition.get("item").getAsString();
                // These require effect/entity data, handled with special stacks instead of plain item aliases.
                if (!oldId.startsWith("minecraft:") || oldId.equals("minecraft:potion") || oldId.equals("minecraft:spawn_egg")) continue;
                int count = variantCount(oldId, definition.get("has_subtypes").getAsBoolean());
                for (int metadata = 0; metadata < count; metadata++) {
                    var oldStack = new CompoundTag();
                    oldStack.putString("id", oldId);
                    oldStack.putByte("Count", (byte) 1);
                    oldStack.putShort("Damage", (short) metadata);
                    var fixed = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, oldStack),
                            99, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
                    String newId = fixed.get("id").asString("");
                    var id = ResourceLocation.tryParse(newId);
                    if (id == null || newId.equals("minecraft:air") || !ForgeRegistries.ITEMS.containsKey(id)) continue;
                    candidates.computeIfAbsent(newId, ignored -> new ArrayList<>()).add(new Candidate(oldId, metadata));
                }
            }
            candidates.put("minecraft:iron_nugget", List.of(new Candidate("Thaumcraft:ItemNugget", 0)));
            var woods = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak");
            for (int i = 0; i < woods.size(); i++) candidates.put("minecraft:" + woods.get(i) + "_wood",
                    List.of(new Candidate(i < 4 ? "minecraft:log" : "minecraft:log2", 12 + (i < 4 ? i : i - 4))));
            var mappings = new JsonArray();
            for (var entry : candidates.entrySet()) {
                // Prefer an unchanged registry ID over a technical block alias, then its ordinary metadata.
                var ordered = entry.getValue().stream().sorted(Comparator
                        .comparing((Candidate c) -> !c.item.equals(entry.getKey()))
                        .thenComparingInt(Candidate::metadata).thenComparing(Candidate::item)).toList();
                var selected = ordered.get(0);
                if (ordered.stream().map(Candidate::item).distinct().count() > 1) {
                    if (!entry.getKey().equals("minecraft:dead_bush") || !selected.item.equals("minecraft:deadbush")) {
                        throw new IllegalStateException("Unreviewed legacy item collision: " + entry.getKey() + " " + ordered);
                    }
                    TC4Revival.LOGGER.info("Legacy item mapping collision {} -> {}; candidates {}", entry.getKey(), selected, ordered);
                }
                var row = new JsonObject();
                row.addProperty("item", entry.getKey());
                row.addProperty("legacy_item", selected.item);
                row.addProperty("metadata", selected.metadata);
                mappings.add(row);
            }
            var result = new JsonObject();
            result.addProperty("format", 1);
            result.add("items", mappings);
            result.add("groups", JsonParser.parseString(Files.readString(directory.resolve("grouped-object-tags.json"))));
            var eggs = new JsonArray();
            var names = new java.util.HashSet<String>();
            for (var value : JsonParser.parseString(Files.readString(directory.resolve("vanilla-spawn-eggs.json"))).getAsJsonArray()) {
                int metadata = value.getAsJsonObject().get("metadata").getAsInt();
                String legacyEntity = value.getAsJsonObject().get("entity").getAsString();
                var entityCandidates = LegacyEntityDefinitions.rules().stream().filter(rule -> rule.legacyName().equals(legacyEntity) && rule.qualifier().isEmpty())
                        .map(tc4revival.aspect.entity.EntityAspectRule::entity).distinct().toList();
                // These old egg types split into multiple entity types; keep the ordinary egg, not each later egg item.
                String entity = switch (legacyEntity) {
                    case "Zombie" -> "minecraft:zombie";
                    case "EntityHorse" -> "minecraft:horse";
                    case "Ozelot" -> "minecraft:ocelot";
                    default -> entityCandidates.size() == 1 ? entityCandidates.get(0) : null;
                };
                if (entity == null || !entityCandidates.contains(entity)) throw new IllegalStateException("Unreviewed old egg entity mapping " + legacyEntity + " " + entityCandidates);
                var type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(entity));
                var egg = net.minecraft.world.item.SpawnEggItem.byId(type);
                if (egg == null) throw new IllegalStateException("No actual modern egg for " + entity);
                String id = ForgeRegistries.ITEMS.getKey(egg).toString();
                if (!names.add(id)) throw new IllegalStateException("Duplicate spawn egg " + metadata + " -> " + id);
                var row = new JsonObject();
                row.addProperty("item", id);
                row.addProperty("metadata", metadata);
                eggs.add(row);
            }
            if (eggs.size() != 24) throw new IllegalStateException("Legacy baseline should have 24 vanilla spawn eggs");
            result.add("spawn_eggs", eggs);
            TC4Revival.LOGGER.info("Generated {} modern legacy item aliases", mappings.size());
            return result;
    }
    static int variantCount(String id, boolean subtypes) {
        if (!subtypes) return 1;
        return switch (id.substring("minecraft:".length())) {
            case "yellow_flower", "filled_map" -> 1;
            case "sand", "cobblestone_wall", "coal", "golden_apple", "cooked_fished" -> 2;
            case "dirt", "sandstone", "tallgrass", "anvil" -> 3;
            case "stonebrick", "fish" -> 4;
            case "skull", "quartz_block" -> 5;
            case "planks", "sapling", "monster_egg", "wooden_slab", "double_wooden_slab", "double_plant" -> 6;
            case "red_flower" -> 9;
            default -> 16; // Color and old block-state bits; aliases collapse to their first valid value.
        };
    }
    private record Candidate(String item, int metadata) {}
    @Override public String getName() { return "TC4 legacy vanilla item identity mapping"; }
}
