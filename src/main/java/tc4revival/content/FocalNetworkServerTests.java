package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FocalNetworkServerTests {
    @GameTest(template="wand_test_empty")
    public static void focalNetworkPaymentResumesAndCompletes(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var sourcePos=pos.east(3);
        level.setBlock(pos,TCArcane.FOCAL_BLOCK.get().defaultBlockState(),2);
        level.setBlock(sourcePos,TCArcane.ENERGIZED_NODE.get().defaultBlockState(),2);
        var tile=(FocalManipulatorBlockEntity)level.getBlockEntity(pos);
        var node=(EnergizedNodeBlockEntity)level.getBlockEntity(sourcePos);
        var aura=new AspectAmounts();for(String name:new String[]{"aer","aqua","terra","perditio"})aura.add(TCAspects.catalog().require(name),22500);
        node.configure(aura,AuraNodeType.NORMAL,null,"focal-payment");node.tick();
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FocalNetwork"));
        player.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);player.giveExperiencePoints(280);
        tile.setItem(0,new ItemStack(TCItems.EXCAVATION_FOCUS.get()));
        Runnable cleanup=()->{level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(sourcePos,Blocks.AIR.defaultBlockState(),2);};
        try {
            test.assertTrue(tile.start(player,FocusUpgrade.FRUGAL.id),"Starts the real upgrade transaction");
            ticks(tile,4);test.assertTrue(tile.remaining().total()==800,"No payment before fifth callback");
            ticks(tile,1);test.assertTrue(tile.remaining().total()==400,"Each aspect consumes at most 100 of available 150");
            ticks(tile,5);test.assertTrue(tile.remaining().total()==200,"Second callback batch uses only remaining 50 per source aspect");
            ticks(tile,20);test.assertTrue(tile.remaining().total()==200,"Repeated callbacks cannot replenish source within same world tick");
            var saved=tile.saveWithoutMetadata();tile.load(saved);
            test.assertTrue(tile.size()==800&&tile.remaining().total()==200&&tile.queue().equals(java.util.List.of(FocusUpgrade.FRUGAL.id))&&TCItems.EXCAVATION_FOCUS.get().nextRank(tile.getItem(0))==1,"Partial payment, queue and original focus persist without applying upgrade");
        } catch(Throwable failure){cleanup.run();throw failure;}
        test.runAfterDelay(2,()->{
            try {
                ticks(tile,5);
                test.assertTrue(tile.size()==0&&tile.remaining().isEmpty()&&TCItems.EXCAVATION_FOCUS.get().nextRank(tile.getItem(0))==2,"Natural world time replenishes network and completes exactly one rank");
                var completed=tile.getItem(0).copy();ticks(tile,25);
                test.assertTrue(ItemStack.matches(completed,tile.getItem(0))&&player.experienceLevel==0,"Idle callbacks neither repeat upgrade nor charge XP again");
                tc4revival.TC4Revival.LOGGER.info("PASS focalNetworkPaymentResumesAndCompletes: five-tick cap, same-tick exhaustion, partial NBT, world-time refill and single completion");
                test.succeed();
            } finally {cleanup.run();}
        });
    }
    private static void ticks(FocalManipulatorBlockEntity tile,int count){for(int i=0;i<count;i++)tile.tick();}
}
