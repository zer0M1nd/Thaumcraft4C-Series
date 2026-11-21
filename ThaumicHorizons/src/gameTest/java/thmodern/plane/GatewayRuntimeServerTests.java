package thmodern.plane;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class GatewayRuntimeServerTests {
 @GameTest(template="empty")
 public static void gatewayPermissionAndDistanceRejectWithoutPayment(GameTestHelper test){
  var s=setup(test);var before=s.wand.copy();var originalPosition=s.player.position();
  s.player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
  test.assertTrue(!s.slot.open(s.player,s.wand)&&s.slot.link()==null&&ItemStack.matches(before,s.wand),"Adventure player cannot open or pay for a gateway");
  s.player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);s.player.setPos(originalPosition.add(30,0,0));
  test.assertTrue(!s.slot.open(s.player,s.wand)&&s.slot.link()==null&&ItemStack.matches(before,s.wand),"Out-of-range interaction cannot open or debit a gateway");
  s.player.setPos(originalPosition);
  test.assertTrue(s.slot.open(s.player,s.wand),"Returning with build permission permits the same valid gateway");s.slot.close();test.succeed();
 }
 @GameTest(template="empty")
 public static void targetProtectionCancellationRestoresBothWorldsAndVis(GameTestHelper test){
  var s=setup(test);var source=test.getLevel();var target=source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,PocketPlanes.DIMENSION));
  var before=s.wand.copy();var targetStates=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
  var outward=GatewayLayout.outward(0);
  for(var mouth:GatewayLayout.mouth(s.plane,0)){
   targetStates.put(mouth,target.getBlockState(mouth));targetStates.put(mouth.relative(outward),target.getBlockState(mouth.relative(outward)));
   for(int depth=1;depth<=3;depth++){var pos=mouth.relative(outward.getOpposite(),depth);targetStates.put(pos,target.getBlockState(pos));if(mouth.getY()==126)targetStates.put(pos.below(),target.getBlockState(pos.below()));}
  }
  int[] denied={0};java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent> listener=event->{if(event.getEntity()==s.player&&event.getLevel()==target){event.setCanceled(true);denied[0]++;}};
  net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(listener);
  try{test.assertTrue(!s.slot.open(s.player,s.wand),"Destination protection cancels the cross-dimensional transaction");}
  finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(listener);}
  test.assertTrue(denied[0]>0&&s.slot.link()==null&&ItemStack.matches(before,s.wand),"Protection was reached and neither link nor vis was committed");
  test.assertTrue(GatewayLayout.matches(source,s.slot.getBlockPos(),true),"Source frame and opening were restored");
  targetStates.forEach((pos,state)->test.assertTrue(target.getBlockState(pos).equals(state),"Destination rollback restores "+pos));
  test.assertTrue(GatewayLinks.get(source.getServer()).at(s.plane.id(),s.plane.allocation(),0).isEmpty(),"Cancelled transaction releases its reserved side");
  test.assertTrue(s.slot.open(s.player,s.wand),"Removing protection permits a subsequent retry without stale reservation");s.slot.close();test.succeed();
 }
 private record Setup(KeystoneSlotEntity slot,FakePlayer player,ItemStack wand,PocketPlanes.Plane plane){}
 private static Setup setup(GameTestHelper test){var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,5,3));level.setBlockAndUpdate(pos,GatewayBlocks.SLOT.get().defaultBlockState());for(var cell:GatewayLayout.cells())level.setBlockAndUpdate(cell.at(pos,true),ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(cell.material())).defaultBlockState());var data=PocketPlanes.get(level.getServer());var plane=data.reserve(UUID.randomUUID(),"Gateway fixture",Map.of(),0,1,new PocketPlanes.Origin(level.dimension().location(),pos));data.progress(plane.id(),"ready",0,true);plane=data.get(plane.id()).orElseThrow();var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GatewayTest"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(pos.below(2).getCenter());var key=new ItemStack(Horizons.KEYSTONE.get());KeystoneItem.bind(key,plane);player.setItemInHand(InteractionHand.MAIN_HAND,key);var slot=(KeystoneSlotEntity)level.getBlockEntity(pos);test.assertTrue(slot.interact(player,InteractionHand.MAIN_HAND),"Bound keystone inserts");var wand=thaumcraft.api.ThaumcraftApi.createWand("primal_staff","thaumium",false,true);player.setItemInHand(InteractionHand.MAIN_HAND,wand);return new Setup(slot,player,wand,plane);}
 @GameTest(template="empty")
 public static void openingPaysOnceAndClosingRestoresBoundary(GameTestHelper test){var s=setup(test);var wand=(tc4revival.content.CastingWandItem)s.wand.getItem();var air=tc4revival.aspect.TCAspects.catalog().require("aer");int before=wand.vis(s.wand,air);test.assertTrue(s.slot.open(s.player,s.wand),"Valid paid gateway opens");var id=s.slot.link();test.assertTrue(id!=null&&wand.vis(s.wand,air)<before,"Opening binds a side and charges vis");int paid=wand.vis(s.wand,air);test.assertTrue(!s.slot.open(s.player,s.wand)&&wand.vis(s.wand,air)==paid,"Open gateway cannot charge again");var link=GatewayLinks.get(test.getLevel().getServer()).get(id).orElseThrow();var pocket=test.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,PocketPlanes.DIMENSION));s.slot.close();test.assertTrue(GatewayLinks.get(test.getLevel().getServer()).get(id).isEmpty(),"Close releases slot");for(var at:GatewayLayout.mouth(s.plane,link.side()))test.assertTrue(pocket.getBlockState(at).is(PlanarBlocks.VOID.get()),"Close seals all nine realm portal cells");test.assertTrue(GatewayLayout.matches(test.getLevel(),s.slot.getBlockPos(),true),"Original source frame remains intact");test.succeed();}
 @GameTest(template="empty")
 public static void occupiedDestinationRollsBackWithoutPayment(GameTestHelper test){var s=setup(test);var pocket=test.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,PocketPlanes.DIMENSION));var at=GatewayLayout.mouth(s.plane,0).get(0);pocket.setBlockAndUpdate(at,Blocks.CHEST.defaultBlockState());var wand=(tc4revival.content.CastingWandItem)s.wand.getItem();var air=tc4revival.aspect.TCAspects.catalog().require("aer");int before=wand.vis(s.wand,air);test.assertTrue(!s.slot.open(s.player,s.wand)&&s.slot.link()==null&&wand.vis(s.wand,air)==before,"Occupied destination rejects without charging");test.assertTrue(pocket.getBlockState(at).is(Blocks.CHEST)&&GatewayLayout.matches(test.getLevel(),s.slot.getBlockPos(),true),"Rollback preserves destination container and original source opening");test.assertTrue(GatewayLinks.get(test.getLevel().getServer()).at(s.plane.id(),s.plane.allocation(),0).isEmpty(),"Failed opening releases reservation");pocket.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());test.succeed();}
}
