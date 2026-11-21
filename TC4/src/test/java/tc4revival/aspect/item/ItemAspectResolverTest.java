package tc4revival.aspect.item;

import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.data.LegacyItemDefinitions;
import static org.junit.jupiter.api.Assertions.*;

class ItemAspectResolverTest {
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final ItemAspectResolver resolver = new ItemAspectResolver(LegacyItemDefinitions.rules(), catalog);
    private AspectAmounts value(String item) { return resolver.resolve("minecraft:" + item, tag -> true).orElseThrow(); }
    private int amount(String item, String aspect) { return value(item).amount(catalog.require(aspect)); }

    @Test void authoredTagRulesCoverNewMembersAndInvalidateAfterTagReload() {
        var members=new java.util.HashSet<>(List.of("test:metal"));
        var wildcard=new ItemAspectRule(List.of("*"),"forge:ingots/test",List.of(new ItemAspectRule.Quantity("metallum",8)));
        var local=new ItemAspectResolver(List.of(wildcard),catalog,false,tc4revival.aspect.recipe.InferenceLimits.server(),
                (item,tag)->members.contains(item));
        assertTrue(local.contains("test:metal"));
        assertEquals(8,local.resolve("test:metal",tag->true).orElseThrow().total());
        assertFalse(local.contains("test:other"));
        members.clear();members.add("test:other");local.clearTagCache();
        assertFalse(local.contains("test:metal"));
        assertEquals(8,local.resolve("test:other",tag->true).orElseThrow().total());
    }

    @Test void repeatedRegistryLookupsDoNotRescanEveryMaterialTag() {
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        var rules=java.util.stream.IntStream.range(0,2000).mapToObj(i->new ItemAspectRule(List.of("*"),
                "test:material_"+i,List.of(new ItemAspectRule.Quantity("metallum",1)))).toList();
        var local=new ItemAspectResolver(rules,catalog,false,tc4revival.aspect.recipe.InferenceLimits.server(),
                (item,tag)->{calls.incrementAndGet();return tag.equals("test:material_19");});
        for(int i=0;i<20;i++){
            assertTrue(local.contains("test:ingot"));
            assertEquals(1,local.resolve("test:ingot",tag->true).orElseThrow().total());
            assertFalse(local.hasNbtRules("test:ingot"));
        }
        assertEquals(2000,calls.get());
    }

    @Test void predicateOnlyLookupsKeepIndependentTagContexts() {
        var wildcard=new ItemAspectRule(List.of("*"),"forge:ingots/test",List.of(new ItemAspectRule.Quantity("metallum",8)));
        var local=new ItemAspectResolver(List.of(wildcard),catalog);
        assertEquals(8,local.resolve("test:ingot",tag->true).orElseThrow().total());
        assertTrue(local.resolve("test:ingot",tag->false).isEmpty());
        assertEquals(8,local.resolve("test:ingot",tag->true).orElseThrow().total());
    }

    @Test void enumeratedMembershipPreservesRuleOrderAndAdditiveAmounts() {
        var rules=List.of(
                new ItemAspectRule(List.of("*"),"test:z",List.of(new ItemAspectRule.Quantity("metallum",2))),
                new ItemAspectRule(List.of("test:ingot"),null,List.of(new ItemAspectRule.Quantity("terra",1))),
                new ItemAspectRule(List.of("*"),"test:a",List.of(new ItemAspectRule.Quantity("ignis",3))),
                new ItemAspectRule(List.of("*"),"test:z",List.of(new ItemAspectRule.Quantity("metallum",4))));
        var members=new java.util.HashSet<>(List.of("test:z","test:a"));
        ItemTagLookup lookup=item->item.equals("test:ingot")?members:List.of();
        for(boolean additive:List.of(false,true)){
            var indexed=new ItemAspectResolver(rules,catalog,additive,tc4revival.aspect.recipe.InferenceLimits.server(),lookup);
            var baseline=new ItemAspectResolver(rules,catalog,additive,tc4revival.aspect.recipe.InferenceLimits.server(),
                    (item,tag)->lookup.test(item,tag));
            assertEquals(baseline.rules("test:ingot"),indexed.rules("test:ingot"));
            assertEquals(baseline.resolve("test:ingot",members::contains).orElseThrow().view(),
                    indexed.resolve("test:ingot",members::contains).orElseThrow().view());
            members.remove("test:a");indexed.clearTagCache();baseline.clearTagCache();
            assertEquals(baseline.rules("test:ingot"),indexed.rules("test:ingot"));
            members.add("test:a");
        }
    }

    @Test void largeSparseTagTableOnlyEnumeratesTheRequestedItems() {
        var rules=java.util.stream.IntStream.range(0,4000).mapToObj(i->new ItemAspectRule(List.of("*"),
                "test:material_"+i,List.of(new ItemAspectRule.Quantity("metallum",1)))).toList();
        var enumerated=new java.util.concurrent.atomic.AtomicInteger();
        ItemTagLookup lookup=new ItemTagLookup(){
            @Override public java.util.Collection<String> tags(String item){enumerated.incrementAndGet();return List.of("test:material_19");}
            @Override public boolean test(String item,String tag){fail("Candidate lookup probed every authored tag");return false;}
        };
        var local=new ItemAspectResolver(rules,catalog,false,tc4revival.aspect.recipe.InferenceLimits.server(),lookup);
        for(int i=0;i<2000;i++){
            assertTrue(local.contains("test:ingot_"+i));
            assertEquals(1,local.resolve("test:ingot_"+i,tag->true).orElseThrow().total());
        }
        assertEquals(2000,enumerated.get());
    }

