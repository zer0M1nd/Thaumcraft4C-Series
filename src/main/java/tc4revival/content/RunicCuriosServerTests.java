package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import tc4revival.TC4Revival;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class RunicCuriosServerTests {
    private static FakePlayer player(GameTestHelper test,String name) {
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),name));
        CuriosApi.getCuriosInventory(player).resolve().orElseThrow().reset();
        player.tickCount=40;
        return player;
    }
    // Equipment attributes are installed by vanilla and Curios during real living ticks.
    private static void tick(FakePlayer player) { player.doTick(); }
    private static void equip(GameTestHelper test,FakePlayer player,String slot,int index,ItemStack stack) {
        test.assertTrue(CuriosApi.getItemStackSlots(stack,player).containsKey(slot),"Loaded item tag must permit "+slot);
        var stacks=CuriosApi.getCuriosInventory(player).resolve().orElseThrow().getStacksHandler(slot).orElseThrow().getStacks();
        test.assertTrue(stacks.insertItem(index,stack,false).isEmpty(),"Actual Curios insertion must succeed");
    }
    @GameTest(template="wand_test_empty")
    public static void wornRunicUpgradesAndAmuletPaymentSurviveCuriosReload(GameTestHelper test) {
        var player=player(test,"RunicCurios");
        for(int i=0;i<80;i++)tick(player);
        var amulet=new ItemStack(TCItems.LESSER_VIS_AMULET.get());
        amulet.getOrCreateTag().putInt("aer",100);amulet.getOrCreateTag().putInt("terra",100);
        equip(test,player,"necklace",0,amulet);
        equip(test,player,"ring",0,new ItemStack(TCItems.runicBauble(RunicBaubleType.CHARGED_RING)));
        equip(test,player,"ring",1,new ItemStack(TCItems.runicBauble(RunicBaubleType.HEALING_RING)));
        equip(test,player,"belt",0,new ItemStack(TCItems.runicBauble(RunicBaubleType.KINETIC_GIRDLE)));
        var goggles=new ItemStack(TCItems.GOGGLES.get());player.setItemSlot(EquipmentSlot.HEAD,goggles);
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);
        player.getInventory().setItem(35,wand);var wandBefore=wand.copy();
        tick(player);tick(player);
        var state=RunicShieldEvents.state(player);
        test.assertTrue(state.maximum()==17 && state.equipment().charged()==1 && state.equipment().healing()==1 && state.equipment().kinetic()==1,"Actual four worn slots contribute all capacities and upgrades");
        test.assertTrue(state.charge()==1,"Actual shield tick must recharge using worn amulet");
        var worn=WandInventory.wornBaubles(player);
        test.assertTrue(worn.size()==4 && worn.get(0).is(TCItems.LESSER_VIS_AMULET.get()),"Legacy necklace/rings/belt order");
        test.assertTrue(worn.get(0).getTag().getInt("aer")==50 && worn.get(0).getTag().getInt("terra")==50 && ItemStack.isSameItemSameTags(wand,wandBefore),"Amulet precedes inventory wand and pays full cost without goggles discount");
        var saved=CuriosApi.getCuriosInventory(player).resolve().orElseThrow().writeTag().copy();
        var loaded=player(test,"RunicCuriosLoad");
        var inventory=CuriosApi.getCuriosInventory(loaded).resolve().orElseThrow();inventory.readTag(saved);
        tick(loaded);tick(loaded);
        test.assertTrue(RunicShieldEvents.state(loaded).maximum()==17 && RunicShieldEvents.state(loaded).charge()==1,"Reloaded Curios equipment supplies fresh transient shield state");
        test.assertTrue(WandInventory.wornBaubles(loaded).get(0).getTag().getInt("aer")==0,"Reload preserves stored vis and charges from it");
        for(String slot:new String[]{"necklace","ring","belt"}) {
            var stacks=inventory.getStacksHandler(slot).orElseThrow().getStacks();
            for(int i=0;i<stacks.getSlots();i++)stacks.extractItem(i,1,false);
        }
        tick(loaded);tick(loaded);
        test.assertTrue(RunicShieldEvents.state(loaded).maximum()==0 && RunicShieldEvents.state(loaded).charge()==0,"Periodic equipment refresh removes capacity and clamps charge after real slot extraction");
        TC4Revival.LOGGER.info("PASS wornRunicUpgradesAndAmuletPaymentSurviveCuriosReload");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void insufficientWornAmuletFallsBackAtomicallyToDiscountedWand(GameTestHelper test) {
        var player=player(test,"CuriosFallback");
        var amulet=new ItemStack(TCItems.LESSER_VIS_AMULET.get());
        amulet.getOrCreateTag().putInt("aer",100);amulet.getOrCreateTag().putInt("terra",49);
        equip(test,player,"necklace",0,amulet);
        player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(TCItems.GOGGLES.get()));
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);
        player.getInventory().setItem(35,wand);
        tick(player);tick(player);
        var air=TCAspects.catalog().require("aer");var earth=TCAspects.catalog().require("terra");
        int before=TCItems.CASTING_WAND.get().vis(wand,air);
        var costs=Map.of(air,50,earth,50);var stored=WandInventory.wornBaubles(player).get(0);var snapshot=stored.copy();
        test.assertTrue(WandInventory.consume(player,costs),"Insufficient worn amulet falls back to inventory wand");
        test.assertTrue(ItemStack.isSameItemSameTags(stored,snapshot),"Amulet failure cannot partially debit air");
        test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,air)==before-47 && TCItems.CASTING_WAND.get().vis(wand,earth)==before-47,"Fallback wand applies actual worn goggles discount");
        player.getInventory().setItem(35,ItemStack.EMPTY);
        test.assertTrue(!WandInventory.consume(player,costs) && ItemStack.isSameItemSameTags(stored,snapshot),"No donor leaves all stored vis unchanged");
        TC4Revival.LOGGER.info("PASS insufficientWornAmuletFallsBackAtomicallyToDiscountedWand");test.succeed();
    }
}
