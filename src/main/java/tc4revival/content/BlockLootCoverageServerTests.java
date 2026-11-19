package tc4revival.content;

import java.util.ArrayList;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Registry-level coverage catches dynamic registrations omitted by a provider's own known-block list. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class BlockLootCoverageServerTests {
    @GameTest(template="wand_test_empty")
    public static void everyRegisteredBlockHasItsDeclaredLootResource(GameTestHelper test) {
        var server=test.getLevel().getServer();var missing=new ArrayList<String>();int blocks=0,tables=0,noLoot=0;
        for(var block:ForgeRegistries.BLOCKS) {
            var id=ForgeRegistries.BLOCKS.getKey(block);if(id==null||!id.getNamespace().equals("thaumcraft"))continue;
            blocks++;var table=block.getLootTable();
            if(table.equals(BuiltInLootTables.EMPTY)) {noLoot++;continue;}
            tables++;var resource=ResourceLocation.fromNamespaceAndPath(table.getNamespace(),"loot_tables/"+table.getPath()+".json");
            if(server.getResourceManager().getResource(resource).isEmpty())missing.add(id+" -> missing "+resource);
            else if(server.getLootData().getLootTable(table)==LootTable.EMPTY)missing.add(id+" -> table did not load "+table);
        }
        test.assertTrue(TCCleansing.PURE_WATER.get().getLootTable().equals(BuiltInLootTables.EMPTY),"Purifying water has no item loot; collection uses its bucket entry point");
        test.assertTrue(blocks>100,"The complete mod block registry was loaded");
        test.assertTrue(missing.isEmpty(),"Missing declared block loot resources: "+missing);
        tc4revival.TC4Revival.LOGGER.info("everyRegisteredBlockHasItsDeclaredLootResource PASS: {} blocks, {} declared tables, {} explicit no-loot blocks",blocks,tables,noLoot);
        test.succeed();
    }
}
