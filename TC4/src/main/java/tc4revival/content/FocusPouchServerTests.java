package tc4revival.content;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FocusPouchServerTests {
    @GameTest(template="wand_test_empty")
    public static void pouchBothHandsConserveEighteenUniqueFoci(GameTestHelper test){
        for(var hand:InteractionHand.values()){
            var player=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PouchStore"));
            var pouch=new ItemStack(TCItems.FOCUS_POUCH.get());player.setItemInHand(hand,pouch);
            var menu=new FocusPouchMenu(1,player.getInventory(),hand);player.containerMenu=menu;
            try {
                test.assertTrue(menu.slots.size()==54&&menu.lockedHotbarSlot()==(hand==InteractionHand.MAIN_HAND?0:-1),"18 storage slots plus player inventory and appropriate held-slot lock");
                player.getInventory().setItem(9,new ItemStack(Items.DIRT,3));test.assertTrue(menu.quickMoveStack(player,18).isEmpty()&&player.getInventory().getItem(9).getCount()==3,"Ordinary item cannot shift into pouch");
                for(int index=0;index<18;index++){
                    var focus=new ItemStack(TCItems.EXCAVATION_FOCUS.get());focus.getOrCreateTag().putInt("audit-index",index);TCItems.EXCAVATION_FOCUS.get().applyUpgrade(focus,FocusUpgrade.POTENCY,1);
                    player.getInventory().setItem(9,focus.copy());
                    test.assertTrue(!menu.quickMoveStack(player,18).isEmpty()&&player.getInventory().getItem(9).isEmpty(),"Focus moves out of player slot");
                    test.assertTrue(ItemStack.matches(FocusPouchItem.contents(pouch).get(index),focus),"Forward slot order and full core NBT persisted immediately");
                }
                var overflow=new ItemStack(TCItems.EXCAVATION_FOCUS.get());player.getInventory().setItem(9,overflow);
                test.assertTrue(menu.quickMoveStack(player,18).isEmpty()&&player.getInventory().getItem(9)==overflow,"Nineteenth focus stays in inventory");player.getInventory().setItem(9,ItemStack.EMPTY);
                var serialized=pouch.save(new CompoundTag());var restored=ItemStack.of(serialized);
                test.assertTrue(ItemStack.matches(restored,pouch)&&FocusPouchItem.contents(restored).stream().filter(s->!s.isEmpty()).count()==18,"Full pouch survives item serialization");
                player.setItemInHand(hand,restored);
                test.assertTrue(!menu.stillValid(player)&&menu.quickMoveStack(player,0).isEmpty(),"Byte-identical replacement invalidates old menu identity");
                menu.clicked(0,0,ClickType.PICKUP,player);test.assertTrue(menu.getCarried().isEmpty()&&ItemStack.matches(pouch,restored),"Invalid old menu cannot extract or overwrite replacement");
                player.setItemInHand(hand,pouch);
                if(hand==InteractionHand.MAIN_HAND){menu.clicked(45,0,ClickType.PICKUP,player);test.assertTrue(menu.getCarried().isEmpty()&&menu.quickMoveStack(player,45).isEmpty(),"Held pouch slot is locked");}
                for(int index=0;index<18;index++)test.assertTrue(!menu.quickMoveStack(player,index).isEmpty(),"Stored core can shift back to inventory");
                test.assertTrue(FocusPouchItem.contents(pouch).stream().allMatch(ItemStack::isEmpty),"All core removals written through before close");
                var seen=new HashSet<Integer>();int count=0;
                for(int slot=0;slot<36;slot++){var stack=player.getInventory().getItem(slot);if(stack.is(TCItems.EXCAVATION_FOCUS.get())){count+=stack.getCount();seen.add(stack.getTag().getInt("audit-index"));test.assertTrue(TCItems.EXCAVATION_FOCUS.get().nextRank(stack)==2,"Upgrade NBT survives extraction");}}
                test.assertTrue(count==18&&seen.size()==18,"All 18 unique cores returned exactly once");
                test.assertTrue(player.getInventory().getItem(8).getTag().getInt("audit-index")==0,"First extracted core uses reverse inventory order");
                menu.removed(player);test.assertTrue(FocusPouchItem.contents(pouch).stream().allMatch(ItemStack::isEmpty),"Close does not restore an obsolete full snapshot");
            }finally{player.containerMenu=player.inventoryMenu;}
        }
        tc4revival.TC4Revival.LOGGER.info("PASS pouchBothHandsConserveEighteenUniqueFoci: two hands, 18 unique upgraded cores, capacity, NBT, identity replacement, held lock and close");test.succeed();
    }
}
