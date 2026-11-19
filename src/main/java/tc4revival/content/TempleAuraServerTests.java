package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class TempleAuraServerTests {
    @GameTest(template="wand_test_empty")
    public static void templeSelectionIndexAndLegacyHash(GameTestHelper test){
        for(String id:new String[]{"desert_pyramid","jungle_pyramid","swamp_hut"})test.assertTrue(TempleAura.temple(ResourceLocation.parse("minecraft:"+id)),"Original scattered structure family");
        test.assertTrue(!TempleAura.temple(ResourceLocation.parse("minecraft:igloo"))&&!TempleAura.temple(ResourceLocation.parse("minecraft:ancient_city")),"New structures do not acquire automatic node adaptation");
        var center=TempleAura.center(new BoundingBox(-10,60,-4,1,69,6));test.assertTrue(center.equals(new BlockPos(-4,65,1)),"Old inclusive-span piece center");
        test.assertTrue(TempleAura.nearest(java.util.List.of(new BlockPos(0,100,0),new BlockPos(3,0,0)),BlockPos.ZERO).orElseThrow().equals(new BlockPos(3,0,0)),"Nearest lookup includes height distance");
        test.assertTrue(TempleAura.nearest(java.util.List.of(new BlockPos(1,0,0),new BlockPos(50000,0,0)),BlockPos.ZERO).orElseThrow().equals(new BlockPos(1,0,0)),"Native double distance avoids squared-int overflow");
        test.assertTrue(TempleAura.hash(BlockPos.ZERO)==TempleAura.hash(new BlockPos(1,0,-8976890)),"Original position hash collision semantics preserved");
        var index=new TempleAura.Index();index.centers.put("minecraft:desert_pyramid@1",center);index.attempted.add(center.asLong());var nbt=index.save(new net.minecraft.nbt.CompoundTag());var restored=TempleAura.Index.load(nbt);test.assertTrue(restored.centers.equals(index.centers)&&restored.attempted.equals(index.attempted),"Structure index and attempted nodes both persist");pass(test,"selection-index-legacy-hash");
    }
    @GameTest(template="wand_test_empty")
    public static void templeChunkCaptureAndNodeHeight(GameTestHelper test){
        var level=test.getLevel();var chunk=level.getChunkAt(test.absolutePos(new BlockPos(4,4,4)));var registry=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);var structure=registry.get(ResourceLocation.parse("minecraft:desert_pyramid"));
        var piece=new net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece(RandomSource.create(3),chunk.getPos().getMinBlockX(),chunk.getPos().getMinBlockZ());
        var start=new StructureStart(structure,chunk.getPos(),0,new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(piece)));
        var priorStarts=new java.util.HashMap<>(chunk.getAllStarts());var index=TempleAura.index(level);var priorCenters=new java.util.LinkedHashMap<>(index.centers);index.centers.clear();
        var center=TempleAura.center(piece.getBoundingBox());var ground=new BlockPos(center.getX(),80,center.getZ());var target=ground.above(4);var oldGround=level.getBlockState(ground);var oldTarget=level.getBlockState(target);
        try {
            chunk.setStartForStructure(structure,start);TempleAura.chunkLoaded(new net.minecraftforge.event.level.ChunkEvent.Load(chunk,false));index=TempleAura.index(level);
            test.assertTrue(index.centers.size()==1&&index.centers.containsValue(center),"Real structure start and chunk-load handler populate temple index");
            level.setBlockAndUpdate(ground,Blocks.STONE.defaultBlockState());test.assertTrue(LegacySurfaceHeight.at(level,ground.getX(),ground.getZ())==81,"Opaque ground supplies old surface height");
            test.assertTrue(TempleAura.generate(level,chunk.getPos(),RandomSource.create(31))&&level.getBlockEntity(target) instanceof AuraNodeBlockEntity node&&node.aspects().total()>0,"Temple node generated three blocks above old height value");
            test.assertTrue(LegacySurfaceHeight.at(level,ground.getX(),ground.getZ())==81,"Transparent node does not raise legacy surface on later attempts");
            test.assertTrue(!TempleAura.generate(level,chunk.getPos(),RandomSource.create(32)),"Same structure center is attempted once per running server");
        }finally{chunk.setAllStarts(priorStarts);index.centers.clear();index.centers.putAll(priorCenters);index.setDirty();level.setBlockAndUpdate(target,oldTarget);level.setBlockAndUpdate(ground,oldGround);}
        pass(test,"chunk-capture-node-height-dedup");
    }
    private static void pass(GameTestHelper test,String name){com.mojang.logging.LogUtils.getLogger().info("Temple aura test passed: {}",name);test.succeed();}
}
