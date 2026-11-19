package automagy.gametest;

import automagy.content.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.*;
import tc4revival.content.GolemEntity;

@GameTestHolder("automagy")
@PrefixGameTestTemplate(false)
public final class CourierGameTests {
 private record Fixture(InventariumBlockEntity core,ChestBlockEntity source,ChestBlockEntity destination,GolemEntity golem,long claim,GlobalPos requester){}
 private static Fixture prepare(GameTestHelper h){
  var center=new BlockPos(3,4,3);h.setBlock(center.below(2),Content.GLOWING_AMBER.get());
  h.setBlock(center.below(),net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("thaumcraft:order_crystal_cluster")));
  int[][] shelves={{-2,-1},{-2,1},{2,-1},{2,1},{-1,-2},{1,-2},{-1,2},{1,2}};
  for(var offset:shelves)h.setBlock(center.offset(offset[0],-1,offset[1]),Content.GREATWOOD_BOOKSHELF.get());
  h.setBlock(center,Content.INVENTARIUM.get());var level=h.getLevel();var absolute=h.absolutePos(center);
  var core=(InventariumBlockEntity)level.getBlockEntity(absolute);InventariumBlockEntity.tick(level,absolute,level.getBlockState(absolute),core);
  h.assertTrue(core.capacity().loaded()&&core.capacity().slots()>0,"Real Inventarium structure did not validate");
  var from=new BlockPos(6,2,3);var to=new BlockPos(0,1,0);var scribePos=new BlockPos(6,2,5);
  h.setBlock(from,Blocks.CHEST);h.setBlock(to,Blocks.CHEST);
  h.setBlock(scribePos,Content.SCRIBE.get().defaultBlockState().setValue(UnseenScribeBlock.FACING,Direction.NORTH));
  var scribe=(UnseenScribeBlockEntity)level.getBlockEntity(h.absolutePos(scribePos));
  h.assertTrue(core.link(scribe.getBlockPos()),"Core rejected scribe link");
  // Saved vis buffer fixture; this scenario tests physical delivery, not vis-network charging.
  var powered=scribe.saveWithoutMetadata();powered.putInt("PoweredTicks",100);scribe.load(powered);
  var source=(ChestBlockEntity)level.getBlockEntity(h.absolutePos(from));var destination=(ChestBlockEntity)level.getBlockEntity(h.absolutePos(to));
  source.setItem(0,new ItemStack(Items.DIAMOND,3));
  var requested=new ItemStack(Items.DIAMOND);var requester=GlobalPos.of(level.dimension(),destination.getBlockPos());core.requests().request(requested,requester,1);
  var claim=core.requests().claim(new RequestedItem(requested),requester,1).orElseThrow();
  var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse("thaumcraft:golem"));
  var golem=(GolemEntity)java.util.Objects.requireNonNull(type).create(level);java.util.Objects.requireNonNull(golem);
  // The placer stores the standing cell, one block outside the clicked inventory face.
  golem.state().home(destination.getBlockPos().above(),Direction.UP);
  h.assertTrue(InventariumDeliveryGoal.assign(golem,core,claim.id(),source.getBlockPos(),null,27),"Courier assignment failed");
  h.assertTrue(net.minecraft.nbt.NbtUtils.readBlockPos(golem.configuration().addonData(InventariumDeliveryGoal.ID).getCompound("Destination")).equals(destination.getBlockPos()),"Courier must target the home inventory, not the standing cell");
  return new Fixture(core,source,destination,golem,claim.id(),requester);
 }
 private static void near(GolemEntity golem,BlockPos pos){golem.moveTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,0,0);}
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void linkerBindsThroughPlayerInteractionBeforeTheGolemMenu(GameTestHelper h){
  var f=prepare(h);var level=h.getLevel();
  var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CourierLinkTest"));
  try{
   f.core.requests().fail(f.claim);
   thaumcraft.api.golems.GolemApi.data(f.golem,InventariumDeliveryGoal.ID,new CompoundTag());
   thaumcraft.api.golems.GolemApi.configure(f.golem,null,null,null);
   var configTag=new CompoundTag();f.golem.configuration().save(configTag);configTag.putByte("Core",(byte)0);
   f.golem.state().configure(tc4revival.content.GolemConfiguration.load(configTag));f.golem.refreshConfiguration();
   var masterPos=new BlockPos(1,2,2);h.setBlock(masterPos,Content.TASKMASTER.get());
   var master=(GolemTaskmasterBlockEntity)level.getBlockEntity(h.absolutePos(masterPos));
   var linker=new ItemStack(Content.GOLEM_LINKER.get());var link=linker.getOrCreateTag();
   link.putUUID("Device",master.linkIdentity());link.put("Position",net.minecraft.nbt.NbtUtils.writeBlockPos(master.getBlockPos()));link.putString("Dimension",level.dimension().location().toString());
   player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,linker);
   var originalMenu=player.containerMenu;
   f.golem.state().owner("AnotherOwner");
   player.interactOn(f.golem,net.minecraft.world.InteractionHand.MAIN_HAND);
   h.assertTrue(!GolemLinkerItem.linked(f.golem,master,master)&&player.containerMenu==originalMenu,"Non-owner interaction linked or opened the golem menu");
   f.golem.state().owner(player.getGameProfile().getName());player.setShiftKeyDown(true);
   player.interactOn(f.golem,net.minecraft.world.InteractionHand.MAIN_HAND);
   h.assertTrue(!GolemLinkerItem.linked(f.golem,master,master),"Sneaking unexpectedly linked the golem");
   player.setShiftKeyDown(false);player.interactOn(f.golem,net.minecraft.world.InteractionHand.MAIN_HAND);
   h.assertTrue(GolemLinkerItem.linked(f.golem,master,master),"Normal player interaction was consumed before the linker bound the golem");
   h.assertTrue(player.containerMenu==originalMenu&&!f.golem.isNoAi(),"Linking opened the golem menu or paused its AI");
   h.assertTrue(player.getMainHandItem().getCount()==1&&player.getMainHandItem().getTag().equals(link),"Linking changed the held linker");
   var saved=new CompoundTag();f.golem.addAdditionalSaveData(saved);f.golem.readAdditionalSaveData(saved);
   h.assertTrue(GolemLinkerItem.linked(f.golem,master,master),"Saved golem lost the device link");
  }finally{player.discard();f.golem.discard();}h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void taskmasterDispatchesPlacedGolemToItsHomeInventory(GameTestHelper h){
  var f=prepare(h);var level=h.getLevel();
  try{
   // Release the direct fixture assignment so the real taskmaster must dispatch the request.
   f.core.requests().fail(f.claim);
   thaumcraft.api.golems.GolemApi.data(f.golem,InventariumDeliveryGoal.ID,new CompoundTag());
   thaumcraft.api.golems.GolemApi.configure(f.golem,null,null,null);
   var config=new tc4revival.content.GolemConfiguration(tc4revival.content.GolemMaterial.WOOD,false);
   var configTag=new CompoundTag();config.save(configTag);configTag.putByte("Core",(byte)0);
   f.golem.state().configure(tc4revival.content.GolemConfiguration.load(configTag));f.golem.refreshConfiguration();
   h.setBlock(new BlockPos(4,2,3),Content.AMBER_MEMORY.get());
   var masterPos=new BlockPos(1,2,2);h.setBlock(masterPos,Content.TASKMASTER.get());
   var master=(GolemTaskmasterBlockEntity)level.getBlockEntity(h.absolutePos(masterPos));
   // Force the structure to revalidate the added memory block without changing requests.
   f.core.load(f.core.saveWithoutMetadata());InventariumBlockEntity.tick(level,f.core.getBlockPos(),f.core.getBlockState(),f.core);
   h.assertTrue(f.core.linkMemory(master.getBlockPos()),"Taskmaster memory link failed");
   var link=new CompoundTag();link.putUUID("Device",master.linkIdentity());link.put("Position",net.minecraft.nbt.NbtUtils.writeBlockPos(master.getBlockPos()));link.putString("Dimension",level.dimension().location().toString());
   thaumcraft.api.golems.GolemApi.data(f.golem,GolemLinkerItem.LINK,link);
   near(f.golem,f.destination.getBlockPos().above());level.addFreshEntity(f.golem);
   GolemTaskmasterBlockEntity.tick(level,master.getBlockPos(),master.getBlockState(),master);
   var job=f.golem.configuration().addonData(InventariumDeliveryGoal.ID);
   h.assertTrue(!job.isEmpty(),"Taskmaster ignored naturally placed golem because its standing cell is not an inventory");
   h.assertTrue(net.minecraft.nbt.NbtUtils.readBlockPos(job.getCompound("Destination")).equals(f.destination.getBlockPos()),"Taskmaster dispatched to the wrong home cell");
  }finally{f.golem.discard();}h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void physicalCourierPersistsReservationAndDeliversExactlyOnce(GameTestHelper h){
  var f=prepare(h);
  try{
   var savedCore=f.core.saveWithoutMetadata();var savedGolem=new CompoundTag();f.golem.addAdditionalSaveData(savedGolem);
   f.core.load(savedCore);f.golem.readAdditionalSaveData(savedGolem);
   InventariumBlockEntity.tick(h.getLevel(),f.core.getBlockPos(),f.core.getBlockState(),f.core);
   h.assertTrue(f.core.requests().owned(f.claim,f.golem.getUUID()),"Reload lost claim ownership");
   var goal=new InventariumDeliveryGoal(f.golem);goal.start();near(f.golem,f.source.getBlockPos());goal.tick();
   h.assertTrue(f.source.getItem(0).getCount()==2&&f.golem.carried().getCount()==1&&f.destination.isEmpty(),"Pickup was not a physical, one-item transfer");
   near(f.golem,f.destination.getBlockPos());goal.tick();goal.tick();
   h.assertTrue(f.destination.countItem(Items.DIAMOND)==1&&f.golem.carried().isEmpty(),"Delivery duplicated/lost cargo");
   h.assertTrue(f.core.requests().claims().isEmpty()&&f.core.requests().requests().isEmpty(),"Delivery left outstanding reservation");
  }finally{f.golem.discard();}h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void cancelledRequestReturnsPickedUpCargoToSource(GameTestHelper h){
  var f=prepare(h);
  try{
   var goal=new InventariumDeliveryGoal(f.golem);goal.start();near(f.golem,f.source.getBlockPos());goal.tick();
   h.assertTrue(f.golem.carried().getCount()==1,"Courier did not pick up reservation");
   f.core.requests().cancelDestination(f.requester);goal.tick();goal.tick();
   h.assertTrue(f.source.countItem(Items.DIAMOND)==3&&f.destination.isEmpty()&&f.golem.carried().isEmpty(),"Cancellation lost cargo or delivered cancelled request");
   h.assertTrue(f.core.requests().claims().isEmpty()&&f.golem.configuration().addonData(InventariumDeliveryGoal.ID).isEmpty(),"Cancellation retained job/reservation");
  }finally{f.golem.discard();}h.succeed();
 }
}
