package thmodern.plane;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
import tc4revival.content.TCItems;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class PlaneCreationServerTests {
 @GameTest(template="empty",timeoutTicks=1200)
 public static void pearlGenerationCompletesOnlyAfterRealTerrainAndReturnPortal(GameTestHelper test){
  var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,3,2));level.setBlockAndUpdate(pos,Horizons.VORTEX.get().defaultBlockState());var source=(VortexEntity)level.getBlockEntity(pos);source.initialize(new tc4revival.aspect.AspectAmounts(),true);for(int i=0;i<50;i++)source.tick();
  var stack=new ItemStack(TCItems.PRIMORDIAL_PEARL.get(),2);stack.setHoverName(Component.literal("Named Test Plane"));var input=new ItemEntity(level,pos.getX(),pos.getY(),pos.getZ(),stack);level.addFreshEntity(input);
  test.assertTrue(source.convert(input)&&input.getItem().getCount()==1&&source.hasEscrow(),"One pearl is escrowed while extra input survives");input.discard();int id=source.planeId();var data=PocketPlanes.get(level.getServer());var allocated=data.get(id).orElseThrow();test.assertTrue(!allocated.ready()&&allocated.name().equals("Named Test Plane"),"Reservation retains name and starts unfinished");
  var saved=new VortexEntity(pos,source.getBlockState());saved.load(source.saveWithoutMetadata());test.assertTrue(saved.hasEscrow()&&saved.planeId()==id&&saved.allocation().equals(source.allocation()),"Rift escrow/link survive NBT save");
  test.succeedWhen(()->{var plane=data.get(id).orElseThrow();test.assertTrue(plane.ready(),"Full scheduled generation has reached ready");var pocket=level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,PocketPlanes.DIMENSION));test.assertTrue(pocket.getBlockEntity(plane.center().above()) instanceof VortexEntity arrival&&plane.allocation().equals(arrival.allocation()),"Generated return rift matches allocation");test.assertTrue(pocket.getBlockState(plane.center().offset(32,0,0)).is(PlanarBlocks.VOID.get()),"Outer boundary actually exists");test.assertTrue(!source.hasEscrow(),"Pearl commits only when ready");});
 }
 @GameTest(template="empty")
 public static void failedGenerationReturnsEscrowAndCancelsPendingPlane(GameTestHelper test){
  var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,3,2));level.setBlockAndUpdate(pos,Horizons.VORTEX.get().defaultBlockState());var source=(VortexEntity)level.getBlockEntity(pos);source.initialize(new tc4revival.aspect.AspectAmounts(),true);for(int i=0;i<50;i++)source.tick();var item=new ItemEntity(level,pos.getX(),pos.getY(),pos.getZ(),new ItemStack(TCItems.PRIMORDIAL_PEARL.get()));level.addFreshEntity(item);test.assertTrue(source.convert(item),"Creation accepts pearl");int id=source.planeId();var data=PocketPlanes.get(level.getServer());data.progress(id,"failed/layers",1,false);
  var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PlaneRefund"));player.setPos(pos.getCenter());test.assertTrue(source.extract(player),"Wand extraction refunds failed creation");test.assertTrue(!source.hasEscrow()&&source.planeId()==-1&&data.get(id).orElseThrow().stage().equals("cancelled"),"Refund cancels allocation before another request");test.assertTrue(!source.extract(player),"Refund cannot repeat");test.succeed();
 }
}
