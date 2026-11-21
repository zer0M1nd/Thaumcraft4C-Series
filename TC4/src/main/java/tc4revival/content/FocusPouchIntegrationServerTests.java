package tc4revival.content;

import java.util.*;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FocusPouchIntegrationServerTests {
    private static FakePlayer player(GameTestHelper test){return new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PouchIntegration"));}
    private static ItemStack core(String id){var stack=new ItemStack(TCItems.EXCAVATION_FOCUS.get());stack.getOrCreateTag().putString("audit",id);return stack;}
    private static ItemStack pouch(ItemStack focus){var stack=new ItemStack(TCItems.FOCUS_POUCH.get());var list=NonNullList.withSize(18,ItemStack.EMPTY);list.set(0,focus);FocusPouchItem.save(stack,list);return stack;}
    @GameTest(template="wand_test_empty")
    public static void wornPouchSelectionPrecedenceAndCuriosStorage(GameTestHelper test){
        var player=player(test);var curios=CuriosApi.getCuriosInventory(player).resolve().orElseThrow();curios.reset();var belt=curios.getStacksHandler("belt").orElseThrow().getStacks();
        var worn=pouch(core("worn"));test.assertTrue(CuriosApi.getItemStackSlots(worn,player).containsKey("belt")&&belt.insertItem(0,worn,false).isEmpty(),"Loaded Curios belt tag accepts actual pouch insertion");worn=belt.getStackInSlot(0);
        var bag=pouch(core("inventory-bag"));player.getInventory().setItem(35,bag);player.getInventory().setItem(9,core("loose"));
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);var old=new ItemStack(TCItems.FIRE_FOCUS.get());old.getOrCreateTag().putString("audit","old");WandFocusItem.attach(wand,old);player.setItemInHand(InteractionHand.MAIN_HAND,wand);
        var key=TCItems.EXCAVATION_FOCUS.get().sortingKey(core("any"));
        test.assertTrue(FocusSelection.available(player).foci().size()==1&&FocusSelection.available(player).foci().get(key).get().get().getTag().getString("audit").equals("inventory-bag"),"Same sorting key resolves to latest inventory bag after worn and loose entries");
        FocusSelection.change(player,InteractionHand.MAIN_HAND,key);
        test.assertTrue(WandFocusItem.attached(wand).getTag().getString("audit").equals("inventory-bag")&&FocusPouchItem.contents(bag).get(0).isEmpty(),"Selection extracts precisely the winning bag entry");
        test.assertTrue(ItemStack.matches(FocusPouchItem.contents(worn).get(1),old)&&player.getInventory().getItem(9).getTag().getString("audit").equals("loose"),"Old focus returns to first worn pouch free slot; shadowed loose core unchanged");
        FocusSelection.change(player,InteractionHand.MAIN_HAND,"REMOVE");test.assertTrue(WandFocusItem.attached(wand).isEmpty()&&FocusPouchItem.contents(worn).get(2).getTag().getString("audit").equals("inventory-bag"),"Removal also prefers available worn pouch space");
        var saved=curios.writeTag().copy();var loaded=player(test);var loadedCurios=CuriosApi.getCuriosInventory(loaded).resolve().orElseThrow();loadedCurios.reset();loadedCurios.readTag(saved);
        var restored=loadedCurios.getStacksHandler("belt").orElseThrow().getStacks().getStackInSlot(0);test.assertTrue(ItemStack.matches(restored,worn)&&FocusSelection.available(loaded).foci().size()==2,"Actual Curios serialized pouch remains discoverable with stored core NBT");
        belt.extractItem(0,1,false);test.assertTrue(FocusSelection.available(player).pouches().size()==1,"Unequipped pouch no longer included among worn sources");
        tc4revival.TC4Revival.LOGGER.info("PASS wornPouchSelectionPrecedenceAndCuriosStorage: belt insertion, duplicate-key precedence, exchange/remove return order, Curios serialization and unequip");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void fullPouchAndInventoryKeepFocusOnFailedRemoval(GameTestHelper test){
        var player=player(test);for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        var bag=new ItemStack(TCItems.FOCUS_POUCH.get());var contents=NonNullList.withSize(18,ItemStack.EMPTY);for(int i=0;i<18;i++)contents.set(i,core("entry-"+i));FocusPouchItem.save(bag,contents);player.getInventory().setItem(35,bag);
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);var old=new ItemStack(TCItems.FIRE_FOCUS.get());WandFocusItem.attach(wand,old);player.setItemInHand(InteractionHand.MAIN_HAND,wand);var before=wand.copy();var saved=bag.copy();
        FocusSelection.change(player,InteractionHand.MAIN_HAND,"REMOVE");test.assertTrue(ItemStack.matches(wand,before)&&ItemStack.matches(bag,saved),"Full storage refuses removal without losing current core");
        FocusSelection.change(player,InteractionHand.MAIN_HAND,TCItems.EXCAVATION_FOCUS.get().sortingKey(core("any")));
        test.assertTrue(WandFocusItem.attached(wand).getTag().getString("audit").equals("entry-17")&&ItemStack.matches(FocusPouchItem.contents(bag).get(17),old),"Full pouch can swap through the selected slot freed by extraction");
        test.assertTrue(FocusPouchItem.contents(bag).stream().filter(s->!s.isEmpty()).count()==18,"Exchange preserves full pouch count");
        tc4revival.TC4Revival.LOGGER.info("PASS fullPouchAndInventoryKeepFocusOnFailedRemoval: full removal rejection and successful exchange through freed source slot");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void pouchUseOpensCorrectHandServerMenu(GameTestHelper test){
        for(var hand:InteractionHand.values()){
            var player=player(test);var packets=new ArrayList<net.minecraft.network.protocol.Packet<?>>();
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){packets.add(packet);}};
            new net.minecraft.server.network.ServerGamePacketListenerImpl(player.server,connection,player);
            var bag=pouch(core("opening"));player.setItemInHand(hand,bag);var result=TCItems.FOCUS_POUCH.get().use(player.level(),player,hand);
            test.assertTrue(result.getResult().consumesAction()&&player.containerMenu instanceof FocusPouchMenu menu&&menu.stillValid(player)&&menu.lockedHotbarSlot()==(hand==InteractionHand.MAIN_HAND?0:-1)&&menu.getSlot(0).getItem().getTag().getString("audit").equals("opening"),"Actual item use opens correct hand and saved contents");
            test.assertTrue(!packets.isEmpty(),"Server emits opening traffic");player.containerMenu=player.inventoryMenu;
        }
        tc4revival.TC4Revival.LOGGER.info("PASS pouchUseOpensCorrectHandServerMenu: both item-use hands, saved contents and server opening traffic");test.succeed();
    }
}
