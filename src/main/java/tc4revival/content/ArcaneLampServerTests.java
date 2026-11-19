package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class ArcaneLampServerTests {
    @GameTest(template="wand_test_empty")
    public static void lampFuelUsesFacingReserveAndLegacySuction(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,3,2));
        level.setBlock(pos.below(),TCArcane.ESSENTIA_JAR.get().defaultBlockState(),3);
        var jar=(EssentiaJarBlockEntity)level.getBlockEntity(pos.below()); jar.addToContainer(TCAspects.get(LegacyAspect.PLANT),4);
        level.setBlock(pos,TCArcane.GROWTH_LAMP.get().defaultBlockState(),3);
        var lamp=(ArcaneLampBlockEntity)level.getBlockEntity(pos);
        for (int i=0;i<7;i++) lamp.fuel().tick();
        test.assertTrue(lamp.fuel().charges()==100 && lamp.fuel().reserve() && jar.amount()==2,"Two herba points fill one hundred updates and one reserve through the bottom port");
        test.assertTrue(lamp.suction(Direction.DOWN)==0 && lamp.suction(Direction.UP)==0 && !lamp.canOutput(Direction.DOWN),"Full growth lamp has no suction and cannot output");
        lamp.fuel().consume(100); lamp.fuel().tick();
        test.assertTrue(lamp.fuel().charges()==100 && !lamp.fuel().reserve(),"Reserve replenishes the next hundred updates without consuming another source point");
        var saved=lamp.saveWithoutMetadata(); var restored=new ArcaneLampBlockEntity(pos,lamp.getBlockState()); restored.load(saved);
        test.assertTrue(restored.fuel().charges()==100 && !restored.fuel().reserve(),"Fuel survives block entity storage");
        jar.empty(); jar.addToContainer(TCAspects.get(LegacyAspect.LIFE),8);
        level.setBlock(pos,TCArcane.FERTILITY_LAMP.get().defaultBlockState(),3); lamp=(ArcaneLampBlockEntity)level.getBlockEntity(pos);
        for (int i=0;i<25;i++) lamp.fuel().tick();
        test.assertTrue(lamp.fuel().charges()==4 && jar.amount()==4 && lamp.suction(Direction.DOWN)==88,"Fertility stores four life points with the original declining suction");
        pass(test,"fuel-and-persistence");
    }
    @GameTest(template="wand_test_empty")
    public static void growthCandidatesAndCompleteScanRemainSafe(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,4,2)); var plant=pos.east(2);
        level.setBlock(plant.below(),Blocks.FARMLAND.defaultBlockState(),3); level.setBlock(plant,Blocks.WHEAT.defaultBlockState(),2);
        test.assertTrue(LampPlantGrowth.candidate(level,plant),"Young old crops accept growth updates");
        level.setBlock(plant,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7),2);
        test.assertTrue(!LampPlantGrowth.candidate(level,plant),"Mature wheat is skipped");
        level.setBlock(plant,Blocks.BEETROOTS.defaultBlockState(),2);
        test.assertTrue(!LampPlantGrowth.candidate(level,plant),"Post-TC4 crops are not automatically adapted");
        level.setBlock(plant,Blocks.AIR.defaultBlockState(),2); level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos,TCArcane.GROWTH_LAMP.get().defaultBlockState(),3); var lamp=(ArcaneLampBlockEntity)level.getBlockEntity(pos);
        var tag=new CompoundTag(); tag.putInt("charges",1000); lamp.load(tag);
        for (int i=0;i<340;i++) ArcaneLampBlockEntity.tick(level,pos,lamp.getBlockState(),lamp);
        test.assertTrue(lamp.fuel().charges()>0,"Two complete 169-column scans do not crash on their final entry");
        level.setBlock(plant.below(),Blocks.SAND.defaultBlockState(),3);
        level.setBlock(plant,Blocks.CACTUS.defaultBlockState().setValue(CactusBlock.AGE,15),2);
        LampPlantGrowth.schedule(level,plant,level.getBlockState(plant));
        var saved=LampPlantGrowth.pending(level).save(new CompoundTag());
        var roundtrip=LampPlantGrowth.Pending.load(saved).save(new CompoundTag());
        test.assertTrue(saved.equals(roundtrip) && !saved.getList("updates",10).isEmpty(),"Scheduled block identity, position and due tick survive storage");
        test.assertTrue(level.isEmptyBlock(plant.above()),"Growth is scheduled rather than applied immediately");
        test.runAfterDelay(3,()->{
            test.assertTrue(level.getBlockState(plant.above()).is(Blocks.CACTUS),"The scheduled update invokes actual vanilla cactus growth");
            pass(test,"growth-candidates-full-scan-and-scheduled-update");
        });
    }
    @GameTest(template="wand_test_empty")
    public static void fertilityPopulationLimitAndPairSelection(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,3,2));
        level.setBlock(pos,TCArcane.FERTILITY_LAMP.get().defaultBlockState(),3); var lamp=(ArcaneLampBlockEntity)level.getBlockEntity(pos);
        var tag=new CompoundTag(); tag.putInt("charges",4); lamp.load(tag);
        var cows=new java.util.ArrayList<Cow>();
        for (int i=0;i<8;i++) { var cow=EntityType.COW.create(level); cow.setPos(pos.getX()+1.5,pos.getY(),pos.getZ()+.5); cow.setNoAi(true); level.addFreshEntity(cow); cows.add(cow); }
        ArcaneLampBlockEntity.tick(level,pos,lamp.getBlockState(),lamp);
        test.assertTrue(cows.stream().noneMatch(Cow::isInLove) && lamp.fuel().charges()==4,"Eight same-class animals prevent breeding");
        cows.remove(7).discard();
        for (int i=0;i<300;i++) ArcaneLampBlockEntity.tick(level,pos,lamp.getBlockState(),lamp);
        test.assertTrue(cows.stream().filter(Cow::isInLove).count()==2 && lamp.fuel().charges()==2,"Seven animals allow exactly one pair for two life charges");
        cows.forEach(Cow::discard); pass(test,"population-and-pair-selection");
    }
    @GameTest(template="wand_test_empty")
    public static void lightAirCleanupAndSupportRules(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,3,2));
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3); level.setBlock(pos,TCArcane.ARCANE_LAMP.get().defaultBlockState(),3);
        var light=pos.east(3); level.setBlock(light,TCArcane.LAMP_AIR.get().defaultBlockState(),3);
        var state=level.getBlockState(light);
        test.assertTrue(state.isAir() && state.canBeReplaced() && state.getCollisionShape(level,light).isEmpty() && state.getLightEmission(level,light)==15,"Invisible light remains replaceable noncolliding air with full light");
        level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),3);
        test.assertTrue(level.isEmptyBlock(pos) && level.getBlockState(light).is(Blocks.AIR),"Losing support drops the lamp and removes its nearby light air");
        level.setBlock(pos,TCArcane.FERTILITY_LAMP.get().defaultBlockState(),3);
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3); level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),3);
        test.assertTrue(level.isEmptyBlock(pos),"Fertility lamp also requires support");
        pass(test,"light-air-and-support");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Arcane lamp system test passed: {}",name); test.succeed(); }
}
