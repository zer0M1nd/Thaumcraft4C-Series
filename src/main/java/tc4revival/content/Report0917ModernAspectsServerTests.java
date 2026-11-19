package tc4revival.content;

import java.util.*;
import com.google.gson.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.item.*;
import tc4revival.aspect.entity.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class Report0917ModernAspectsServerTests {
    private static JsonElement data(String kind,String name) {
        try(var in=Report0917ModernAspectsServerTests.class.getResourceAsStream("/data/tc4ai_aspects/"+kind+"/"+name+".json");
            var reader=new java.io.InputStreamReader(java.util.Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch(java.io.IOException e) {throw new IllegalStateException(e);}
    }
    @GameTest(template="wand_test_empty") public static void modernVanillaAspectTablesResolveAllEntries(GameTestHelper t) {
        var rows=ItemAspectJson.decode(data("item_aspects","ai_generated"));
        t.assertTrue(rows.isEmpty(),"Old AI file is retired after materialization");
        var expected=new ItemAspectResolver(rows,TCAspects.catalog());
        for(var row:rows) for(var id:row.items()) {
            t.assertTrue(ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse(id)),"Registered item "+id);
            var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id)));
            // A raw potion item without Potion NBT is not a valid vanilla potion stack.
            if(stack.getItem() instanceof net.minecraft.world.item.PotionItem) net.minecraft.world.item.alchemy.PotionUtils.setPotion(stack,net.minecraft.world.item.alchemy.Potions.WATER);
            t.assertTrue(!ItemAspects.direct(stack).orElseThrow().view().isEmpty() && !expected.resolve(id,tag->true).orElseThrow().view().isEmpty(),"Imported quantities available (addons may override) "+id);
            t.assertTrue(ItemStackAspects.resolve(stack).isPresent() && ItemStackAspects.scanHash(stack).isPresent(),"Real item scan resolves "+id);
        }
        var blocks=BlockScanMapping.decode(data("block_scan_targets","ai_modern"));BlockScanTargets.validate(blocks);
        t.assertTrue(blocks.rules().size()==549,"All real block rows retained; both item frames are entities");
        for(var rule:blocks.rules()) {
            var block=ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(rule.block()));
            for(var state:block.getStateDefinition().getPossibleStates())
                t.assertTrue(BlockScanTargets.target(state).orElseThrow().equals(rule.target()),"State scan target "+state);
            var actual=tc4revival.research.ScanTargetResolver.blockState(block.defaultBlockState()).orElseThrow();
            t.assertTrue(actual.identity().isPresent() && actual.aspects().view().equals(BlockAspects.resolve(block.defaultBlockState()).orElseGet(()->BlockScanTargets.aspects(rule.target())).view()),"Current explicit block values, including intentionally empty definitions, resolve "+rule.block());
        }
        var entities=EntityAspectJson.decode(data("entity_aspects","ai_generated"));
        t.assertTrue(entities.size()==43,"All supplied entity rows retained");
        for(var row:entities) {
            var entity=ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(row.entity())).create(t.getLevel());
            t.assertTrue(entity!=null && !EntityAspects.resolve(entity).orElseThrow().aspects().view().isEmpty(),"Entity quantities "+row.entity());
        }
        t.succeed();
    }
    @GameTest(template="wand_test_empty") public static void originalItemAspectsTakePrecedenceOverImportedValues(GameTestHelper t) {
        var chest=new ItemStack(Blocks.CHEST);
        t.assertTrue(ItemAspects.directResolver().contains("minecraft:chest"),"Complete chest definition installed");
        var aspects=ItemStackAspects.resolve(chest).orElseThrow();
        t.assertTrue(aspects.view().equals(Map.of(TCAspects.catalog().require("arbor"),6,TCAspects.catalog().require("vacuos"),4)),"Chest restores original 6 arbor and 4 vacuos");
        var imported=new ItemAspectResolver(ItemAspectJson.decode(data("item_aspects","ai_generated")),TCAspects.catalog());
        for(var id:List.of("minecraft:bow","minecraft:diamond_sword","minecraft:fire_charge","minecraft:lead","minecraft:black_bed"))
            t.assertTrue(!imported.contains(id),"Original base, bonus and addon definitions protected: "+id);
        t.assertTrue(ItemAspects.directResolver().contains("minecraft:cherry_log"),"Previously undefined modern item retained");
        t.succeed();
    }
    @GameTest(template="wand_test_empty") public static void invalidScanRulesCannotDiscardValidModernBlocks(GameTestHelper t) {
        var good=new BlockScanMapping.Rule("minecraft:cherry_log",Map.of("axis","x"),new BlockScanMapping.Target("minecraft:cherry_log",null));
        var invalid=List.of(new BlockScanMapping.Rule("minecraft:missing_test_block",Map.of(),good.target()),
            new BlockScanMapping.Rule("minecraft:cherry_log",Map.of("axis","invalid"),good.target()),
            new BlockScanMapping.Rule("minecraft:cherry_planks",Map.of(),new BlockScanMapping.Target("minecraft:missing_test_item",null)));
        var files=new LinkedHashMap<ResourceLocation,JsonElement>();
        files.put(ResourceLocation.parse("validation:valid"),BlockScanMapping.encode(List.of(good)));
        for(int i=0;i<invalid.size();i++) {
            var rule=invalid.get(i);boolean rejected=false;
            try{BlockScanTargets.validate(new BlockScanMapping(List.of(rule)));}catch(IllegalArgumentException expected){rejected=true;}
            t.assertTrue(rejected,"Strict validation still rejects invalid rule");
            files.put(ResourceLocation.parse("validation:invalid_"+i),BlockScanMapping.encode(List.of(rule)));
        }
        var loaded=BlockScanTargets.validatedMapping(files);
        t.assertTrue(loaded.rules().equals(List.of(good)),"Invalid references skipped without dropping valid entries");
        t.assertTrue(loaded.resolve("minecraft:cherry_log",Map.of("axis","x")).isPresent(),"Valid property matching retained");
        t.assertTrue(loaded.resolve("minecraft:cherry_log",Map.of("axis","y")).isEmpty(),"Property constraints not bypassed");
        t.succeed();
    }
}
