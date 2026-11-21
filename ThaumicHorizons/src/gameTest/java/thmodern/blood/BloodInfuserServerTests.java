package thmodern.blood;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class BloodInfuserServerTests {
    private static BloodInfuserEntity machine(GameTestHelper test){var pos=test.absolutePos(new BlockPos(3,2,3));test.getLevel().setBlock(pos,Horizons.BLOOD_INFUSER.get().defaultBlockState(),3);return (BloodInfuserEntity)test.getLevel().getBlockEntity(pos);}
    @GameTest(template="empty")
    public static void acceptsOnlyRequiredEssentiaAndProducesSingleInjection(GameTestHelper test){
        var tile=machine(test);tile.setItem(0,new ItemStack(Horizons.HUMAN_BLOOD.get(),2));tile.select("aer",true);tile.mode(1);var air=TCAspects.catalog().require("aer");
        test.assertTrue(tile.addEssentia(air,1,Direction.UP)==0,"Upper face disconnected");
        test.assertTrue(tile.addEssentia(TCAspects.catalog().require("terra"),1,Direction.DOWN)==0,"Rejects wrong aspect");
        test.assertTrue(tile.addEssentia(air,0,Direction.DOWN)==0&&tile.addEssentia(air,99,Direction.DOWN)==1,"Only consumes required positive amount");
        tile.tick();test.assertTrue(tile.mode()==0&&tile.getItem(0).getCount()==1&&tile.getItem(1).is(Horizons.INJECTION.get()),"Consumes one blood and stops");
        var effects=PotionUtils.getMobEffects(tile.getItem(1));test.assertTrue(effects.size()==1&&effects.get(0).getEffect()==net.minecraft.world.effect.MobEffects.JUMP&&effects.get(0).getDuration()==400,"Uses formula result");
        test.assertTrue(tile.acquired("aer")==0&&tile.suction(Direction.DOWN)==0,"Spent essentia cleared and suction stopped");test.succeed();
    }
    @GameTest(template="empty")
    public static void fullOutputPausesAndInventorySidesProtectInputs(GameTestHelper test){
        var tile=machine(test);tile.setItem(0,new ItemStack(Horizons.BLOOD_PHIAL.get(),2));tile.select("aer",true);tile.mode(2);
        for(int i=1;i<10;i++)tile.setItem(i,new ItemStack(Horizons.INJECTION_PHIAL.get()));
        test.assertTrue(tile.suction(Direction.DOWN)==0&&tile.addEssentia(TCAspects.catalog().require("aer"),1,Direction.DOWN)==0,"No input while output full");
        tile.tick();test.assertTrue(tile.getItem(0).getCount()==2,"No blood consumed without space");
        test.assertTrue(!tile.canTakeItemThroughFace(0,tile.getItem(0),Direction.DOWN)&&!tile.canPlaceItemThroughFace(1,new ItemStack(Items.DIAMOND),Direction.UP),"Automation cannot bypass input/output rules");
        tile.removeItem(1,1);tile.addEssentia(TCAspects.catalog().require("aer"),1,Direction.DOWN);tile.tick();
        test.assertTrue(tile.mode()==2&&tile.getItem(1).is(Horizons.INJECTION_PHIAL.get())&&tile.getItem(0).getCount()==1,"Resumes with matching phial output");test.succeed();
    }
    @GameTest(template="empty")
    public static void formulaAndPartialProgressSurviveSaveWithoutExtraMaterial(GameTestHelper test){
        var tile=machine(test);tile.setItem(0,new ItemStack(Horizons.HUMAN_BLOOD.get()));tile.select("aer",true);tile.select("aer",true);tile.mode(2);
        tile.addEssentia(TCAspects.catalog().require("aer"),1,Direction.DOWN);var saved=tile.saveWithoutMetadata();tile.load(saved);
        test.assertTrue(tile.selection().get("aer")==2&&tile.acquired("aer")==1&&tile.mode()==2,"Reload retains exact work");
        tile.select("terra",true);test.assertTrue(tile.acquired("aer")==1,"Editing preserves compatible paid material");
        for(int i=0;i<10;i++)tile.select("terra",true);test.assertTrue(tile.selection().values().stream().mapToInt(Integer::intValue).sum()==8,"Server enforces eight-point budget");test.succeed();
    }
    @GameTest(template="empty")
    public static void sampleCannotCloneEquipmentOrEntityIdentity(GameTestHelper test){
        var horse=net.minecraft.world.entity.EntityType.HORSE.create(test.getLevel());horse.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
        test.assertTrue(BloodSampleItem.eligible(horse),"Living animal can be sampled");
        var genes=BloodSampleItem.genetics(horse);
        test.assertTrue(!genes.contains("UUID")&&!genes.contains("ArmorItems")&&!genes.contains("Pos")&&genes.getString("id").equals("minecraft:horse"),"Genetics omit equipment, identity and location");
        var zombie=net.minecraft.world.entity.EntityType.ZOMBIE.create(test.getLevel());test.assertTrue(!BloodSampleItem.eligible(zombie),"Undead excluded");test.succeed();
    }
    @GameTest(template="empty")
    public static void incubatedEggHatchesExactlyOnce(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var egg=new net.minecraft.world.entity.projectile.ThrownEgg(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5);egg.setItem(new ItemStack(Horizons.INCUBATED_EGG.get()));level.addFreshEntity(egg);
        var impact=new net.minecraftforge.event.entity.ProjectileImpactEvent(egg,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
        IncubatedEggItem.impact(impact);IncubatedEggItem.impact(impact);
        var chicks=level.getEntitiesOfClass(net.minecraft.world.entity.animal.Chicken.class,new net.minecraft.world.phys.AABB(pos).inflate(2));
        test.assertTrue(impact.isCanceled()&&egg.isRemoved()&&chicks.size()==1&&chicks.get(0).getAge()==-24000,"Bypasses random vanilla hatch, creates one baby and guards repeated impact");test.succeed();
    }
}
