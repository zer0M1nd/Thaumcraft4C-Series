package tc4revival.content;

import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FocusPouchSwapServerTests {
    private static ItemStack core(String id){var result=new ItemStack(TCItems.EXCAVATION_FOCUS.get());result.getOrCreateTag().putString("audit",id);return result;}
    @GameTest(template="wand_test_empty")
    public static void pouchNumberKeysSwapCoresButNeverMoveOpenPouch(GameTestHelper test){
        int cases=0;
        for(var hand:InteractionHand.values())for(int target:new int[]{0,1,2,3,4,5,6,7,8,40}){
            var player=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PouchSwap"));player.getInventory().selected=3;
            var pouch=new ItemStack(TCItems.FOCUS_POUCH.get());var contents=NonNullList.withSize(18,ItemStack.EMPTY);var inside=core("inside");var outside=core("outside");contents.set(0,inside.copy());FocusPouchItem.save(pouch,contents);
            player.setItemInHand(hand,pouch);boolean protectedTarget=target==(hand==InteractionHand.MAIN_HAND?3:40);
            if(!protectedTarget)player.getInventory().setItem(target,outside.copy());
            var menu=new FocusPouchMenu(1,player.getInventory(),hand);player.containerMenu=menu;
            try {
                menu.clicked(0,target,ClickType.SWAP,player);
                test.assertTrue(player.getItemInHand(hand)==pouch&&menu.stillValid(player),"Open pouch stays bound to original hand");
                if(protectedTarget){
                    test.assertTrue(ItemStack.matches(FocusPouchItem.contents(pouch).get(0),inside),"Swap targeting held pouch cannot alter contents");
                    player.getInventory().setItem(9,new ItemStack(Items.DIRT));menu.clicked(18,target,ClickType.SWAP,player);
                    test.assertTrue(player.getItemInHand(hand)==pouch&&player.getInventory().getItem(9).is(Items.DIRT),"Held pouch cannot be exchanged into ordinary inventory slot");
                }else{
                    test.assertTrue(ItemStack.matches(FocusPouchItem.contents(pouch).get(0),outside)&&ItemStack.matches(player.getInventory().getItem(target),inside),"Normal hotbar or offhand core swap writes exact opposite NBT");
                    menu.clicked(0,target,ClickType.SWAP,player);
                    test.assertTrue(ItemStack.matches(FocusPouchItem.contents(pouch).get(0),inside)&&ItemStack.matches(player.getInventory().getItem(target),outside),"Reverse swap restores exactly both cores");
                    player.getInventory().setItem(target,new ItemStack(Items.DIRT,2));menu.clicked(0,target,ClickType.SWAP,player);
                    test.assertTrue(ItemStack.matches(FocusPouchItem.contents(pouch).get(0),inside)&&player.getInventory().getItem(target).getCount()==2,"Vanilla slot rule rejects nonfocus exchange into pouch");
                }
                if(hand==InteractionHand.MAIN_HAND){menu.clicked(48,0,ClickType.SWAP,player);test.assertTrue(player.getMainHandItem()==pouch,"Direct held-slot click remains locked");}
                cases++;
            }finally{player.containerMenu=player.inventoryMenu;}
        }
        tc4revival.TC4Revival.LOGGER.info("PASS pouchNumberKeysSwapCoresButNeverMoveOpenPouch: {} hand/target cases with reversible NBT swaps and held-pouch guards",cases);test.succeed();
    }
}
