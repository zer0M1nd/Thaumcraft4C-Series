package tc4revival.content;

import java.nio.file.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class TempleAuraMigrationServerTests {
    private static  class Worker extends IOWorker {
        Worker(Path path) { super(path, false, "temple-migration-test"); }
    }
    private static CompoundTag chunk(String id, ChunkPos pos) {
        var piece=new CompoundTag();piece.putIntArray("BB",new int[]{-10,60,-4,1,69,6});
        var pieces=new ListTag();pieces.add(piece);
        var start=new CompoundTag();start.putString("id",id);start.putInt("ChunkX",pos.x);start.putInt("ChunkZ",pos.z);start.put("Children",pieces);
        var starts=new CompoundTag();starts.put(id,start);var structures=new CompoundTag();structures.put("starts",starts);
        var root=new CompoundTag();root.put("structures",structures);return root;
    }
    @GameTest(template="wand_test_empty", timeoutTicks=400)
    public static void templeMigrationRegionRoundTrip(GameTestHelper test) throws Exception {
        var folder=Files.createTempDirectory("tc4-temple-index-");
        try(var writer=new Worker(folder)) {
            writer.store(new ChunkPos(-40,65),chunk("minecraft:desert_pyramid",new ChunkPos(-40,65))).join();
            writer.store(new ChunkPos(40,65),chunk("minecraft:swamp_hut",new ChunkPos(40,65))).join();
            writer.store(new ChunkPos(41,65),chunk("minecraft:igloo",new ChunkPos(41,65))).join();
            writer.synchronize(true).join();
        }
        var reader=new Worker(folder);var index=new TempleAura.Index();var scan=new TempleAuraMigration.Scan(folder,reader::loadAsync);
        test.runAtTickTime(399,()->{try{reader.close();}catch(Exception ignored){}});
        test.succeedWhen(()->{
            scan.step(index);test.assertTrue(!scan.failed,"Region scan must not fail");test.assertTrue(scan.done,"Waiting for asynchronous region reads");
            test.assertTrue(scan.read==3&&index.centers.size()==2,"Read positive and negative region coordinates; exclude igloo");
            test.assertTrue(index.centers.containsKey("minecraft:desert_pyramid@"+ChunkPos.asLong(-40,65)),"Negative region slot is mapped correctly");
            test.assertTrue(index.centers.values().stream().allMatch(new BlockPos(-4,65,1)::equals),"First piece supplies inclusive center");
            var restored=TempleAura.Index.load(index.save(new CompoundTag()));test.assertTrue(restored.diskScanComplete&&restored.centers.equals(index.centers),"Completion and index survive save/load");
            try{reader.close();}catch(Exception error){throw new RuntimeException(error);}
            com.mojang.logging.LogUtils.getLogger().info("Temple migration test passed: real-region-round-trip");
        });
    }
    @GameTest(template="wand_test_empty")
    public static void templeMigrationNewEmptyRegion(GameTestHelper test) throws Exception {
        var folder=Files.createTempDirectory("tc4-empty-region-");var file=folder.resolve("r.0.0.mca");Files.createFile(file);
        var index=new TempleAura.Index();var scan=new TempleAuraMigration.Scan(folder,p->{throw new AssertionError("Empty region has no saved chunks");});scan.step(index);
        test.assertTrue(scan.done&&!scan.failed&&index.diskScanComplete,"Newly created zero-byte region does not block natural aura");
        Files.write(file,new byte[]{0,0,1});boolean rejected=false;
        try{TempleAuraMigration.positions(file);}catch(java.io.EOFException expected){rejected=true;}
        test.assertTrue(rejected,"Nonempty truncated headers remain corruption errors");
        com.mojang.logging.LogUtils.getLogger().info("Temple migration test passed: new-empty-region-truncation");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void templeMigrationLiveObservationWins(GameTestHelper test) throws Exception {
        var index=new TempleAura.Index();var pos=new ChunkPos(2,3);var key="minecraft:swamp_hut@"+pos.toLong();
        index.centers.put(key,BlockPos.ZERO);TempleAuraMigration.accept(chunk("minecraft:swamp_hut",pos),index);
        test.assertTrue(index.centers.get(key).equals(new BlockPos(-4,65,1)),"Disk data refreshes unobserved cache");
        index.observed.add(key);index.centers.put(key,BlockPos.ZERO);TempleAuraMigration.accept(chunk("minecraft:swamp_hut",pos),index);
        test.assertTrue(index.centers.get(key).equals(BlockPos.ZERO),"Live observation wins over stale disk data");
        var scan=new TempleAuraMigration.Scan(Files.createTempDirectory("tc4-temple-empty-"),p->{throw new AssertionError("Empty folder must not read chunks");});scan.step(index);
        test.assertTrue(scan.done&&index.diskScanComplete,"Empty save completes without generating chunks");
        com.mojang.logging.LogUtils.getLogger().info("Temple migration test passed: live-observation-empty-save");test.succeed();
    }
}
