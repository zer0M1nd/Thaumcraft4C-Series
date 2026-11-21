package tc4revival.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class MultiblockLootServerTests {
    @GameTest(template="wand_test_empty")
    public static void multiblockConstituentLootTables(GameTestHelper test) {
        var ordinary=new ItemStack(Items.DIAMOND_PICKAXE);
        var fortune=ordinary.copy();fortune.enchant(Enchantments.BLOCK_FORTUNE,3);
        var silk=ordinary.copy();silk.enchant(Enchantments.SILK_TOUCH,1);
        var alchemy=new Item[]{TCItems.ALCHEMY_FURNACE.get(),TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get(),
                TCItems.ALEMBIC.get(),TCItems.ALCHEMICAL_CONSTRUCT.get(),TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get()};
        var infernal=new Item[]{Items.AIR,Items.NETHER_BRICKS,Items.OBSIDIAN,Items.NETHER_BRICKS,Items.OBSIDIAN,
                Items.OBSIDIAN,Items.OBSIDIAN,Items.NETHER_BRICKS,Items.OBSIDIAN,Items.NETHER_BRICKS,Items.IRON_BARS};
        int states=0;
        for(var block:List.of(TCArcane.ADVANCED_ALCHEMY_FURNACE.get(),TCArcane.INFERNAL_FURNACE.get(),TCArcane.THAUMATORIUM.get())) {
            for(var state:block.getStateDefinition().getPossibleStates()) {
                Item expected=block instanceof AdvancedAlchemyBlock?alchemy[state.getValue(AdvancedAlchemyBlock.PART)]
                        :block instanceof InfernalFurnaceBlock?infernal[state.getValue(InfernalFurnaceBlock.PART)]:TCItems.ALCHEMICAL_CONSTRUCT.get();
                for(var tool:List.of(ItemStack.EMPTY,ordinary,fortune,silk)) {
                    var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
                    var table=level.getServer().getLootData().getLootTable(block.getLootTable());
                    var params=new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE,state)
                            .withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL,tool)
                            .create(LootContextParamSets.BLOCK);
                    assertDrop(test,table.getRandomItems(params),expected,"Loaded table "+state);
                    assertDrop(test,Block.getDrops(state,level,pos,null,null,tool),expected,"Block loot entry "+state);
                }
                states++;
            }
        }
        test.assertTrue(states==149,"All five alchemy parts, 132 infernal part/layer/facing combinations, and 12 thaumatorium states checked");
        tc4revival.TC4Revival.LOGGER.info("multiblockConstituentLootTables PASS: {} states, four tools, loaded tables and block drops",states);
        test.succeed();
    }
    private static void assertDrop(GameTestHelper test,List<ItemStack> drops,Item expected,String label) {
        test.assertTrue(expected==Items.AIR?drops.isEmpty():drops.size()==1&&drops.get(0).is(expected)&&drops.get(0).getCount()==1&&!drops.get(0).hasTag(),label);
    }
}
