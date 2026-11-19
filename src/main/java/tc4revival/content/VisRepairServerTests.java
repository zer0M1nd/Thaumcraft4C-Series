package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import tc4revival.aspect.item.ItemAspects;

/** Exercises registered equipment and real inventory payment against loaded aspect definitions. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class VisRepairServerTests {
    private static FakePlayer player(GameTestHelper test) {
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"RepairTest"));
        player.setGameMode(GameType.SURVIVAL);return player;
    }
    private static ItemStack damaged(Item item,int rank) {
        var stack=new ItemStack(item);stack.setDamageValue(10);stack.enchant(TCEnchantments.REPAIR.get(),rank);return stack;
    }
    private static ItemStack wand() { return TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true); }
    private static void tick(FakePlayer player,int time,TickEvent.Phase phase) {
        player.tickCount=time;VisRepair.tick(new TickEvent.PlayerTickEvent(phase,player));
    }
    @GameTest(template="wand_test_empty")
    public static void visRepairInventoryCadenceAndExclusions(GameTestHelper test) {
        var player=player(test);var hotbar=damaged(TCItems.tool(ThaumiumToolType.PICKAXE),2);
        var stored=hotbar.copy();var helmet=damaged(TCItems.armor(ThaumiumArmorType.HELMET),1);
        var harness=damaged(TCItems.HOVER_HARNESS.get(),2);var ordinary=damaged(Items.IRON_PICKAXE,2);
        player.getInventory().setItem(0,hotbar);player.getInventory().setItem(9,stored);
        player.getInventory().setItem(1,harness);player.getInventory().setItem(2,ordinary);
        player.getInventory().setItem(35,wand());player.setItemSlot(EquipmentSlot.HEAD,helmet);
        tick(player,39,TickEvent.Phase.END);tick(player,40,TickEvent.Phase.START);
        test.assertTrue(hotbar.getDamageValue()==10 && helmet.getDamageValue()==10,"Only the end of every fortieth tick repairs");
        tick(player,40,TickEvent.Phase.END);
        test.assertTrue(hotbar.getDamageValue()==8 && helmet.getDamageValue()==9,"Hotbar and worn armor repair at their enchantment rank");
        test.assertTrue(stored.getDamageValue()==10 && harness.getDamageValue()==10 && ordinary.getDamageValue()==10,"Main storage, carried harness and non-repairable tools are excluded");
        player.setGameMode(GameType.CREATIVE);tick(player,80,TickEvent.Phase.END);
        test.assertTrue(hotbar.getDamageValue()==8 && helmet.getDamageValue()==9,"Creative players do not pay for automatic repair");
        passed(test,"cadence-and-exclusions");
    }
    @GameTest(template="wand_test_empty")
    public static void visRepairPaymentOrderAtomicityAndRank(GameTestHelper test) {
        var player=player(test);var tool=damaged(TCItems.tool(ThaumiumToolType.PICKAXE),5);
        var lower=wand();var higher=wand();player.getInventory().setItem(10,lower);player.getInventory().setItem(35,higher);
        var lowerBefore=lower.copy();var higherBefore=higher.copy();
        var base=ItemAspects.baseSession().base("thaumcraft:thaumium_pickaxe").orElseThrow();
        var primals=TCAspects.catalog().toPrimals(base,AspectCatalog.Reduction.SUM);
        test.assertTrue(primals.size()>1,"Fixture requires a real multi-aspect recipe cost");
        VisRepair.repair(tool,player);
        test.assertTrue(tool.getDamageValue()==8 && ItemStack.isSameItemSameTags(lower,lowerBefore),"Rank clamps at two and reverse inventory order chooses the last wand");
        for(var aspect:primals.aspects()) {
            int oldCost=(int)Math.sqrt(primals.amount(aspect)*2.0)*2;
            test.assertTrue(TCItems.CASTING_WAND.get().vis(higherBefore,aspect)-TCItems.CASTING_WAND.get().vis(higher,aspect)==oldCost,"Repair costs use base aspects and centivis without scan enchantment bonuses");
        }
        // No donor may partially pay a cost and then fall through to another donor.
        var missing=primals.aspects().iterator().next();higher.getOrCreateTag().putInt(missing.tag(),0);
        player.getInventory().setItem(10,ItemStack.EMPTY);var insufficient=higher.copy();
        VisRepair.repair(tool,player);
        test.assertTrue(tool.getDamageValue()==8 && ItemStack.isSameItemSameTags(higher,insufficient),"One missing component leaves all charge and durability unchanged");
        player.getInventory().setItem(10,lower);VisRepair.repair(tool,player);
        test.assertTrue(tool.getDamageValue()==6 && ItemStack.isSameItemSameTags(higher,insufficient) && !ItemStack.isSameItemSameTags(lower,lowerBefore),"An insufficient later wand is untouched while the earlier wand pays the complete cost");
        tool.setDamageValue(1);VisRepair.repair(tool,player);
        test.assertTrue(tool.getDamageValue()==0,"Repair clamps at zero damage");
        var paid=lower.copy();VisRepair.repair(tool,player);
        test.assertTrue(ItemStack.isSameItemSameTags(lower,paid),"Undamaged equipment never consumes charge");
        passed(test,"payment-order-atomicity-rank");
    }
    private static void passed(GameTestHelper test,String name) { tc4revival.TC4Revival.LOGGER.info("Vis repair PASS: {}",name);test.succeed(); }
}
