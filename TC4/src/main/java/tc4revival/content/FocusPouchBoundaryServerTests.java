package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FocusPouchBoundaryServerTests {
    private static FakePlayer player(GameTestHelper test){var p=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PouchBoundary"));p.setGameMode(GameType.SURVIVAL);return p;}
    @GameTest(template="wand_test_empty")
    public static void focusSelectionFallbackAndInvalidContexts(GameTestHelper test){
        for(String key:new String[]{"","AZ","ZZ"}){
            var p=player(test);var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);p.setItemInHand(InteractionHand.MAIN_HAND,wand);
            p.getInventory().setItem(9,new ItemStack(TCItems.FIRE_FOCUS.get()));p.getInventory().setItem(10,new ItemStack(TCItems.EXCAVATION_FOCUS.get()));
            FocusSelection.change(p,InteractionHand.MAIN_HAND,key);
            test.assertTrue(WandFocusItem.attached(wand).is(key.equals("AZ")?TCItems.EXCAVATION_FOCUS.get():TCItems.FIRE_FOCUS.get()),"Missing key chooses next key or wraps at end: "+key);
        }
        for(int mode=0;mode<4;mode++){
            var p=player(test);var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,mode==2,true);p.setItemInHand(InteractionHand.MAIN_HAND,wand);var incoming=new ItemStack(TCItems.FIRE_FOCUS.get());p.getInventory().setItem(9,incoming);
            if(mode==0)p.setGameMode(GameType.SPECTATOR);if(mode==1)p.setHealth(0);if(mode==3){p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(TCItems.FOCUS_POUCH.get()));p.containerMenu=new FocusPouchMenu(1,p.getInventory(),InteractionHand.OFF_HAND);}
            var before=wand.copy();FocusSelection.change(p,InteractionHand.MAIN_HAND,TCItems.FIRE_FOCUS.get().sortingKey(incoming));
            test.assertTrue(ItemStack.matches(wand,before)&&p.getInventory().getItem(9)==incoming,"Spectator/dead/sceptre/open-menu context cannot change core: "+mode);
        }
        tc4revival.TC4Revival.LOGGER.info("PASS focusSelectionFallbackAndInvalidContexts: three ordering boundaries and four blocked contexts");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void pouchFullPlayerInventoryAndDeathRejectExtraction(GameTestHelper test){
        for(var hand:InteractionHand.values()){
            var p=player(test);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
            var pouch=new ItemStack(TCItems.FOCUS_POUCH.get());var contents=NonNullList.withSize(18,ItemStack.EMPTY);var focus=new ItemStack(TCItems.FIRE_FOCUS.get());contents.set(0,focus);FocusPouchItem.save(pouch,contents);p.setItemInHand(hand,pouch);
            var menu=new FocusPouchMenu(1,p.getInventory(),hand);p.containerMenu=menu;var before=pouch.copy();
            test.assertTrue(menu.quickMoveStack(p,0).isEmpty()&&ItemStack.matches(pouch,before),"Full player inventory leaves core in pouch");
            p.getInventory().setItem(9,ItemStack.EMPTY);test.assertTrue(!menu.quickMoveStack(p,0).isEmpty()&&ItemStack.matches(p.getInventory().getItem(9),focus),"Opening exactly one slot permits one core extraction");
            menu.quickMoveStack(p,18);before=pouch.copy();p.setHealth(0);menu.clicked(0,0,ClickType.PICKUP,p);
            test.assertTrue(!menu.stillValid(p)&&menu.quickMoveStack(p,0).isEmpty()&&menu.getCarried().isEmpty()&&ItemStack.matches(pouch,before),"Dead player's stale menu cannot extract or change saved pouch");p.containerMenu=p.inventoryMenu;
        }
        tc4revival.TC4Revival.LOGGER.info("PASS pouchFullPlayerInventoryAndDeathRejectExtraction: both hands, full inventory preservation, freed-slot extraction and dead-menu rejection");test.succeed();
    }
}
