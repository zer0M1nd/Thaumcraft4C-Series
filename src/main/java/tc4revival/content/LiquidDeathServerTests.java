package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class LiquidDeathServerTests {
    @GameTest(template="wand_test_empty")
    public static void finiteVolumeConservation(GameTestHelper test) {
        var level=test.getLevel(); int index=0;
        for (FiniteFluidBlock block:java.util.List.of(TCArcane.LIQUID_DEATH.get(),TCArcane.FLUX_GOO.get(),TCArcane.FLUX_GAS.get())) {
            var pos=test.absolutePos(new BlockPos(2+index++*2,4,3)); var destination=pos.relative(block.flowDirection);
            level.setBlock(destination,Blocks.AIR.defaultBlockState(),3);
            level.setBlock(pos,block.defaultBlockState().setValue(FiniteFluidBlock.QUANTA,block.capacity()),3);
            block.flow(level,pos,RandomSource.create(13));
            test.assertTrue(level.isEmptyBlock(pos) && block.quantity(level,destination)==block.capacity(),"Vertical transfer removes source without duplicating fluid: "+block);
            level.setBlock(destination,block.defaultBlockState().setValue(FiniteFluidBlock.QUANTA,2),3);
            level.setBlock(pos,block.defaultBlockState().setValue(FiniteFluidBlock.QUANTA,block.capacity()-1),3);
            block.flow(level,pos,RandomSource.create(13));
            test.assertTrue(block.quantity(level,pos)==1 && block.quantity(level,destination)==block.capacity(),"Overflow retains exactly one quantum: "+block);
        }
        var goo=TCArcane.FLUX_GOO.get(); var pos=test.absolutePos(new BlockPos(3,4,6));
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos,goo.defaultBlockState(),3);
        long seed=0; while(RandomSource.create(seed).nextInt(30)!=0) seed++;
        goo.tick(level.getBlockState(pos),level,pos,RandomSource.create(seed));
        test.assertTrue(level.isEmptyBlock(pos),"Decay consumes the last quantum instead of recreating it");
        pass(test,"volume-and-last-quantum");
    }
    @GameTest(template="wand_test_empty")
    public static void liquidDeathBucketAndRecipe(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,4,3));
        var block=TCArcane.LIQUID_DEATH.get(); var bucket=TCItems.LIQUID_DEATH_BUCKET.get();
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        test.assertTrue(bucket.placeLiquid(level,pos),"Bucket places liquid");
        var full=level.getBlockState(pos);
        test.assertTrue(full.getValue(FiniteFluidBlock.QUANTA)==4 && full.getFluidState().isSource() && full.getFluidState().getOwnHeight()==1,"Full bucket contains four source quanta");
        test.assertTrue(block.pickupBlock(level,pos,full).is(bucket) && level.isEmptyBlock(pos),"Full source can be collected");
        test.assertTrue(block.pickupBlock(level,pos,full).isEmpty(),"Stale pickup cannot duplicate bucket");
        for(int n=1;n<4;n++) {
            var partial=block.defaultBlockState().setValue(FiniteFluidBlock.QUANTA,n); level.setBlock(pos,partial,3);
            test.assertTrue(block.pickupBlock(level,pos,partial).isEmpty() && level.getBlockState(pos).equals(partial),"Partial fluid cannot be collected");
        }
        level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
        test.assertTrue(!bucket.placeLiquid(level,pos) && level.getBlockState(pos).is(Blocks.STONE),"Solid block is not overwritten");
        var costs=new AspectAmounts(); for(String tag:java.util.List.of("mortuus","venenum","perditio")) costs.add(TCAspects.catalog().require(tag),32);
        var input=new CrucibleRecipe.Input(new ItemStack(Items.BUCKET),costs,key->key.equals("LIQUIDDEATH"));
        var recipe=CrucibleRecipe.find(level,input).orElseThrow();
        test.assertTrue(recipe.assemble(input,level.registryAccess()).is(bucket) && recipe.remainder(costs).orElseThrow().isEmpty(),"Actual recipe consumes the old 96-aspect cost");
        test.assertTrue(!recipe.matches(new CrucibleRecipe.Input(new ItemStack(Items.BUCKET),costs,key->false),level),"Research is required");
        pass(test,"bucket-and-recipe");
    }
    @GameTest(template="wand_test_empty")
    public static void liquidDeathDamageAndCrystals(GameTestHelper test) {
        var level=test.getLevel(); var cow=EntityType.COW.create(level); var pos=test.absolutePos(new BlockPos(3,4,3));
        cow.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        cow.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
        float health=cow.getHealth();
        TCArcane.LIQUID_DEATH.get().entityInside(TCArcane.LIQUID_DEATH.get().defaultBlockState().setValue(FiniteFluidBlock.QUANTA,4),level,pos,cow);
        test.assertTrue(cow.getHealth()==health-4 && LiquidDeathDrops.source(level).is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR),"Dissolve deals four damage through armor");
        var item=TCItems.ESSENCE_CRYSTAL.get(); var stack=new ItemStack(item,16);
        item.inventoryTick(stack,level,cow,8,false); var original=stack.copy();
        test.assertTrue(item.storedAspects(stack).size()==1 && item.storedAspects(stack).total()==1 && stack.getCount()==16,"An entire unselected stack initializes to one aspect");
        item.inventoryTick(stack,level,cow,8,false);
        test.assertTrue(ItemStack.isSameItemSameTags(stack,original),"Initialized aspect remains stable");
        var aspect=TCAspects.catalog().require("aer"); var amounts=new AspectAmounts().add(aspect,8); int successes=0;
        for(int seed=0;seed<64;seed++) for(var crystal:LiquidDeathDrops.crystals(amounts,RandomSource.create(seed*104729L))) {
            successes++; test.assertTrue(crystal.getCount()>=1 && crystal.getCount()<=4 && item.storedAspects(crystal).amount(aspect)==1,"Crystals retain aspect and old quantity bounds");
        }
        test.assertTrue(successes>0 && successes<64,"The seeded sample includes both skipped and generated drops");
        var drops=new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        var ordinary=new net.minecraft.world.entity.item.ItemEntity(level,0,0,0,new ItemStack(Items.LEATHER)); drops.add(ordinary);
        long seed=0; var entityAspects=tc4revival.aspect.entity.EntityAspects.resolve(cow).orElseThrow().aspects();
        test.assertTrue(!entityAspects.isEmpty(),"Cow must have nonempty resolved aspects for dissolution-drop fixture");
        while(seed<10000 && LiquidDeathDrops.crystals(entityAspects,RandomSource.create(seed)).isEmpty()) seed++;
        test.assertTrue(seed<10000,"A bounded seed search finds at least one dissolution drop");
        level.random.setSeed(seed);
        LiquidDeathDrops.drops(new net.minecraftforge.event.entity.living.LivingDropsEvent(cow,LiquidDeathDrops.source(level),drops,0,false));
        test.assertTrue(drops.contains(ordinary) && drops.size()>1 && drops.stream().skip(1).allMatch(drop->drop.getItem().is(item)),"Dissolve adds crystals without replacing normal drops");
        cow.discard(); pass(test,"damage-and-crystals");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Liquid death test passed: {}",name); test.succeed(); }
}
