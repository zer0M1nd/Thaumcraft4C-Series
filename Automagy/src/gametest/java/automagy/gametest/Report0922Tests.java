package automagy.gametest;
import automagy.content.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.ClickType;
@GameTestHolder("automagy") @PrefixGameTestTemplate(false)
public class Report0922Tests {
 private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper h){var p=new net.minecraftforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AM0922"));p.moveTo(h.absolutePos(new BlockPos(2,2,2)).getCenter());return p;}
 @GameTest(template="empty",templateNamespace="automagy") public static void restoredChestModesHideAndProtectOppositeSlots(GameTestHelper h){
  var pos=new BlockPos(2,2,2);h.setBlock(pos,Content.EAGER_CHEST.get());var tile=(EagerChestBlockEntity)h.getBlockEntity(pos);var p=player(h);
  var storage=new EagerChestMenu(1,p.getInventory(),tile,false);var config=new EagerChestMenu(2,p.getInventory(),tile,true);
  h.assertTrue(storage.getSlot(0).isActive()&&!storage.getSlot(27).isActive()&&!storage.getSlot(27).mayPickup(p),"Storage exposes items, hides filters");
  h.assertTrue(!config.getSlot(0).isActive()&&!config.getSlot(0).mayPlace(new ItemStack(Items.STONE))&&config.getSlot(27).isActive(),"Configuration hides stored items and exposes filters");storage.removed(p);config.removed(p);p.discard();h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy") public static void mirrorSettingsUseGhostEyesWithoutConsumptionOrDuplication(GameTestHelper h){
  var pos=new BlockPos(2,2,2);h.setBlock(pos,Content.REQUISITION_TOME.get());var tome=(RequisitionTomeBlockEntity)h.getBlockEntity(pos);var p=player(h);var menu=new MirrorSettingsMenu(1,p.getInventory(),h.absolutePos(pos));var eyes=new ItemStack(Content.CRYSTAL_EYE.get(),4);menu.setCarried(eyes);
  h.assertTrue(!menu.stillValid(p),"Unpowered mirror GUI must reject access");tome.configureEye(0,eyes,p);menu.broadcastChanges();h.assertTrue(menu.getCarried().getCount()==4&&tome.mirrorEye(0).getCount()==1,"Ghost selection does not consume the held stack");
  tome.configureEye(0,ItemStack.EMPTY,p);h.assertTrue(tome.mirrorEye(0).isEmpty()&&p.getInventory().countItem(Content.CRYSTAL_EYE.get())==0,"Clearing a ghost eye cannot create an item");
  tome.configureEye(1,eyes,p);tome.load(tome.saveWithoutMetadata());tome.removeMirrors(p);h.assertTrue(p.getInventory().countItem(Content.CRYSTAL_EYE.get())==0,"Saved ghost references never become physical drops");p.discard();h.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy") public static void scribeRestoresSixtyEntriesFifteenRequestsAndRealInventory(GameTestHelper h){
  var pos=new BlockPos(2,2,2);h.setBlock(pos,Content.REQUISITION_TOME.get());var p=player(h);var menu=new InventariumMenu(1,p.getInventory(),h.absolutePos(pos));
  h.assertTrue(InventariumMenu.PAGE_SIZE==60&&InventariumMenu.DEMAND_SIZE==15&&menu.slots.size()==112,"Old main/request panes coexist with inventory and mirror deposit");
  h.assertTrue(menu.getSlot(0).x==42&&menu.getSlot(9).x==210&&menu.getSlot(60).x==-19,"Legacy grid columns and separate requests align");
  h.assertTrue(!menu.getSlot(0).mayPickup(p)&&!menu.getSlot(60).mayPickup(p)&&menu.getSlot(75).mayPickup(p),"Display stacks are ghosts, player inventory is real");p.discard();h.succeed();
 }
}
