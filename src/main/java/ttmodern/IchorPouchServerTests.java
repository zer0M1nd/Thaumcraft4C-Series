package ttmodern;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class IchorPouchServerTests {
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper test){return new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTPouch"));}
    @GameTest(template="wand_test_empty") public static void tinkererIchorPouchAllSlotsAndSerialization(GameTestHelper test){
        for(var hand:InteractionHand.values()){
            var player=player(test);var pouch=new ItemStack(Tinkerer.ICHOR_POUCH.get());player.setItemInHand(hand,pouch);var menu=new IchorPouchMenu(1,player.getInventory(),hand);
            for(int slot=0;slot<IchorPouchItem.SIZE;slot++){var value=new ItemStack(Items.DIAMOND,1+slot%64);value.setHoverName(net.minecraft.network.chat.Component.literal("Slot "+slot));menu.getSlot(slot).set(value);}
            var saved=IchorPouchItem.contents(ItemStack.of(pouch.save(new net.minecraft.nbt.CompoundTag())));test.assertTrue(saved.size()==117,"All 117 slots survive item serialization");
            for(int slot=0;slot<117;slot++)test.assertTrue(saved.get(slot).getCount()==1+slot%64&&saved.get(slot).getHoverName().getString().equals("Slot "+slot),"Every count and custom name is preserved");
            test.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Tinkerer.ICHOR_POUCH.get()))&&!menu.getSlot(0).mayPlace(new ItemStack(tc4revival.content.TCItems.FOCUS_POUCH.get())),"Pouches cannot be nested");
            menu.clicked(0,hand==InteractionHand.MAIN_HAND?player.getInventory().selected:40,ClickType.SWAP,player);test.assertTrue(player.getItemInHand(hand)==pouch&&menu.getSlot(0).getItem().is(Items.DIAMOND),"Hotbar/offhand swap cannot move the open bag");
            if(hand==InteractionHand.MAIN_HAND){menu.clicked(144+player.getInventory().selected,0,ClickType.THROW,player);test.assertTrue(player.getItemInHand(hand)==pouch,"Open bag cannot be thrown from its locked slot");}
            var replacement=new ItemStack(Tinkerer.ICHOR_POUCH.get());player.setItemInHand(hand,replacement);test.assertTrue(!menu.stillValid(player),"Another identical item cannot take over the old menu");menu.clicked(0,0,ClickType.PICKUP,player);test.assertTrue(IchorPouchItem.contents(replacement).stream().allMatch(ItemStack::isEmpty),"Invalid menu cannot write into a replacement pouch");
        }test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererIchorPouchQuickTransferAndEmptyUpgrade(GameTestHelper test){
        var player=player(test);var pouch=new ItemStack(Tinkerer.ICHOR_POUCH.get());player.setItemInHand(InteractionHand.MAIN_HAND,pouch);player.getInventory().setItem(9,new ItemStack(Items.IRON_INGOT,64));var menu=new IchorPouchMenu(1,player.getInventory(),InteractionHand.MAIN_HAND);
        menu.quickMoveStack(player,117);test.assertTrue(player.getInventory().getItem(9).isEmpty()&&IchorPouchItem.contents(pouch).get(0).getCount()==64,"Quick move deposits one stack without duplication");
        menu.quickMoveStack(player,0);test.assertTrue(IchorPouchItem.contents(pouch).get(0).isEmpty()&&player.getInventory().countItem(Items.IRON_INGOT)==64,"Quick withdrawal restores exactly the deposited quantity");
        var ingredient=new EmptyFocusPouchIngredient();var input=new ItemStack(tc4revival.content.TCItems.FOCUS_POUCH.get());test.assertTrue(ingredient.test(input),"An empty focus pouch can be upgraded");
        var foci=net.minecraft.core.NonNullList.withSize(18,ItemStack.EMPTY);foci.set(17,new ItemStack(Tinkerer.FLIGHT_FOCUS.get()));tc4revival.content.FocusPouchItem.save(input,foci);test.assertTrue(!ingredient.test(input),"An upgrade must reject a bag containing a focus, including the last slot");test.succeed();
    }
}
