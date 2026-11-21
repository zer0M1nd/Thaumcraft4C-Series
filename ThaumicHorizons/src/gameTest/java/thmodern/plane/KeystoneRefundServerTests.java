package thmodern.plane;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class KeystoneRefundServerTests {
 @GameTest(template="empty")
 public static void firstPlaneBindsWithoutOverwritingUnrelatedItemData(GameTestHelper test){
  var plan=new PocketPlanes.Plane(0,UUID.randomUUID(),"First Realm",Map.of(),0xabcdef,1,new PocketPlanes.Origin(test.getLevel().dimension().location(),BlockPos.ZERO),"ready",0,true);var stack=new ItemStack(Horizons.KEYSTONE.get());stack.getOrCreateTag().putString("CustomAddon","preserved");
  test.assertTrue(KeystoneItem.bind(stack,plan)&&KeystoneItem.bound(stack),"Plane zero is a valid bound key");test.assertTrue(KeystoneItem.color(stack)==0xabcdef&&stack.getTag().getString("CustomAddon").equals("preserved"),"Binding preserves unrelated data and carries display color");test.assertTrue(!KeystoneItem.bind(stack,plan),"A bound key is not silently overwritten");
  var restored=ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));test.assertTrue(KeystoneItem.bound(restored)&&KeystoneItem.resolve(restored,test.getLevel()).isEmpty(),"Key survives save but rejects absent/stale server allocation");test.succeed();
 }
 @GameTest(template="empty")
 public static void pendingRefundSurvivesSaveAndDeliversOnlyOnce(GameTestHelper test){
  var data=new PlaneRefunds();var request=UUID.randomUUID();var pos=test.absolutePos(new BlockPos(2,3,2));var stack=new ItemStack(tc4revival.content.TCItems.PRIMORDIAL_PEARL.get());stack.setHoverName(net.minecraft.network.chat.Component.literal("Preserved pearl"));
  data.enqueue(request,test.getLevel().dimension().location(),pos,stack);data.enqueue(request,test.getLevel().dimension().location(),pos,stack);var restored=PlaneRefunds.load(data.save(new net.minecraft.nbt.CompoundTag()));test.assertTrue(restored.pendingCount()==1,"Repeated removal request remains one persisted refund");restored.deliver(test.getLevel().getServer());restored.deliver(test.getLevel().getServer());
  UUID entityId=UUID.nameUUIDFromBytes((request+":pearl-refund").getBytes(java.nio.charset.StandardCharsets.UTF_8));test.assertTrue(test.getLevel().getEntity(entityId) instanceof net.minecraft.world.entity.item.ItemEntity item&&item.getItem().getHoverName().getString().equals("Preserved pearl")&&restored.pendingCount()==0,"Delivery preserves original pearl and clears outbox");test.getLevel().getEntity(entityId).discard();test.succeed();
 }
}
