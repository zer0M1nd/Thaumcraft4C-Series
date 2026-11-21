package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class FocusKeyValidationServerTests {
    @GameTest(template="wand_test_empty")
    public static void registeredFocusKeysAreDistinct(GameTestHelper test) {
        // Startup already refuses duplicate keys; repeat the scan here so the real registry contents are asserted
        // and printed rather than only implied by a successful launch.
        FocusKeyValidation.verifyRegisteredFoci();
        var ids=new ArrayList<String>();var keys=new ArrayList<String>();
        for(var item:ForgeRegistries.ITEMS) if(item instanceof WandFocusItem focus){
            ids.add(String.valueOf(ForgeRegistries.ITEMS.getKey(item)));
            keys.add(focus.sortingKey(new ItemStack(item)));
        }
        test.assertTrue(ids.size()>=10,"Core build registers its own foci: "+ids);
        test.assertTrue(FocusKeyValidation.duplicates(entries(ids,keys)).isEmpty(),"Every registered focus key is unique: "+keys);
        tc4revival.TC4Revival.LOGGER.info("PASS registeredFocusKeysAreDistinct: {} foci, distinct keys {}",ids.size(),keys);
        test.succeed();
    }
    /** The reported pair: core excavation and Thaumic Horizons liquefaction must both be selectable. */
    @GameTest(template="wand_test_empty")
    public static void excavationAndLiquefactionAreBothSelectable(GameTestHelper test) {
        var liquid=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumichorizons:focus_liquefaction"));
        if(!(liquid instanceof WandFocusItem liquefaction)){
            tc4revival.TC4Revival.LOGGER.info("SKIP excavationAndLiquefactionAreBothSelectable: Thaumic Horizons is not installed in this runtime");
            test.succeed();return;
        }
        var excavation=TCItems.EXCAVATION_FOCUS.get();
        var excavationStack=new ItemStack(excavation);var liquefactionStack=new ItemStack(liquefaction);
        var excavationKey=excavation.sortingKey(excavationStack);var liquefactionKey=liquefaction.sortingKey(liquefactionStack);
        test.assertTrue(!excavationKey.equals(liquefactionKey),"Excavation and liquefaction keys differ: "+excavationKey+" vs "+liquefactionKey);
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"FocusKeySelection"));
        player.getInventory().items.set(0,excavationStack);player.getInventory().items.set(1,liquefactionStack);
        var available=FocusSelection.available(player).foci();
        test.assertTrue(available.containsKey(excavationKey)&&available.containsKey(liquefactionKey)&&available.size()>=2,
                "Both foci remain selectable at once: "+available.keySet());
        tc4revival.TC4Revival.LOGGER.info("PASS excavationAndLiquefactionAreBothSelectable: {} / {}",excavationKey,liquefactionKey);
        test.succeed();
    }
    private static List<FocusKeyValidation.Entry> entries(List<String> ids,List<String> keys){
        var entries=new ArrayList<FocusKeyValidation.Entry>();
        for(int i=0;i<ids.size();i++)entries.add(new FocusKeyValidation.Entry(ids.get(i),keys.get(i)));
        return entries;
    }
}
