package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import tc4revival.research.LegacyStackNbt;
import tc4revival.research.ResearchDisplayItems;

/** Original stacks are converted during DataGen, never by running DataFixer in the render loop. */
public  class ResearchDisplayItemsProvider implements DataProvider {
    private final PackOutput output;
    public ResearchDisplayItemsProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var directory = Path.of(java.util.Objects.requireNonNull(System.getProperty("tc4revival.legacyBaseline")));
        var researchDirectory = Path.of(java.util.Objects.requireNonNull(System.getProperty("tc4revival.researchBaseline")));
        try {
            var stacks = new LinkedHashMap<ResearchDisplayItems.Identity, JsonObject>();
            for (String file : List.of("research-catalog.json", "research-pages.json", "research-page-recipes.json"))
                collect(JsonParser.parseString(Files.readString(researchDirectory.resolve(file))), stacks);
            var pageRecipes = tc4revival.research.ResearchPageRecipes.decode(JsonParser.parseString(
                    Files.readString(researchDirectory.resolve("research-page-recipes.json"))).getAsJsonArray());
            for (var entry : pageRecipes.entries().values()) {
                if (!(entry.definition() instanceof tc4revival.research.ResearchPageRecipes.Infusion infusion)) continue;
                var preview = tc4revival.research.ResearchInfusionDisplay.output(infusion).stack();
                var row = new JsonObject();
                row.addProperty("item", preview.key().item()); row.addProperty("metadata", preview.key().metadata());
                row.addProperty("count", preview.count()); row.addProperty("damageable", preview.damageable());
                preview.nbt().ifPresent(nbt -> row.addProperty("nbt", nbt));
                stacks.putIfAbsent(ResearchDisplayItems.Identity.of(preview), row);
            }
            var definitions = new LinkedHashMap<String, JsonObject>();
            for (var element : JsonParser.parseString(Files.readString(directory.resolve("item-definitions.json"))).getAsJsonArray()) {
                var row = element.getAsJsonObject(); definitions.put(row.get("item").getAsString(), row);
            }
            var modItems = new LinkedHashMap<String, String>();
            for (var element : TCItemMappingProvider.definition().getAsJsonArray("items")) {
                var row = element.getAsJsonObject();
                modItems.put(row.get("legacy_item").getAsString() + ":" + row.get("metadata").getAsInt(), row.get("item").getAsString());
            }
            // Both legacy halves depict the same modern research table inventory item.
            modItems.put("Thaumcraft:blockWoodenDevice:8", "minecraft:black_banner");
            modItems.put("Thaumcraft:blockTable:1", "thaumcraft:research_table");
            modItems.put("Thaumcraft:ItemNugget:0", "minecraft:iron_nugget");
            // These are observer-only stand-ins for the copper ore dictionary entries in MC 1.7.10.
            modItems.put("tc4baselineprobe:copper_ingot:0", "minecraft:copper_ingot");
            modItems.put("tc4baselineprobe:copper_ore:0", "minecraft:copper_ore");
            var entries = new JsonArray();
            stacks.forEach((identity, stack) -> entries.add(convert(identity, stack, definitions, modItems)));
            var root = new JsonObject(); root.addProperty("format", 1); root.add("entries", entries);
            var checked = ResearchDisplayItems.decode(root);
            tc4revival.TC4Revival.LOGGER.info("Book display migrations: {} identities, {} resolved, {} pending", checked.entries().size(),
                    checked.entries().values().stream().filter(e -> e.missing().isEmpty()).count(),
                    checked.entries().values().stream().filter(e -> e.missing().isPresent()).count());
            return DataProvider.saveStable(cache, root, output.getOutputFolder(PackOutput.Target.DATA_PACK)
                    .resolve("thaumcraft/research_display_items/legacy.json"));
        } catch (java.io.IOException failure) { return CompletableFuture.failedFuture(failure); }
    }
    private JsonObject convert(ResearchDisplayItems.Identity identity, JsonObject stack, Map<String, JsonObject> definitions, Map<String, String> modItems) {
        var row = new JsonObject(); var legacy = new JsonObject();
        legacy.addProperty("item", identity.item()); legacy.addProperty("metadata", identity.metadata());
        identity.nbt().ifPresent(nbt -> legacy.addProperty("nbt", nbt)); row.add("legacy", legacy);
        var variants = new JsonArray(); row.add("variants", variants); row.addProperty("cycle_damage", false); row.addProperty("cycle_count", false);
        boolean vanilla = identity.item().startsWith("minecraft:"), wildcard = identity.metadata() == Short.MAX_VALUE;
        boolean damageable = stack.get("damageable").getAsBoolean();
        var definition = definitions.get(identity.item());
        boolean subtypes = definition != null && definition.get("has_subtypes").getAsBoolean();
        int count = 1;
        if (identity.item().equals("Thaumcraft:ItemCompassStone") && identity.metadata()==1 && identity.nbt().isEmpty()) {
            var tag=new CompoundTag(); tag.putBoolean("LegacyActive",true);
            var item=new JsonObject(); item.addProperty("item","thaumcraft:sinister_stone");
            item.addProperty("tag",tag.toString()); variants.add(item); return row;
        }
        if (identity.item().equals("Thaumcraft:ItemCrystalEssence") && identity.metadata()==0) {
            var tag=identity.nbt().map(LegacyStackNbt::read).orElseGet(CompoundTag::new);
            if (!java.util.Set.of("Aspects").containsAll(tag.getAllKeys())) { row.addProperty("missing","MOD_TAG_ADAPTER_REQUIRED"); return row; }
            var item=new JsonObject(); item.addProperty("item","thaumcraft:essence_crystal");
            if (!tag.isEmpty()) item.addProperty("tag",tag.toString()); variants.add(item); return row;
        }
        if (identity.item().equals("Thaumcraft:ItemGolemPlacer")) {
            var tag=identity.nbt().map(LegacyStackNbt::read).orElseGet(CompoundTag::new);
            // Portable scalar configuration is unchanged. Inventory and marker tags require their own migrations.
            if (!java.util.Set.of("advanced","core","upgrades","deco").containsAll(tag.getAllKeys())) {
                row.addProperty("missing","MOD_TAG_ADAPTER_REQUIRED"); return row;
            }
            int first=wildcard?0:identity.metadata(),limit=wildcard?tc4revival.content.GolemMaterial.values().length:first+1;
            for (int material=first;material<limit;material++) {
                String id=modItems.get(identity.item()+":"+material);
                if (id==null || !ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse(id)))
                    throw new IllegalStateException("Missing golem display material "+material);
                var item=new JsonObject(); item.addProperty("item",id);
                if (!tag.isEmpty()) item.addProperty("tag",tag.toString());
                variants.add(item);
            }
            row.addProperty("cycle_count",wildcard);
            return row;
        }
        if (identity.item().equals("Thaumcraft:blockMirror") && java.util.Set.of(0,1,6,7).contains(identity.metadata())) {
            var tag=identity.nbt().map(LegacyStackNbt::read).orElseGet(CompoundTag::new);
            tag.putInt("tc4_metadata",identity.metadata());
            if (tag.contains("linkDim",3)) {
                String dimension=switch(tag.getInt("linkDim")) {
                    case -1->"minecraft:the_nether"; case 0->"minecraft:overworld"; case 1->"minecraft:the_end"; default->null;
                };
                if (dimension!=null) tag.putString("linkDimension",dimension);
            }
            var item=new JsonObject(); item.addProperty("item",identity.metadata()<6?"thaumcraft:item_mirror":"thaumcraft:essentia_mirror"); item.addProperty("tag",tag.toString()); variants.add(item);
            return row;
        }
        if (identity.item().equals("Thaumcraft:WandCasting") && !wildcard) {
            var old = identity.nbt().map(LegacyStackNbt::read).orElseGet(CompoundTag::new);
            if (old.contains("focus")) { row.addProperty("missing", "FOCUS_DISPLAY_ADAPTER_REQUIRED"); return row; }
            var rod = tc4revival.content.WandParts.rod(old); var cap = tc4revival.content.WandParts.cap(old);
            var wand = tc4revival.content.TCItems.CASTING_WAND.get().create(rod, cap, tc4revival.content.WandParts.sceptre(old), false);
            for (var aspect : tc4revival.aspect.TCAspects.catalog().primals()) if (old.contains(aspect.tag())) wand.getOrCreateTag().putInt(aspect.tag(), old.getInt(aspect.tag()));
            wand.getOrCreateTag().putInt("tc4_metadata", identity.metadata());
            var item = new JsonObject(); item.addProperty("item", "thaumcraft:casting_wand"); item.addProperty("tag", wand.getTag().toString()); variants.add(item);
            return row;
        }
        if (wildcard && subtypes) {
            if (vanilla) count = LegacyItemMappingProvider.variantCount(identity.item(), true);
            else if (identity.item().equals("Thaumcraft:ItemShard")) count = tc4revival.content.ShardType.values().length;
            else if (identity.item().equals("Thaumcraft:blockCustomOre")) count=8;
            else if (identity.item().equals("Thaumcraft:PrimalArrow")) count=6;
            else { row.addProperty("missing", "UNIMPLEMENTED_VARIANTS"); return row; }
        }
        CompoundTag portable=null;
        if (!vanilla && identity.nbt().isPresent()) {
            portable=identity.nbt().map(LegacyStackNbt::read).orElseThrow();
            java.util.Set<String> allowed=switch(identity.item()) {
                case "Thaumcraft:ItemResource" -> identity.metadata()==13?java.util.Set.of("Aspects"):java.util.Set.of();
                case "Thaumcraft:ItemEssence" -> java.util.Set.of("Aspects");
                case "Thaumcraft:BlockJarNodeItem" -> java.util.Set.of("Aspects","nodetype","nodemod","nodeid");
                case "Thaumcraft:ItemChestplateRobe" -> java.util.Set.of("RS.HARDEN");
                case "Thaumcraft:ItemHelmetFortress" -> java.util.Set.of("goggles","mask");
                case "Thaumcraft:blockWoodenDevice" -> identity.metadata()==8?java.util.Set.of("color","aspect"):java.util.Set.of();
                default -> java.util.Set.of();
            };
            if (allowed.isEmpty() || !allowed.containsAll(portable.getAllKeys())) {
                row.addProperty("missing", "MOD_TAG_ADAPTER_REQUIRED"); return row;
            }
            // These portable layouts are intentionally retained by the modern item implementations.
            // Keep zero-amount label aspects and byte-valued augmentation/banner fields unchanged.
        }
        for (int variant = 0; variant < count; variant++) {
            int metadata = wildcard ? variant : identity.metadata();
            CompoundTag modern;
            if (vanilla) modern = vanilla(identity.item(), metadata, identity.nbt().map(LegacyStackNbt::read).orElse(null));
            else {
                String id = modItems.get(identity.item() + ":" + (damageable ? 0 : metadata));
                if (id == null) { variants = new JsonArray(); row.add("variants", variants); row.addProperty("missing", "ITEM_NOT_IMPLEMENTED"); return row; }
                modern = new CompoundTag(); modern.putString("id", id);
                var tag=portable==null?new CompoundTag():portable.copy();
                if (damageable) tag.putInt("Damage",metadata);
                if (!tag.isEmpty()) modern.put("tag",tag);
            }
            String id = modern.getString("id"); var location = ResourceLocation.tryParse(id);
            if (location == null || id.equals("minecraft:air") || !ForgeRegistries.ITEMS.containsKey(location)) {
                row.add("variants", new JsonArray()); row.addProperty("missing", "NO_MODERN_ITEM_FORM"); return row;
            }
            var result = new JsonObject(); result.addProperty("item", id);
            if (modern.contains("tag", 10) && !modern.getCompound("tag").isEmpty()) result.addProperty("tag", modern.getCompound("tag").toString());
            variants.add(result);
        }
        row.addProperty("cycle_damage", wildcard && !subtypes && damageable);
        row.addProperty("cycle_count", wildcard && (subtypes || damageable));
        return row;
    }
    static CompoundTag vanilla(String item, int metadata, CompoundTag tag) {
        var old = new CompoundTag(); old.putString("id", item); old.putByte("Count", (byte) 1); old.putShort("Damage", (short) metadata);
        if (tag != null) old.put("tag", tag.copy());
        else if(item.equals("minecraft:potion")) old.put("tag",new CompoundTag());
        var converted = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, old),
                99, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        return (CompoundTag) converted.getValue();
    }
    private static void collect(JsonElement element, Map<ResearchDisplayItems.Identity, JsonObject> stacks) {
        if (element.isJsonArray()) element.getAsJsonArray().forEach(value -> collect(value, stacks));
        else if (element.isJsonObject()) {
            var row = element.getAsJsonObject();
            if (row.has("item") && row.has("metadata") && row.has("count") && row.has("damageable"))
                stacks.putIfAbsent(ResearchDisplayItems.Identity.decode(row), row);
            row.entrySet().forEach(entry -> collect(entry.getValue(), stacks));
        }
    }
    @Override public String getName() { return "TC4 book display item migrations and pending dependencies"; }
}
