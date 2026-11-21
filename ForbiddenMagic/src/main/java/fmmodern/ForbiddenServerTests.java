package fmmodern;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;

/** Written during implementation; run only in the final consolidated server validation. */
@GameTestHolder("forbidden_magic") @PrefixGameTestTemplate(false)
public final class ForbiddenServerTests {
    @GameTest(template="test_empty")
    public static void fmPurifyingSpadeRemovesActualFluid(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
        var goo=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("thaumcraft:flux_goo"));
        level.setBlock(pos,goo.defaultBlockState().setValue(tc4revival.content.FiniteFluidBlock.QUANTA,4),3);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"fm_spade_test"));
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);var stack=new ItemStack(Content.TAINT_SPADE.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
        stack.getItem().useOn(context);
        test.assertTrue(level.getBlockState(pos).isAir()&&level.getFluidState(pos).isEmpty(),"Purification removes block and fluid together");
        test.assertTrue(stack.getDamageValue()==1,"One cleared fluid block costs one durability");
        stack.getItem().useOn(context);test.assertTrue(stack.getDamageValue()==1,"Reusing on cleared area does not charge again");test.succeed();
    }
    @GameTest(template="test_empty")
    public static void fmLeafLootAndCreativeWands(GameTestHelper test){
        var tool=new ItemStack(Items.DIAMOND_AXE);tool.enchant(Enchantments.SILK_TOUCH,1);
        var drops=net.minecraft.world.level.block.Block.getDrops(Content.TAINT_LEAVES.get().defaultBlockState(),test.getLevel(),test.absolutePos(new BlockPos(3,2,3)),null,null,tool);
        test.assertTrue(drops.size()==1&&drops.get(0).is(Content.TAINT_LEAVES.get().asItem()),"Loaded silk-touch leaf loot returns the block");
        var wands=new java.util.ArrayList<ItemStack>();DarkWands.creative(wands::add);
        int count=5+(ForbiddenConfig.value(ForbiddenConfig.CROSS_WAND)?(IntegrationContent.bloodEnabled()?2:0)+(IntegrationContent.botanyEnabled()?4:0):0);
        test.assertTrue(wands.size()==count,"Creative wand presets match enabled integrations");
        test.assertTrue(wands.stream().allMatch(stack->stack.getItem() instanceof tc4revival.content.CastingWandItem&&tc4revival.content.WandParts.rod(stack.getTag()).capacity>0),"Every preset resolves a real functional rod");
        test.succeed();
    }
    @GameTest(template="test_empty")
    public static void fmMorphProfilesPreserveNamesAndIndependentEnchantments(GameTestHelper test){
        var stack=new ItemStack(Content.MORPH_PICK.get());stack.enchant(Enchantments.SILK_TOUCH,1);stack.setHoverName(Component.literal("silk"));
        MorphTools.cycle(stack);test.assertTrue(!stack.isEnchanted()&&!stack.hasCustomHoverName(),"Empty second profile clears both active fields");
        stack.enchant(Enchantments.BLOCK_FORTUNE,3);stack.setHoverName(Component.literal("fortune"));
        MorphTools.cycle(stack);stack.enchant(Enchantments.BLOCK_EFFICIENCY,4);MorphTools.cycle(stack);
        test.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,stack)==1&&stack.getHoverName().getString().equals("silk"),"First profile restored");
        stack.enchant(Enchantments.UNBREAKING,2);MorphTools.cycle(stack);
        test.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,stack)==3&&EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING,stack)==0&&stack.getHoverName().getString().equals("fortune"),"Editing first profile does not mutate second");
        var restored=ItemStack.of(stack.save(new CompoundTag()));MorphTools.cycle(restored);
        test.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY,restored)==4,"Inactive profile survives ItemStack serialization");test.succeed();
    }
    @GameTest(template="test_empty")
    public static void fmCageTransferLimitsPersistenceAndCrystalExchange(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(2,3,2));test.getLevel().setBlock(pos,Content.CAGE.get().defaultBlockState(),3);var cage=(WrathCageEntity)test.getLevel().getBlockEntity(pos);
        cage.setMob(new ResourceLocation("minecraft","pig"));var beast=TCAspects.catalog().require("bestia");var fire=TCAspects.catalog().require("ignis");
        test.assertTrue(cage.addEssentia(beast,100,Direction.UP)==64&&cage.addEssentia(beast,1,Direction.DOWN)==0,"Each fuel reservoir is bounded to 64");
        test.assertTrue(cage.addEssentia(beast,-1,Direction.UP)==0&&cage.addEssentia(fire,5,Direction.UP)==0,"Invalid transfer accepts nothing");
        test.assertTrue(cage.takeEssentia(beast,5,Direction.DOWN)==0&&!cage.takeFromContainer(beast,1),"Cage does not output fuel");
        var saved=cage.saveWithFullMetadata();var copy=new WrathCageEntity(pos,Content.CAGE.get().defaultBlockState());copy.load(saved);
        test.assertTrue(copy.essentiaAmount(Direction.UP)==64&&SpecialItems.MobCrystal.mob(copy.crystal()).toString().equals("minecraft:pig"),"Fuel and captured type survive persistence");
        cage.setMob(new ResourceLocation("minecraft","blaze"));test.assertTrue(cage.essentiaAmount(Direction.UP)==0,"Changing required special aspect clears incompatible fuel");
        test.assertTrue(!CageMobs.allowed(new ResourceLocation("minecraft","wither")),"Boss is not in cage allowlist");test.succeed();
    }
    @GameTest(template="test_empty")
    public static void fmTaintedTreeFailurePreservesSaplingAndSuccessPlacesCrown(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(5,2,5));level.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),3);level.setBlock(pos,Content.TAINT_SAPLING.get().defaultBlockState(),3);level.setBlock(pos.above(),Blocks.OBSIDIAN.defaultBlockState(),3);
        test.assertTrue(!TaintedTree.grow(level,pos,net.minecraft.util.RandomSource.create(20))&&level.getBlockState(pos).is(Content.TAINT_SAPLING.get()),"Obstruction does not consume sapling");
        level.removeBlock(pos.above(),false);test.assertTrue(TaintedTree.grow(level,pos,net.minecraft.util.RandomSource.create(20)),"Clear site grows");
        test.assertTrue(level.getBlockState(pos).is(Content.TAINT_LOG.get())&&level.getBlockState(pos.above(3)).is(Content.TAINT_LOG.get()),"Minimum four-block trunk");test.succeed();
    }
    @GameTest(template="test_empty")
    public static void fmCakeRegeneratesAndHasTwelveValidShapes(GameTestHelper test){
        var cake=(ArcaneCake)Content.CAKE.get();var pos=test.absolutePos(new BlockPos(2,2,2));test.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        for(int bites=0;bites<12;bites++){var state=cake.defaultBlockState().setValue(ArcaneCake.BITES,bites);test.assertTrue(!state.getShape(test.getLevel(),pos).isEmpty(),"Every serving has a valid shape");}
        var state=cake.defaultBlockState().setValue(ArcaneCake.BITES,11);test.getLevel().setBlock(pos,state,3);cake.randomTick(state,test.getLevel(),pos,test.getLevel().random);
        test.assertTrue(test.getLevel().getBlockState(pos).getValue(ArcaneCake.BITES)==10,"Random tick restores one serving");test.succeed();
    }
    @GameTest(template="test_empty")
    public static void fmInkAndBlinkUpgradeContracts(GameTestHelper test){
        var crystal=new ItemStack(Content.CRYSTALWELL.get());var ordinary=(SpecialItems.Inkwell)crystal.getItem();ordinary.setInk(crystal,0);test.assertTrue(ordinary.getInk(crystal)==0&&ordinary.isFoil(crystal),"Empty crystalwell retains its spent form");
        var prime=new ItemStack(Content.PRIMEWELL.get());var infinite=(SpecialItems.Inkwell)prime.getItem();infinite.setInk(prime,0);test.assertTrue(infinite.getInk(prime)==100,"Primal ink is inexhaustible");
        var focus=new ItemStack(Content.BLINK.get());var item=(BlinkFocus)focus.getItem();var fire=tc4revival.content.FocusUpgrade.require("forbidden_magic:hellfire");
        test.assertTrue(item.upgradesForRank(focus,1).size()==1&&item.upgradesForRank(focus,2).contains(fire),"Exclusive upgrade offered at rank two");
        item.applyUpgrade(focus,fire,2);test.assertTrue(item.costs(focus).get(TCAspects.catalog().require("ignis"))==100&&item.upgradesForRank(focus,3).contains(tc4revival.content.FocusUpgrade.POTENCY),"Hellfire adds fire cost and enables potency");test.succeed();
    }
}
