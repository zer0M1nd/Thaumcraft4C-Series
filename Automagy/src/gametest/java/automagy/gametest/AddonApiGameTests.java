package automagy.gametest;

import automagy.content.*;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("automagy")
@PrefixGameTestTemplate(false)
public class AddonApiGameTests {
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=40)
 public static void placedMilkActuallySpreadsOnServerTicks(GameTestHelper h){
  for(int x=0;x<7;x++)for(int z=0;z<7;z++)h.setBlock(new BlockPos(x,1,z),net.minecraft.world.level.block.Blocks.STONE);
  h.setBlock(new BlockPos(3,2,3),SoupFluids.MILK_BLOCK.get());
  h.runAtTickTime(20,()->{
   h.assertTrue(h.getBlockState(new BlockPos(3,2,3)).is(SoupFluids.MILK_BLOCK.get()),"Milk source vanished");
   h.assertTrue(h.getBlockState(new BlockPos(4,2,3)).is(SoupFluids.MILK_BLOCK.get()),"Milk never flowed to adjacent cell");h.succeed();
  });
 }
 @GameTest(template="empty",templateNamespace="automagy")
 public static void forgeMilkCanBePlacedFlowAndPickedUp(GameTestHelper h){
  var fluid=net.minecraftforge.common.ForgeMod.MILK.get();var pos=h.absolutePos(new BlockPos(2,2,2));
  var result=net.minecraftforge.fluids.FluidUtil.tryPlaceFluid(null,h.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,pos,new ItemStack(Items.MILK_BUCKET),new net.minecraftforge.fluids.FluidStack(fluid,1000));
  h.assertTrue(result.isSuccess()&&result.getResult().is(Items.BUCKET),"Milk bucket failed to place/empty");
  h.assertTrue(h.getLevel().getBlockState(pos).is(SoupFluids.MILK_BLOCK.get())&&h.getLevel().getFluidState(pos).isSource(),"Milk source not represented in world");
  var flowing=((net.minecraft.world.level.material.FlowingFluid)fluid).getFlowing(5,false);
  h.assertTrue(flowing.createLegacyBlock().getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL)==3,"Flowing milk level mapping wrong");
  var pickup=SoupFluids.MILK_BLOCK.get().pickupBlock(h.getLevel(),pos,h.getLevel().getBlockState(pos));
  h.assertTrue(pickup.is(Items.MILK_BUCKET)&&h.getLevel().isEmptyBlock(pos),"Milk pickup lost/duplicated fluid");h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy")
 public static void arcaneFilterConversionPreservesNbtWithoutMutatingInput(GameTestHelper h){
  var menu=new AbstractContainerMenu(null,0){public boolean stillValid(Player p){return true;}public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}};
  var grid=new TransientCraftingContainer(menu,3,3);
  var input=new ItemStack(Content.WHITE_FILTER.get());var filter=ItemFilter.load(input);filter.entry(0,new ItemStack(Items.DIAMOND));
  input.getOrCreateTag().put("Filter",filter.save());grid.setItem(0,input);var before=input.copy();
  var config=new JsonObject();config.addProperty("type","automagy:preserve_filter");
  var output=thaumcraft.api.crafting.ArcaneResultTransforms.apply(config,grid,h.getLevel().registryAccess(),new ItemStack(Content.BLACK_FILTER.get()));
  h.assertTrue(output.is(Content.BLACK_FILTER.get())&&ItemFilter.load(output).entry(0).is(Items.DIAMOND),"Conversion lost filter contents");
  h.assertTrue(ItemStack.matches(input,before),"Crafting mutated input");h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy")
 public static void mercatusTradeValueRegistered(GameTestHelper h){
  h.assertTrue(thaumcraft.api.entities.PechTradeValues.find(new ItemStack(Content.MERCATUS_AMALGAM.get())).orElse(0)==7,"Mercatus trade integration missing");h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy")
 public static void brainStoneBalancesBothDirectionsWithoutLosingExperience(GameTestHelper h){
  var pos=new BlockPos(2,2,2);h.setBlock(pos,Content.AMNESIAC_STONE.get());h.setBlock(pos.east(),Content.EXPERIENCE_STONE.get());
  h.setBlock(pos.above(),tc4revival.content.TCArcane.BRAIN_JAR.get());
  var amnesiac=(AmnesiacStoneBlockEntity)h.getBlockEntity(pos);var stone=(ExperienceStoneBlockEntity)h.getBlockEntity(pos.east());
  var brain=(tc4revival.content.BrainJarBlockEntity)h.getBlockEntity(pos.above());brain.insertExperience(80,false);
  for(int i=0;i<8;i++)amnesiac.balance(stone);
  h.assertTrue(brain.experience()==40&&stone.experience()==40,"Brain-to-stone balance incorrect");
  brain.extractExperience(40,false);for(int i=0;i<8;i++)amnesiac.balance(stone);
  h.assertTrue(brain.experience()==20&&stone.experience()==20,"Stone-to-brain balance incorrect");h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy")
 public static void mirrorOutputSimulationAndPersistence(GameTestHelper h){
  var pos=new BlockPos(2,2,2);h.setBlock(pos,Content.MELDING_MIRROR.get());var mirror=(DeliveryMirrorBlockEntity)h.getBlockEntity(pos);
  var offered=new ItemStack(Items.DIAMOND,3);h.assertTrue(mirror.insertOutput(offered,true).isEmpty(),"Simulation rejected output");
  h.assertTrue(mirror.saveWithoutMetadata().getList("Output",Tag.TAG_COMPOUND).isEmpty(),"Simulation mutated queue");
  mirror.insertOutput(offered,false);var saved=mirror.saveWithoutMetadata();mirror.load(saved);
  h.assertTrue(ItemStack.of(mirror.saveWithoutMetadata().getList("Output",Tag.TAG_COMPOUND).getCompound(0)).getCount()==3&&offered.getCount()==3,"Persistence lost/aliased cargo");
  DeliveryMirrorBlockEntity.tick(h.getLevel(),mirror.getBlockPos(),mirror.getBlockState(),mirror);
  h.assertTrue(ItemStack.of(mirror.saveWithoutMetadata().getList("Output",Tag.TAG_COMPOUND).getCompound(0)).getCount()==2,"Output did not emit exactly one item");h.succeed();
 }
}
