package thmodern.plane;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class CloudServerTests {
 @GameTest(template="empty",batch="th_cloud")
 public static void serverRainProducesFoodAndPersistsCountdown(GameTestHelper test){
  var level=test.getLevel();var overworld=level.getServer().overworld();float before=overworld.getRainLevel(1);
  var pos=test.absolutePos(new BlockPos(2,8,2));
  try{
   overworld.setRainLevel(1);level.setBlockAndUpdate(pos,CloudBlocks.BLOCKS.get("flesh_cloud").get().defaultBlockState());
   var cloud=(CloudEntity)level.getBlockEntity(pos);var tag=new CompoundTag();tag.putInt("Remaining",1);cloud.load(tag);cloud.tick();
   var items=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1));test.assertTrue(items.size()==1,"Actual server rain produces exactly one food item");test.assertTrue(items.get(0).isInvulnerable(),"Cloud food resists rain hazards");
   int remaining=cloud.remaining();test.assertTrue(remaining>=240&&remaining<480,"Post-drop interval follows legacy range");
   var saved=cloud.saveWithoutMetadata();var restored=new CloudEntity(pos,cloud.getBlockState());restored.setLevel(level);restored.load(saved);restored.tick();test.assertTrue(restored.remaining()==remaining-1,"Reload preserves the active countdown");
   overworld.setRainLevel(0);var dry=new CloudEntity(pos,cloud.getBlockState());dry.setLevel(level);dry.load(saved);dry.tick();test.assertTrue(dry.remaining()==remaining,"Dry weather pauses output");
   items.forEach(ItemEntity::discard);
  }finally{overworld.setRainLevel(before);level.removeBlock(pos,false);}
  test.succeed();
 }
 @GameTest(template="empty",batch="th_cloud")
 public static void rainChangesTheActualGroundUnderCloud(GameTestHelper test){
  var level=test.getLevel();var overworld=level.getServer().overworld();float before=overworld.getRainLevel(1);var pos=test.absolutePos(new BlockPos(2,8,2));var ground=pos.below(5);
  try{
   overworld.setRainLevel(1);level.setBlockAndUpdate(ground,Blocks.GRASS_BLOCK.defaultBlockState());level.setBlockAndUpdate(pos,CloudBlocks.BLOCKS.get("acid_cloud").get().defaultBlockState());
   ((CloudEntity)level.getBlockEntity(pos)).tick();test.assertTrue(level.getBlockState(ground).is(Blocks.DIRT),"Acid rain strips grass at its raycast ground");
   level.setBlockAndUpdate(pos,CloudBlocks.BLOCKS.get("fire_cloud").get().defaultBlockState());level.setBlockAndUpdate(ground,Blocks.PACKED_ICE.defaultBlockState());((CloudEntity)level.getBlockEntity(pos)).tick();
   test.assertTrue(level.getBlockState(ground).is(Blocks.WATER),"Fire rain melts ice");
  }finally{overworld.setRainLevel(before);level.removeBlock(pos,false);level.removeBlock(ground,false);}
  test.succeed();
 }
 @GameTest(template="empty",batch="th_cloud")
 public static void animusCreatesAnOwnerlessTemporaryGolem(GameTestHelper test){
  var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,3,2));level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
  test.assertTrue(thmodern.golem.AnimatedGolems.animateNaturally(level,pos,1200),"Animus can animate without an online player");
  var golems=level.getEntitiesOfClass(tc4revival.content.GolemEntity.class,new AABB(pos).inflate(2));test.assertTrue(golems.size()==1&&level.isEmptyBlock(pos),"One golem replaces one source block");
  var data=golems.get(0).configuration().addonData(thmodern.golem.AnimatedGolems.DATA);test.assertTrue(data.getBoolean("Berserk")&&!data.hasUUID("Owner")&&data.getInt("Remaining")==1200,"No fabricated owner; temporary berserk behavior");
  golems.get(0).discard();level.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());test.assertTrue(!thmodern.golem.AnimatedGolems.animateNaturally(level,pos,1200)&&level.getBlockEntity(pos)!=null,"Container cannot be animated");level.removeBlock(pos,false);test.succeed();
 }
 @GameTest(template="empty",batch="th_cloud")
 public static void alloyFoodRecipesAndCloudRegistrationsAreUsable(GameTestHelper test){
  test.assertTrue(CloudDrops.alloy(0).is(Items.GOLD_NUGGET)&&CloudDrops.alloy(74).is(Items.IRON_NUGGET),"Vanilla metal products reused");
  test.assertTrue(CloudDrops.alloy(40).is(tc4revival.content.TCItems.nugget(tc4revival.content.NuggetType.QUICKSILVER)),"Legacy nugget metadata five is mercury");
  for(var type:CloudTypes.ALL){var block=CloudBlocks.BLOCKS.get(type.id()).get();var state=block.defaultBlockState();test.assertTrue(state.getCollisionShape(test.getLevel(),test.absolutePos(BlockPos.ZERO)).isEmpty(),"Cloud is permeable: "+type.id());test.assertTrue(state.getLightEmission()==(type.glowing()?15:0),"Correct light for "+type.id());}
  var inventory=new net.minecraft.world.SimpleContainer(new ItemStack(CloudBlocks.MEAT.get()));
  var recipe=test.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,inventory,test.getLevel());
  test.assertTrue(recipe.isPresent()&&recipe.get().getResultItem(test.getLevel().registryAccess()).is(CloudBlocks.COOKED_MEAT.get()),"Cloud meat cooks through the real recipe manager");
  test.assertTrue(tc4revival.content.InfernalBonusRecipe.find(test.getLevel(),inventory.getItem(0)).orElseThrow().getResultItem(test.getLevel().registryAccess()).is(CloudBlocks.MEAT_NUGGET.get()),"Infernal bonus uses the own nugget");test.succeed();
 }
 @GameTest(template="empty",batch="th_cloud")
 public static void wandCollectionRequiresResearchAndPaysOnce(GameTestHelper test){
  var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,3,2));var block=(CloudBlock)CloudBlocks.BLOCKS.get("vis_cloud").get();level.setBlockAndUpdate(pos,block.defaultBlockState());
  var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CloudCollector"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(pos.getCenter());
  var stack=thaumcraft.api.ThaumcraftApi.createWand("greatwood","gold",false,true);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
  var wand=(tc4revival.content.CastingWandItem)stack.getItem();var air=tc4revival.aspect.TCAspects.catalog().require("aer");int before=wand.vis(stack,air);
  var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false));
  block.onWandUseFirst(stack,context);test.assertTrue(level.getBlockState(pos).is(block)&&wand.vis(stack,air)==before,"Unresearched player neither harvests nor pays");
  thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,"planarClouds");block.onWandUseFirst(stack,context);
  test.assertTrue(level.isEmptyBlock(pos)&&wand.vis(stack,air)<before,"Researched harvest removes the cloud and pays air vis");
  int after=wand.vis(stack,air);block.onWandUseFirst(stack,context);test.assertTrue(wand.vis(stack,air)==after,"Stale second harvest cannot charge again");
  var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1),e->e.getItem().is(block.asItem()));test.assertTrue(drops.size()==1&&drops.get(0).isInvulnerable(),"One protected cloud item returned");drops.forEach(ItemEntity::discard);test.succeed();
 }
}
