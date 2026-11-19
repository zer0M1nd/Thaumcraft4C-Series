package thmodern.vat;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
import thmodern.soul.*;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class SoulExtractorServerTests {
    @GameTest(template="empty")
    public static void oneSandTransfersExactly1200BitsAndProducesOneSand(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));level.setBlock(pos,Horizons.SOUL_EXTRACTOR.get().defaultBlockState(),2);level.setBlock(pos.above(),Horizons.SOULFORGE.get().defaultBlockState(),2);
        var extractor=(SoulExtractorEntity)level.getBlockEntity(pos);var forge=(SoulforgeEntity)level.getBlockEntity(pos.above());
        var handler=extractor.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,Direction.NORTH).resolve().orElseThrow();
        test.assertTrue(handler.insertItem(0,new ItemStack(Items.DIRT),false).is(Items.DIRT),"Only soul sand enters through automation");handler.insertItem(0,new ItemStack(Items.SOUL_SAND),false);test.assertTrue(handler.extractItem(0,1,false).isEmpty(),"Automation cannot extract the input");
        extractor.tick();test.assertTrue(extractor.remaining()==0&&extractor.input.getStackInSlot(0).is(Items.SOUL_SAND)&&extractor.input.getStackInSlot(0).getCount()==1&&!extractor.working(),"An unpowered receiver leaves the soul sand unconsumed");
        var mind=tc4revival.aspect.TCAspects.catalog().require("cognitio");forge.addEssentia(mind,1,Direction.UP);forge.addEssentia(mind,1,Direction.UP);
        extractor.tick();test.assertTrue(extractor.remaining()==1199&&extractor.input.getStackInSlot(0).isEmpty()&&forge.progress()==1&&forge.energy()==1999,"A ready receiver reserves one sand and pays for the first bit");
        extractor.load(extractor.saveWithoutMetadata());test.assertTrue(extractor.remaining()==1199&&extractor.input.getStackInSlot(0).isEmpty(),"Reserved work survives reload without a second input debit");
        for(int i=0;i<1199;i++)extractor.tick();
        test.assertTrue(extractor.remaining()==0&&forge.progress()==1200&&forge.energy()==800,"Unboosted production transfers exactly the paid 1200 bits");
        var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1));test.assertTrue(drops.stream().filter(e->e.getItem().is(Items.SAND)).mapToInt(e->e.getItem().getCount()).sum()==1,"Missing output inventory drops one sand, not zero or multiple");test.succeed();
    }
}
