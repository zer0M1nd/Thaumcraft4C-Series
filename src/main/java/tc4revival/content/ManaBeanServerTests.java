package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.aspect.TCAspects;

/** Integrated registration, persistence and harvesting checks for the deferred server checkpoint. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class ManaBeanServerTests {
    private ManaBeanServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void podLootTableCoversGrowthAndNbt(GameTestHelper test) {
        var level=test.getLevel(); var block=TCArcane.MANA_POD.get(); var pos=test.absolutePos(new BlockPos(2,2,2));
        var table=level.getServer().getLootData().getLootTable(net.minecraft.resources.ResourceLocation.parse("thaumcraft:blocks/mana_pod"));
        var tool=new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        tool.enchant(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE,3);
        tool.enchant(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,1);
        boolean single=false,doubleDrop=false;
        for(int age=0;age<8;age++) for(boolean known:new boolean[]{false,true}) {
            var state=block.defaultBlockState().setValue(ManaPodBlock.AGE,age);
            var pod=new ManaPodBlockEntity(pos,state);
            var aspect=TCAspects.catalog().require(known?"praecantatio":"herba");
            if(known)pod.setAspect(aspect);
            for(int sample=0;sample<32;sample++) {
                long seed=111+sample*173L;
                float roll=net.minecraft.util.RandomSource.create(seed).nextFloat();
                int expected=age<2?0:age==7&&roll>.33f?2:1;
                var params=new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE,state)
                        .withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL,tool)
                        .withParameter(LootContextParams.BLOCK_ENTITY,pod).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
                level.random.setSeed(seed);
                var drops=table.getRandomItems(params);
                test.assertTrue(drops.size()==expected && drops.stream().allMatch(drop->drop.is(TCItems.MANA_BEAN.get())
                        && drop.getCount()==1 && TCItems.MANA_BEAN.get().storedAspects(drop).amount(aspect)==1),
                        "Actual loot table supplies age/count/aspect; fortune and silk do not alter beans");
                if(age==7) { single|=expected==1; doubleDrop|=expected==2; }
                if(drops.size()==2) {
                    drops.get(0).getOrCreateTag().putBoolean("isolated",true);
                    test.assertTrue(!drops.get(1).getTag().contains("isolated") && !pod.saveWithoutMetadata().contains("isolated"),
                            "Two bean outputs and pod data do not share mutable NBT");
                }
            }
        }
        test.assertTrue(single&&doubleDrop,"Both old mature-pod probability outcomes were exercised");
        tc4revival.TC4Revival.LOGGER.info("Mana pod table PASS: eight-ages-aspect-fallback-count-random-silk-fortune-independent-nbt");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void podPersistenceAndHarvestPreserveAspectWithoutAliasing(GameTestHelper test) {
        var block=TCArcane.MANA_POD.get(); var pos=test.absolutePos(new BlockPos(2,2,2));
        var state=block.defaultBlockState().setValue(ManaPodBlock.AGE,6);
        var pod=new ManaPodBlockEntity(pos,state); var aspect=TCAspects.catalog().require("lux"); pod.setAspect(aspect);
        var restored=new ManaPodBlockEntity(pos,state); restored.load(pod.saveWithoutMetadata());
        test.assertTrue(restored.aspect().equals(aspect),"The lowercase legacy aspect field survives a save round trip");
        test.assertTrue(restored.revealedAspects().isEmpty(),"A growing pod does not reveal its contents yet");
        var mature=new ManaPodBlockEntity(pos,state.setValue(ManaPodBlock.AGE,7)); mature.load(restored.getUpdateTag());
        test.assertTrue(mature.revealedAspects().amount(aspect)==1,"The mature client update exposes exactly one aspect");
        var params=new LootParams.Builder(test.getLevel()).withParameter(LootContextParams.BLOCK_ENTITY,restored)
                .withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL,ItemStack.EMPTY);
        var drops=block.getDrops(state,params);
        test.assertTrue(drops.size()==1 && TCItems.MANA_BEAN.get().storedAspects(drops.get(0)).amount(aspect)==1,"Actual pod loot carries the stored aspect");
        drops.get(0).getOrCreateTag().putString("output_only","changed");
        test.assertTrue(!block.getDrops(state,params).get(0).getTag().contains("output_only"),"Harvest output NBT is independent");
        test.assertTrue(block.getDrops(state.setValue(ManaPodBlock.AGE,1),params).isEmpty(),"Immature pods cannot yield seeds");
        var fallback=block.getDrops(state,new LootParams.Builder(test.getLevel())
                .withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL,ItemStack.EMPTY));
        test.assertTrue(TCItems.MANA_BEAN.get().storedAspects(fallback.get(0)).amount(TCAspects.catalog().require("herba"))==1,"Missing pod data uses the old plant-aspect fallback");
        tc4revival.TC4Revival.LOGGER.info("Mana pod persistence/harvest PASS: save-sync-aspect-nbt-immature-fallback");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void beanInitializationDistinguishesMissingFromEmptyNbt(GameTestHelper test) {
        var item=TCItems.MANA_BEAN.get(); var player=test.makeMockPlayer();
        var fresh=new ItemStack(item); item.inventoryTick(fresh,test.getLevel(),player,0,false);
        test.assertTrue(item.storedAspects(fresh).size()==1 && item.storedAspects(fresh).total()==1,"A fresh inventory bean acquires one aspect");
        var saved=fresh.copy(); item.inventoryTick(fresh,test.getLevel(),player,0,false);
        test.assertTrue(ItemStack.isSameItemSameTags(saved,fresh),"Inventory ticks do not reroll an initialized bean");
        var emptyTag=new ItemStack(item); emptyTag.setTag(new CompoundTag()); item.onCraftedBy(emptyTag,test.getLevel(),player);
        test.assertTrue(item.storedAspects(emptyTag).size()==1,"Empty NBT no longer prevents aspect initialization");
        test.assertTrue(item.getUseDuration(fresh)==10 && fresh.getMaxStackSize()==64,"The bean retains its short use duration and normal stack size");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void effectSlotsAndGazeConeRetainLegacyBoundaries(GameTestHelper test) {
        int present=0,empty=0; var unique=new java.util.HashSet<net.minecraft.world.effect.MobEffect>();
        for (int slot=0;slot<ManaBeanEffects.SLOTS;slot++) {
            var effect=ManaBeanEffects.at(slot);
            if (effect==null) empty++; else { present++; unique.add(effect); }
        }
        test.assertTrue(present==32 && unique.size()==32 && empty==8,"The forty-slot pool contains 23 vanilla effects, nine TC effects, and eight blanks");
        test.assertTrue(ManaBeanEffects.at(31)==TCEffects.FLUX_TAINT.get() && ManaBeanEffects.at(39)==TCEffects.THAUMARHIA.get(),"TC occupies 31 through 39, not 32 onward");
        test.assertTrue(DeathGaze.inCone(new Vec3(0,0,7.9),new Vec3(0,0,1),8),"A target in front and before the cap is visible");
        test.assertTrue(!DeathGaze.inCone(new Vec3(0,0,8),new Vec3(0,0,1),8),"The old axial range comparison is strict");
        test.assertTrue(!DeathGaze.inCone(new Vec3(0,0,-1),new Vec3(0,0,1),8) && !DeathGaze.inCone(Vec3.ZERO,new Vec3(0,0,1),8),"Rear and coincident points are outside the cone");
        test.succeed();
    }
}
