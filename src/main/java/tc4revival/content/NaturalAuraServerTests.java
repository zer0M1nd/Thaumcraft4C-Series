package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.util.RandomSource;
import net.minecraftforge.gametest.*;
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class NaturalAuraServerTests {
    @GameTest(template="tree_test_empty",timeoutTicks=200)
    public static void naturalMoundPlanAndLoot(GameTestHelper test) {
        var level=test.getLevel();var anchor=test.absolutePos(new BlockPos(2,0,2));var origin=new BlockPos(anchor.getX(),91,anchor.getZ());
        for(int x=0;x<=18;x++)for(int z=0;z<=18;z++)level.setBlockAndUpdate(origin.offset(x,8,z),Blocks.GRASS_BLOCK.defaultBlockState());
        test.assertTrue(LegacyMound.plan().size()==2441,"Complete ordered old mound writes are present");
        test.assertTrue(LegacyMound.plan().stream().anyMatch(w->w.state().is(Blocks.CHISELED_STONE_BRICKS)),"Carved stone brick metadata is migrated");
        var forcedTrap=new net.minecraft.world.level.levelgen.LegacyRandomSource(42){@Override public int nextInt(int bound){return bound==3?0:super.nextInt(bound);}};
        test.assertTrue(LegacyMound.generate(level,origin,forcedTrap),"Mound accepts its five terrain probes");
        test.assertTrue(level.getBlockState(origin.offset(10,1,9)).is(Blocks.TRAPPED_CHEST)&&level.getBlockState(origin.offset(10,-1,9)).is(Blocks.TNT),"Original trapped chest and buried TNT branch");
        test.assertTrue(level.getBlockEntity(origin.offset(10,1,9)) instanceof ChestBlockEntity chest&&!chest.isEmpty(),"Dungeon chest is immediately populated from old pool");
        for(int z:new int[]{7,11})test.assertTrue(level.getBlockState(origin.offset(9,1,z)).getBlock() instanceof EldritchLootBlock,"Both rarity-selected loot containers placed");
        for(int z:new int[]{4,14}) {var spawner=(SpawnerBlockEntity)level.getBlockEntity(origin.offset(4,5,z));var data=spawner.saveWithoutMetadata();String id=data.getCompound("SpawnData").getCompound("entity").getString("id");test.assertTrue(id.equals(z==4?"minecraft:skeleton":"minecraft:zombie"),"Original skeleton and zombie spawners");}
        var invalid=origin.offset(32,0,0);test.assertTrue(!LegacyMound.generate(level,invalid,RandomSource.create(1)),"Invalid terrain rejects mound before writing");pass(test,"mound-plan-loot-spawners");
    }
    @GameTest(template="tree_test_empty",timeoutTicks=200)
    public static void naturalHilltopGeometryAndChest(GameTestHelper test) {
        var level=test.getLevel();var anchor=test.absolutePos(new BlockPos(8,0,8));var ground=new BlockPos(anchor.getX(),100,anchor.getZ());
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)level.setBlockAndUpdate(ground.offset(x,-1,z),Blocks.STONE.defaultBlockState());
        test.assertTrue(!HilltopStones.generate(level,new BlockPos(ground.getX(),84,ground.getZ()),RandomSource.create(1)),"Old minimum hill elevation is 85");
        test.assertTrue(HilltopStones.generate(level,ground,RandomSource.create(27)),"Hilltop generates on level valid high terrain");
        int tiles=0;for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){var state=level.getBlockState(ground.offset(x,0,z));if(state.is(Blocks.OBSIDIAN)||state.is(TCEldritch.OBSIDIAN_TILE.get())||state.is(Blocks.SPAWNER))tiles++;}
        test.assertTrue(tiles==45,"Seven-square floor excludes four corners");
        test.assertTrue(level.getBlockEntity(ground.above(2)) instanceof ChestBlockEntity chest&&!chest.isEmpty(),"Two old dungeon fills supply the central chest");
        var spawner=(SpawnerBlockEntity)level.getBlockEntity(ground);test.assertTrue(spawner.saveWithoutMetadata().getCompound("SpawnData").getCompound("entity").getString("id").equals("thaumcraft:wisp"),"Central spawner produces TC4 wisps");
        for(int x:new int[]{-3,3})for(int z:new int[]{-1,1})test.assertTrue(ObsidianTotemBlock.isTotem(level.getBlockState(ground.offset(x,2,z))),"Outer columns have at least two totem segments");pass(test,"hilltop-geometry-chest-spawner");
    }
    @GameTest(template="wand_test_empty")
    public static void naturalAuraQueueAndWildNode(GameTestHelper test) {
        var pending=new NaturalAuraQueue.Pending();pending.entries.put(12L,34L);pending.entries.put(-56L,78L);var restored=NaturalAuraQueue.Pending.load(pending.save(new net.minecraft.nbt.CompoundTag()));test.assertTrue(restored.entries.equals(pending.entries),"Queue preserves signed chunks and deterministic seeds");
        var config=tc4revival.config.TCConfig.SERVER;int previous=config.nodeRarity.get();
        try {
            config.nodeRarity.set(1);var level=test.getLevel();var chunk=new net.minecraft.world.level.ChunkPos(test.absolutePos(new BlockPos(0,0,0)));var random=new net.minecraft.world.level.levelgen.LegacyRandomSource(91){@Override public int nextInt(int bound){return 0;}};
            test.assertTrue(!NaturalAuraStructures.wildNode(level,chunk,random,true),"Existing aura suppresses wild placement and preserves old false return");
            int x=chunk.getMinBlockX(),z=chunk.getMinBlockZ();for(int y=5;y<=10;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
            test.assertTrue(NaturalAuraStructures.wildNode(level,chunk,random,false)&&level.getBlockEntity(new BlockPos(x,6,z)) instanceof AuraNodeBlockEntity node&&node.aspects().total()>0,"Wild search starts at old Y=5 and creates real node at first air above");
            test.assertTrue(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE).containsKey(net.minecraft.resources.ResourceLocation.parse("thaumcraft:natural_aura")),"Natural dispatch feature is present in runtime registry");
        }finally{config.nodeRarity.set(previous);}pass(test,"queue-wild-node-registry");
    }
    private static void pass(GameTestHelper test,String name){com.mojang.logging.LogUtils.getLogger().info("Natural aura test passed: {}",name);test.succeed();}
}
