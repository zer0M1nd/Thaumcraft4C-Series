package thmodern.vat;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class InfusedChickenServerTests {
    @GameTest(template="empty")
    public static void scholarProducesOnlyInkEggAndFeatherAndSavesSchedules(GameTestHelper test){
        var chicken=Horizons.SCHOLAR_CHICKEN.get().create(test.getLevel());chicken.moveTo(test.absolutePos(new BlockPos(3,3,3)).getCenter());
        var tag=new CompoundTag();chicken.save(tag);tag.putInt("FeatherTime",1);chicken.load(tag);chicken.eggTime=1;
        chicken.aiStep();var drops=test.getLevel().getEntitiesOfClass(ItemEntity.class,chicken.getBoundingBox().inflate(2));
        test.assertTrue(drops.stream().filter(e->e.getItem().is(Horizons.INK_EGG.get())).mapToInt(e->e.getItem().getCount()).sum()==1,"One ink egg is laid");
        test.assertTrue(drops.stream().filter(e->e.getItem().is(Items.FEATHER)).mapToInt(e->e.getItem().getCount()).sum()==1&&!drops.stream().anyMatch(e->e.getItem().is(Items.EGG)),"One feather and no extra vanilla egg");
        var saved=new CompoundTag();chicken.save(saved);test.assertTrue(chicken.eggTime>=6000&&chicken.eggTime<12000&&saved.getInt("FeatherTime")>=4000&&saved.getInt("FeatherTime")<8000,"Legacy random production ranges restored after laying");
        var restored=Horizons.SCHOLAR_CHICKEN.get().create(test.getLevel());restored.load(saved);var again=new CompoundTag();restored.save(again);
        test.assertTrue(restored.eggTime==chicken.eggTime&&again.getInt("FeatherTime")==saved.getInt("FeatherTime"),"Both timers persist across save/load");drops.forEach(ItemEntity::discard);test.succeed();
    }
    @GameTest(template="empty")
    public static void goldenProductionWaitsForAdulthood(GameTestHelper test){
        var chicken=Horizons.GOLD_CHICKEN.get().create(test.getLevel());chicken.moveTo(test.absolutePos(new BlockPos(3,3,3)).getCenter());chicken.setAge(-1000);chicken.eggTime=1;chicken.aiStep();
        test.assertTrue(chicken.eggTime==1,"Baby does not consume its laying countdown");chicken.setAge(0);chicken.aiStep();
        var drops=test.getLevel().getEntitiesOfClass(ItemEntity.class,chicken.getBoundingBox().inflate(2));
        test.assertTrue(drops.stream().filter(e->e.getItem().is(Horizons.GOLD_EGG.get())).mapToInt(e->e.getItem().getCount()).sum()==1&&!drops.stream().anyMatch(e->e.getItem().is(Items.EGG)),"Adult lays one golden egg without a vanilla duplicate");
        test.assertTrue(chicken.eggTime>=8000&&chicken.eggTime<16000,"Golden laying range is 8000..15999 ticks");drops.forEach(ItemEntity::discard);test.succeed();
    }
}
