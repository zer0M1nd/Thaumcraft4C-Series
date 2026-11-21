package tc4revival.baseline;

import com.google.gson.GsonBuilder;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Independent observation tool, compiled only for the isolated legacy test server. Never part of the new mod. */
@Mod(modid = "tc4baselineprobe", name = "TC4 baseline observer", version = "1", dependencies = "required-after:Thaumcraft", acceptableRemoteVersions = "*")
public final class LegacyBaselineProbe {
    private Class<?> itemClass;
    private Class<?> stackClass;
    private Object registry;
    private Method nameOf;
    private final Map<String, Object> items = new TreeMap<String, Object>();
    private final Map<String, TreeSet<Integer>> queries = new TreeMap<String, TreeSet<Integer>>();

    /** Optional isolated fixture: lets TC discover copper naturally before its post-initialization. */
    @Mod.EventHandler
    public void prepareCopper(FMLPreInitializationEvent event) {
        if (!Boolean.getBoolean("tc4baseline.copperFixture")) return;
        try {
            Class<?> item = Class.forName("net.minecraft.item.Item");
            Class<?> stack = Class.forName("net.minecraft.item.ItemStack");
            java.lang.reflect.Constructor<?> constructor = item.getDeclaredConstructor(); constructor.setAccessible(true);
            Method register = Class.forName("cpw.mods.fml.common.registry.GameRegistry").getMethod("registerItem", item, String.class);
            Method ore = Class.forName("net.minecraftforge.oredict.OreDictionary").getMethod("registerOre", String.class, stack);
            for (String kind : new String[]{"ingot", "ore"}) {
                Object fixture = constructor.newInstance();
                item.getMethod("func_77655_b", String.class).invoke(fixture, "tc4baseline.copper_" + kind);
                register.invoke(null, fixture, "copper_" + kind);
                Object fixtureStack = stack.getConstructor(item, int.class, int.class).newInstance(fixture, 1, 0);
                ore.invoke(null, kind + "Copper", fixtureStack);
            }
        } catch (Exception failure) { throw new IllegalStateException("Cannot prepare isolated copper fixture", failure); }
    }

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) {
        if (Boolean.getBoolean("tc4baseline.excavationMaterialsOnly")) {
            try { save("excavation-materials.json", excavationMaterials()); }
            catch (Exception failure) { throw new IllegalStateException("Cannot observe excavation materials", failure); }
            return;
        }
        try { dump(); }
        catch (Exception failure) { throw new IllegalStateException("Legacy baseline observation failed", failure); }
    }

    @SuppressWarnings("unchecked")
    private void dump() throws Exception {
        Map<String, Object> environment = row();
        environment.put("copper_fixture", Boolean.getBoolean("tc4baseline.copperFixture"));
        Class<?> configuration = Class.forName("thaumcraft.common.config.Config");
        environment.put("found_copper_ingot", configuration.getField("foundCopperIngot").get(null));
        environment.put("found_copper_ore", configuration.getField("foundCopperOre").get(null));
        save("observation-environment.json", environment);
        itemClass = Class.forName("net.minecraft.item.Item");
        stackClass = Class.forName("net.minecraft.item.ItemStack");
        registry = itemClass.getField("field_150901_e").get(null);
        nameOf = registry.getClass().getMethod("func_148750_c", Object.class);
        List<Object> itemDefinitions = new ArrayList<Object>();
        for (Object item : (Iterable<?>) registry) {
            String name = name(item);
            items.put(name, item);
            Map<String, Object> definition = row();
            definition.put("item", name);
            definition.put("damageable", itemClass.getMethod("func_77645_m").invoke(item));
            definition.put("has_subtypes", itemClass.getMethod("func_77614_k").invoke(item));
            Object container = itemClass.getMethod("func_77668_q").invoke(item);
            definition.put("container", container == null ? null : name(container));
            itemDefinitions.add(definition);
            if (name.startsWith("minecraft:")) {
                queue(name, 0);
                if ((Boolean) itemClass.getMethod("func_77614_k").invoke(item) && !name.equals("minecraft:potion")) {
                    for (int meta = 1; meta < 16; meta++) queue(name, meta);
                }
            }
        }
        save("item-definitions.json", itemDefinitions);
        Class<?> api = Class.forName("thaumcraft.api.ThaumcraftApi");
        Map<Object, Object> live = (Map<Object, Object>) api.getField("objectTags").get(null);
        Map<Object, Object> initial = copyTags(live);
        List<Object> registered = new ArrayList<Object>();
        for (Map.Entry<Object, Object> entry : initial.entrySet()) {
            List<?> key = (List<?>) entry.getKey();
            if (key.isEmpty() || !itemClass.isInstance(key.get(0))) continue;
            String id = name(key.get(0));
            Map<String, Object> row = row();
            row.put("item", id);
            row.put("metadata", key.get(1));
            row.put("aspects", aspects(entry.getValue()));
            registered.add(row);
            if (key.get(1) instanceof Number) queue(id, ((Number) key.get(1)).intValue());
        }
        registered.sort(Comparator.comparing(value -> ((Map<?, ?>) value).get("item").toString()
                + ":" + ((Map<?, ?>) value).get("metadata")));
        save("registered-aspects.json", registered);
        Map<?, ?> groups = (Map<?, ?>) api.getField("groupedObjectTags").get(null);
        List<Object> grouped = new ArrayList<Object>();
        for (Map.Entry<?, ?> entry : groups.entrySet()) {
            List<?> key = (List<?>) entry.getKey();
            Map<String, Object> row = row();
            row.put("item", name(key.get(0)));
            row.put("metadata", key.get(1));
            row.put("group", entry.getValue());
            grouped.add(row);
        }
        grouped.sort(Comparator.comparing(value -> ((Map<?, ?>) value).get("item").toString()
                + ":" + ((Map<?, ?>) value).get("metadata")));
        save("grouped-object-tags.json", grouped);

        Class<?> manager = Class.forName("net.minecraft.item.crafting.CraftingManager");
        Object crafting = manager.getMethod("func_77594_a").invoke(null);
        List<?> vanilla = (List<?>) manager.getMethod("func_77592_b").invoke(crafting);
        List<Object> recipes = new ArrayList<Object>();
        for (Object recipe : vanilla) recipes.add(recipe(recipe));
        save("crafting-recipes.json", recipes);
        recipes = new ArrayList<Object>();
        for (Object recipe : (List<?>) api.getMethod("getCraftingRecipes").invoke(null)) recipes.add(recipe(recipe));
        save("thaumcraft-recipes.json", recipes);

        Class<?> tcManager = Class.forName("thaumcraft.common.lib.crafting.ThaumcraftCraftingManager");
        Method resolve = tcManager.getMethod("getObjectTags", stackClass);
        Method generate = tcManager.getMethod("generateTags", itemClass, int.class);
        Class<?> aspectListClass = Class.forName("thaumcraft.api.aspects.AspectList");
        Method bonuses = tcManager.getMethod("getBonusTags", stackClass, aspectListClass);
        Method itemHash = Class.forName("thaumcraft.common.lib.research.ScanManager")
                .getMethod("generateItemHash", itemClass, int.class);
        List<Object> results = new ArrayList<Object>();
        List<Object> scanResults = new ArrayList<Object>();
        List<Object> bonusResults = new ArrayList<Object>();
        for (Map.Entry<String, TreeSet<Integer>> query : queries.entrySet()) {
            if (!query.getKey().startsWith("minecraft:")) continue;
            for (int meta : query.getValue()) {
                // Each query starts from the same initialized map; observer order must not change evidence.
                live.clear();
                live.putAll(copyTags(initial));
                Map<String, Object> row = row();
                row.put("item", query.getKey());
                row.put("metadata", meta);
                Object stack = stackClass.getConstructor(itemClass, int.class, int.class).newInstance(items.get(query.getKey()), 1, meta);
                Object value = resolve.invoke(null, stack);
                row.put("has_aspect_list", value != null);
                row.put("aspects", aspects(value));
                live.clear();
                live.putAll(copyTags(initial));
                Object canonical = generate.invoke(null, items.get(query.getKey()), meta);
                row.put("has_canonical_list", canonical != null);
                row.put("canonical_aspects", aspects(canonical));
                results.add(row);
                live.clear();
                live.putAll(copyTags(initial));
                Object item = items.get(query.getKey());
                int[] group = (int[]) groups.get(java.util.Arrays.asList(item, meta));
                int scanMeta = group == null ? meta : group[0];
                Map<String, Object> scan = row();
                scan.put("item", query.getKey());
                scan.put("metadata", meta);
                scan.put("scan_metadata", scanMeta);
                // Observe the initial hash before resolution mutates the old aspect cache.
                scan.put("initial_hash", itemHash.invoke(null, item, meta));
                Object scanStack = stackClass.getConstructor(itemClass, int.class, int.class).newInstance(item, 1, scanMeta);
                Object scanValue = resolve.invoke(null, scanStack);
                scan.put("has_aspect_list", scanValue != null);
                scan.put("aspects", aspects(scanValue));
                scan.put("resolved_hash", itemHash.invoke(null, item, meta));
                scanResults.add(scan);
                Map<String, Object> bonus = row();
                bonus.put("item", query.getKey());
                bonus.put("metadata", meta);
                bonus.put("scan_metadata", scanMeta);
                bonus.put("base", aspects(scanValue));
                bonus.put("equipment_only", aspects(bonuses.invoke(null, scanStack, aspectListClass.newInstance())));
                bonus.put("final_aspects", aspects(bonuses.invoke(null, scanStack, scanValue)));
                bonusResults.add(bonus);
            }
        }
        live.clear();
        live.putAll(initial);
        save("resolved-vanilla-aspects.json", results);
        save("scan-vanilla-aspects.json", scanResults);
        save("bonus-vanilla-aspects.json", bonusResults);
        save("enchanted-aspects.json", enchantedObservations(resolve, bonuses));
        save("potion-aspects.json", potionObservations(resolve, generate, live, initial));
        save("block-scan-api.json", blockScanApi());
        save("block-item-links.json", blockItemLinks());
        save("research-table-materials.json", researchTableMaterials());
        save("block-picked-metadata.json", blockPickedMetadata());
        save("special-item-stacks.json", specialItemStacks(resolve, bonuses, itemHash, live, initial));
        save("foundation-items.json", foundationItems(resolve, generate, bonuses, itemHash, live, initial));
        save("research-catalog.json", researchCatalog());
        List<Object> pages = researchPages();
        save("research-pages.json", pages);
        save("research-page-recipes.json", researchPageRecipes(pages));
        save("research-notes.json", researchNotes());
        System.out.println("TC4 BASELINE COMPLETE: " + registered.size() + " registered rows; " + vanilla.size()
                + " crafting recipes; " + results.size() + " isolated vanilla queries.");
    }

    private List<Object> foundationItems(Method resolve, Method generate, Method bonuses, Method itemHash,
                                          Map<Object, Object> live, Map<Object, Object> initial) throws Exception {
        List<Object> result = new ArrayList<Object>();
        try {
            for (String id : new String[] {"Thaumcraft:ItemShard", "Thaumcraft:ItemThaumometer"}) {
                Object item = items.get(id);
                if (item == null) throw new IllegalStateException("Missing foundation item " + id);
                for (int metadata = 0; metadata < (id.endsWith("ItemShard") ? 7 : 1); metadata++) {
                    live.clear();
                    live.putAll(copyTags(initial));
                    Object stack = stackClass.getConstructor(itemClass, int.class, int.class).newInstance(item, 1, metadata);
                    Map<String, Object> entry = row();
                    entry.put("item", id);
                    entry.put("metadata", metadata);
                    entry.put("initial_hash", itemHash.invoke(null, item, metadata));
                    Object base = resolve.invoke(null, stack);
                    entry.put("has_aspect_list", base != null);
                    entry.put("aspects", aspects(base));
                    entry.put("resolved_hash", itemHash.invoke(null, item, metadata));
                    entry.put("final_aspects", aspects(bonuses.invoke(null, stack, base)));
                    live.clear();
                    live.putAll(copyTags(initial));
                    Object canonical = generate.invoke(null, item, metadata);
                    entry.put("has_canonical_list", canonical != null);
                    entry.put("canonical_aspects", aspects(canonical));
                    result.add(entry);
                }
            }
        } finally {
            live.clear();
            live.putAll(copyTags(initial));
        }
        return result;
    }

    private Map<String, Object> blockScanApi() throws Exception {
        Class<?> block = Class.forName("net.minecraft.block.Block");
        Map<String, Object> result = row();
        for (String name : new String[] {"getPickBlock", "func_149694_d", "func_149643_k", "func_149644_j", "createStackedBlock"}) {
            List<String> signatures = new ArrayList<String>();
            for (Method method : block.getDeclaredMethods()) if (method.getName().equals(name)) signatures.add(method.toString());
            result.put(name, signatures);
        }
        return result;
    }

    private List<Object> excavationMaterials() throws Exception {
        Class<?> blockClass=Class.forName("net.minecraft.block.Block");
        Object blocks=blockClass.getField("field_149771_c").get(null);
        Method lookup=blocks.getClass().getMethod("func_82594_a",String.class);
        Method name=blocks.getClass().getMethod("func_148750_c",Object.class);
        Method material=blockClass.getMethod("func_149688_o");
        Map<Object,String> groups=new java.util.IdentityHashMap<Object,String>();
        for(String[] sample:new String[][]{{"rock","stone"},{"grass","grass"},{"ground","dirt"},{"sand","sand"}})
            groups.put(material.invoke(lookup.invoke(blocks,"minecraft:"+sample[1])),sample[0]);
        List<Object> result=new ArrayList<Object>();
        for(Object block:(Iterable<?>)blocks){
            Map<String,Object> value=row();value.put("block",name.invoke(blocks,block));
            String group=groups.get(material.invoke(block));value.put("fast",group!=null);value.put("material",group==null?"other":group);result.add(value);
        }
        result.sort(Comparator.comparing(value->((Map<?,?>)value).get("block").toString()));return result;
    }

    private List<Object> researchTableMaterials() throws Exception {
        Class<?> blockClass = Class.forName("net.minecraft.block.Block");
        Object blocks = blockClass.getField("field_149771_c").get(null);
        Method lookup = blocks.getClass().getMethod("func_82594_a", String.class);
        Method blockName = blocks.getClass().getMethod("func_148750_c", Object.class);
        Method material = blockClass.getMethod("func_149688_o");
        String[][] samples = {{"ground", "dirt"}, {"fire", "fire"}, {"lava", "flowing_lava"},
                {"water", "water"}, {"circuits", "redstone_wire"}, {"piston", "piston"}};
        Map<Object, String> groups = new java.util.IdentityHashMap<Object, String>();
        for (String[] sample : samples) groups.put(material.invoke(lookup.invoke(blocks, "minecraft:" + sample[1])), sample[0]);
        List<Object> result = new ArrayList<Object>();
        for (Object block : (Iterable<?>) blocks) {
            String group = groups.get(material.invoke(block));
            if (group == null) continue;
            Map<String, Object> value = row();
            value.put("block", blockName.invoke(blocks, block));
            value.put("material", group);
            result.add(value);
        }
        result.sort(Comparator.comparing(value -> ((Map<?, ?>) value).get("block").toString()));
        return result;
    }

    private List<Object> blockItemLinks() throws Exception {
        Class<?> blockClass = Class.forName("net.minecraft.block.Block");
        Object blockRegistry = blockClass.getField("field_149771_c").get(null);
        Method blockName = blockRegistry.getClass().getMethod("func_148750_c", Object.class);
        Method blockId = blockClass.getMethod("func_149682_b", blockClass);
        Method fromBlock = itemClass.getMethod("func_150898_a", blockClass);
        Method fromId = itemClass.getMethod("func_150899_d", int.class);
        Method stacked = Class.forName("thaumcraft.common.lib.utils.BlockUtils").getMethod("createStackedBlock", blockClass, int.class);
        List<Object> result = new ArrayList<Object>();
        for (Object block : (Iterable<?>) blockRegistry) {
            String id = String.valueOf(blockName.invoke(blockRegistry, block));
            if (!id.startsWith("minecraft:")) continue;
            Map<String, Object> row = row();
            row.put("block", id);
            row.put("class", block.getClass().getName());
            int numericId = (Integer) blockId.invoke(null, block);
            row.put("numeric_id", numericId);
            Object item = fromBlock.invoke(null, block);
            row.put("has_block_item", item != null);
            if (item != null) row.put("block_item", name(item));
            Object fallback = fromId.invoke(null, numericId);
            row.put("has_numeric_fallback", fallback != null);
            if (fallback != null) row.put("numeric_fallback", name(fallback));
            row.put("stacked_metadata_zero", normalized(stacked.invoke(null, block, 0)));
            result.add(row);
        }
        return result;
    }

    private List<Object> blockPickedMetadata() throws Exception {
        Class<?> blockClass = Class.forName("net.minecraft.block.Block");
        Class<?> worldClass = Class.forName("net.minecraft.world.World");
        Class<?> serverClass = Class.forName("net.minecraft.server.MinecraftServer");
        Object server = serverClass.getMethod("func_71276_C").invoke(null);
        Object world = serverClass.getMethod("func_71218_a", int.class).invoke(server, 0);
        Object blockRegistry = blockClass.getField("field_149771_c").get(null);
        Method byName = blockRegistry.getClass().getMethod("func_82594_a", String.class);
        Method setBlock = worldClass.getMethod("func_147465_d", int.class, int.class, int.class, blockClass, int.class, int.class);
        Method getBlock = worldClass.getMethod("func_147439_a", int.class, int.class, int.class);
        Method getMetadata = worldClass.getMethod("func_72805_g", int.class, int.class, int.class);
        Method getDamage = blockClass.getMethod("func_149643_k", worldClass, int.class, int.class, int.class);
        Object air = byName.invoke(blockRegistry, "minecraft:air");
        if (getBlock.invoke(world, 0, 200, 0) != air) throw new IllegalStateException("Legacy metadata probe location must be empty");
        List<Object> results = new ArrayList<Object>();
        try {
            for (String name : new String[] {"farmland", "dirt", "log", "log2", "quartz_block", "stone_slab", "double_stone_slab", "wooden_slab", "anvil", "monster_egg"}) {
                Object block = byName.invoke(blockRegistry, "minecraft:" + name);
                // Selection is independently inferred from client bytecode; getPickBlock is NOT executed here.
                String lookupName = name.equals("farmland") ? "dirt" : name.equals("double_stone_slab") ? "stone_slab" : name;
                Object lookupBlock = byName.invoke(blockRegistry, "minecraft:" + lookupName);
                for (int metadata = 0; metadata < 16; metadata++) {
                    setBlock.invoke(world, 0, 200, 0, block, metadata, 2);
                    Map<String, Object> row = row();
                    row.put("block", "minecraft:" + name);
                    row.put("metadata", metadata);
                    row.put("stored_metadata", getMetadata.invoke(world, 0, 200, 0));
                    row.put("metadata_lookup_block", "minecraft:" + lookupName);
                    row.put("picked_metadata", getDamage.invoke(lookupBlock, world, 0, 200, 0));
                    results.add(row);
                }
            }
        } finally { setBlock.invoke(world, 0, 200, 0, air, 0, 2); }
        return results;
    }

    private List<Object> specialItemStacks(Method resolve, Method bonuses, Method hash, Map<Object, Object> live,
                                           Map<Object, Object> initial) throws Exception {
        Class<?> entityList = Class.forName("net.minecraft.entity.EntityList");
        Map<?, ?> eggs = (Map<?, ?>) entityList.getField("field_75627_a").get(null);
        TreeSet<Integer> eggIds = new TreeSet<Integer>();
        List<Object> definitions = new ArrayList<Object>();
        for (Object key : eggs.keySet()) {
            int id = ((Number) key).intValue();
            Class<?> type = (Class<?>) entityList.getMethod("func_90035_a", int.class).invoke(null, id);
            if (!type.getName().startsWith("net.minecraft.")) continue;
            eggIds.add(id);
        }
        for (int id : eggIds) {
            Map<String, Object> row = row();
            row.put("metadata", id);
            row.put("entity", entityList.getMethod("func_75617_a", int.class).invoke(null, id));
            definitions.add(row);
        }
        save("vanilla-spawn-eggs.json", definitions);
        List<Object> results = new ArrayList<Object>();
        for (String item : new String[] {"minecraft:spawn_egg", "minecraft:filled_map"}) {
            Iterable<Integer> metadata = item.equals("minecraft:spawn_egg") ? eggIds
                    : java.util.Arrays.asList(-1, 0, 1, 2, 15, 16, 42, 32767, 32768, 65535, Integer.MAX_VALUE);
            for (int requested : metadata) for (boolean enchanted : new boolean[] {false, true}) {
                live.clear();
                live.putAll(copyTags(initial));
                Object stack = stackClass.getConstructor(itemClass, int.class, int.class).newInstance(items.get(item), 1, requested);
                if (enchanted) stackClass.getMethod("func_77983_a", String.class, Class.forName("net.minecraft.nbt.NBTBase"))
                        .invoke(stack, "ench", enchantmentList(new int[][] {{16, 7}}));
                int actual = (Integer) stackClass.getMethod("func_77960_j").invoke(stack);
                Object base = resolve.invoke(null, stack);
                Map<String, Object> row = row();
                row.put("item", item);
                row.put("requested_metadata", requested);
                row.put("metadata", actual);
                row.put("sharpness", enchanted ? 7 : 0);
                row.put("has_aspect_list", base != null);
                row.put("base", aspects(base));
                row.put("final_aspects", aspects(bonuses.invoke(null, stack, base)));
                row.put("hash", hash.invoke(null, items.get(item), actual));
                results.add(row);
            }
        }
        live.clear();
        live.putAll(initial);
        return results;
    }

    private List<Object> potionObservations(Method resolve, Method generate, Map<Object, Object> live,
                                            Map<Object, Object> initial) throws Exception {
        Object item = items.get("minecraft:potion");
        Class<?> potionClass = Class.forName("net.minecraft.item.ItemPotion");
        Class<?> effectClass = Class.forName("net.minecraft.potion.PotionEffect");
        Method getEffects = potionClass.getMethod("func_77834_f", int.class);
        TreeSet<Integer> metadata = new TreeSet<Integer>(queries.get("minecraft:potion"));
        metadata.add(32767); // Wildcard lookup can reach the registered water entry before effect decoration.
        for (int flags : new int[] {0, 8192, 16384, 24576}) for (int value = 0; value < 128; value++) metadata.add(flags + value);
        List<Object> results = new ArrayList<Object>();
        for (int meta : metadata) {
            Map<String, Object> row = row();
            row.put("metadata", meta);
            row.put("splash", potionClass.getMethod("func_77831_g", int.class).invoke(null, meta));
            List<?> effects = (List<?>) getEffects.invoke(item, meta);
            row.put("has_effect_list", effects != null);
            List<Object> effectRows = new ArrayList<Object>();
            if (effects != null) for (Object effect : effects) {
                Map<String, Object> fields = row();
                fields.put("effect", effectClass.getMethod("func_76456_a").invoke(effect));
                fields.put("amplifier", effectClass.getMethod("func_76458_c").invoke(effect));
                fields.put("duration", effectClass.getMethod("func_76459_b").invoke(effect));
                effectRows.add(fields);
            }
            row.put("effects", effectRows);
            live.clear();
            live.putAll(copyTags(initial));
            Object stack = stackClass.getConstructor(itemClass, int.class, int.class).newInstance(item, 1, meta);
            Object resolved = resolve.invoke(null, stack);
            row.put("has_aspect_list", resolved != null);
            row.put("aspects", aspects(resolved));
            live.clear();
            live.putAll(copyTags(initial));
            Object canonical = generate.invoke(null, item, meta);
            row.put("has_canonical_list", canonical != null);
            row.put("canonical_aspects", aspects(canonical));
            results.add(row);
        }
        live.clear();
        live.putAll(copyTags(initial));
        return results;
    }

    private List<Object> enchantedObservations(Method resolve, Method bonuses) throws Exception {
        // Explicit vanilla 1.7.10 IDs plus the two actual TC enchantment instances. No client required.
        List<Integer> ids = new ArrayList<Integer>(java.util.Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7,
                16, 17, 18, 19, 20, 21, 32, 33, 34, 35, 48, 49, 50, 51, 61, 62));
        Class<?> enchantment = Class.forName("net.minecraft.enchantment.Enchantment");
        Class<?> config = Class.forName("thaumcraft.common.config.Config");
        for (String field : new String[] {"enchHaste", "enchRepair"}) {
            ids.add(enchantment.getField("field_77352_x").getInt(config.getField(field).get(null)));
        }
        List<Object> results = new ArrayList<Object>();
        for (String item : new String[] {"minecraft:paper", "minecraft:enchanted_book"}) {
            for (int id : ids) results.add(enchantedCase(item, new int[][] {{id, 3}}, resolve, bonuses));
        }
        int[][] mixed = new int[ids.size()][2];
        for (int i = 0; i < ids.size(); i++) mixed[i] = new int[] {ids.get(i), 1 + i % 5};
        for (String item : new String[] {"minecraft:paper", "minecraft:diamond_sword", "minecraft:diamond_helmet",
                "minecraft:diamond_pickaxe", "minecraft:bow", "minecraft:enchanted_book"}) {
            results.add(enchantedCase(item, mixed, resolve, bonuses));
            results.add(enchantedCase(item, new int[][] {{16, 2}, {16, 5}, {19, 0}, {20, -2}, {999, 3}}, resolve, bonuses));
        }
        return results;
    }

    private Object enchantedCase(String id, int[][] entries, Method resolve, Method bonuses) throws Exception {
        Object stack = stackClass.getConstructor(itemClass, int.class, int.class).newInstance(items.get(id), 1, 0);
        String label = id.equals("minecraft:enchanted_book") ? "StoredEnchantments" : "ench";
        stackClass.getMethod("func_77983_a", String.class, Class.forName("net.minecraft.nbt.NBTBase")).invoke(stack, label, enchantmentList(entries));
        Object base = resolve.invoke(null, stack);
        Map<String, Object> row = row();
        row.put("item", id);
        row.put("enchantments", entries);
        row.put("base", aspects(base));
        row.put("final_aspects", aspects(bonuses.invoke(null, stack, base)));
        return row;
    }

    private Object enchantmentList(int[][] entries) throws Exception {
        Class<?> compoundClass = Class.forName("net.minecraft.nbt.NBTTagCompound");
        Class<?> listClass = Class.forName("net.minecraft.nbt.NBTTagList");
        Class<?> baseClass = Class.forName("net.minecraft.nbt.NBTBase");
        Object list = listClass.newInstance();
        for (int[] entry : entries) {
            Object compound = compoundClass.newInstance();
            compoundClass.getMethod("func_74777_a", String.class, short.class).invoke(compound, "id", (short) entry[0]);
            compoundClass.getMethod("func_74777_a", String.class, short.class).invoke(compound, "lvl", (short) entry[1]);
            listClass.getMethod("func_74742_a", baseClass).invoke(list, compound);
        }
        return list;
    }

    private Map<Object, Object> copyTags(Map<Object, Object> source) throws Exception {
        Map<Object, Object> copy = new LinkedHashMap<Object, Object>();
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            copy.put(entry.getKey(), value == null ? null : value.getClass().getMethod("copy").invoke(value));
        }
        return copy;
    }

    private Map<String, Object> recipe(Object recipe) throws Exception {
        Map<String, Object> row = row();
        row.put("class", recipe.getClass().getName());
        row.put("aspect_recipe_kind", recipeKind(recipe));
        Object output = invokeOptional(recipe, "func_77571_b", "getRecipeOutput");
        row.put("output", normalized(output));
        if (output == null) {
            row.put("requires_runtime_input", true);
            return row;
        }
        Object input = invokeOptional(recipe, "getInput", "getComponents");
        if (input == null) {
            for (Field field : recipe.getClass().getFields()) {
                if (field.getType().equals(List.class) || (field.getType().isArray() && field.getType().getComponentType().equals(stackClass))) {
                    input = field.get(recipe);
                    break;
                }
            }
        }
        if (Class.forName("net.minecraft.item.crafting.ShapedRecipes").isInstance(recipe)) {
            row.put("width", recipe.getClass().getField("field_77576_b").get(recipe));
            row.put("height", recipe.getClass().getField("field_77577_c").get(recipe));
        }
        if (Class.forName("thaumcraft.api.crafting.ShapedArcaneRecipe").isInstance(recipe)) {
            row.put("width", recipe.getClass().getField("width").get(recipe));
            row.put("height", recipe.getClass().getField("height").get(recipe));
        }
        row.put("ingredients", normalized(input));
        row.put("center", normalized(invokeOptional(recipe, "getRecipeInput")));
        Object cost = invokeOptional(recipe, "getAspects");
        if (cost == null) {
            try { cost = recipe.getClass().getField("aspects").get(recipe); }
            catch (NoSuchFieldException ignored) { }
        }
        row.put("aspects", aspects(cost));
        try { row.put("catalyst", normalized(recipe.getClass().getField("catalyst").get(recipe))); }
        catch (NoSuchFieldException ignored) { }
        return row;
    }

    private String recipeKind(Object recipe) throws Exception {
        String[][] types = {
                {"net.minecraft.item.crafting.ShapedRecipes", "crafting_shaped"},
                {"net.minecraft.item.crafting.ShapelessRecipes", "crafting_shapeless"},
                {"net.minecraftforge.oredict.ShapedOreRecipe", "crafting_ore"},
                {"net.minecraftforge.oredict.ShapelessOreRecipe", "crafting_ore"},
                {"thaumcraft.api.crafting.ShapedArcaneRecipe", "arcane_shaped"},
                {"thaumcraft.api.crafting.ShapelessArcaneRecipe", "arcane_shapeless"},
                {"thaumcraft.api.crafting.IArcaneRecipe", "arcane_other"},
                {"thaumcraft.api.crafting.InfusionRecipe", "infusion"},
                {"thaumcraft.api.crafting.CrucibleRecipe", "crucible"}
        };
        for (String[] type : types) if (Class.forName(type[0]).isInstance(recipe)) return type[1];
        return "unsupported";
    }

    /** Reads the registered catalog, including nullable triggers and the actual map iteration order. */
    private List<Object> researchCatalog() throws Exception {
        List<Object> result = new ArrayList<Object>();
        Class<?> categories = Class.forName("thaumcraft.api.research.ResearchCategories");
        Class<?> api = Class.forName("thaumcraft.api.ThaumcraftApi");
        Map<?, ?> registered = (Map<?, ?>) categories.getField("researchCategories").get(null);
        for (Map.Entry<?, ?> category : registered.entrySet()) {
            Object list = category.getValue();
            Map<String, Object> categoryRow = row();
            categoryRow.put("key", category.getKey());
            categoryRow.put("icon", String.valueOf(list.getClass().getField("icon").get(list)));
            categoryRow.put("background", String.valueOf(list.getClass().getField("background").get(list)));
            List<Object> entries = new ArrayList<Object>();
            for (Object research : ((Map<?, ?>) list.getClass().getField("research").get(list)).values()) {
                Class<?> type = research.getClass();
                Map<String, Object> entry = row();
                for (String field : new String[] {"key", "category", "parents", "parentsHidden", "siblings", "displayColumn", "displayRow"}) {
                    entry.put(field, normalized(type.getField(field).get(research)));
                }
                entry.put("aspects", aspects(type.getField("tags").get(research)));
                entry.put("complexity", type.getMethod("getComplexity").invoke(research));
                entry.put("warp", api.getMethod("getWarp", Object.class).invoke(null, type.getField("key").get(research)));
                for (String flag : new String[] {"Special", "Secondary", "Round", "Stub", "Virtual", "Concealed", "Hidden", "Lost", "AutoUnlock"}) {
                    entry.put("is" + flag, type.getMethod("is" + flag).invoke(research));
                }
                Object resource = type.getField("icon_resource").get(research);
                entry.put("icon_resource", resource == null ? null : resource.toString());
                entry.put("icon_item", researchStack(type.getField("icon_item").get(research)));
                Object triggers = type.getMethod("getItemTriggers").invoke(research);
                if (triggers != null) {
                    List<Object> stacks = new ArrayList<Object>();
                    for (int i = 0; i < Array.getLength(triggers); i++) stacks.add(researchStack(Array.get(triggers, i)));
                    entry.put("itemTriggers", stacks);
                }
                entry.put("entityTriggers", normalized(type.getMethod("getEntityTriggers").invoke(research)));
                Object aspectTriggers = type.getMethod("getAspectTriggers").invoke(research);
                if (aspectTriggers != null) {
                    List<String> tags = new ArrayList<String>();
                    for (Object aspect : (Object[]) aspectTriggers) tags.add((String) aspect.getClass().getMethod("getTag").invoke(aspect));
                    entry.put("aspectTriggers", tags);
                }
                Object pages = type.getMethod("getPages").invoke(research);
                entry.put("pageCount", pages == null ? 0 : Array.getLength(pages));
                entries.add(entry);
            }
            categoryRow.put("research", entries);
            result.add(categoryRow);
        }
        return result;
    }

    /** Read page order and actual registered recipe identities, without calling any client renderer. */
    private List<Object> researchPages() throws Exception {
        Map<?, ?> registered = (Map<?, ?>) Class.forName("thaumcraft.api.research.ResearchCategories").getField("researchCategories").get(null);
        Map<?, ?> recipes = (Map<?, ?>) Class.forName("thaumcraft.common.config.ConfigResearch").getField("recipes").get(null);
        List<Object> result = new ArrayList<Object>();
        for (Object category : registered.values()) for (Object research : ((Map<?, ?>) category.getClass().getField("research").get(category)).values()) {
            Map<String, Object> entry = row(); entry.put("key", research.getClass().getField("key").get(research));
            List<Object> pages = new ArrayList<Object>();
            Object array = research.getClass().getMethod("getPages").invoke(research);
            if (array != null) for (int i = 0; i < Array.getLength(array); i++) {
                Object page = Array.get(array, i);
                if (page == null) { pages.add(null); continue; }
                Map<String, Object> value = row();
                for (String field : new String[] {"type", "text", "research", "image"}) {
                    Object content = page.getClass().getField(field).get(page);
                    value.put(field, content == null ? null : content.toString());
                }
                value.put("aspects", aspects(page.getClass().getField("aspects").get(page)));
                value.put("recipeOutput", researchStack(page.getClass().getField("recipeOutput").get(page)));
                value.put("recipe", pageRecipe(page.getClass().getField("recipe").get(page), recipes));
                pages.add(value);
            }
            entry.put("pages", pages); result.add(entry);
        }
        return result;
    }
    private Object pageRecipe(Object value, Map<?, ?> recipes) throws Exception {
        if (value == null) return null;
        List<String> aliases = new ArrayList<String>();
        for (Map.Entry<?, ?> named : recipes.entrySet()) if (named.getValue() == value) aliases.add(named.getKey().toString());
        java.util.Collections.sort(aliases);
        Map<String, Object> result = row();
        if (!aliases.isEmpty()) { result.put("kind", "reference"); result.put("keys", aliases); result.put("class", value.getClass().getName()); return result; }
        if (stackClass.isInstance(value)) { result.put("kind", "stack"); result.put("stack", researchStack(value)); return result; }
        if (value.getClass().isArray() || value instanceof Iterable) {
            List<Object> elements = new ArrayList<Object>();
            if (value.getClass().isArray()) for (int i = 0; i < Array.getLength(value); i++) elements.add(pageRecipe(Array.get(value, i), recipes));
            else for (Object element : (Iterable<?>) value) elements.add(pageRecipe(element, recipes));
            result.put("kind", "sequence"); result.put("elements", elements); return result;
        }
        if (value instanceof Number || value instanceof String || value instanceof Boolean) {
            result.put("kind", "literal"); result.put("value", value); return result;
        }
        if (Class.forName("thaumcraft.api.aspects.AspectList").isInstance(value)) {
            result.put("kind", "aspects"); result.put("aspects", aspects(value)); return result;
        }
        if (value.getClass().getName().equals("thaumcraft.api.crafting.ShapedArcaneRecipe")) {
            result.put("kind", "inline_arcane");
            for (String field : new String[] {"research", "width", "height"})
                result.put(field, value.getClass().getField(field).get(value));
            result.put("output", researchStack(value.getClass().getField("output").get(value)));
            result.put("ingredients", pageRecipe(value.getClass().getField("input").get(value), recipes));
            result.put("aspects", aspects(value.getClass().getField("aspects").get(value)));
            Field mirrored = value.getClass().getDeclaredField("mirrored"); mirrored.setAccessible(true);
            result.put("mirrored", mirrored.get(value));
            return result;
        }
        if (value.getClass().getName().equals("thaumcraft.common.lib.crafting.InfusionRunicAugmentRecipe")) {
            // Evaluated examples shown by the book, not five fixed gameplay recipes.
            result.put("kind", "runic_augmentation_example");
            result.put("research", value.getClass().getMethod("getResearch").invoke(value));
            result.put("input", researchStack(value.getClass().getMethod("getRecipeInput").invoke(value)));
            result.put("output", researchStack(value.getClass().getMethod("getRecipeOutput").invoke(value)));
            result.put("ingredients", pageRecipe(value.getClass().getMethod("getComponents").invoke(value), recipes));
            result.put("aspects", aspects(value.getClass().getMethod("getAspects").invoke(value)));
            result.put("instability", value.getClass().getMethod("getInstability").invoke(value));
            return result;
        }
        result.put("kind", "unresolved"); result.put("class", value.getClass().getName());
        return result;
    }

    private void collectPageReferences(Object value, java.util.Set<String> names) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if ("reference".equals(map.get("kind"))) for (Object name : (Iterable<?>) map.get("keys")) names.add(name.toString());
            for (Object child : map.values()) collectPageReferences(child, names);
        } else if (value instanceof Iterable) for (Object child : (Iterable<?>) value) collectPageReferences(child, names);
    }

    private List<Object> researchPageRecipes(List<Object> pages) throws Exception {
        java.util.Set<String> names = new TreeSet<String>(); collectPageReferences(pages, names);
        Map<?, ?> recipes = (Map<?, ?>) Class.forName("thaumcraft.common.config.ConfigResearch").getField("recipes").get(null);
        List<Object> result = new ArrayList<Object>();
        for (String name : names) {
            Object recipe = recipes.get(name);
            if (recipe == null) throw new IllegalStateException("Missing named page recipe " + name);
            Map<String, Object> entry = row(); entry.put("key", name); entry.put("class", recipe.getClass().getName());
            entry.put("data", namedPageRecipe(recipe)); result.add(entry);
        }
        return result;
    }

    private Object recipeValue(Object value) throws Exception {
        if (value == null) return null;
        if (Class.forName("net.minecraft.nbt.NBTBase").isInstance(value)) {
            Map<String, Object> result = row(); result.put("kind", "nbt"); result.put("value", value.toString());
            result.put("type", value.getClass().getName()); return result;
        }
        if (value.getClass().isArray() || value instanceof Iterable) {
            List<Object> elements = new ArrayList<Object>();
            if (value.getClass().isArray()) for (int i = 0; i < Array.getLength(value); i++) elements.add(recipeValue(Array.get(value, i)));
            else for (Object child : (Iterable<?>) value) elements.add(recipeValue(child));
            Map<String, Object> result = row(); result.put("kind", "sequence"); result.put("elements", elements);
            if (value instanceof List) {
                Class<?> ores = Class.forName("net.minecraftforge.oredict.OreDictionary");
                List<String> aliases = new ArrayList<String>();
                for (String name : (String[]) ores.getMethod("getOreNames").invoke(null))
                    if (ores.getMethod("getOres", String.class).invoke(null, name) == value) aliases.add(name);
                if (!aliases.isEmpty()) { java.util.Collections.sort(aliases); result.put("kind", "ore"); result.put("names", aliases); }
            }
            return result;
        }
        return pageRecipe(value, java.util.Collections.emptyMap());
    }

    private Object recipeField(Object value, String name) throws Exception {
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(value); }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(value.getClass().getName() + "." + name);
    }

    /** Observes display inputs without executing crafting or copying implementation code. */
    private Map<String, Object> namedPageRecipe(Object recipe) throws Exception {
        Map<String, Object> result = row();
        String type = recipe.getClass().getName();
        if (recipe instanceof Iterable) {
            result.put("kind", "compound"); result.put("structure", recipeValue(recipe)); return result;
        }
        if (type.equals("thaumcraft.api.crafting.InfusionEnchantmentRecipe")) {
            result.put("kind", "infusion_enchantment");
            for (String field : new String[] {"research", "recipeXP", "instability"}) result.put(field, recipeField(recipe, field));
            result.put("aspects", aspects(recipeField(recipe, "aspects")));
            result.put("ingredients", recipeValue(recipeField(recipe, "components")));
            Object enchantment = recipeField(recipe, "enchantment");
            Map<String, Object> enchant = row();
            enchant.put("id", enchantment.getClass().getField("field_77352_x").get(enchantment));
            enchant.put("name", enchantment.getClass().getMethod("func_77320_a").invoke(enchantment));
            enchant.put("max_level", enchantment.getClass().getMethod("func_77325_b").invoke(enchantment));
            result.put("enchantment", enchant); return result;
        }
        String kind = recipeKind(recipe); result.put("kind", kind);
        result.put("output", recipeValue(invokeOptional(recipe, "func_77571_b", "getRecipeOutput")));
        if (kind.equals("infusion")) {
            result.put("input", recipeValue(invokeOptional(recipe, "getRecipeInput")));
            result.put("ingredients", recipeValue(invokeOptional(recipe, "getComponents")));
            result.put("research", invokeOptional(recipe, "getResearch"));
            result.put("instability", invokeOptional(recipe, "getInstability"));
            result.put("aspects", aspects(invokeOptional(recipe, "getAspects"))); return result;
        }
        if (kind.equals("crucible")) {
            result.put("research", recipeField(recipe, "key")); result.put("catalyst", recipeValue(recipeField(recipe, "catalyst")));
            result.put("aspects", aspects(recipeField(recipe, "aspects"))); return result;
        }
        if (kind.equals("crafting_shaped")) {
            result.put("width", recipeField(recipe, "field_77576_b")); result.put("height", recipeField(recipe, "field_77577_c"));
            result.put("ingredients", recipeValue(recipeField(recipe, "field_77574_d"))); return result;
        }
        if (kind.equals("crafting_shapeless")) {
            result.put("ingredients", recipeValue(recipeField(recipe, "field_77579_b"))); return result;
        }
        if (kind.equals("crafting_ore") || kind.startsWith("arcane_")) {
            result.put("ingredients", recipeValue(recipeField(recipe, "input")));
            boolean shaped = type.equals("net.minecraftforge.oredict.ShapedOreRecipe") || kind.equals("arcane_shaped");
            result.put("shaped", shaped);
            if (shaped) for (String field : new String[] {"width", "height", "mirrored"}) result.put(field, recipeField(recipe, field));
            if (kind.startsWith("arcane_")) {
                result.put("research", recipeField(recipe, "research")); result.put("aspects", aspects(recipeField(recipe, "aspects")));
            }
            result.put("matches_input_nbt", type.equals("thaumcraft.common.lib.crafting.ShapelessNBTOreRecipe")); return result;
        }
        throw new IllegalArgumentException("Unsupported named page recipe " + type);
    }

    /** Observe real TC4 generation; temporarily isolate and then restore the world's RNG object. */
    private List<Object> researchNotes() throws Exception {
        List<Object> result = new ArrayList<Object>();
        Class<?> worldClass = Class.forName("net.minecraft.world.World");
        Object world = Class.forName("net.minecraftforge.common.DimensionManager").getMethod("getWorld", int.class).invoke(null, 0);
        Field randomField = worldClass.getField("field_73012_v");
        randomField.setAccessible(true);
        Object originalRandom = randomField.get(world);
        Class<?> manager = Class.forName("thaumcraft.common.lib.research.ResearchManager");
        Method create = manager.getMethod("createNote", stackClass, String.class, worldClass);
        Method decode = manager.getMethod("getData", stackClass);
        Map<?, ?> categories = (Map<?, ?>) Class.forName("thaumcraft.api.research.ResearchCategories")
                .getField("researchCategories").get(null);
        Object item = Class.forName("thaumcraft.common.config.ConfigItems").getField("itemResearchNotes").get(null);
        try {
            for (Object category : categories.values()) {
                for (Object research : ((Map<?, ?>) category.getClass().getField("research").get(category)).values()) {
                    if (research.getClass().getMethod("getResearchPrimaryTag").invoke(research) == null) continue;
                    String key = (String) research.getClass().getField("key").get(research);
                    for (long seed : new long[] {0L, 1L, 42L, 987654321L}) {
                        java.util.Random random = new java.util.Random(seed);
                        randomField.set(world, random);
                        Object stack = stackClass.getConstructor(itemClass, int.class, int.class).newInstance(item, 1, 0);
                        create.invoke(null, stack, key, world);
                        Object data = decode.invoke(null, stack);
                        Map<String, Object> entry = row();
                        entry.put("seed", seed);
                        for (String field : new String[] {"key", "color", "complete", "copies"}) {
                            entry.put(field, data.getClass().getField(field).get(data));
                        }
                        List<Object> grid = new ArrayList<Object>();
                        Map<?, ?> cells = (Map<?, ?>) data.getClass().getField("hexEntries").get(data);
                        for (Object hex : ((Map<?, ?>) data.getClass().getField("hexes").get(data)).values()) {
                            Map<String, Object> cell = row();
                            cell.put("q", hex.getClass().getField("q").get(hex));
                            cell.put("r", hex.getClass().getField("r").get(hex));
                            Object content = cells.get(hex.toString());
                            cell.put("type", content.getClass().getField("type").get(content));
                            Object aspect = content.getClass().getField("aspect").get(content);
                            cell.put("aspect", aspect == null ? null : aspect.getClass().getMethod("getTag").invoke(aspect));
                            grid.add(cell);
                        }
                        entry.put("grid", grid);
                        entry.put("next_random_int", random.nextInt());
                        result.add(entry);
                    }
                }
            }
        } finally {
            randomField.set(world, originalRandom);
        }
        if (randomField.get(world) != originalRandom) throw new IllegalStateException("World RNG was not restored");
        System.out.println("TC4 RESEARCH NOTES OBSERVED: " + result.size() + "; original world RNG restored.");
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object researchStack(Object stack) throws Exception {
        if (stack == null) return null;
        Map<String, Object> result = (Map<String, Object>) normalized(stack);
        Object nbt = stackClass.getMethod("func_77978_p").invoke(stack);
        if (nbt != null) result.put("nbt", nbt.toString());
        result.put("damageable", stackClass.getMethod("func_77984_f").invoke(stack));
        Class<?> ores = Class.forName("net.minecraftforge.oredict.OreDictionary");
        int ore = (Integer) ores.getMethod("getOreID", stackClass).invoke(null, stack);
        if (ore >= 0) result.put("ore", ores.getMethod("getOreName", int.class).invoke(null, ore));
        return result;
    }

    private Object invokeOptional(Object target, String... names) throws Exception {
        for (String name : names) {
            try { return target.getClass().getMethod(name).invoke(target); }
            catch (NoSuchMethodException ignored) { }
        }
        return null;
    }

    private Object normalized(Object value) throws Exception {
        if (value == null) return null;
        if (stackClass.isInstance(value)) {
            Object item = stackClass.getMethod("func_77973_b").invoke(value);
            if (item == null) return null;
            String id = name(item);
            int meta = (Integer) stackClass.getMethod("func_77960_j").invoke(value);
            Map<String, Object> row = row();
            row.put("item", id);
            row.put("metadata", meta);
            row.put("count", stackClass.getField("field_77994_a").get(value));
            queue(id, meta);
            return row;
        }
        if (value.getClass().isArray()) {
            List<Object> result = new ArrayList<Object>();
            for (int i = 0; i < Array.getLength(value); i++) result.add(normalized(Array.get(value, i)));
            return result;
        }
        if (value instanceof Iterable) {
            List<Object> result = new ArrayList<Object>();
            for (Object element : (Iterable<?>) value) result.add(normalized(element));
            return result;
        }
        if (value instanceof Number || value instanceof String || value instanceof Boolean) return value;
        return value.getClass().getName();
    }

    private Map<String, Integer> aspects(Object list) throws Exception {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        if (list == null) return result;
        for (Object aspect : (Object[]) list.getClass().getMethod("getAspects").invoke(list)) {
            if (aspect == null) continue;
            String tag = (String) aspect.getClass().getMethod("getTag").invoke(aspect);
            int amount = (Integer) list.getClass().getMethod("getAmount", aspect.getClass()).invoke(list, aspect);
            result.put(tag, amount);
        }
        return result;
    }

    private String name(Object item) throws Exception { return String.valueOf(nameOf.invoke(registry, item)); }
    private void queue(String id, int meta) {
        if (!queries.containsKey(id)) queries.put(id, new TreeSet<Integer>());
        queries.get(id).add(meta == Short.MAX_VALUE ? 0 : meta);
    }
    private Map<String, Object> row() { return new LinkedHashMap<String, Object>(); }
    private void save(String name, Object data) throws Exception {
        Path directory = Paths.get("baseline-output");
        Files.createDirectories(directory);
        try (Writer writer = Files.newBufferedWriter(directory.resolve(name), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(data, writer);
        }
    }
}
