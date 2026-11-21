package tc4revival.content;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.research.*;
import tc4revival.network.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class InventoryAspectDisplayServerTests {
    @GameTest(template="wand_test_empty")
    public static void inventoryAspectVisibilityAndStackBonuses(GameTestHelper test) {
        var knowledge=new PlayerKnowledge(TCAspects.catalog());var stack=new ItemStack(Items.IRON_SWORD);
        test.assertTrue(InventoryAspectDisplay.resolve(stack,knowledge).isEmpty(),"Unscanned items reveal no aspect count or identity");
        var hash=ItemStackAspects.scanHash(stack).orElseThrow();knowledge.rememberScan(PlayerKnowledge.ScanKind.OBJECT,"@"+hash);
        var initial=InventoryAspectDisplay.resolve(stack,knowledge);
        test.assertTrue(!initial.isEmpty() && initial.stream().anyMatch(icon->icon.tag().isEmpty()&&icon.amount()==0),"Undiscovered compounds are anonymous without a quantity");
        TCAspects.catalog().all().forEach(knowledge::discover);
        var values=ItemStackAspects.resolve(stack).orElseThrow();var shown=InventoryAspectDisplay.resolve(stack,knowledge);
        test.assertTrue(shown.size()==values.size(),"All defined aspects appear after discovery");
        int previous=Integer.MAX_VALUE;
        for(var icon:shown) { test.assertTrue(icon.amount()<=previous && icon.amount()==values.amount(TCAspects.catalog().require(icon.tag())),"Descending per-item amounts");previous=icon.amount(); }
        stack.setCount(32);test.assertTrue(InventoryAspectDisplay.resolve(stack,knowledge).equals(shown),"Display amounts are not multiplied by stack count");
        stack.enchant(Enchantments.SHARPNESS,3);
        test.assertTrue(!InventoryAspectDisplay.resolve(stack,knowledge).equals(shown),"Stack enchantment bonuses recompute under the already-scanned identity");
        var enhanced=new PlayerKnowledge(TCAspects.catalog());enhanced.rememberScan(PlayerKnowledge.ScanKind.OBJECT,"#"+hash);
        test.assertTrue(!InventoryAspectDisplay.resolve(stack,enhanced).isEmpty(),"Enhanced scan also unlocks display");
        test.assertTrue(InventoryAspectDisplay.resolve(new ItemStack(Items.ECHO_SHARD),knowledge).isEmpty(),"Unknown modern item remains without invented aspects");
        for(boolean setting:new boolean[]{false,true})for(boolean shift:new boolean[]{false,true})
            test.assertTrue(InventoryAspectDisplay.visible(setting,shift)==(setting!=shift),"Shift reverses the display setting");
        passed(test,"knowledge-gating-and-stack-values");
    }
    @GameTest(template="wand_test_empty")
    public static void inventoryAspectPacketsPreserveMasking(GameTestHelper test) {
        var stack=new ItemStack(Items.DIAMOND_SWORD);stack.enchant(Enchantments.SHARPNESS,2);
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            new InventoryAspectsRequest(42,stack).encode(buffer);var read=InventoryAspectsRequest.decode(buffer);
            test.assertTrue(read.request()==42 && ItemStack.isSameItemSameTags(read.stack(),stack),"Request preserves enchanted stack metadata");
            buffer.clear();var expected=new InventoryAspectsReply(42,List.of(new InventoryAspectDisplay.Icon("terra",7),new InventoryAspectDisplay.Icon("",0)));
            expected.encode(buffer);test.assertTrue(InventoryAspectsReply.decode(buffer).equals(expected),"Response preserves unknown mask and request identity");
            buffer.clear();buffer.writeVarInt(1);buffer.writeVarInt(129);
            boolean rejected=false;try { InventoryAspectsReply.decode(buffer); }catch(IllegalArgumentException invalid) { rejected=true; }
            test.assertTrue(rejected,"Oversized response rejected before allocation");
        } finally { buffer.release(); }
        passed(test,"packet-roundtrip-and-mask");
    }
    private static void passed(GameTestHelper test,String name) { tc4revival.TC4Revival.LOGGER.info("Inventory aspect display PASS: {}",name);test.succeed(); }
}

