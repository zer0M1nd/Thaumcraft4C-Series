package thmodern.focus;
import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class DisintegrationServerTests {
 @GameTest(template="empty")
 public static void crystalYieldConservesAspectsAndCannotBeReprocessed(GameTestHelper test){
  var level=test.getLevel();var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"DissolveItem"));var stack=thaumcraft.api.ThaumcraftApi.createWand("wood","iron",false,false);var wand=(CastingWandItem)stack.getItem();var focus=(DisintegrationFocusItem)Horizons.DISINTEGRATION_FOCUS.get();WandFocusItem.attach(stack,new ItemStack(focus));player.setItemInHand(InteractionHand.MAIN_HAND,stack);var pos=test.absolutePos(new net.minecraft.core.BlockPos(2,2,2));var source=new ItemStack(Items.COBBLESTONE,64);var expected=DisintegrationFocusItem.aspects(source);test.assertTrue(!expected.isEmpty(),"Fixture has object aspects");var item=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,source);level.addFreshEntity(item);
  test.assertTrue(!focus.dissolveItem(player,stack,wand,item)&&item.isAlive()&&item.getItem().getCount()==64,"Insufficient vis cannot erase input");for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())wand.addVis(stack,aspect,1000,false,true);
  test.assertTrue(focus.dissolveItem(player,stack,wand,item)&&!item.isAlive(),"Paid operation consumes source exactly once");var actual=new tc4revival.aspect.AspectAmounts();var outputs=level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1),e->e.getItem().getItem() instanceof EssenceCrystalItem);
  for(var output:outputs){var crystal=output.getItem();test.assertTrue(crystal.getCount()<=crystal.getMaxStackSize()&&output.isInvulnerable(),"Each output is bounded and damage protected");for(var entry:TCItems.ESSENCE_CRYSTAL.get().storedAspects(crystal).view().entrySet())actual.merge(entry.getKey(),entry.getValue()*crystal.getCount());test.assertTrue(!focus.dissolveItem(player,stack,wand,output)&&output.isAlive(),"Crystals are never targets");}
  for(var entry:expected.view().entrySet())test.assertTrue(actual.amount(entry.getKey())==64*entry.getValue(),"Every aspect count is conserved across split stacks");outputs.forEach(ItemEntity::discard);test.succeed();
 }
 @GameTest(template="empty")
 public static void protectedBlocksStayIntactAndOrdinaryBlocksDissolve(GameTestHelper test){
  var level=test.getLevel();var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"DissolveBlock"));var stack=thaumcraft.api.ThaumcraftApi.createWand("wood","iron",false,false);var wand=(CastingWandItem)stack.getItem();var focus=(DisintegrationFocusItem)Horizons.DISINTEGRATION_FOCUS.get();WandFocusItem.attach(stack,new ItemStack(focus));player.setItemInHand(InteractionHand.MAIN_HAND,stack);for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())wand.addVis(stack,aspect,1000,false,true);var pos=test.absolutePos(new net.minecraft.core.BlockPos(2,2,2));player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
  for(var block:List.of(Blocks.BEDROCK,Blocks.CHEST)){level.setBlock(pos,block.defaultBlockState(),3);test.assertTrue(!focus.dissolveBlock(player,stack,wand,pos)&&level.getBlockState(pos).is(block),"Unbreakable blocks and containers remain intact");}
  level.setBlock(pos,Blocks.COBBLESTONE.defaultBlockState(),3);test.assertTrue(focus.dissolveBlock(player,stack,wand,pos)&&level.isEmptyBlock(pos),"Ordinary aspect-bearing block turns into crystals");test.assertTrue(!focus.dissolveBlock(player,stack,wand,pos),"Same empty position cannot produce twice");test.succeed();
 }
}
