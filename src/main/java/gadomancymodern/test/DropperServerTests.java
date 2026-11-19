package gadomancymodern.test;
import gadomancymodern.Gadomancy;
import gadomancymodern.automation.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;
@GameTestHolder("gadomancy") @PrefixGameTestTemplate(false)
public class DropperServerTests {
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void inventoryViewSimulationAndExtraction(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(3,4,3));test.getLevel().setBlockAndUpdate(pos,Gadomancy.DROPPER.get().defaultBlockState().setValue(ArcaneDropperBlock.FACING,Direction.DOWN));
        var dropper=(ArcaneDropperBlockEntity)test.getLevel().getBlockEntity(pos);Vec3 center=Vec3.atCenterOf(pos.below(2));
        ItemStack named=new ItemStack(Items.DIAMOND,9);named.setHoverName(net.minecraft.network.chat.Component.literal("payload"));
        var entity=new ItemEntity(test.getLevel(),center.x,center.y,center.z,named);entity.setNoPickUpDelay();test.getLevel().addFreshEntity(entity);
        test.assertTrue(dropper.getSlots()==11,"One entity plus ten insertion slots");
        test.assertTrue(dropper.extractItem(0,4,true).getCount()==4&&entity.getItem().getCount()==9,"Simulating extraction is pure");
        ItemStack removed=dropper.extractItem(0,4,false);test.assertTrue(removed.getCount()==4&&entity.getItem().getCount()==5&&ItemStack.isSameItemSameTags(removed,named),"Commit exact count and retain NBT");
        ItemStack copy=dropper.getStackInSlot(0);copy.setCount(60);test.assertTrue(entity.getItem().getCount()==5,"Callers cannot mutate world stack by alias");
        test.assertTrue(dropper.extractItem(0,64,false).getCount()==5&&!entity.isAlive(),"Last extraction discards entity");
        test.assertTrue(dropper.extractItem(0,64,false).isEmpty(),"No repeated extraction of discarded entity");test.succeed();
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void insertionSpawnsOnceAndRestrictsSides(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(3,4,3));test.getLevel().setBlockAndUpdate(pos,Gadomancy.DROPPER.get().defaultBlockState().setValue(ArcaneDropperBlock.FACING,Direction.EAST));
        var dropper=(ArcaneDropperBlockEntity)test.getLevel().getBlockEntity(pos);
        int before=test.getLevel().getEntitiesOfClass(ItemEntity.class,dropper.area().inflate(1)).size();
        test.assertTrue(dropper.insertItem(0,new ItemStack(Items.GOLD_INGOT,12),true).isEmpty(),"Simulated insert accepted");
        test.assertTrue(test.getLevel().getEntitiesOfClass(ItemEntity.class,dropper.area().inflate(1)).size()==before,"Simulation creates no item");
        test.assertTrue(dropper.insertItem(0,new ItemStack(Items.GOLD_INGOT,12),false).isEmpty(),"Insert committed");
        test.assertTrue(test.getLevel().getEntitiesOfClass(ItemEntity.class,dropper.area().inflate(1)).size()==before+1,"Exactly one item entity spawned");
        test.assertTrue(dropper.extractItem(0,64,false).isEmpty(),"Three-tick pickup delay prevents instant re-extraction");
        test.assertTrue(!dropper.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.EAST).isPresent(),"Output opening is not an automation socket");
        test.assertTrue(dropper.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.WEST).isPresent(),"Back flat face accepts automation");test.succeed();
    }
}
