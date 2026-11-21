package tc4revival.content;

import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class MaterialClassificationServerTests {
    @GameTest(template="wand_test_empty")
    public static void materialClassificationUsesLoadedTagsWithoutMergingMetals(GameTestHelper test) {
        var block=TCArcane.THAUMIUM_BLOCK.get();
        for(String tag:java.util.List.of("forge:storage_blocks","forge:storage_blocks/thaumium")) {
            test.assertTrue(block.defaultBlockState().is(BlockTags.create(ResourceLocation.parse(tag))),"Actual block classification "+tag);
            test.assertTrue(new ItemStack(block).is(ItemTags.create(ResourceLocation.parse(tag))),"Actual item classification "+tag);
        }
        test.assertTrue(!new ItemStack(Items.IRON_BLOCK).is(ItemTags.create(ResourceLocation.parse("forge:storage_blocks/thaumium"))),"Iron cannot replace thaumium through broad classification");
        test.assertTrue(!new ItemStack(block).is(ItemTags.create(ResourceLocation.parse("forge:ingots/thaumium"))),"Storage block is not an ingot");
        test.assertTrue(new ItemStack(Items.IRON_NUGGET).is(ItemTags.create(ResourceLocation.parse("forge:nuggets/iron"))) && !ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse("thaumcraft:iron_nugget")),"Iron nugget is the tagged vanilla identity only");
        for(var type:ShardType.values()) for(var other:ShardType.values()) if(other!=ShardType.BALANCED)
            test.assertTrue(new ItemStack(TCItems.shard(type)).is(ItemTags.create(ResourceLocation.parse("forge:shards/"+other.material())))==(type==other),"Six primal shard categories remain mutually exclusive and exclude balanced");
        for(var type:ClusterType.values()) {
            var stack=new ItemStack(TCItems.cluster(type));
            test.assertTrue(stack.is(ItemTags.create(ResourceLocation.parse(type.tag()))) && stack.is(ItemTags.create(ResourceLocation.parse("forge:clusters"))),"Cluster leaf and parent membership");
            test.assertTrue(!stack.is(ItemTags.create(ResourceLocation.parse("forge:ores"))) && !stack.is(ItemTags.create(ResourceLocation.parse("forge:raw_materials"))),"Clusters do not enter ores or raw materials");
        }
        tc4revival.TC4Revival.LOGGER.info("PASS materialClassificationUsesLoadedTagsWithoutMergingMetals: storage block/item parents, vanilla iron nugget, 42 shard comparisons and four clusters");test.succeed();
    }
}
