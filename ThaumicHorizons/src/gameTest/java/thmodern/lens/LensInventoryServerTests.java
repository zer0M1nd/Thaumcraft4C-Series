package thmodern.lens;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class LensInventoryServerTests {
    @GameTest(template="empty")
    public static void caseSwapPreservesMetadataAndRefusesFullRemoval(GameTestHelper test){
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"LensSwapTest"));var goggles=new ItemStack(tc4revival.content.TCItems.GOGGLES.get());player.setItemSlot(EquipmentSlot.HEAD,goggles);
        for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.STONE,64));var bag=new ItemStack(Horizons.LENS_CASE.get());player.getInventory().setItem(0,bag);
        var old=new ItemStack(Horizons.LENSES.get("fire").get());old.setHoverName(net.minecraft.network.chat.Component.literal("Old lens"));goggles.getOrCreateTag().put(LensEquipment.DATA,old.save(new net.minecraft.nbt.CompoundTag()));
        for(int i=0;i<18;i++)LensCaseItem.set(bag,i,new ItemStack(Horizons.LENSES.get("water").get()));
        test.assertTrue(!LensEquipment.change(player,"")&&LensEquipment.active(player).equals("fire"),"No space means no removal or dropped lens");
        test.assertTrue(LensEquipment.change(player,"water")&&LensEquipment.active(player).equals("water"),"Selected case slot can receive the old lens during an otherwise full swap");
        var returned=LensCaseItem.get(player.getInventory().getItem(0),0);test.assertTrue(returned.is(old.getItem())&&returned.hasCustomHoverName(),"Complete previous lens NBT survives exchange");
        var before=goggles.copy();test.assertTrue(!LensEquipment.change(player,"forged")&&ItemStack.matches(before,goggles),"Unknown network choice cannot mutate the helmet");
        player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);test.assertTrue(!LensEquipment.change(player,"fire"),"No revealing equipment means no install");test.succeed();
    }
    @GameTest(template="empty")
    public static void openCaseLocksItselfAndPersistsExactLensStack(GameTestHelper test){
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"LensCaseTest"));player.getInventory().selected=0;var bag=new ItemStack(Horizons.LENS_CASE.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,bag);
        var menu=new LensCaseMenu(1,player.getInventory(),net.minecraft.world.InteractionHand.MAIN_HAND);player.containerMenu=menu;
        test.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT))&&!menu.getSlot(45).mayPickup(player),"Case accepts only lenses and cannot pick itself up");
        var lens=new ItemStack(Horizons.LENSES.get("air").get());lens.setHoverName(net.minecraft.network.chat.Component.literal("Finder"));menu.getSlot(0).set(lens);
        test.assertTrue(LensCaseItem.get(bag,0).hasCustomHoverName(),"Slot changes immediately persist to the held case");
        menu.clicked(-999,40,net.minecraft.world.inventory.ClickType.SWAP,player);test.assertTrue(player.getMainHandItem()==bag,"Invalid outside swap cannot move the held case");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Horizons.LENS_CASE.get()));test.assertTrue(!menu.stillValid(player),"Replacing the bag revokes the old menu");test.succeed();
    }
}
