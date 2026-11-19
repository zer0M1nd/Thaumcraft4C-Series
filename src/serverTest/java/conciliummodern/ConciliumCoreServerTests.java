package conciliummodern;

import java.util.*;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import tc4revival.content.*;

@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumCoreServerTests {
    private static TransientCraftingContainer grid(){return new TransientCraftingContainer(new AbstractContainerMenu(null,0){
        @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot){return ItemStack.EMPTY;}
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
    },3,3);}
    private static ResourceLocation id(String key){return ResourceLocation.parse("thaumicconcilium:validation/"+key);}
    @GameTest(template="empty") public static void coatingPreservesInputAndRejectsAmbiguousAndReusableInputs(GameTestHelper test){
        var recipe=new NetherMembraneRecipe(id("membrane"),CraftingBookCategory.MISC);var grid=grid();
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(29);sword.getOrCreateTag().putString("owner","kept");
        grid.setItem(0,new ItemStack(ConciliumItems.NETHER_MEMBRANE.get()));grid.setItem(1,sword);
        test.assertTrue(recipe.matches(grid,test.getLevel()),"valid coating recipe");var result=recipe.assemble(grid,test.getLevel().registryAccess());
        test.assertTrue(NetherMembraneRecipe.coated(result)&&result.getDamageValue()==29&&result.getTag().getString("owner").equals("kept"),"coating keeps complete original NBT");
        test.assertTrue(!NetherMembraneRecipe.coated(sword),"craft preview leaves input unchanged");
        grid.setItem(1,result);test.assertTrue(!recipe.matches(grid,test.getLevel()),"coating cannot be applied twice");
        grid.setItem(1,sword);grid.setItem(2,new ItemStack(Items.STICK));test.assertTrue(!recipe.matches(grid,test.getLevel()),"multiple targets rejected");
        grid.setItem(2,ItemStack.EMPTY);grid.setItem(1,new ItemStack(Items.LAVA_BUCKET));test.assertTrue(!recipe.matches(grid,test.getLevel()),"container remainder cannot duplicate reusable bucket");test.succeed();
    }
    @GameTest(template="empty") public static void xylographyPreservesWandNbtAndRejectsMultipleWands(GameTestHelper test){
        var recipe=new WandXylographyRecipe(id("name"),CraftingBookCategory.MISC);var grid=grid();var wand=new ItemStack(TCItems.CASTING_WAND.get());
        wand.getOrCreateTag().putString("rod","silverwood");wand.getTag().putInt("aer",7200);wand.getTag().putString("custom","keep");grid.setItem(0,wand);
        test.assertTrue(recipe.matches(grid,test.getLevel()),"named wand is craftable");var result=recipe.assemble(grid,test.getLevel().registryAccess());
        test.assertTrue(result.getTag().getInt("aer")==7200&&result.getTag().getString("custom").equals("keep")&&result.getTag().getString(WandXylographyRecipe.TAG).equals(" "),"only name marker changes");
        test.assertTrue(!wand.getTag().contains(WandXylographyRecipe.TAG),"preview does not change source");
        grid.setItem(1,wand.copy());test.assertTrue(!recipe.matches(grid,test.getLevel()),"two wands cannot collapse into one");test.succeed();
    }
    private static DestabilizedCrystalBlockEntity crystal(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(3,3,3));test.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        test.getLevel().setBlock(pos,ConciliumBlocks.CRYSTAL.get().defaultBlockState(),3);return (DestabilizedCrystalBlockEntity)test.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="empty") public static void crystalRefillsAfterEmptyAndKeepsCountAndType(GameTestHelper test){
        var crystal=crystal(test);var player=FakePlayerFactory.get(test.getLevel(),new GameProfile(UUID.randomUUID(),"ConciliumTest"));
        var aer=TCAspects.catalog().require("aer");var fire=TCAspects.catalog().require("ignis");
        var first=TCItems.ESSENCE_CRYSTAL.get().of(aer,3);test.assertTrue(crystal.insertCrystal(player,first)&&crystal.amount()==1&&first.getCount()==2,"first insertion counts and costs one");
        test.assertTrue(!crystal.insertCrystal(player,TCItems.ESSENCE_CRYSTAL.get().of(fire,1)),"mixed crystal rejected while occupied");
        test.assertTrue(crystal.drain("ignis",9)==0&&crystal.drain("aer",9)==1&&crystal.amount()==0,"drain is exact and bounded");
        test.assertTrue(crystal.insertCrystal(player,TCItems.ESSENCE_CRYSTAL.get().of(fire,1))&&crystal.amount()==1&&crystal.aspectKey().equals("ignis"),"empty crystal accepts different aspect without losing first unit");
        var copy=new DestabilizedCrystalBlockEntity(crystal.getBlockPos(),crystal.getBlockState());copy.load(crystal.saveWithoutMetadata());
        test.assertTrue(copy.amount()==1&&copy.aspectKey().equals("ignis"),"saved contents survive deserialize");test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=30) public static void crystalAnimationExpiresAndIsNotPersisted(GameTestHelper test){
        var crystal=crystal(test);crystal.refreshDrainAnimation();test.assertTrue(crystal.draining(),"drain starts on server");
        test.assertTrue(!crystal.saveWithoutMetadata().contains("draining"),"transient animation not in save");
        test.assertTrue(crystal.getUpdateTag().getBoolean("draining"),"animation included in client update");
        test.runAfterDelay(8,()->{test.assertTrue(!crystal.draining(),"stopped casting clears animation after five ticks");test.succeed();});
    }
    @GameTest(template="empty") public static void fullCrystalRejectsOverflowWithoutConsumingInput(GameTestHelper test){
        var crystal=crystal(test);var nbt=new CompoundTag();nbt.putString("aspect","aer");nbt.putInt("capacity",Integer.MAX_VALUE);crystal.load(nbt);
        var player=FakePlayerFactory.get(test.getLevel(),new GameProfile(UUID.randomUUID(),"CrystalLimit"));var input=TCItems.ESSENCE_CRYSTAL.get().of(TCAspects.catalog().require("aer"),2);
        test.assertTrue(!crystal.insertCrystal(player,input)&&input.getCount()==2&&crystal.amount()==Integer.MAX_VALUE,"full crystal preserves input and count");
        test.assertTrue(crystal.drain("aer",-1)==0,"negative drain rejected");nbt.putInt("capacity",-1);crystal.load(nbt);test.assertTrue(crystal.amount()==0,"negative old save is bounded");test.succeed();
    }
    @GameTest(template="empty") public static void quicksilverCapacityPreservesRemainderAndReload(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(3,3,3));test.getLevel().setBlock(pos,ConciliumBlocks.QUICKSILVER_CRUCIBLE.get().defaultBlockState(),3);
        var crucible=(ConciliumCrucibleBlockEntity)test.getLevel().getBlockEntity(pos);crucible.initialize(60);
        var mercury=new ItemEntity(test.getLevel(),pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(TCItems.material(MaterialType.QUICKSILVER),5));
        test.getLevel().addFreshEntity(mercury);crucible.smelt(mercury);test.assertTrue(crucible.amount()==64&&mercury.getItem().getCount()==3,"two mercury fill four capacity, remainder survives");
        crucible.smelt(mercury);test.assertTrue(mercury.getItem().getCount()==3,"full crucible never consumes remainder");
        var saved=crucible.saveWithoutMetadata();var restored=new ConciliumCrucibleBlockEntity(pos,crucible.getBlockState());restored.load(saved);
        test.assertTrue(restored.amount()==64&&!restored.flesh(),"type and bounded quantity survive NBT");test.succeed();
    }
    @GameTest(template="empty") public static void quicksilverCombinesLastTwoAspectsOnly(GameTestHelper test){
        var c=TCAspects.catalog();var aer=c.require("aer");var terra=c.require("terra");var aqua=c.require("aqua");
        var input=new AspectAmounts().add(aer,7).add(terra,2).add(aqua,3);var result=ConciliumCrucibleBlockEntity.combineLastPair(input);
        test.assertTrue(result!=null&&result.amount(c.require("victus"))==1&&result.amount(aer)==7&&result.amount(terra)==1&&result.amount(aqua)==2,"last terra+aqua creates one life preserving other stores");
        test.assertTrue(input.amount(terra)==2&&input.amount(aqua)==3,"combination never mutates caller before commit");
        test.assertTrue(ConciliumCrucibleBlockEntity.combineLastPair(new AspectAmounts().add(aer,5))==null,"single aspect cannot combine");test.succeed();
    }
    @GameTest(template="empty") public static void exactFocusIngredientChecksFiveOrderedStableUpgradeKeys(GameTestHelper test){
        var json=JsonParser.parseString("{\"item\":\"thaumcraft:focus_fire\",\"upgrades\":[\"thaumcraft:potency\",\"thaumcraft:frugal\",\"thaumcraft:potency\",\"thaumcraft:frugal\",\"thaumcraft:potency\"]}").getAsJsonObject();
        var ingredient=FocusPatternIngredient.SERIALIZER.parse(json);var stack=ingredient.getItems()[0];test.assertTrue(ingredient.test(stack),"five ordered ranks accepted");
        var wrong=stack.copy();var upgrades=wrong.getTag().getList("upgrade",Tag.TAG_COMPOUND);var first=upgrades.getCompound(0).copy();upgrades.set(0,upgrades.getCompound(1).copy());upgrades.set(1,first);
        test.assertTrue(!ingredient.test(wrong),"same upgrade multiset in wrong order rejected");
        upgrades.remove(4);test.assertTrue(!ingredient.test(wrong),"four-rank focus rejected");
        var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{FocusPatternIngredient.SERIALIZER.write(buffer,ingredient);var decoded=FocusPatternIngredient.SERIALIZER.parse(buffer);test.assertTrue(decoded.test(stack),"ordered rank keys survive network");}finally{buffer.release();}test.succeed();
    }
    @GameTest(template="empty") public static void chainedRiftRequiresCountKnowledgeAndExactCostsAndRoundTrips(GameTestHelper test){
        var json=JsonParser.parseString("{\"research\":\"thaumicconcilium:ASTRALMONITOR\",\"catalyst\":{\"item\":\"minecraft:diamond\"},\"count\":2,\"aspects\":{\"aer\":8},\"result\":{\"item\":\"minecraft:emerald\",\"count\":3,\"nbt\":\"{marker:1}\"}}").getAsJsonObject();
        var serializer=ChainedRiftRecipe.SERIALIZER.get();var recipe=serializer.fromJson(id("rift"),json);var stored=new AspectAmounts().add(TCAspects.catalog().require("aer"),8);
        test.assertTrue(!recipe.matches(new ItemStack(Items.DIAMOND),stored,key->true),"short catalyst count rejected");
        test.assertTrue(!recipe.matches(new ItemStack(Items.DIAMOND,2),stored,key->false),"research gate enforced");
        test.assertTrue(recipe.matches(new ItemStack(Items.DIAMOND,2),stored,key->true),"exact costs accepted");
        var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{serializer.toNetwork(buffer,recipe);var decoded=serializer.fromNetwork(id("rift"),buffer);
            var output=decoded.getResultItem(test.getLevel().registryAccess());test.assertTrue(output.getCount()==3&&output.getTag().getInt("marker")==1,"output NBT survives recipe synchronization");
            output.shrink(2);test.assertTrue(decoded.getResultItem(test.getLevel().registryAccess()).getCount()==3,"result copies never mutate recipe");
        }finally{buffer.release();}test.succeed();
    }
}
