package thmodern.vat;
import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class MorphicFingersServerTests {
    @GameTest(template="empty")
    public static void portableNormalCraftConsumesGridAndCloseReturnsInputs(GameTestHelper test){
        var player=new thmodern.testing.NetworkTestPlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"FingersNormal"));SelfInfusions.replace(player,Set.of("morphic_fingers"));var menu=new MorphicFingersMenu(1,player.getInventory());
        for(int slot:new int[]{2,3,5,6})menu.getSlot(slot).set(new ItemStack(Items.OAK_PLANKS));menu.refresh();
        test.assertTrue(menu.getSlot(0).getItem().is(Items.CRAFTING_TABLE),"Portable grid finds ordinary recipes without a block in the world");
        menu.clicked(0,0,ClickType.PICKUP,player);test.assertTrue(menu.getCarried().is(Items.CRAFTING_TABLE)&&menu.getSlot(2).getItem().isEmpty(),"Taking output debits actual input");
        menu.setCarried(ItemStack.EMPTY);menu.getSlot(2).set(new ItemStack(Items.DIAMOND,3));var wand=thaumcraft.api.ThaumcraftApi.createWand("wood","iron",false,false);var expectedWand=wand.copy();menu.getSlot(1).set(wand);menu.removed(player);
        test.assertTrue(player.getInventory().countItem(Items.DIAMOND)==3&&player.getInventory().contains(expectedWand)&&menu.getSlot(2).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty(),"Close returns ingredients and wand and empties owned slots");
        menu.removed(player);test.assertTrue(player.getInventory().countItem(Items.DIAMOND)==3,"Repeated removal cannot duplicate returned ingredients");test.succeed();
    }
    @GameTest(template="empty")
    public static void portableArcaneCraftChecksResearchVisAndRemainder(GameTestHelper test){
        var player=new thmodern.testing.NetworkTestPlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"FingersArcane"));SelfInfusions.replace(player,Set.of("morphic_fingers"));var menu=new MorphicFingersMenu(2,player.getInventory());
        menu.getSlot(2).set(new ItemStack(Horizons.CHOCOLATE_MILK.get()));menu.getSlot(3).set(new ItemStack(Items.WHEAT));menu.refresh();test.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Locked research has no takeable arcane result");
        thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,"cowInfusion");menu.refresh();test.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Unlocked recipe still needs a charged wand");
        var wand=thaumcraft.api.ThaumcraftApi.createWand("wood","iron",false,false);var item=(tc4revival.content.CastingWandItem)wand.getItem();var aqua=tc4revival.aspect.TCAspects.catalog().require("aqua");item.addVis(wand,aqua,1000,false,true);menu.getSlot(1).set(wand);menu.refresh();int before=item.vis(wand,aqua);
        test.assertTrue(menu.getSlot(0).getItem().is(Horizons.ICE_CREAM.get()),"Known paid recipe offers its output");menu.clicked(0,0,ClickType.PICKUP,player);
        test.assertTrue(menu.getCarried().is(Horizons.ICE_CREAM.get())&&item.vis(wand,aqua)<before&&player.getInventory().countItem(Items.BUCKET)==1&&menu.getSlot(2).getItem().isEmpty()&&menu.getSlot(3).getItem().isEmpty(),"Craft charges vis and returns the milk bucket while consuming ingredients");
        SelfInfusions.replace(player,Set.of());test.assertTrue(!menu.stillValid(player),"Losing morphic fingers invalidates its open menu");test.succeed();
    }
}
