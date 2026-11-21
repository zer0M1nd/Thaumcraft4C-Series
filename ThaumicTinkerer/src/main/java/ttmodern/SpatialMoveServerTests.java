package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class SpatialMoveServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererOptionalAeSpatialMovementRetainsInventory(GameTestHelper test) throws Exception {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("ae2")){test.succeed();return;}
        var registry=Class.forName("appeng.api.movable.BlockEntityMoveStrategies");var api=Class.forName("appeng.api.movable.IBlockEntityMoveStrategy");
        var begin=api.getMethod("beginMove",BlockEntity.class);var complete=api.getMethod("completeMove",BlockEntity.class,BlockState.class,CompoundTag.class,Level.class,BlockPos.class);
        var world=test.getLevel();var from=test.absolutePos(new BlockPos(1,1,1));var to=from.east(2);
        for(var block:java.util.List.of(Tinkerer.RESTORER.get(),Tinkerer.ASPECT_ANALYZER.get())){
            world.setBlockAndUpdate(from,block.defaultBlockState());var original=world.getBlockEntity(from);var stack=new ItemStack(Items.IRON_PICKAXE);stack.setDamageValue(53);stack.setHoverName(net.minecraft.network.chat.Component.literal("Spatial test"));
            if(original instanceof RestorerEntity restorer)restorer.inventory.setStackInSlot(0,stack);else ((AspectAnalyzerEntity)original).items.setStackInSlot(0,stack);
            var strategy=registry.getMethod("get",BlockEntity.class).invoke(null,original);var state=world.getBlockState(from);var saved=(CompoundTag)begin.invoke(strategy,original);test.assertTrue(saved!=null,"AE2 permits moving old movable machines");
            world.removeBlockEntity(from);world.removeBlock(from,false);world.setBlockAndUpdate(to,state);
            test.assertTrue((Boolean)complete.invoke(strategy,original,state,saved,world,to),"AE2 completes block entity reconstruction");
            var moved=world.getBlockEntity(to);var actual=moved instanceof RestorerEntity restorer?restorer.inventory.getStackInSlot(0):((AspectAnalyzerEntity)moved).items.getStackInSlot(0);
            test.assertTrue(moved!=original&&moved.getBlockPos().equals(to)&&ItemStack.matches(actual,stack),"Move retains inventory, damage and custom name at new location");world.removeBlock(to,false);
        }
        test.succeed();
    }
}
