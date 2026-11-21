package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class BrainJarServerTests {
    private static BrainJarBlockEntity jar(GameTestHelper test,BlockPos pos,int xp){test.getLevel().setBlockAndUpdate(pos,TCArcane.BRAIN_JAR.get().defaultBlockState());var jar=(BrainJarBlockEntity)test.getLevel().getBlockEntity(pos);var tag=new CompoundTag();tag.putInt("XP",xp);jar.load(tag);return jar;}
    private static ExperienceOrb orb(GameTestHelper test,Vec3 pos,int value,int count){var orb=new ExperienceOrb(test.getLevel(),pos.x,pos.y,pos.z,value);var tag=new CompoundTag();orb.addAdditionalSaveData(tag);tag.putInt("Count",count);orb.readAdditionalSaveData(tag);orb.setDeltaMovement(Vec3.ZERO);test.getLevel().addFreshEntity(orb);return orb;}
    @GameTest(template="tree_test_empty")
    public static void brainJarAttractionBatchAndCapacity(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(8,3,8));var jar=jar(test,pos,0);var center=Vec3.atCenterOf(pos);
        var near=orb(test,center.add(2,0,0),7,1);var farther=orb(test,center.add(4,0,0),3,1);jar.tick();
        test.assertTrue(near.getDeltaMovement().x<0&&farther.getDeltaMovement().lengthSqr()==0,"Only closest orb is attracted");
        double expected=-Math.pow(1-2/7d,2)*.15;test.assertTrue(Math.abs(near.getDeltaMovement().x-expected)<1e-8,"Old horizontal attraction coefficient");near.discard();farther.discard();
        var merged=orb(test,center,7,10);jar.tick();test.assertTrue(merged.isRemoved()&&jar.experience()==70,"Merged XP orb counts all ten members");test.assertTrue(Double.isFinite(merged.getDeltaMovement().lengthSqr()),"Centered orb avoids zero-distance NaN");
        var tag=new CompoundTag();tag.putInt("XP",1999);jar.load(tag);var first=orb(test,center,7,1);var second=orb(test,center,3,1);jar.tick();
        test.assertTrue(jar.experience()==2000 && BrainJarBlockEntity.orbExperience(first)+BrainJarBlockEntity.orbExperience(second)==9,"Only available capacity is absorbed; nine XP remain");jar.tick();test.assertTrue(jar.experience()==2000&&jar.comparator()==15,"Full jar neither consumes nor truncates XP");
        var fullOrb=orb(test,center,1,1);jar.tick();test.assertTrue(fullOrb.isAlive(),"Full jar leaves XP untouched");fullOrb.discard();
        test.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());test.getLevel().getEntitiesOfClass(ExperienceOrb.class,new AABB(pos).inflate(3)).forEach(Entity::discard);pass(test,"attraction-merged-batch-capacity");
    }
    @GameTest(template="wand_test_empty")
    public static void brainJarReleaseDelaySaveAndBreak(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(3,3,3));var jar=jar(test,pos,1000);test.getLevel().random.setSeed(37);int released=jar.release();
        test.assertTrue(released>0&&released<=63&&jar.experience()==1000-released&&jar.eatDelay==40,"Random release conserves XP and starts delay");
        for(int i=0;i<40;i++)jar.tick();test.assertTrue(jar.experience()==1000-released&&jar.eatDelay==0,"Forty delayed ticks do not reabsorb release");jar.tick();test.assertTrue(jar.experience()==1000,"Absorption resumes on following tick");
        var loaded=new BrainJarBlockEntity(pos,jar.getBlockState());loaded.load(jar.saveWithoutMetadata());test.assertTrue(loaded.experience()==1000&&loaded.eatDelay==0,"Only XP persists");
        var block=TCArcane.BRAIN_JAR.get();var state=block.defaultBlockState();test.assertTrue(block.getEnchantPowerBonus(state,test.getLevel(),pos)==2&&state.getLightEmission(test.getLevel(),pos)==9&&jar.comparator()==8,"Enchanting bonus, light, comparator");
        test.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());var orbs=test.getLevel().getEntitiesOfClass(ExperienceOrb.class,new AABB(pos).inflate(2));test.assertTrue(orbs.stream().mapToInt(BrainJarBlockEntity::orbExperience).sum()==1000,"Breaking releases all XP");orbs.forEach(Entity::discard);
        var malformed=new CompoundTag();malformed.putInt("XP",-100);loaded.load(malformed);test.assertTrue(loaded.experience()==0,"Malformed negative XP cannot break click random bound");pass(test,"release-delay-save-break");
    }
    @GameTest(template="wand_test_empty")
    public static void brainJarInfusionAndDrops(GameTestHelper test){
        var items=java.util.List.of(new ItemStack(TCItems.ZOMBIE_BRAIN.get()),new ItemStack(Items.SPIDER_EYE),new ItemStack(Items.WATER_BUCKET),new ItemStack(Items.SPIDER_EYE));
        var input=new InfusionRecipe.Input(new ItemStack(TCItems.ESSENTIA_JAR.get()),items,key->key.equals("JARBRAIN"));var recipe=InfusionRecipe.find(test.getLevel(),input).orElseThrow();var plan=recipe.plan(input.getItem(0));
        test.assertTrue(plan.instability()==4&&plan.aspects().total()==40&&plan.aspects().amount(tc4revival.aspect.TCAspects.catalog().require("exanimis"))==20&&recipe.assemble(input,test.getLevel().registryAccess()).is(TCItems.BRAIN_JAR.get()),"Original infusion ingredients, instability and aspects");
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(input.getItem(0),items,key->false),test.getLevel()),"Research gate enforced");
        var pos=test.absolutePos(new BlockPos(3,3,3));var jar=jar(test,pos,50);var drops=net.minecraft.world.level.block.Block.getDrops(jar.getBlockState(),test.getLevel(),pos,jar);
        test.assertTrue(drops.size()==1&&drops.get(0).is(TCItems.BRAIN_JAR.get())&&!drops.get(0).hasTag(),"Jar item carries no XP; block destruction handles XP separately");
        test.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());test.getLevel().getEntitiesOfClass(ExperienceOrb.class,new AABB(pos).inflate(3)).forEach(Entity::discard);pass(test,"infusion-drop");
    }
    private static void pass(GameTestHelper test,String name){com.mojang.logging.LogUtils.getLogger().info("Brain jar test passed: {}",name);test.succeed();}
}
