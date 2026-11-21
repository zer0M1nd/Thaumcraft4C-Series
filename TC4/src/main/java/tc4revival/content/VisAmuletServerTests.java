package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import tc4revival.TC4Revival;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import tc4revival.research.KnowledgeSavedData;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class VisAmuletServerTests {
    private static FakePlayer player(GameTestHelper test,String name) {
        return new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),name));
    }
    private static void wornTick(FakePlayer player,ItemStack amulet,int time) {
        player.tickCount=time;
        CuriosApi.getCurio(amulet).resolve().orElseThrow().curioTick(new SlotContext("necklace",player,0,false,true));
    }
    @GameTest(template="wand_test_empty")
    public static void amuletCapabilityTransfersAllPrimalsAtLegacyCadenceAndGatesResearch(GameTestHelper test) {
        var player=player(test,"AmuletTransfer");var context=new SlotContext("necklace",player,0,false,true);
        var lesser=new ItemStack(TCItems.LESSER_VIS_AMULET.get());var greater=new ItemStack(TCItems.VIS_AMULET.get());
        var small=CuriosApi.getCurio(lesser).resolve().orElseThrow();var large=CuriosApi.getCurio(greater).resolve().orElseThrow();
        test.assertTrue(small.canEquip(context) && !large.canEquip(context),"Only greater amulet requires VISAMULET research");
        var knowledge=KnowledgeSavedData.get(player.getServer());
        knowledge.update(player.getUUID(),bound->0,state->state.rememberResearch("VISAMULET"));
        test.assertTrue(large.canEquip(context) && large.canUnequip(context) && !large.canEquipFromUse(context),"Research permits equip, while direct right-click equip stays disabled");
        for(var amulet:new ItemStack[]{lesser,greater}) {
            var item=(VisAmuletItem)amulet.getItem();int cap=amulet==lesser?2500:25000;
            test.assertTrue(item.capacity(amulet)==cap,"Legacy capacities in centi-vis");
            var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);
            player.getInventory().setItem(0,wand);player.getInventory().selected=0;
            var primals=TCAspects.catalog().primals();
            for(int i=0;i<primals.size();i++) {
                var aspect=primals.get(i);amulet.getOrCreateTag().putInt(aspect.tag(),i+1);
                wand.getOrCreateTag().putInt(aspect.tag(),TCItems.CASTING_WAND.get().capacity(wand)-(i==0?0:i==1?1:10));
            }
            var before=wand.copy();var amuletBefore=amulet.copy();wornTick(player,amulet,4);
            test.assertTrue(ItemStack.isSameItemSameTags(wand,before) && ItemStack.isSameItemSameTags(amulet,amuletBefore),"No transfer outside five-tick boundary");
            wornTick(player,amulet,5);
            for(int i=0;i<primals.size();i++) {
                var aspect=primals.get(i);int expected=i==0?0:i==1?1:Math.min(5,i+1);
                test.assertTrue(item.vis(amulet,aspect)==i+1-expected && TCItems.CASTING_WAND.get().vis(wand,aspect)==before.getTag().getInt(aspect.tag())+expected,"Per-primal transfer respects wand room, amulet balance and five-unit ceiling: "+aspect.tag());
            }
            var stored=amulet.copy();player.getInventory().setItem(0,ItemStack.EMPTY);player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,wand);
            wornTick(player,amulet,10);test.assertTrue(ItemStack.isSameItemSameTags(amulet,stored),"Legacy main-hand-only transfer does not recharge an offhand wand");
        }
        TC4Revival.LOGGER.info("PASS amuletCapabilityTransfersAllPrimalsAtLegacyCadenceAndGatesResearch");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void amuletRelayRechargesAfterTransferAndRejectsStaleRelay(GameTestHelper test) {
        var level=test.getLevel();var relayPos=test.absolutePos(new BlockPos(4,4,4));var nodePos=relayPos.west(3);
        level.setBlockAndUpdate(relayPos.below(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(nodePos,TCArcane.ENERGIZED_NODE.get().defaultBlockState());
        level.setBlockAndUpdate(relayPos,TCArcane.VIS_RELAY.get().defaultBlockState());
        var node=(EnergizedNodeBlockEntity)level.getBlockEntity(nodePos);var relay=(VisRelayBlockEntity)level.getBlockEntity(relayPos);
        var aura=new AspectAmounts();for(var aspect:TCAspects.catalog().primals())aura.add(aspect,400);
        node.configure(aura,AuraNodeType.NORMAL,null,"amulet-relay");node.tick();
        var player=player(test,"AmuletRelay");player.moveTo(relayPos.getCenter().add(0,0,2));level.addNewPlayer(player);
        try {
            for(int i=0;i<40;i++)relay.tick();
            test.assertTrue(relay.root()==node && VisRelayBlockEntity.nearby(player)==relay,"Real relay topology and nearby-player lookup");
            var amulet=new ItemStack(TCItems.LESSER_VIS_AMULET.get());var item=(VisAmuletItem)amulet.getItem();
            var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,false);player.getInventory().setItem(0,wand);
            wornTick(player,amulet,5);
            for(var aspect:TCAspects.catalog().primals())test.assertTrue(item.vis(amulet,aspect)==5 && TCItems.CASTING_WAND.get().vis(wand,aspect)==0,"Empty amulet receives relay vis after, not before, the held-wand transfer");
            wornTick(player,amulet,10);
            for(var aspect:TCAspects.catalog().primals())test.assertTrue(item.vis(amulet,aspect)==5 && TCItems.CASTING_WAND.get().vis(wand,aspect)==5,"Following callback transfers five and replaces five from network");
            player.getInventory().setItem(0,ItemStack.EMPTY);
            for(var aspect:TCAspects.catalog().primals())amulet.getOrCreateTag().putInt(aspect.tag(),2498);
            wornTick(player,amulet,15);
            for(var aspect:TCAspects.catalog().primals())test.assertTrue(item.vis(amulet,aspect)==2500 && node.consumeVis(aspect,100)==8,"Room-limited recharge only drains two more from source budget of twenty");
            amulet.setTag(null);player.moveTo(relayPos.getCenter().add(6,0,0));wornTick(player,amulet,20);
            test.assertTrue(!amulet.hasTag() && VisRelayBlockEntity.nearby(player)==null,"Out-of-range cached relay is rejected");
            player.moveTo(relayPos.getCenter().add(0,0,2));for(int i=0;i<20;i++)relay.tick();
            test.assertTrue(VisRelayBlockEntity.nearby(player)==relay,"Reentering range is discovered again");
            level.setBlockAndUpdate(relayPos,Blocks.AIR.defaultBlockState());wornTick(player,amulet,25);
            test.assertTrue(!amulet.hasTag() && VisRelayBlockEntity.nearby(player)==null,"Removed relay cannot supply phantom vis");
        } finally { player.discard();level.setBlockAndUpdate(relayPos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(nodePos,Blocks.AIR.defaultBlockState()); }
        TC4Revival.LOGGER.info("PASS amuletRelayRechargesAfterTransferAndRejectsStaleRelay");test.succeed();
    }
}