    @Test void clientTagMembershipSnapshotDoesNotFollowLaterMutation() {
        var members=new java.util.HashSet<>(List.of("test:old"));
        var tags=new java.util.HashMap<String,java.util.Set<String>>();tags.put("test:metal",members);
        var snapshot=ItemTagLookup.snapshot(tags);members.clear();members.add("test:new");tags.clear();
        assertTrue(snapshot.test("test:old","test:metal"));
        assertFalse(snapshot.test("test:new","test:metal"));
        assertEquals(java.util.Set.of("test:metal"),snapshot.tags("test:old"));
    }

    @Test void ironUsesVanillaItemAndRequiresItsActualMaterialTag() {
        assertEquals(1, amount("iron_nugget", "metallum"));
        assertTrue(resolver.resolve("minecraft:iron_nugget", tag -> false).isEmpty());
        assertTrue(resolver.resolve("thaumcraft:iron_nugget", tag -> true).isEmpty());
        assertEquals(4, amount("iron_ingot", "metallum"));
    }

    @Test void wildcardMetadataIsExpandedOnlyToOldVariants() {
        assertEquals(value("coal").view(), value("charcoal").view());
        assertEquals(value("cod").view(), value("pufferfish").view());
        assertEquals(value("anvil").view(), value("damaged_anvil").view());
        assertEquals(68, value("chipped_anvil").total());
        assertEquals(1, amount("mossy_stone_bricks", "terra"));
        assertEquals(1, amount("cracked_stone_bricks", "perditio"));
        assertEquals(1, amount("chiseled_stone_bricks", "ordo"));
        assertEquals(4, amount("dark_oak_wood", "arbor"));
    }

    @Test void modernSharedTagsDoNotGrantAspectsToNewContent() {
        for (String item : List.of("deepslate_iron_ore", "deepslate_diamond_ore", "cherry_log", "mangrove_planks",
                "stripped_oak_log", "netherite_ingot", "raw_iron", "copper_block", "rabbit", "black_dye", "blue_dye",
                "white_dye", "brown_dye", "cornflower", "dragon_head", "music_disc_pigstep")) {
            assertTrue(resolver.resolve("minecraft:" + item, tag -> true).isEmpty(), item);
        }
        assertEquals(3, amount("copper_ingot", "metallum"));
        assertEquals(1, amount("deepslate_copper_ore", "permutatio"));
    }

    @Test void legacyDyeIngredientsRetainTheirOldIdentity() {
        assertEquals(16, LegacyItemDefinitions.legacyDyes().size());
        for (String item : List.of("ink_sac", "lapis_lazuli", "bone_meal", "cocoa_beans", "red_dye")) {
            assertEquals(1, amount(item, "sensus"));
        }
    }

    @Test void matchingDefinitionsMergeAndResultsAreIsolatedWithoutImplicitCaps() {
        var first = new ItemAspectRule(List.of("minecraft:stone"), null,
                List.of(new ItemAspectRule.Quantity("terra", 2)));
        var override = new ItemAspectRule(List.of("minecraft:stone"), "forge:stone",
                List.of(new ItemAspectRule.Quantity("ignis", 100), new ItemAspectRule.Quantity("aqua", 70)));
        var local = new ItemAspectResolver(List.of(first, override), catalog);
        var current = local.resolve("minecraft:stone", tag -> true).orElseThrow();
        assertEquals(172, current.total());
        current.add(catalog.require("ignis"), 20);
        assertEquals(172, local.resolve("minecraft:stone", tag -> true).orElseThrow().total());
        assertEquals(2, local.resolve("minecraft:stone", tag -> false).orElseThrow().total());
    }

    @Test void jsonRetainsSelectorsAmountsAndOrder() {
        var rules = LegacyItemDefinitions.rules();
        assertEquals(rules, ItemAspectJson.decode(JsonParser.parseString(ItemAspectJson.encode(rules).toString())));
        var uniqueItems = rules.stream().flatMap(rule -> rule.items().stream()).distinct().count();
        assertEquals(uniqueItems, resolver.itemCount());
        assertEquals(rules.size(), resolver.ruleCount());
    }

    @Test void malformedDefinitionsCannotReplaceWorkingTable() {
        var json = ItemAspectJson.encode(LegacyItemDefinitions.rules());
        json.getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("aspects").get(0).getAsJsonObject().addProperty("amount", 1.5);
        assertThrows(ArithmeticException.class, () -> ItemAspectJson.decode(json));
        assertThrows(IllegalArgumentException.class, () -> new ItemAspectRule(List.of("minecraft:stone", "minecraft:stone"), null, List.of()));
        assertTrue(new ItemAspectResolver(List.of(new ItemAspectRule(List.of("minecraft:stone"), null,
                List.of(new ItemAspectRule.Quantity("unknown", 1)))), catalog).resolve("minecraft:stone",tag->true).orElseThrow().isEmpty());
    }
}
