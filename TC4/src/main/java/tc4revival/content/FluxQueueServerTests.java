package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FluxQueueServerTests {
    @GameTest(template="wand_test_empty")
    public static void deferredFluxQueuePersistsAndRespectsBudgets(GameTestHelper test) {
        var level=test.getLevel();var storage=level.getDataStorage();String key="thaumcraft_flux_spills";
        var original=storage.computeIfAbsent(FluxSpills::load,FluxSpills::new,key);
        var pos=test.absolutePos(new BlockPos(2,2,2));var unloaded=new BlockPos(29000000,80,29000000);
        test.assertTrue(!level.hasChunkAt(unloaded),"Unloaded fixture must not load a chunk");
        var data=new FluxSpills();storage.set(key,data);
        try {
            for(var p:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
            for(long amount:new long[]{-1,0,1,256})FluxSpills.empty(level,pos,amount);
            test.assertTrue(total(data)==0,"Small and negative requests leave no deferred debt");
            FluxSpills.empty(level,pos,1000);test.assertTrue(total(data)==744,"Only first 256 attempts execute immediately");
            FluxSpills.empty(level,pos,300);test.assertTrue(total(data)==788,"Same-origin remaining attempts merge");
            var saved=data.save(new CompoundTag());data=FluxSpills.load(saved);storage.set(key,data);
            test.assertTrue(saved.equals(data.save(new CompoundTag())),"Queue survives NBT round trip independently of crucible");
            tick(level,TickEvent.Phase.START);test.assertTrue(total(data)==788,"START phase does not debit queue");
            tick(level,TickEvent.Phase.END);test.assertTrue(total(data)==532,"END phase budget is 256 attempts");
            tick(level,TickEvent.Phase.END);tick(level,TickEvent.Phase.END);test.assertTrue(total(data)==20,"Debt preserved across bounded batches");
            tick(level,TickEvent.Phase.END);test.assertTrue(total(data)==0,"Final short batch removes entry");
            var pending=new ListTag();
            for(int i=0;i<65;i++)pending.add(entry(unloaded.offset(i*16,0,0),5));
            pending.add(entry(pos,300));var tag=new CompoundTag();tag.put("Pending",pending);
            data=FluxSpills.load(tag);storage.set(key,data);
            tick(level,TickEvent.Phase.END);test.assertTrue(total(data)==625,"At most 64 unloaded positions visited per tick");
            tick(level,TickEvent.Phase.END);test.assertTrue(total(data)==369,"Rotation reaches loaded entry without spending budget on unloaded entries");
            tick(level,TickEvent.Phase.END);test.assertTrue(total(data)==369,"Remaining loaded entry waits behind 64 rotated positions");
            tick(level,TickEvent.Phase.END);test.assertTrue(total(data)==325,"Loaded debt exhausted while unloaded debt retained");
            test.assertTrue(!level.hasChunkAt(unloaded),"Queue never force-loads missing chunks");
        } finally {
            storage.set(key,original);
            for(var p:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
        }
        tc4revival.TC4Revival.LOGGER.info("PASS deferredFluxQueuePersistsAndRespectsBudgets: immediate/merge/NBT/phase/budget/64-position rotation/unloaded retention");test.succeed();
    }
    private static CompoundTag entry(BlockPos pos,long amount){var tag=new CompoundTag();tag.putLong("Position",pos.asLong());tag.putLong("Attempts",amount);return tag;}
    private static long total(FluxSpills data){long total=0;for(var entry:data.save(new CompoundTag()).getList("Pending",Tag.TAG_COMPOUND))total+=((CompoundTag)entry).getLong("Attempts");return total;}
    private static void tick(net.minecraft.server.level.ServerLevel level,TickEvent.Phase phase){FluxSpills.tick(new TickEvent.LevelTickEvent(LogicalSide.SERVER,phase,level,()->true));}
}
