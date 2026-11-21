package tc4revival.content;

import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Remaining ConfigItems.registerOre rows; shards/clusters/vanilla iron have separate passed evidence. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class LegacyOreRegistrationServerTests {
    private static final String ROWS="""
        oreCinnabar cinnabar_ore forge:ores/cinnabar block
        oreInfusedAir air_infused_stone forge:ores/infused_air block
        oreInfusedFire fire_infused_stone forge:ores/infused_fire block
        oreInfusedWater water_infused_stone forge:ores/infused_water block
        oreInfusedEarth earth_infused_stone forge:ores/infused_earth block
        oreInfusedOrder order_infused_stone forge:ores/infused_order block
        oreInfusedEntropy entropy_infused_stone forge:ores/infused_entropy block
        oreAmber amber_ore forge:ores/amber block
        quicksilver quicksilver forge:quicksilver item
        gemAmber amber forge:gems/amber item
        nuggetCopper copper_nugget forge:nuggets/copper item
        nuggetThaumium thaumium_nugget forge:nuggets/thaumium item
        nuggetVoid void_nugget forge:nuggets/void item
        nuggetQuicksilver quicksilver_drop forge:nuggets/quicksilver item
        nuggetGold gold_coin forge:nuggets/gold item
        ingotThaumium thaumium_ingot forge:ingots/thaumium item
        ingotVoid void_ingot forge:ingots/void item
        logWood greatwood_log minecraft:logs block
        logWood silverwood_log minecraft:logs block
        plankWood greatwood_planks minecraft:planks block
        plankWood silverwood_planks minecraft:planks block
        slabWood greatwood_slab minecraft:wooden_slabs block
        slabWood silverwood_slab minecraft:wooden_slabs block
        treeSapling greatwood_sapling minecraft:saplings block
        treeSapling silverwood_sapling minecraft:saplings block
        """;
    @GameTest(template="wand_test_empty")
    public static void remainingLegacyOreRegistrationsMatchLoadedItemAndBlockTags(GameTestHelper test) {
        int count=0;
        for(var line:ROWS.strip().split("\\R")) {
            var row=line.strip().split(" ");var id=ResourceLocation.parse("thaumcraft:"+row[1]);var tag=ResourceLocation.parse(row[2]);
            test.assertTrue(ForgeRegistries.ITEMS.containsKey(id),"Registered old material: "+row[0]);
            test.assertTrue(new ItemStack(ForgeRegistries.ITEMS.getValue(id)).is(ItemTags.create(tag)),"Loaded item membership: "+line);
            if(row[3].equals("block")) {
                test.assertTrue(ForgeRegistries.BLOCKS.containsKey(id),"Registered material block: "+row[0]);
                test.assertTrue(ForgeRegistries.BLOCKS.getValue(id).defaultBlockState().is(BlockTags.create(tag)),"Loaded block membership: "+line);
            }
            count++;
        }
        test.assertTrue(count==25,"Exactly the remaining 25 retained registration rows");
        for(String metal:java.util.List.of("tin","silver","lead")) for(String family:java.util.List.of("nugget","cluster")) {
            test.assertTrue(!ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse("thaumcraft:"+metal+"_"+family)),"Excluded integration item "+metal+" "+family);
            var tag=ItemTags.create(ResourceLocation.parse("forge:"+family+"s/"+metal));
            test.assertTrue(ForgeRegistries.ITEMS.getValues().stream().filter(item->ForgeRegistries.ITEMS.getKey(item).getNamespace().equals("thaumcraft")).noneMatch(item->new ItemStack(item).is(tag)),"No base item leaks into excluded metal tag");
        }
        var small=new ItemStack(TCItems.nugget(NuggetType.QUICKSILVER));
        test.assertTrue(!small.is(ItemTags.create(ResourceLocation.parse("forge:quicksilver"))) && small.is(ItemTags.create(ResourceLocation.parse("forge:nuggets/quicksilver"))),"One-ninth drop uses the nugget tag only");
        test.assertTrue(!new ItemStack(net.minecraft.world.item.Items.STONE_SLAB).is(ItemTags.WOODEN_SLABS),"Stone slab not wood");
        tc4revival.TC4Revival.LOGGER.info("PASS remainingLegacyOreRegistrationsMatchLoadedItemAndBlockTags: 25 remaining rows, 16 block memberships, six exclusions and quicksilver boundary");test.succeed();
    }
}
