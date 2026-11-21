package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class AspectAnalyzerServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererAnalyzerCountsOneSampleAndPreservesInventory(GameTestHelper test){
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,1,2));world.setBlockAndUpdate(pos,Tinkerer.ASPECT_ANALYZER.get().defaultBlockState());var analyzer=(AspectAnalyzerEntity)world.getBlockEntity(pos);test.assertTrue(analyzer.aspects().isEmpty(),"Empty analyzer returns an empty table");
        var stack=new ItemStack(Items.DIAMOND,27);stack.setHoverName(net.minecraft.network.chat.Component.literal("Retained sample"));analyzer.items.setStackInSlot(0,stack);var expected=thaumcraft.api.ThaumcraftApiHelper.getObjectAspects(stack);var observed=analyzer.aspects();test.assertTrue(!observed.isEmpty(),"Sample uses actual base item aspects");for(var aspect:expected.getAspects())test.assertTrue(observed.get(aspect.getTag())==expected.getAmount(aspect),"Result is per item, not multiplied by stack count");test.assertTrue(stack.getCount()==27,"Reading never consumes samples");
        var restored=new AspectAnalyzerEntity(pos,analyzer.getBlockState());restored.load(analyzer.saveWithoutMetadata());test.assertTrue(restored.items.getStackInSlot(0).getCount()==27&&restored.items.getStackInSlot(0).getHoverName().getString().equals("Retained sample"),"Count and complete NBT survive save/load");
        var cap=analyzer.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);test.assertTrue(cap.isPresent(),"Automation is available");analyzer.invalidateCaps();test.assertTrue(!cap.isPresent(),"Capability and peripheral lifetimes end when removed");analyzer.reviveCaps();world.removeBlock(pos,false);var drops=world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2),entity->entity.getItem().is(Items.DIAMOND));test.assertTrue(drops.stream().mapToInt(entity->entity.getItem().getCount()).sum()==27&&drops.stream().allMatch(entity->entity.getItem().getHoverName().getString().equals("Retained sample")),"Breaking preserves stored sample metadata and amount");drops.forEach(net.minecraft.world.entity.Entity::discard);test.succeed();
    }
}
