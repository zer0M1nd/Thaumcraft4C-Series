package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class RegenerationRestartServerTests {
    private static final String SESSION=java.util.UUID.randomUUID().toString();
    private static final ResourceLocation STORAGE=ResourceLocation.parse("thaumcraft:regeneration_validation");
    private static final ChunkPos TARGET=new ChunkPos(1200,1200);
    @GameTest(template="wand_test_empty",timeoutTicks=200)
    public static void regenerationPrepareRestart(GameTestHelper test){
        var level=test.getLevel();var chunk=level.getChunk(TARGET.x,TARGET.z);var random=new java.util.Random(ChunkRegeneration.seed(level.getSeed(),TARGET));var positions=new ListTag();
        boolean expanded=tc4revival.config.TCConfig.SERVER.branchFeatures.get();
        int minimum=level.getMinBuildHeight(),span=Math.min(level.getMaxBuildHeight(),51)-minimum;
        double expected=18d*span/51;
        int attempts=expanded?(int)Math.floor(expected)+(random.nextDouble()<expected-Math.floor(expected)?1:0):18;
        for(int i=0;i<attempts;i++){
            BlockPos pos;
            if(expanded){int column=(int)(random.nextDouble()*256);pos=new BlockPos(TARGET.getMinBlockX()+(column&15),minimum+random.nextInt(span),TARGET.getMinBlockZ()+(column>>4));}
            else pos=new BlockPos(TARGET.getMinBlockX()+random.nextInt(16),random.nextInt(51),TARGET.getMinBlockZ()+random.nextInt(16));
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);positions.add(LongTag.valueOf(pos.asLong()));
        }
        net.minecraftforge.common.ForgeConfigSpec.BooleanValue flag=tc4revival.config.TCConfig.SERVER_SPEC.getValues().get("world_regeneration.cinnibar_ore");boolean old=flag.get();
        try{flag.set(true);var wrapped=new net.minecraft.world.level.chunk.ImposterProtoChunk(chunk,false);var event=new net.minecraftforge.event.level.ChunkDataEvent.Load(wrapped,new CompoundTag(),net.minecraft.world.level.chunk.ChunkStatus.ChunkType.LEVELCHUNK);ChunkRegeneration.load(event);ChunkRegeneration.load(event);}finally{flag.set(old);}
        var pending=ChunkRegeneration.drain(level);test.assertTrue(pending.entries.containsKey(TARGET.toLong()),"Wrapped legacy chunk load reaches durable queue");
        long before=pending.entries.get(TARGET.toLong());ChunkRegeneration.process(level);test.assertTrue(pending.entries.containsKey(TARGET.toLong())&&pending.entries.get(TARGET.toLong())==before,"Missing full neighbors keep candidate pending");
        var record=new CompoundTag();record.putString("Session",SESSION);record.putBoolean("Expanded",expanded);record.put("Positions",positions);record.putLong("Seed",before);level.getServer().getCommandStorage().set(STORAGE,record);
        com.mojang.logging.LogUtils.getLogger().info("Regeneration restart prepared: chunk={}, seed={}",TARGET,before);test.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=200)
    public static void regenerationVerifyRestart(GameTestHelper test){
        var level=test.getLevel();var record=level.getServer().getCommandStorage().get(STORAGE);test.assertTrue(record.contains("Session")&&!record.getString("Session").equals(SESSION),"Verification requires a genuinely new server process");
        test.assertTrue(record.getBoolean("Expanded")==tc4revival.config.TCConfig.SERVER.branchFeatures.get(),"Generation branch must match the prepared fixture");
        var pending=ChunkRegeneration.drain(level);test.assertTrue(pending.entries.containsKey(TARGET.toLong())&&pending.entries.get(TARGET.toLong())==record.getLong("Seed"),"Pending candidate and seed survived server shutdown and disk reload");
        for(int x=TARGET.x-1;x<=TARGET.x+2;x++)for(int z=TARGET.z-1;z<=TARGET.z+2;z++)level.getChunk(x,z);
        var positions=record.getList("Positions",Tag.TAG_LONG);for(int i=0;i<positions.size();i++)test.assertTrue(level.getBlockState(BlockPos.of(((LongTag)positions.get(i)).getAsLong())).is(Blocks.STONE),"Stone fixture also survived disk reload before regeneration");
        net.minecraftforge.common.ForgeConfigSpec.BooleanValue flag=tc4revival.config.TCConfig.SERVER_SPEC.getValues().get("world_regeneration.cinnibar_ore");boolean old=flag.get();
        try{flag.set(true);ChunkRegeneration.process(level);}finally{flag.set(old);}
        test.assertTrue(!pending.entries.containsKey(TARGET.toLong()),"Ready regeneration is consumed");
        for(int i=0;i<positions.size();i++)test.assertTrue(level.getBlockState(BlockPos.of(((LongTag)positions.get(i)).getAsLong())).is(TCArcane.ore(0)),"Restarted queue executed actual deterministic cinnabar regeneration");
        ChunkRegeneration.process(level);test.assertTrue(!pending.entries.containsKey(TARGET.toLong()),"Consumed candidate is not repeated");
        level.getServer().getCommandStorage().set(STORAGE,new CompoundTag());com.mojang.logging.LogUtils.getLogger().info("Regeneration restart test passed: disk-queue-real-ore-consumption");test.succeed();
    }
}
