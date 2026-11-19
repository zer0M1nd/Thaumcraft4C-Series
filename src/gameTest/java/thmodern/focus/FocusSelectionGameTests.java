package thmodern.focus;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import thmodern.Horizons;
/** The reported defect: the original "BE" prefix made liquefaction hide core excavation in the focus wheel. */
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class FocusSelectionGameTests {
    @GameTest(template="empty")
    public static void liquefactionAndExcavationAreBothSelectable(GameTestHelper test){
        // Runs in a runtime that loads both mods, so this also audits every installed focus key.
        FocusKeyValidation.verifyRegisteredFoci();
        var excavation=TCItems.EXCAVATION_FOCUS.get();var liquid=Horizons.LIQUEFACTION_FOCUS.get();
        var excavationStack=new ItemStack(excavation);var liquidStack=new ItemStack(liquid);
        var excavationKey=excavation.sortingKey(excavationStack);var liquidKey=((WandFocusItem)liquid).sortingKey(liquidStack);
        test.assertTrue(!excavationKey.equals(liquidKey),"Excavation and liquefaction keys differ: "+excavationKey+" vs "+liquidKey);
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"FocusKeys"));
        player.getInventory().items.set(0,excavationStack);player.getInventory().items.set(1,liquidStack);
        var foci=FocusSelection.available(player).foci();
        test.assertTrue(foci.containsKey(excavationKey)&&foci.containsKey(liquidKey),"Both foci remain selectable at once: "+foci.keySet());
        com.mojang.logging.LogUtils.getLogger().info("PASS liquefactionAndExcavationAreBothSelectable: {} / {} among {}",excavationKey,liquidKey,foci.keySet());
        test.succeed();
    }
}
