package thmodern.vat;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
import thmodern.blood.BloodSampleItem;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class VatServerTests {
    @GameTest(template="empty")
    public static void effigyRequiresBothCostsAndSurvivesReload(GameTestHelper test){
        var vat=vat(test);vat.setItem(0,new ItemStack(Horizons.CORPSE_EFFIGY.get()));vat.tick();
        test.assertTrue(vat.mode()==VatEntity.GROWING_EFFIGY&&vat.progress()==80&&vat.accept("victus",8)==0,"Incubation precedes payment and needs no nutrients");
        for(int i=0;i<80;i++)vat.tick();vat.accept("victus",8);vat.tick();
        test.assertTrue(!vat.hasEffigy()&&!vat.getItem(0).isEmpty(),"Life alone cannot create an effigy");
        vat.accept("sano",8);vat.tick();
        test.assertTrue(vat.mode()==VatEntity.EFFIGY&&vat.effigyHealth()==20&&vat.subject()==null&&vat.getItem(0).isEmpty(),"Both costs atomically produce a non-entity body");
        var restored=new VatEntity(vat.getBlockPos(),vat.getBlockState());restored.setLevel(test.getLevel());restored.load(vat.saveWithoutMetadata());restored.tick();
        test.assertTrue(restored.hasEffigy()&&restored.mode()==VatEntity.EFFIGY&&restored.essentia().stored().isEmpty(),"Body survives reload without refunded payment or live entity duplication");
        restored.destroySubject();test.assertTrue(!restored.hasEffigy()&&restored.mode()==VatEntity.IDLE,"Dismantling clears the body");test.succeed();
    }
    @GameTest(template="empty")
    public static void replacingEffigyCancelsPaidReservation(GameTestHelper test){
        var vat=vat(test);vat.setItem(0,new ItemStack(Horizons.CORPSE_EFFIGY.get()));vat.tick();for(int i=0;i<80;i++)vat.tick();vat.accept("victus",8);
        vat.setItem(0,new ItemStack(Items.BEEF));vat.tick();
        test.assertTrue(vat.mode()==VatEntity.IDLE&&!vat.hasEffigy()&&vat.essentia().stored().isEmpty()&&vat.getItem(0).is(Items.BEEF),"Replacing a reserved effigy cancels without consuming the new sample");test.succeed();
    }
    private static VatEntity vat(GameTestHelper test){
        var center=test.absolutePos(new BlockPos(3,5,3));
        for(int part=0;part<36;part++)test.getLevel().setBlock(center.offset(VatStructure.offset(part)),Horizons.VAT.get().defaultBlockState().setValue(VatBlock.PART,part),2);
        return (VatEntity)test.getLevel().getBlockEntity(center);
    }
    @GameTest(template="empty")
    public static void cloningPaysSurchargeAndPersistsIncubation(GameTestHelper test){
        var vat=vat(test);var cow=EntityType.COW.create(test.getLevel());var genes=BloodSampleItem.genetics(cow);
        var extra=new CompoundTag();extra.putInt("cognitio",3);var infusion=new CompoundTag();infusion.put("CloneCosts",extra);var data=new CompoundTag();data.put("thaumichorizons:infusions",infusion);genes.put("ForgeData",data);
        var sample=new ItemStack(Horizons.BLOOD_SAMPLE.get(),2);sample.getOrCreateTag().put("critter",genes);vat.setItem(0,sample);vat.setItem(1,new ItemStack(Horizons.NUTRIENTS.get(),2));vat.tick();
        for(int i=0;i<40;i++)vat.tick();vat.accept("victus",4);vat.tick();test.assertTrue(vat.subject()==null&&vat.getItem(0).getCount()==2,"Life alone does not start an infused clone");
        vat.accept("cognitio",3);vat.tick();test.assertTrue(vat.subject()!=null&&vat.progress()==800&&vat.getItem(0).getCount()==1&&vat.getItem(1).getCount()==1,"All costs consumed once before incubation");
        var restored=new VatEntity(vat.getBlockPos(),vat.getBlockState());restored.setLevel(test.getLevel());restored.load(vat.saveWithoutMetadata());restored.tick();
        test.assertTrue(restored.subject()!=null&&restored.progress()==799&&restored.subject().getUUID().equals(vat.subject().getUUID()),"Detached subject and remaining time survive save without a duplicate world spawn");test.succeed();
    }
    @GameTest(template="empty")
    public static void changingSampleAbandonsPaidReservation(GameTestHelper test){
        var vat=vat(test);vat.setItem(0,new ItemStack(Items.BEEF));vat.setItem(1,new ItemStack(Horizons.NUTRIENTS.get()));vat.tick();for(int i=0;i<40;i++)vat.tick();vat.accept("victus",3);
        vat.setItem(0,new ItemStack(Items.CHICKEN));vat.tick();test.assertTrue(vat.mode()==VatEntity.IDLE&&vat.essentia().stored().isEmpty()&&vat.getItem(1).getCount()==1,"Changing sample cancels partial job without consuming nutrients or moving essentia to next clone");test.succeed();
    }
    @GameTest(template="empty")
    public static void healingUsesWeakLifeWhenStrongHealingIsAbsent(GameTestHelper test){
        var vat=vat(test);var cow=EntityType.COW.create(test.getLevel());cow.moveTo(vat.getBlockPos().above().getCenter());cow.setHealth(1);test.getLevel().addFreshEntity(cow);
        test.assertTrue(vat.capture(cow)&&cow.isRemoved(),"Capture removes the live instance only after preparing its stored copy");vat.tick();vat.accept("victus",1);vat.tick();
        test.assertTrue(vat.subject().getHealth()==5&&vat.progress()==50&&vat.essentia().amount("victus")==0,"Weak healing restores four health and consumes one unit");test.succeed();
    }
    @GameTest(template="empty")
    public static void healingAdvertisesTypedSuctionAndStillDrawsFromJars(GameTestHelper test){
        var vat=vat(test);var cow=EntityType.COW.create(test.getLevel());cow.moveTo(vat.getBlockPos().above().getCenter());cow.setHealth(1);test.getLevel().addFreshEntity(cow);
        test.assertTrue(vat.capture(cow),"Patient enters vat");vat.tick();
        var portPos=vat.getBlockPos().offset(VatStructure.offset(1));
        var port=(VatPartEntity)test.getLevel().getBlockEntity(portPos);
        test.assertTrue(vat.mode()==VatEntity.IDLE&&port.suction(net.minecraft.core.Direction.NORTH)==128&&port.suctionType(net.minecraft.core.Direction.NORTH).tag().equals("sano"),"Healing mode advertises a typed 128 suction for ME providers");
        var jarPos=portPos.below();test.getLevel().setBlockAndUpdate(jarPos,tc4revival.content.TCArcane.ESSENTIA_JAR.get().defaultBlockState());
        var jar=(tc4revival.content.EssentiaJarBlockEntity)test.getLevel().getBlockEntity(jarPos);jar.addToContainer(tc4revival.aspect.TCAspects.catalog().require("victus"),3);
        VatStructure.pull(vat);
        test.assertTrue(vat.essentia().amount("victus")==1&&jar.essentiaAmount(net.minecraft.core.Direction.UP)==2,"An alternative healing aspect is drawn once through the jar's top opening");
        vat.tick();test.assertTrue(vat.subject().getHealth()==5&&port.suction(net.minecraft.core.Direction.NORTH)==0&&port.suctionType(net.minecraft.core.Direction.NORTH)==null,"Healing consumes the unit and stops demand during cooldown");
        test.succeed();
    }
    @GameTest(template="empty")
    public static void dismantlingRestoresOnlySurvivingComponents(GameTestHelper test){
        var vat=vat(test);var center=vat.getBlockPos();var broken=center.offset(VatStructure.offset(0));test.getLevel().setBlock(broken,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        test.assertTrue(test.getLevel().getBlockState(broken).is(net.minecraft.world.level.block.Blocks.STONE),"Dismantle does not overwrite the broken block's replacement");
        test.assertTrue(test.getLevel().getBlockState(center).equals(VatStructure.original(31))&&test.getLevel().getBlockState(center.below()).is(net.minecraft.world.level.block.Blocks.WATER),"Other vat parts revert to their construction materials");test.succeed();
    }
}
