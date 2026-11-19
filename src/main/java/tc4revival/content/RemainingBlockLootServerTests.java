package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class RemainingBlockLootServerTests {
    @GameTest(template="wand_test_empty")
    public static void doorConfigurationAndContainerLootTables(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var setting=tc4revival.config.TCConfig.SERVER.wardedStone;boolean old=setting.get();
        try {
            for(boolean warded:new boolean[]{false,true}) {
                setting.set(warded);
                for(var state:TCArcane.ARCANE_DOOR.get().getStateDefinition().getPossibleStates()) {
                    boolean allowed=!warded&&state.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER;
                    var expected=allowed?List.of(new ItemStack(TCItems.ARCANE_DOOR.get())):List.<ItemStack>of();
                    same(test,raw(test,state,ItemStack.EMPTY),expected,"Door table config and half");
                    same(test,Block.getDrops(state,level,pos,null),expected,"Actual door loot config and half");
                }
            }
        } finally {setting.set(old);}
        var fortune=new ItemStack(Items.DIAMOND_PICKAXE);fortune.enchant(Enchantments.BLOCK_FORTUNE,3);
        var silk=new ItemStack(Items.DIAMOND_PICKAXE);silk.enchant(Enchantments.SILK_TOUCH,1);
        for(boolean crate:new boolean[]{false,true})for(int rarity=0;rarity<3;rarity++) {
            var state=TCEldritch.loot(crate,rarity).defaultBlockState();
            for(var tool:List.of(ItemStack.EMPTY,fortune,silk))for(int attempt=0;attempt<32;attempt++) {
                var drops=raw(test,state,tool);
                test.assertTrue(drops.size()>=1+rarity&&drops.size()<=3+rarity,"Container roll count obeys tier independently of tool");
                test.assertTrue(drops.stream().noneMatch(ItemStack::isEmpty),"Referenced native bag tables produce real loot");
            }
        }
        test.succeed();
    }
    private static List<ItemStack> raw(GameTestHelper test,BlockState state,ItemStack tool) {
        var params=new LootParams.Builder(test.getLevel()).withParameter(LootContextParams.BLOCK_STATE,state)
                .withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(test.absolutePos(new BlockPos(3,3,3))))
                .withParameter(LootContextParams.TOOL,tool).create(LootContextParamSets.BLOCK);
        return test.getLevel().getServer().getLootData().getLootTable(state.getBlock().getLootTable()).getRandomItems(params);
    }
    private static void same(GameTestHelper test,List<ItemStack> actual,List<ItemStack> expected,String label) {
        test.assertTrue(actual.size()==expected.size(),label+" count "+actual+" expected "+expected);
        for(int i=0;i<actual.size();i++)test.assertTrue(ItemStack.isSameItemSameTags(actual.get(i),expected.get(i))&&actual.get(i).getCount()==expected.get(i).getCount(),label+" entry "+i);
    }
}
