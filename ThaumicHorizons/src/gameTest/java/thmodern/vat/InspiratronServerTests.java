package thmodern.vat;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
import thmodern.soul.*;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class InspiratronServerTests {
    @GameTest(template="empty")
    public static void paperMemoryOutputAndAutomationAreConserved(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));level.setBlock(pos,Horizons.INSPIRATRON.get().defaultBlockState(),2);
        var device=(InspiratronEntity)level.getBlockEntity(pos);
        test.assertTrue(device.acceptSoulBits(100)==0,"No paper means no soul consumption");
        var items=device.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).resolve().orElseThrow();
        test.assertTrue(items.insertItem(0,new ItemStack(Items.DIRT),false).is(Items.DIRT),"Input rejects non-paper");items.insertItem(0,new ItemStack(Items.PAPER,2),false);
        test.assertTrue(items.extractItem(0,1,false).isEmpty()&&!items.insertItem(1,new ItemStack(InspiratronEntity.fragment()),false).isEmpty(),"Automation cannot remove input or insert output");
        var saved=device.saveWithoutMetadata();saved.putInt("Progress",99);device.load(saved);
        test.assertTrue(device.soulRoom()==101,"A second paper permits full boosted batches near a page boundary");
        long seed=0;while(net.minecraft.util.RandomSource.create(seed).nextDouble()<.97)seed++;
        level.random.setSeed(seed);test.assertTrue(device.acceptSoulBits(1)==1,"One successful memory is accepted");
        test.assertTrue(device.progress()==0&&device.inventory.getStackInSlot(0).getCount()==1&&device.inventory.getStackInSlot(1).getCount()==1,"Exactly one page consumes one paper and produces one fragment");
        var restored=new InspiratronEntity(pos,device.getBlockState());restored.setLevel(level);restored.load(device.saveWithoutMetadata());
        test.assertTrue(restored.inventory.getStackInSlot(1).is(InspiratronEntity.fragment())&&restored.inventory.getStackInSlot(0).getCount()==1,"Both inventories survive reload");
        test.assertTrue(items.extractItem(1,1,false).is(InspiratronEntity.fragment()),"Output may be extracted through automation");
        device.inventory.setStackInSlot(1,new ItemStack(InspiratronEntity.fragment(),64));test.assertTrue(device.acceptSoulBits(1)==0,"Full output refuses further consumption");
        test.succeed();
    }
}
