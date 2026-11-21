package thmodern.focus;
import java.util.UUID;
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
public final class LiquefactionServerTests {
 @GameTest(template="empty")
 public static void blockMeltingPaysOnlyForChangesAndKeepsStoneLavaRule(GameTestHelper test){
  var level=test.getLevel();var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"MeltBlock"));var stack=thaumcraft.api.ThaumcraftApi.createWand("wood","iron",false,false);var wand=(CastingWandItem)stack.getItem();var focus=(LiquefactionFocusItem)Horizons.LIQUEFACTION_FOCUS.get();WandFocusItem.attach(stack,new ItemStack(focus));player.setItemInHand(InteractionHand.MAIN_HAND,stack);var fire=tc4revival.aspect.TCAspects.catalog().require("ignis");wand.addVis(stack,fire,1000,false,true);var pos=test.absolutePos(new net.minecraft.core.BlockPos(2,2,2));player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
  level.setBlock(pos,Blocks.BEDROCK.defaultBlockState(),3);int initial=wand.vis(stack,fire);test.assertTrue(!focus.meltBlock(player,stack,wand,pos)&&wand.vis(stack,fire)==initial,"Unbreakable target neither changes nor charges vis");
  var manager=level.getRecipeManager();var originalRecipes=new java.util.ArrayList<>(manager.getRecipes());
  try {
  if(LiquefactionFocusItem.smelt(level,new ItemStack(Items.SAND)).isEmpty()){
   level.setBlock(pos,Blocks.SAND.defaultBlockState(),3);
   test.assertTrue(!focus.meltBlock(player,stack,wand,pos)&&level.getBlockState(pos).is(Blocks.SAND)&&wand.vis(stack,fire)==initial,"An integration-removed furnace recipe neither changes the target nor charges vis");
   var recipes=new java.util.ArrayList<>(originalRecipes);
   recipes.add(new net.minecraft.world.item.crafting.SmeltingRecipe(new net.minecraft.resources.ResourceLocation("thaumichorizons","validation_sand_to_glass"),"",net.minecraft.world.item.crafting.CookingBookCategory.MISC,net.minecraft.world.item.crafting.Ingredient.of(Items.SAND),new ItemStack(Items.GLASS),.1f,200));
   manager.replaceRecipes(recipes);
  }
  level.setBlock(pos,Blocks.SAND.defaultBlockState(),3);boolean melted=focus.meltBlock(player,stack,wand,pos);test.assertTrue(melted&&level.getBlockState(pos).is(Blocks.GLASS)&&wand.vis(stack,fire)<initial,"Sand becomes glass and pays vis; result="+melted+" block="+level.getBlockState(pos)+" vis="+wand.vis(stack,fire)+" before="+initial+" mode="+player.gameMode.getGameModeForPlayer()+" mayBuild="+player.mayBuild()+" mayInteract="+level.mayInteract(player,pos)+" mayUse="+player.mayUseItemAt(pos,net.minecraft.core.Direction.UP,stack)+" smelt="+LiquefactionFocusItem.smelt(level,new ItemStack(Items.SAND))+" canPay="+wand.consumeForPlayer(stack,player,focus.costs(WandFocusItem.attached(stack)),false,false,false));
  level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);test.assertTrue(focus.meltBlock(player,stack,wand,pos)&&level.getBlockState(pos).is(Blocks.LAVA),"Legacy stone melting is not overridden by modern smooth-stone recipe");
  level.setBlock(pos,Blocks.CHEST.defaultBlockState(),3);int before=wand.vis(stack,fire);test.assertTrue(!focus.meltBlock(player,stack,wand,pos)&&wand.vis(stack,fire)==before,"Containers cannot be silently erased");test.succeed();
  } finally {manager.replaceRecipes(originalRecipes);}
 }
 @GameTest(template="empty")
 public static void droppedStackSmeltsOnceWithoutCreatingFreeItems(GameTestHelper test){
  var level=test.getLevel();var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"MeltItem"));var stack=thaumcraft.api.ThaumcraftApi.createWand("wood","iron",false,false);var wand=(CastingWandItem)stack.getItem();var focus=(LiquefactionFocusItem)Horizons.LIQUEFACTION_FOCUS.get();WandFocusItem.attach(stack,new ItemStack(focus));player.setItemInHand(InteractionHand.MAIN_HAND,stack);var fire=tc4revival.aspect.TCAspects.catalog().require("ignis");var pos=test.absolutePos(new net.minecraft.core.BlockPos(2,2,2));var item=new ItemEntity(level,pos.getX(),pos.getY(),pos.getZ(),new ItemStack(Items.RAW_IRON,12));level.addFreshEntity(item);
  test.assertTrue(!focus.meltItem(player,stack,wand,item)&&item.getItem().is(Items.RAW_IRON),"No vis leaves input unchanged");wand.addVis(stack,fire,1000,false,true);int before=wand.vis(stack,fire);test.assertTrue(focus.meltItem(player,stack,wand,item)&&item.getItem().is(Items.IRON_INGOT)&&item.getItem().getCount()==12&&wand.vis(stack,fire)<before,"Whole stack converts once for a paid operation");before=wand.vis(stack,fire);test.assertTrue(!focus.meltItem(player,stack,wand,item)&&wand.vis(stack,fire)==before,"Unsmeltable result cannot duplicate or charge again");item.discard();test.succeed();
 }
}
